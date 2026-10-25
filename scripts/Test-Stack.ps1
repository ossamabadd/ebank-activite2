param([string]$GatewayUrl = 'http://localhost:8080', [switch]$Chat)
$ErrorActionPreference = 'Stop'
$ready = $false
for ($attempt = 0; $attempt -lt 90; $attempt++) {
    try {
        $customers = Invoke-RestMethod "$GatewayUrl/api/customers" -TimeoutSec 5
        $accounts = Invoke-RestMethod "$GatewayUrl/api/accounts" -TimeoutSec 15
        $tools = Invoke-RestMethod "$GatewayUrl/api/tools" -TimeoutSec 15
        if ($customers.Count -gt 0 -and $accounts.Count -gt 0 -and
                $accounts[0].customer.available -and $tools.Count -ge 5) {
            $ready = $true
            break
        }
    } catch { }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'Customer/account/tool routes or OpenFeign enrichment did not become ready.' }
$account = $accounts[0]
$key = [Guid]::NewGuid().ToString()
$creditUrl = "$GatewayUrl/api/accounts/$($account.id)/credit"
$headers = @{ 'Idempotency-Key' = "smoke-credit-$key" }
$credit = Invoke-RestMethod $creditUrl -Method Post -Headers $headers -ContentType 'application/json' -Body '{"amount":1.00}'
$duplicate = Invoke-RestMethod $creditUrl -Method Post -Headers $headers -ContentType 'application/json' -Body '{"amount":1.00}'
if ($credit.id -ne $duplicate.id) { throw 'Credit was not idempotent.' }
$debit = Invoke-RestMethod "$GatewayUrl/api/accounts/$($account.id)/debit" -Method Post -Headers @{ 'Idempotency-Key' = "smoke-debit-$key" } -ContentType 'application/json' -Body '{"amount":1.00}'
if ([decimal]$debit.balanceAfter -ne [decimal]$account.balance) { throw 'The final balance did not match the initial balance.' }
Write-Host "PASS: Gateway, customers, OpenFeign enrichment, $($tools.Count) MCP tools and idempotent balance operations."
if ($Chat) {
    $answer = Invoke-RestMethod "$GatewayUrl/api/chat" -Method Post -ContentType 'application/json' -Body '{"message":"Quel est le solde du compte 11111111-1111-1111-1111-111111111111 ?"}' -TimeoutSec 180
    if ([string]::IsNullOrWhiteSpace($answer.answer)) { throw 'The model returned an empty answer.' }
    Write-Host $answer.answer
}
