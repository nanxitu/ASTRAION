@echo off
set PATH=C:\Data\TYPDM\PostgreSQL\12\bin;%PATH%
set PGPASSWORD=***
psql -U postgres -h localhost -l 2>&1 | findstr astraion
if %errorlevel% equ 0 (echo ASTRAION DB EXISTS) else (echo NOT FOUND)
pause
