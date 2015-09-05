create table users (
    id SERIAL NOT NULL PRIMARY KEY,
    name varchar(100) NOT NULL,
    message_datetime timestamp
);