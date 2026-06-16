@echo off
set PATH=C:\Data\TYPDM\PostgreSQL\12\bin;%PATH%
set PGPASSWORD=Abc@12345

echo Creating database astraion...

psql -U postgres -h localhost -c "CREATE DATABASE astraion WITH ENCODING 'UTF8';" 2>&1
if %errorlevel% equ 0 (
    echo SUCCESS: Database astraion created!
) else (
    echo Trying with tyadmin user...
    psql -U tyadmin -h localhost -d tyadmin -c "CREATE DATABASE astraion WITH ENCODING 'UTF8';" 2>&1
)

echo.
echo Done.
pause
