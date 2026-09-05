#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  --set=app_password="$APP_DB_PASSWORD" <<'SQL'
CREATE ROLE movie_app LOGIN PASSWORD :'app_password';
CREATE DATABASE movie_booking OWNER movie_app;
REVOKE CONNECT ON DATABASE movie_booking FROM PUBLIC;
GRANT CONNECT ON DATABASE movie_booking TO movie_app;
SQL

psql --single-transaction -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname movie_booking <<'SQL'
SET ROLE movie_app;
\i /opt/movie-booking/schema.sql
SQL
