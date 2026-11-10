create table email_template (
    id uuid not null,
    name varchar(100) not null,
    subject varchar(255) not null,
    body text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null,
    constraint pk_email_template primary key (id)
);