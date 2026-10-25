$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$statePath = Join-Path $projectRoot '.runtime/frontend-process.json'
if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $process = Get-Process -Id $state.ProcessId -ErrorAction SilentlyContinue
    if ($process) {
        $expectedCli = Join-Path $projectRoot 'frontend/node_modules/@angular/cli/bin/ng.js'
        $sameStart = [Math]::Abs(($process.StartTime.ToUniversalTime() - ([DateTime]$state.StartedAt).ToUniversalTime()).TotalSeconds) -lt 2
        $command = (Get-CimInstance Win32_Process -Filter "ProcessId = $($state.ProcessId)").CommandLine
        if ($sameStart -and $state.Cli -eq $expectedCli -and $process.ProcessName -eq 'node' -and $command -and $command.Contains($expectedCli)) {
            Stop-Process -Id $state.ProcessId -Force
            Remove-Item -LiteralPath $statePath
            Write-Host 'Angular stopped'
        } else { throw 'The process no longer matches the managed Angular client; it was not stopped.' }
    } else { Remove-Item -LiteralPath $statePath }
}
& (Join-Path $PSScriptRoot 'Stop-Stack.ps1')
