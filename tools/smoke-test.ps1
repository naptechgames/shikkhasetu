# End-to-end smoke test against a RUNNING backend (default http://127.0.0.1:8080).
# It logs in with the seeded demo accounts and walks through one complete loan.
# Usage:  .\tools\smoke-test.ps1            (start the backend first, see README)
param([string]$Server = 'http://127.0.0.1:8080')

$base = "$Server/api"
$propsFile = Join-Path $PSScriptRoot '..\backend\config\application.properties'
$props = @{}
Get-Content $propsFile | Where-Object { $_ -match '^app\.seed\.[^=]+=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2; $props[$k] = $v
}

function Call($method, $path, $token, $body) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    if ($body) {
        Invoke-RestMethod -Method $method "$base$path" -Headers $headers -ContentType 'application/json' -Body ($body | ConvertTo-Json)
    } else {
        Invoke-RestMethod -Method $method "$base$path" -Headers $headers
    }
}

# Windows PowerShell 5.1 returns a JSON array as ONE object; this turns it into a normal list.
function Flat($value) { , @($value | ForEach-Object { $_ }) }

"health: " + (Call GET '/health').status
$student = Call POST '/auth/login' $null @{ email = $props['app.seed.student-email']; password = $props['app.seed.student-password'] }
"student login: role=$($student.user.role)"
$coord = Call POST '/auth/login' $null @{ email = $props['app.seed.coordinator-email']; password = $props['app.seed.coordinator-password'] }
"coordinator login: role=$($coord.user.role)"

$items = Flat (Call GET '/items?q=casio&category=CALCULATOR&status=AVAILABLE' $student.token)
"search 'casio' + CALCULATOR + AVAILABLE: $($items.Count) item(s)"
if ($items.Count -eq 0) { throw 'No available demo calculator. Is another loan still open?' }
"all items visible to the student: " + (Flat (Call GET '/items' $student.token)).Count

$request = Call POST '/requests' $student.token @{ itemId = $items[0].id; loanDays = 7 }
"request #$($request.id): $($request.status)"

try {
    Call POST "/requests/$($request.id)/approve" $student.token | Out-Null
    "student approve: ALLOWED  <-- BUG"
} catch {
    "student approve: refused with HTTP $([int]$_.Exception.Response.StatusCode)"
}

$approved = Call POST "/requests/$($request.id)/approve" $coord.token
"coordinator approve: $($approved.status), item $($approved.item.status), code shown to coordinator: $([bool]$approved.pickupCode)"
$mine = Call GET "/requests/$($request.id)" $student.token
"student sees the pickup code: $([bool]$mine.pickupCode)"

$handed = Call POST "/requests/$($request.id)/handover" $coord.token @{ pickupCode = $mine.pickupCode }
"handover: $($handed.status), item $($handed.item.status), due $($handed.dueDate)"
$returned = Call POST "/requests/$($request.id)/return" $coord.token @{ condition = 'GOOD' }
"return: $($returned.status), item $($returned.item.status)"

"student notifications: " + (Flat (Call GET '/notifications' $student.token)).Count
"dashboard: " + ((Call GET '/dashboard' $student.token) | ConvertTo-Json -Compress)
