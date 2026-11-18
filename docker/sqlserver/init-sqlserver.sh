#!/usr/bin/env bash
set -euo pipefail

: "${SQLSERVER_SA_USER:?SQLSERVER_SA_USER is required}"
: "${SQLSERVER_SA_PASSWORD:?SQLSERVER_SA_PASSWORD is required}"
: "${ENROLLMENT_DB:?ENROLLMENT_DB is required}"
: "${SQLSERVER_USER:?SQLSERVER_USER is required}"
: "${SQLSERVER_PASSWORD:?SQLSERVER_PASSWORD is required}"

sqlcmd=(/opt/mssql-tools18/bin/sqlcmd
  -S enrollment-db,1433
  -U "$SQLSERVER_SA_USER"
  -P "$SQLSERVER_SA_PASSWORD"
  -C
  -b
  -l 30
)

escape_sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

database_name="$(escape_sql_literal "$ENROLLMENT_DB")"
app_user="$(escape_sql_literal "$SQLSERVER_USER")"
app_password="$(escape_sql_literal "$SQLSERVER_PASSWORD")"

"${sqlcmd[@]}" -d master <<SQL
SET NOCOUNT ON;

DECLARE @database_name sysname = N'$database_name';
IF DB_ID(@database_name) IS NULL
BEGIN
    DECLARE @create_database nvarchar(max) =
        N'CREATE DATABASE ' + QUOTENAME(@database_name);
    EXEC sys.sp_executesql @create_database;
END;

DECLARE @login_name sysname = N'$app_user';
DECLARE @login_password nvarchar(128) = N'$app_password';
DECLARE @login_statement nvarchar(max);

IF SUSER_ID(@login_name) IS NULL
BEGIN
    SET @login_statement =
        N'CREATE LOGIN ' + QUOTENAME(@login_name) +
        N' WITH PASSWORD = ' + QUOTENAME(@login_password, '''') +
        N', CHECK_POLICY = ON';
END
ELSE
BEGIN
    SET @login_statement =
        N'ALTER LOGIN ' + QUOTENAME(@login_name) +
        N' WITH PASSWORD = ' + QUOTENAME(@login_password, '''') +
        N', CHECK_POLICY = ON';
END;

EXEC sys.sp_executesql @login_statement;
SQL

"${sqlcmd[@]}" -d "$ENROLLMENT_DB" <<SQL
SET NOCOUNT ON;

DECLARE @login_name sysname = N'$app_user';
DECLARE @user_statement nvarchar(max);

IF DATABASE_PRINCIPAL_ID(@login_name) IS NULL
BEGIN
    SET @user_statement =
        N'CREATE USER ' + QUOTENAME(@login_name) +
        N' FOR LOGIN ' + QUOTENAME(@login_name);
END
ELSE
BEGIN
    SET @user_statement =
        N'ALTER USER ' + QUOTENAME(@login_name) +
        N' WITH LOGIN = ' + QUOTENAME(@login_name);
END;

EXEC sys.sp_executesql @user_statement;

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members AS role_members
    JOIN sys.database_principals AS role_principal
      ON role_principal.principal_id = role_members.role_principal_id
    JOIN sys.database_principals AS member_principal
      ON member_principal.principal_id = role_members.member_principal_id
    WHERE role_principal.name = N'db_owner'
      AND member_principal.name = @login_name
)
BEGIN
    SET @user_statement =
        N'ALTER ROLE [db_owner] ADD MEMBER ' + QUOTENAME(@login_name);
    EXEC sys.sp_executesql @user_statement;
END;
SQL

echo "SQL Server database and application login are ready."
