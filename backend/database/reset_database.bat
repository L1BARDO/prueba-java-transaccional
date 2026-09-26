@echo off
rem =====================================================================================
rem  reset_database.bat
rem  Elimina y vuelve a crear la base de datos del Switch Transaccional, ejecutando en
rem  orden: estructura, indices, funciones, vistas y datos de prueba.
rem
rem  Uso:
rem      reset_database.bat              (estructura + datos de prueba)
rem      reset_database.bat --sin-datos  (solo estructura)
rem
rem  Configuracion: edite los valores por defecto de abajo o defina las variables de
rem  entorno DB_HOST, DB_PORT, DB_NAME, DB_USER y DB_PASSWORD antes de ejecutar.
rem  Requiere psql (cliente de PostgreSQL) en el PATH.
rem =====================================================================================
setlocal EnableExtensions
chcp 65001 > nul

if not defined DB_HOST     set "DB_HOST=localhost"
if not defined DB_PORT     set "DB_PORT=5432"
if not defined DB_NAME     set "DB_NAME=switch_transaccional"
if not defined DB_USER     set "DB_USER=postgres"
if not defined DB_PASSWORD set "DB_PASSWORD=postgres"

set "PGPASSWORD=%DB_PASSWORD%"
set "PGCLIENTENCODING=UTF8"
set "PGOPTIONS=-c client_min_messages=warning"
set "SCRIPT_DIR=%~dp0"
set "PSQL=psql -X -q -v ON_ERROR_STOP=1 -h %DB_HOST% -p %DB_PORT% -U %DB_USER%"

set "FILES=01_creates.sql 02_indices.sql 03_funciones.sql 04_vistas.sql 05_inserts.sql"
if /I "%~1"=="--sin-datos" set "FILES=01_creates.sql 02_indices.sql 03_funciones.sql 04_vistas.sql"

where psql > nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se encontro psql. Agregue la carpeta bin de PostgreSQL al PATH,
    echo         por ejemplo: C:\Program Files\PostgreSQL\16\bin
    exit /b 1
)

echo =====================================================================
echo  Reinicio de base de datos: %DB_NAME%  (%DB_USER%@%DB_HOST%:%DB_PORT%)
echo  ATENCION: se eliminaran TODOS los datos existentes.
echo =====================================================================

echo [1] 00_drop_create_database.sql
%PSQL% -d postgres -v db_name=%DB_NAME% -f "%SCRIPT_DIR%00_drop_create_database.sql"
if errorlevel 1 goto :error

set /a STEP=2
for %%F in (%FILES%) do (
    call :run "%%F"
    if errorlevel 1 goto :error
)

call :run "99_verificacion.sql"
if errorlevel 1 goto :error

echo.
echo [OK] Base de datos %DB_NAME% creada correctamente.
endlocal
exit /b 0

:run
echo [%STEP%] %~1
%PSQL% -d %DB_NAME% -f "%SCRIPT_DIR%%~1"
if errorlevel 1 exit /b 1
set /a STEP+=1
exit /b 0

:error
echo.
echo [ERROR] Fallo la ejecucion de los scripts. Revise el mensaje anterior.
endlocal
exit /b 1
