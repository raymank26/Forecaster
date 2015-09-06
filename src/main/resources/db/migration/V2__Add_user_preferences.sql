create table preferences (
    id SERIAL NOT NULL PRIMARY KEY,
    message_datetime timestamp NOT NULL,
    user_id integer NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    FOREIGN KEY (user_id) references users(id)
);

alter table users
    drop message_datetime;
