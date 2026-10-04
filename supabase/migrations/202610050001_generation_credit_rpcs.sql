-- Atomic credit reservation/refund for server-side generation.
-- Apply once after 202610040001_public_backend.sql.
create or replace function public.reserve_generation(
  p_user_id uuid,
  p_kind text,
  p_model_id text,
  p_prompt text,
  p_credits integer,
  p_idempotency_key text
)
returns table(job_id uuid, remaining_credits bigint)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_balance bigint;
  v_job_id uuid;
  v_existing public.generation_jobs%rowtype;
begin
  if coalesce(current_setting('request.jwt.claim.role', true), '') <> 'service_role' then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  if p_kind not in ('image','video') or p_credits < 1 or p_credits > 100000 then
    raise exception 'invalid_request' using errcode = '22023';
  end if;
  if length(trim(p_prompt)) < 3 or length(p_prompt) > 2000 then
    raise exception 'invalid_prompt' using errcode = '22023';
  end if;
  if length(p_idempotency_key) < 8 or length(p_idempotency_key) > 128 then
    raise exception 'invalid_idempotency_key' using errcode = '22023';
  end if;

  -- Serialize reservations per user so two simultaneous requests cannot overspend.
  perform 1 from public.profiles where id = p_user_id and status = 'active' for update;
  if not found then
    raise exception 'account_unavailable' using errcode = '42501';
  end if;

  select j.id into v_job_id
  from public.generation_jobs j
  where j.user_id = p_user_id and j.idempotency_key = p_idempotency_key;
  if v_job_id is not null then
    select coalesce(sum(l.amount),0) into v_balance
    from public.credit_ledger l where l.user_id = p_user_id;
    return query select v_job_id, v_balance;
    return;
  end if;

  select coalesce(sum(l.amount),0) into v_balance
  from public.credit_ledger l where l.user_id = p_user_id;
  if v_balance < p_credits then
    raise exception 'insufficient_credits' using errcode = 'P0001';
  end if;

  insert into public.generation_jobs(user_id, kind, model_id, prompt, status, reserved_credits, idempotency_key)
  values (p_user_id, p_kind, p_model_id, trim(p_prompt), 'processing', p_credits, p_idempotency_key)
  returning id into v_job_id;

  insert into public.credit_ledger(user_id, amount, entry_type, reference_id, idempotency_key, note)
  values (p_user_id, -p_credits, 'generation_reserve', v_job_id, 'reserve:' || v_job_id::text, 'Generation credit reservation');

  select coalesce(sum(l.amount),0) into v_balance
  from public.credit_ledger l where l.user_id = p_user_id;
  return query select v_job_id, v_balance;
end;
$$;

create or replace function public.finish_generation(
  p_user_id uuid,
  p_job_id uuid,
  p_success boolean,
  p_media_url text default null,
  p_safe_error_code text default null
)
returns table(final_status text, remaining_credits bigint)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_job public.generation_jobs%rowtype;
  v_balance bigint;
begin
  if coalesce(current_setting('request.jwt.claim.role', true), '') <> 'service_role' then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  select * into v_job from public.generation_jobs
  where id = p_job_id and user_id = p_user_id for update;
  if not found then raise exception 'job_not_found' using errcode = 'P0002'; end if;

  if v_job.status in ('succeeded','failed','cancelled') then
    select coalesce(sum(l.amount),0) into v_balance from public.credit_ledger l where l.user_id = p_user_id;
    return query select v_job.status, v_balance;
    return;
  end if;

  if p_success then
    update public.generation_jobs set status='succeeded', settled_credits=reserved_credits,
      media_url=p_media_url, safe_error_code=null, updated_at=now() where id=p_job_id;
  else
    update public.generation_jobs set status='failed', settled_credits=0,
      safe_error_code=coalesce(nullif(p_safe_error_code,''),'provider_failed'), updated_at=now()
      where id=p_job_id;
    insert into public.credit_ledger(user_id, amount, entry_type, reference_id, idempotency_key, note)
    values (p_user_id, v_job.reserved_credits, 'generation_refund', p_job_id,
      'refund:' || p_job_id::text, 'Refund for failed generation')
    on conflict (user_id, idempotency_key) do nothing;
  end if;
  select coalesce(sum(l.amount),0) into v_balance from public.credit_ledger l where l.user_id = p_user_id;
  return query select case when p_success then 'succeeded' else 'failed' end, v_balance;
end;
$$;

revoke all on function public.reserve_generation(uuid,text,text,text,integer,text) from public, anon, authenticated;
revoke all on function public.finish_generation(uuid,uuid,boolean,text,text) from public, anon, authenticated;
grant execute on function public.reserve_generation(uuid,text,text,text,integer,text) to service_role;
grant execute on function public.finish_generation(uuid,uuid,boolean,text,text) to service_role;
