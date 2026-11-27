-- Run interactively in psql as a PostgreSQL administrator, not as an app migration.
CREATE ROLE bookeat LOGIN;
-- Set a private password interactively: \password bookeat
CREATE DATABASE bookeat OWNER bookeat ENCODING 'UTF8';
-- On an existing database, inspect ownership and grants instead of recreating it.
