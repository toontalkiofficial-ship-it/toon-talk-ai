-- Basic server-side abuse protection for public generation.
-- Apply after 202610050001_generation_credit_rpcs.sql.
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
  v_recent_count integer;
  v_active_count integer;
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

  perform 1 from public.profiles where id = p_user_id and status = 'active' for update;
  if not found then
    raise exception 'account_unavailable' using errcode = '42501';
  end if;

  select j.id into v_job_id
  from public.generation_jobs j
  where j.user_id = p_user_id and j.idempotency_key = p_idempotency_key;
  if v_job_id is not null then
    raise exception 'duplicate_request' using errcode = '23505';
  end if;

  -- Prevent a single account from consuming the shared provider budget too quickly.
  select count(*) into v_recent_count
  from public.generation_jobs
  where user_id = p_user_id
    and created_at > now() - interval '1 hour';
  if v_recent_count >= 20 then
    raise exception 'rate_limited_hour' using errcode = 'P0003';
  end if;

  -- At most two in-flight provider requests per account.
  select count(*) into v_active_count
  from public.generation_jobs
  where user_id = p_user_id
    and status in ('queued','processing');
  if v_active_count >= 2 then
    raise exception 'generation_concurrency_limit' using errcode = 'P0004';
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

revoke all on function public.reserve_generation(uuid,text,text,text,integer,text) from public, anon, authenticated;
grant execute on function public.reserve_generation(uuid,text,text,text,integer,text) to service_role;
