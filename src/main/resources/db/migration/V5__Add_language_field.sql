create type lang as enum ('ru', 'en');

alter table preferences
    add column language lang not null default 'en',
    add column webcams_ids varchar[] not null default '{}';

