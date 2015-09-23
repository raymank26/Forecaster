alter table users
    add column first_name varchar(100) not null unique default '',
    alter column username drop not null;