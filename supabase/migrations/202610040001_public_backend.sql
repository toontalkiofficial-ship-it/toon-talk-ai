-- Toon Talk AI Supabase schema (initial public-backend foundation)
-- Apply with Supabase CLI or SQL Editor. Never expose the service-role key in the Android app.
create extension if not exists pgcrypto;
create table if not exists public.profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 display_name text not null default '',
 status text not null default 'active' check (status in ('active','suspended','deletion_pending')),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table if not exists public.credit_ledger (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 amount integer not null check (amount <> 0),
 entry_type text not null check (entry_type in ('welcome_grant','purchase','generation_reserve','generation_refund','generation_settlement','admin_adjustment')),
 reference_id uuid, idempotency_key text, note text not null default '', created_at timestamptz not null default now(),
 unique (user_id, idempotency_key)
);
create index if not exists credit_ledger_user_created_idx on public.credit_ledger(user_id, created_at desc);
create table if not exists public.generation_jobs (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 kind text not null check (kind in ('image','video')),
 model_id text not null, prompt text not null check (char_length(prompt) between 1 and 2000),
 status text not null default 'queued' check (status in ('queued','processing','succeeded','failed','cancelled')),
 reserved_credits integer not null check (reserved_credits >= 0), settled_credits integer,
 provider_job_id text, media_url text, safe_error_code text, idempotency_key text not null,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 unique (user_id, idempotency_key)
);
create index if not exists generation_jobs_user_created_idx on public.generation_jobs(user_id, created_at desc);
alter table public.profiles enable row level security;
alter table public.credit_ledger enable row level security;
alter table public.generation_jobs enable row level security;
drop policy if exists "Users can read own profile" on public.profiles;
create policy "Users can read own profile" on public.profiles for select to authenticated using (auth.uid() = id);
drop policy if exists "Users can update own display name" on public.profiles;
create policy "Users can update own display name" on public.profiles for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);
drop policy if exists "Users can read own credit ledger" on public.credit_ledger;
create policy "Users can read own credit ledger" on public.credit_ledger for select to authenticated using (auth.uid() = user_id);
drop policy if exists "Users can read own generation jobs" on public.generation_jobs;
create policy "Users can read own generation jobs" on public.generation_jobs for select to authenticated using (auth.uid() = user_id);
-- No client write policies for financial/job records: trusted server-side code only.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
 insert into public.profiles (id, display_name)
 values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', ''))
 on conflict (id) do nothing;
 return new;
end;
$$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users for each row execute procedure public.handle_new_user();
create or replace function public.get_my_credit_balance()
returns bigint language sql stable security invoker set search_path = ''
as $$
 select coalesce(sum(amount), 0) from public.credit_ledger where user_id = auth.uid();
$$;
grant execute on function public.get_my_credit_balance() to authenticated;
