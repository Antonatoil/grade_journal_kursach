create table if not exists oauth_account (
    oauth_account_id bigserial primary key,
    user_account_id integer not null references user_account(user_account_id) on delete cascade,
    provider varchar(30) not null,
    provider_user_id varchar(150) not null,
    provider_email varchar(150),
    created_at timestamptz not null default now(),
    unique (provider, provider_user_id),
    unique (user_account_id, provider)
);

create index if not exists idx_oauth_account_user on oauth_account(user_account_id);

alter table user_account
    add column if not exists github_username varchar(100);