param([string]$GatewayUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
$ready = $false
for ($attempt=0;$attempt -lt 90;$attempt++) {
    try {
        $customers = Invoke-RestMethod "$GatewayUrl/api/customers" -TimeoutSec 5
        $products = Invoke-RestMethod "$GatewayUrl/api/products" -TimeoutSec 5
        $invoices = Invoke-RestMethod "$GatewayUrl/api/bills" -TimeoutSec 5
        $dynamic = Invoke-RestMethod "$GatewayUrl/inventory-service/api/products" -TimeoutSec 5
        if ($customers.Count -gt 0 -and $products.Count -ge 2 -and $dynamic.Count -ge 2) { $ready=$true; break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'Billing routes did not become ready.' }
$body = @{
    customerId = $customers[0].id
    dueDate = [DateTime]::Today.AddDays(14).ToString('yyyy-MM-dd')
    items = @(@{productId=$products[0].id;quantity=2},@{productId=$products[1].id;quantity=1})
} | ConvertTo-Json -Depth 5
$headers = @{ 'Idempotency-Key' = 'smoke-' + [Guid]::NewGuid().ToString() }
$bill = Invoke-RestMethod "$GatewayUrl/api/bills" -Method Post -Headers $headers -ContentType 'application/json' -Body $body
$duplicate = Invoke-RestMethod "$GatewayUrl/api/bills" -Method Post -Headers $headers -ContentType 'application/json' -Body $body
$expected = ([decimal]$products[0].unitPrice * 2) + [decimal]$products[1].unitPrice
if ($bill.id -ne $duplicate.id -or [decimal]$bill.total -ne $expected -or $bill.items.Count -ne 2) { throw 'Invoice computation or idempotency failed.' }
$paid = Invoke-RestMethod "$GatewayUrl/api/bills/$($bill.id)/status" -Method Patch -ContentType 'application/json' -Body '{"status":"PAID"}'
if ($paid.status -ne 'PAID') { throw 'Invoice status update failed.' }
$dynamicBill = Invoke-RestMethod "$GatewayUrl/billing-service/api/bills/$($bill.id)" -TimeoutSec 5
if ($dynamicBill.customerId -ne $customers[0].id) { throw 'Dynamic billing route failed.' }
Write-Host "PASS: customer, inventory, billing, OpenFeign prices, exact total $expected MAD, idempotency, payment and dynamic Eureka routes."
