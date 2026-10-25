param([int]$Port = 8090)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runtimePath = Join-Path $projectRoot '.runtime'
$jarPath = Join-Path $projectRoot 'gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Build gateway-service first.' }
$socket = [Net.Sockets.TcpClient]::new()
try {
    if ($socket.ConnectAsync('127.0.0.1',$Port).Wait(300) -and $socket.Connected) { throw "Port $Port is already in use." }
} catch [AggregateException] { } finally { $socket.Dispose() }
New-Item -ItemType Directory -Path $runtimePath -Force | Out-Null
$javaCommand = (Get-Command java -ErrorAction Stop).Source
$process = Start-Process -FilePath $javaCommand -ArgumentList @('-Xms64m','-Xmx256m','-jar',('"' + $jarPath + '"'),'--spring.profiles.active=static',("--server.port=$Port"),'--spring.cloud.config.enabled=false') -WindowStyle Hidden -PassThru -WorkingDirectory $projectRoot -RedirectStandardOutput (Join-Path $runtimePath 'gateway-static.out.log') -RedirectStandardError (Join-Path $runtimePath 'gateway-static.err.log')
try {
    $ready=$false
    for ($attempt=0;$attempt -lt 90;$attempt++) {
        $process.Refresh()
        if ($process.HasExited) { throw 'Static Gateway exited. Inspect .runtime/gateway-static logs.' }
        try { $health = Invoke-RestMethod "http://127.0.0.1:$Port/actuator/health" -TimeoutSec 3; if ($health.status -eq 'UP') { $ready=$true; break } } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Static Gateway did not become ready.' }
    foreach ($route in @('customers','products','bills')) {
        $result = Invoke-RestMethod "http://127.0.0.1:$Port/api/$route" -TimeoutSec 10
        Write-Host "PASS: static /api/$route ($($result.Count) results)"
    }
} finally {
    $process.Refresh()
    if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force }
}
