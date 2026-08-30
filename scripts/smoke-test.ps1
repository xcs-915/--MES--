param(
    [string]$BaseUrl = "http://127.0.0.1:8080/tns-mes",
    [string]$Username = "admin",
    [string]$Password = "admin123"
)

$ErrorActionPreference = "Stop"

function Invoke-Mes {
    param(
        [ValidateSet("GET", "POST", "PUT", "DELETE")][string]$Method,
        [string]$Path,
        $Body,
        [hashtable]$Headers = @{}
    )
    $uri = "$BaseUrl$Path"
    if ($Method -in @("POST", "PUT", "DELETE") -and -not $Headers.ContainsKey("X-Idempotency-Key")) {
        $Headers = @{} + $Headers
        $Headers["X-Idempotency-Key"] = [guid]::NewGuid().ToString()
    }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 10
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $Headers -ContentType "application/json" -Body $json
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $Headers
}

$login = Invoke-Mes -Method POST -Path "/api/v1/auth/login" -Body @{ username = $Username; password = $Password }
if ($login.code -ne 0) { throw "Login failed: $($login.message)" }
$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }
$suffix = Get-Date -Format "yyyyMMddHHmmss"

$types = Invoke-Mes -Method GET -Path "/api/v1/master-data/types?lang=en" -Headers $headers
if ($types.data.Count -ne 13) { throw "Expected 13 master data types" }

$enterprise = Invoke-Mes -Method POST -Path "/api/v1/master-data/enterprise" -Headers $headers -Body @{ code = "ENT-$suffix"; nameZh = "Smoke enterprise"; nameEn = "Smoke enterprise"; nameAr = "مؤسسة اختبار" }
$factory = Invoke-Mes -Method POST -Path "/api/v1/master-data/factory" -Headers $headers -Body @{ code = "FAC-$suffix"; nameZh = "Smoke factory"; nameEn = "Smoke factory"; nameAr = "مصنع اختبار"; parentId = $enterprise.data.id }

$product = Invoke-Mes -Method POST -Path "/api/v1/products" -Headers $headers -Body @{ code = "FG-$suffix"; nameZh = "Smoke finished product"; nameEn = "Smoke finished product"; nameAr = "منتج اختبار"; unit = "PCS" }
$component = Invoke-Mes -Method POST -Path "/api/v1/products" -Headers $headers -Body @{ code = "COMP-$suffix"; nameZh = "Smoke component"; nameEn = "Smoke component"; nameAr = "مكون اختبار"; unit = "PCS" }

$bom = Invoke-Mes -Method POST -Path "/api/v1/boms" -Headers $headers -Body @{ productId = $product.data.id; code = "BOM-$suffix"; versionCode = "1.0"; nameZh = "Smoke BOM"; nameEn = "Smoke BOM"; nameAr = "قائمة اختبار"; items = @(@{ componentProductId = $component.data.id; sequenceNo = 10; quantity = 2; unit = "PCS"; issueMethod = "BACKFLUSH" }) }
$route = Invoke-Mes -Method POST -Path "/api/v1/process-routes" -Headers $headers -Body @{ productId = $product.data.id; code = "ROUTE-$suffix"; versionCode = "1.0"; nameZh = "Smoke route"; nameEn = "Smoke route"; nameAr = "مسار اختبار"; operations = @(@{ sequenceNo = 10; code = "OP-$suffix"; nameZh = "Assembly"; nameEn = "Assembly"; nameAr = "تجميع"; standardTimeSeconds = 30 }) }
$rule = Invoke-Mes -Method POST -Path "/api/v1/inspection-rules" -Headers $headers -Body @{ code = "IQ-$suffix"; nameZh = "Smoke inspection"; nameEn = "Smoke inspection"; nameAr = "فحص اختبار"; inspectionType = "IN_PROCESS"; samplingMethod = "FULL"; items = @(@{ sequenceNo = 10; code = "DIM-$suffix"; nameZh = "Dimension"; nameEn = "Dimension"; nameAr = "الأبعاد"; dataType = "NUMBER"; minValue = 9.5; maxValue = 10.5; unit = "mm" }) }

$order = Invoke-Mes -Method POST -Path "/api/v1/work-orders" -Headers $headers -Body @{ productId = $product.data.id; bomId = $bom.data.id; routeId = $route.data.id; factoryId = $factory.data.id; quantity = 10; priority = 10; source = "SMOKE_TEST" }
$released = Invoke-Mes -Method POST -Path "/api/v1/work-orders/$($order.data.id)/release" -Headers $headers
$started = Invoke-Mes -Method POST -Path "/api/v1/work-orders/$($order.data.id)/start" -Headers $headers
$completed = Invoke-Mes -Method POST -Path "/api/v1/work-orders/$($order.data.id)/complete" -Headers $headers -Body @{ quantity = 10 }

[pscustomobject]@{
    BaseUrl = $BaseUrl
    Login = $login.code -eq 0
    MasterDataTypes = $types.data.Count
    EnterpriseId = $enterprise.data.id
    FactoryId = $factory.data.id
    ProductId = $product.data.id
    BomId = $bom.data.id
    RouteId = $route.data.id
    InspectionRuleId = $rule.data.id
    WorkOrderNo = $completed.data.orderNo
    FinalWorkOrderStatus = $completed.data.status
} | Format-List
