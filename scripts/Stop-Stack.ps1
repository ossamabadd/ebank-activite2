$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$statePath = Join-Path $projectRoot '.runtime/processes.json'
if (-not (Test-Path -LiteralPath $statePath)) { Write-Host 'No managed stack is running.'; return }
$services = @(Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json)
$unresolved = @()
[Array]::Reverse($services)
foreach ($service in $services) {
    $process = Get-Process -Id $service.ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) { continue }
    $jarPath = [IO.Path]::GetFullPath($service.Jar)
    $expectedJar = Join-Path $projectRoot "$($service.Name)/target/$($service.Name)-0.0.1-SNAPSHOT.jar"
    $sameStart = [Math]::Abs(($process.StartTime.ToUniversalTime() - ([DateTime]$service.StartedAt).ToUniversalTime()).TotalSeconds) -lt 2
    $command = (Get-CimInstance Win32_Process -Filter "ProcessId = $($service.ProcessId)").CommandLine
    if ($sameStart -and $jarPath -eq $expectedJar -and $process.ProcessName -eq 'java' -and $command -and $command.Contains($jarPath)) {
        Stop-Process -Id $service.ProcessId -Force
        Write-Host "$($service.Name) stopped"
    } else {
        Write-Warning "Process $($service.ProcessId) no longer matches the managed service; it was not stopped."
        $unresolved += $service
    }
}
if ($unresolved.Count -eq 0) { Remove-Item -LiteralPath $statePath }
else { ConvertTo-Json -InputObject @($unresolved) | Set-Content -LiteralPath $statePath -Encoding UTF8 }
