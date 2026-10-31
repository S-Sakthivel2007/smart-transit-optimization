$db = 'C:\Users\sakth\.gemini\antigravity\scratch\smart-transit-optimization\vetrinagar_transit.db'
if (Test-Path $db) {
    # Update B01 capacity to 30 in database
    $conn = New-Object System.Data.SQLite.SQLiteConnection("Data Source=$db;Version=3;")
    $conn.Open()
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = "UPDATE buses SET capacity = 30 WHERE bus_id = 'B01';"
    $cmd.ExecuteNonQuery()
    $conn.Close()
    Write-Host "Updated SQLite B01 capacity to 30."
}
