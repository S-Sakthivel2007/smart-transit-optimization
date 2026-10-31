@echo off
echo ==============================================================
echo Importing Vetrinagar Transit Database into XAMPP MySQL...
echo ==============================================================
"C:\xampp\mysql\bin\mysql.exe" -u root < "%~dp0vetrinagar_transit.sql"
if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: Database 'vetrinagar_transit' imported into phpMyAdmin!
    echo.
    "C:\xampp\mysql\bin\mysql.exe" -u root -e "SHOW TABLES FROM vetrinagar_transit;"
) else (
    echo.
    echo ERROR: Could not import. Make sure XAMPP MySQL is running.
)
echo ==============================================================
pause
