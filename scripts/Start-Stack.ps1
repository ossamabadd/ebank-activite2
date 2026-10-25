param([switch]$SkipBuild, [switch]$NoChatbot, [switch]$BillingOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runtimePath = Join-Path $projectRoot '.runtime'
$statePath = Join-Path $runtimePath 'processes.json'
if (Test-Path -LiteralPath $statePath) { throw 'Run scripts/Stop-Stack.ps1 before starting another stack.' }
New-Item -ItemType Directory -Path $runtimePath -Force | Out-Null
if (-not $SkipBuild) {
    Push-Location $projectRoot
    try {
        & (Join-Path $projectRoot 'mvnw.cmd') -B -ntp verify
        if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed.' }
    } finally { Pop-Location }
}
$javaCommand = (Get-Command java -ErrorAction Stop).Source
function Read-Port([string]$Variable, [int]$Default) {
    $value = [Environment]::GetEnvironmentVariable($Variable)
    if ($value) { return [int]$value }
    return $Default
}
$services = @(
    @{ Name = 'discovery-service'; Port = (Read-Port 'DISCOVERY_PORT' 8761) },
    @{ Name = 'config-service'; Port = (Read-Port 'CONFIG_PORT' 8888) },
    @{ Name = 'customer-service'; Port = (Read-Port 'CUSTOMER_PORT' 8081) },
    @{ Name = 'inventory-service'; Port = (Read-Port 'INVENTORY_PORT' 8084) },
    @{ Name = 'billing-service'; Port = (Read-Port 'BILLING_PORT' 8085) }
)
if (-not $BillingOnly) {
    $services += @{ Name = 'ebank-service'; Port = (Read-Port 'BANK_PORT' 8082) }
    if (-not $NoChatbot) { $services += @{ Name = 'chatbot-service'; Port = (Read-Port 'CHATBOT_PORT' 8083) } }
}
$services += @{ Name = 'gateway-service'; Port = (Read-Port 'GATEWAY_PORT' 8080) }
foreach ($service in $services) {
    $socket = [Net.Sockets.TcpClient]::new()
    try {
        if ($socket.ConnectAsync('127.0.0.1', $service.Port).Wait(300) -and $socket.Connected) {
            throw "Port $($service.Port) is already in use."
        }
    } catch [AggregateException] { } finally { $socket.Dispose() }
    $jarPath = Join-Path $projectRoot "$($service.Name)/target/$($service.Name)-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $jarPath)) { throw "Missing $jarPath. Build the project first." }
}
$started = @()
try {
    foreach ($service in $services) {
        $jarPath = Join-Path $projectRoot "$($service.Name)/target/$($service.Name)-0.0.1-SNAPSHOT.jar"
        $process = Start-Process -FilePath $javaCommand -ArgumentList @('-Xms64m', '-Xmx256m', '-jar', ('"' + $jarPath + '"')) -WindowStyle Hidden -PassThru -WorkingDirectory $projectRoot -RedirectStandardOutput (Join-Path $runtimePath "$($service.Name).out.log") -RedirectStandardError (Join-Path $runtimePath "$($service.Name).err.log")
        $started += @{ Name = $service.Name; ProcessId = $process.Id; Jar = $jarPath; StartedAt = $process.StartTime.ToUniversalTime().ToString('o') }
        ConvertTo-Json -InputObject @($started) | Set-Content -LiteralPath $statePath -Encoding UTF8
        $ready = $false
        $deadline = [DateTime]::UtcNow.AddSeconds(120)
        while ([DateTime]::UtcNow -lt $deadline) {
            $process.Refresh()
            if ($process.HasExited) { throw "$($service.Name) exited. Inspect .runtime logs." }
            try {
                $health = Invoke-RestMethod -Uri "http://127.0.0.1:$($service.Port)/actuator/health" -TimeoutSec 3
                if ($health.status -eq 'UP') { $ready = $true; break }
            } catch { }
            Start-Sleep -Seconds 1
        }
        if (-not $ready) { throw "$($service.Name) did not become ready. Inspect .runtime logs." }
        Write-Host "$($service.Name) ready on port $($service.Port)"
    }
    Write-Host 'Stack ready. Eureka routes may need a few seconds to appear.'
} catch {
    $failure = $_
    & (Join-Path $PSScriptRoot 'Stop-Stack.ps1')
    throw $failure
}
