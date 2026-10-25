param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$frontendPath = Join-Path $projectRoot 'frontend'
$runtimePath = Join-Path $projectRoot '.runtime'
$frontendState = Join-Path $runtimePath 'frontend-process.json'
if (Test-Path -LiteralPath $frontendState) { throw 'Run scripts/Stop-Billing.ps1 before starting again.' }
if (-not $SkipBuild) {
    Push-Location $projectRoot
    try {
        & (Join-Path $projectRoot 'mvnw.cmd') -B -ntp verify
        if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
    } finally { Pop-Location }
    Push-Location $frontendPath
    try {
        & npm.cmd ci --no-fund --no-audit
        if ($LASTEXITCODE -ne 0) { throw 'Angular dependency installation failed.' }
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Angular build failed.' }
    } finally { Pop-Location }
}
$cliPath = Join-Path $frontendPath 'node_modules/@angular/cli/bin/ng.js'
if (-not (Test-Path -LiteralPath $cliPath)) { throw 'Run npm ci in frontend first.' }
$socket = [Net.Sockets.TcpClient]::new()
try {
    if ($socket.ConnectAsync('127.0.0.1',4200).Wait(300) -and $socket.Connected) { throw 'Port 4200 is already in use.' }
} catch [AggregateException] { } finally { $socket.Dispose() }
& (Join-Path $PSScriptRoot 'Start-Stack.ps1') -SkipBuild -BillingOnly
try {
    $routesReady = $false
    for ($routeAttempt=0;$routeAttempt -lt 90;$routeAttempt++) {
        try {
            foreach ($route in @('customers','products','bills')) {
                Invoke-RestMethod "http://127.0.0.1:8080/api/$route" -TimeoutSec 5 | Out-Null
            }
            $routesReady = $true
            break
        } catch { Start-Sleep -Seconds 1 }
    }
    if (-not $routesReady) { throw 'The business routes did not become ready. Inspect .runtime logs.' }
    $nodeCommand = (Get-Command node -ErrorAction Stop).Source
    $process = Start-Process -FilePath $nodeCommand -ArgumentList @(('"' + $cliPath + '"'),'serve','--host','127.0.0.1','--port','4200') -WindowStyle Hidden -PassThru -WorkingDirectory $frontendPath -RedirectStandardOutput (Join-Path $runtimePath 'frontend.out.log') -RedirectStandardError (Join-Path $runtimePath 'frontend.err.log')
    @{ ProcessId = $process.Id; Cli = $cliPath; StartedAt = $process.StartTime.ToUniversalTime().ToString('o') } | ConvertTo-Json | Set-Content -LiteralPath $frontendState -Encoding UTF8
    $ready = $false
    for ($attempt=0;$attempt -lt 60;$attempt++) {
        $process.Refresh()
        if ($process.HasExited) { throw 'Angular exited. Inspect .runtime/frontend logs.' }
        try { $response = Invoke-WebRequest http://127.0.0.1:4200 -TimeoutSec 3 -UseBasicParsing; if ($response.StatusCode -eq 200) { $ready=$true; break } } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Angular did not become ready.' }
    Write-Host 'Facture is running at http://localhost:4200'
} catch {
    $failure = $_
    & (Join-Path $PSScriptRoot 'Stop-Billing.ps1')
    throw $failure
}
