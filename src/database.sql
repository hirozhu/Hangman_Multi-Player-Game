DROP DATABASE if exists hangman;
CREATE DATABASE hangman;
USE hangman;

CREATE TABLE user(
username varchar(50) not null,
password varchar(50) not null,
wins int(11) not null,
losses int(11) not null
);