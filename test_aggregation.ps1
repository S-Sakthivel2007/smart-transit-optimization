$body1 = @{ passengerName = 'Ravi'; pickupStopId = 'S01'; destinationStopId = 'S02'; passengerCount = 5 } | ConvertTo-Json
$res1 = Invoke-RestMethod -Uri 'http://localhost:8080/api/passenger/request' -Method Post -Body $body1 -ContentType 'application/json'
Write-Host "Passenger 1 (5 pax) Response:" ($res1 | ConvertTo-Json -Compress)

$body2 = @{ passengerName = 'Priya'; pickupStopId = 'S01'; destinationStopId = 'S02'; passengerCount = 20 } | ConvertTo-Json
$res2 = Invoke-RestMethod -Uri 'http://localhost:8080/api/passenger/request' -Method Post -Body $body2 -ContentType 'application/json'
Write-Host "Passenger 2 (20 pax) Response:" ($res2 | ConvertTo-Json -Compress)

$status = Invoke-RestMethod -Uri 'http://localhost:8080/api/status'
$batch = $status.rankedDemand | Where-Object { $_.id -eq $res1.batchId }
Write-Host "Aggregated Batch in PriorityQueue:" ($batch | ConvertTo-Json -Compress)

$stopCrowd = $status.stopCrowds | Where-Object { $_.id -eq 'S01' }
Write-Host "Stop S01 Consolidated Crowd:" ($stopCrowd | ConvertTo-Json -Compress)
