Write-Host "=== Step 1: Clear Queue and Reset All ==="
$resetRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/supervisor/reset-all' -Method Post
Write-Host "Reset Response:" ($resetRes | ConvertTo-Json -Compress)

$status1 = Invoke-RestMethod -Uri 'http://localhost:8080/api/status'
Write-Host "Waiting Queue Count after reset:" $status1.rankedDemand.Count
$b01 = $status1.buses | Where-Object { $_.id -eq 'B01' }
Write-Host "Bus B01 Initial State: Capacity =" $b01.capacity ", Load =" $b01.load ", Remaining Seats =" $b01.remaining

Write-Host "`n=== Step 2: Passenger 1 Demands 10 Seats at Depot (D00 -> S01) ==="
$req1 = @{ passengerName = "Commuter 1"; pickupStopId = "D00"; destinationStopId = "S01"; passengerCount = 10 } | ConvertTo-Json
$res1 = Invoke-RestMethod -Uri 'http://localhost:8080/api/passenger/request' -Method Post -Body $req1 -ContentType 'application/json'
Write-Host "Passenger 1 Booking Response:" ($res1 | ConvertTo-Json -Compress)

Write-Host "`n=== Step 3: Allocate Relief Bus ==="
$allocRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/supervisor/allocate' -Method Post
Write-Host "Allocation Response:" ($allocRes | ConvertTo-Json -Compress)

Write-Host "`n=== Step 4: Verify Dynamic Capacity Reduction (30 -> 20) ==="
$status2 = Invoke-RestMethod -Uri 'http://localhost:8080/api/status'
$b01Updated = $status2.buses | Where-Object { $_.id -eq 'B01' }
Write-Host "Bus B01 Dynamic State: Max Capacity =" $b01Updated.capacity ", Onboard Load =" $b01Updated.load ", Remaining Available Seats =" $b01Updated.remaining

$trip = $status2.trips | Where-Object { $_.busId -eq 'B01' }
Write-Host "Dispatched Trip:" ($trip | ConvertTo-Json -Compress)
