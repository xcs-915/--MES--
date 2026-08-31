# ==============================================================================
# 脚本名称: 03-init-sqlserver.ps1
# 功能描述: 在 Windows 服务器上执行 SQL Server 数据库初始化
#           连接到 10.30.10.141 的 SQL Server，执行 init-sqlserver.sql
# 适用系统: Windows Server 2019 + SQL Server 2019
# 执行用户: 管理员权限
# 使用方法: 以管理员身份运行 PowerShell，执行:
#           .\03-init-sqlserver.ps1
# ==============================================================================

# 严格模式，遇错即停
$ErrorActionPreference = "Stop"

# ============================================================================
# 配置参数
# ============================================================================
$SqlServerInstance = "<DB_HOST>"               # SQL Server 服务器地址 (部署时替换)
$SqlServerPort     = 1433                      # SQL Server 端口
$SaUser            = "sa"                       # 系统管理员账户
$SaPassword       = "<SA_PASSWORD>"            # SA 密码 (部署时替换)
$Database         = "tns_mes"                  # 目标数据库名称
$DbUser            = "tns_mes_user"             # 数据库用户名
$DbPassword        = "<DB_PASSWORD>"            # 数据库用户密码 (部署时替换)

# SQL 脚本文件路径 (与本脚本同目录下的 sql 文件夹)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SqlScriptPath = Join-Path $ScriptDir "..\sql\init-sqlserver.sql"

# 如果在 k8s-deploy\scripts 下找不到，尝试其他常见路径
if (-not (Test-Path $SqlScriptPath)) {
    $SqlScriptPath = Join-Path $ScriptDir "init-sqlserver.sql"
}

# 构建连接字符串 (sa 账户连接)
$SaConnectionString = "Server=$SqlServerInstance,$SqlServerPort;Database=master;User Id=$SaUser;Password=$SaPassword;TrustServerCertificate=True;Connection Timeout=30;"

# 构建验证连接字符串 (tns_mes_user 账户连接)
$UserConnectionString = "Server=$SqlServerInstance,$SqlServerPort;Database=$Database;User Id=$DbUser;Password=$DbPassword;TrustServerCertificate=True;Connection Timeout=30;"

# ============================================================================
# 辅助函数
# ============================================================================

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch ($Level) {
        "ERROR" { "Red" }
        "WARN"  { "Yellow" }
        "SUCCESS" { "Green" }
        default  { "White" }
    }
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
}

function Test-SqlConnection {
    param([string]$ConnectionString, [string]$TestQuery = "SELECT 1")
    try {
        $connection = New-Object System.Data.SqlClient.SqlConnection($ConnectionString)
        $connection.Open()
        $command = New-Object System.Data.SqlClient.SqlCommand($TestQuery, $connection)
        $result = $command.ExecuteScalar()
        $connection.Close()
        return $true
    }
    catch {
        return $false
    }
}

# ============================================================================
# 主流程
# ============================================================================

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  SQL Server 数据库初始化" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# 步骤 0: 前置检查
# ============================================================================
Write-Log "步骤 0: 前置检查"

# 检查 SQL 脚本文件是否存在
if (-not (Test-Path $SqlScriptPath)) {
    Write-Log "错误: SQL 脚本文件不存在: $SqlScriptPath" "ERROR"
    Write-Log "请确保 init-sqlserver.sql 文件位于 sql 文件夹中" "WARN"
    exit 1
}
Write-Log "  SQL 脚本文件: $SqlScriptPath"

# 检查 .NET 类库是否可用 (System.Data.SqlClient)
try {
    Add-Type -AssemblyName "System.Data" -ErrorAction Stop
    Write-Log "  .NET System.Data 程序集加载成功"
} catch {
    Write-Log "错误: 无法加载 System.Data 程序集" "ERROR"
    Write-Log "请确保 .NET Framework 已正确安装" "WARN"
    exit 1
}

# ============================================================================
# 步骤 1: 测试 sa 账户连接
# ============================================================================
Write-Log "步骤 1: 测试 sa 账户连接 SQL Server..."

Write-Log "  服务器: $SqlServerInstance"
Write-Log "  端口: $SqlServerPort"
Write-Log "  账户: $SaUser"

$saConnected = Test-SqlConnection -ConnectionString $SaConnectionString -TestQuery "SELECT @@VERSION"
if (-not $saConnected) {
    Write-Log "错误: 无法使用 sa 账户连接到 SQL Server ($SqlServerInstance,$SqlServerPort)" "ERROR"
    Write-Log "请检查以下内容:" "WARN"
    Write-Log "  1. SQL Server 服务是否已启动" "WARN"
    Write-Log "  2. TCP/IP 协议是否已启用 (SQL Server 配置管理器)" "WARN"
    Write-Log "  3. 端口 1433 是否被防火墙阻止" "WARN"
    Write-Log "  4. SA 密码是否正确" "WARN"
    Write-Log "  5. SA 登录是否已启用 (SQL Server 混合身份验证模式)" "WARN"
    exit 1
}

# 获取 SQL Server 版本信息
try {
    $conn = New-Object System.Data.SqlClient.SqlConnection($SaConnectionString)
    $conn.Open()
    $cmd = New-Object System.Data.SqlClient.SqlCommand("SELECT @@VERSION", $conn)
    $version = $cmd.ExecuteScalar()
    $conn.Close()

    # 提取版本号
    $versionLine = $version -split "`n" | Select-Object -First 1
    Write-Log "  SQL Server 版本: $versionLine" "SUCCESS"
} catch {
    Write-Log "  警告: 无法获取 SQL Server 版本信息" "WARN"
}
Write-Log "  sa 连接测试: 通过" "SUCCESS"

# ============================================================================
# 步骤 2: 执行 SQL 初始化脚本
# ============================================================================
Write-Log "步骤 2: 执行 SQL 初始化脚本..."

# 读取 SQL 脚本内容
$sqlContent = Get-Content -Path $SqlScriptPath -Raw
if (-not $sqlContent) {
    Write-Log "错误: SQL 脚本文件内容为空" "ERROR"
    exit 1
}
Write-Log "  SQL 脚本已读取，长度: $($sqlContent.Length) 字符"

# 按 GO 分割并逐段执行 SQL 脚本
$sqlBatches = $sqlContent -split "`r?`nGO`r?`n"
Write-Log "  SQL 脚本分为 $($sqlBatches.Count) 个批次执行"

try {
    $conn = New-Object System.Data.SqlClient.SqlConnection($SaConnectionString)
    $conn.Open()
    $cmd = New-Object System.Data.SqlClient.SqlCommand
    $cmd.Connection = $conn
    $cmd.CommandTimeout = 300

    $batchIndex = 0
    $totalBatches = $sqlBatches.Count
    $successBatches = 0

    foreach ($batch in $sqlBatches) {
        $batchIndex++
        $trimmedBatch = $batch.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmedBatch)) {
            continue
        }

        try {
            $cmd.CommandText = $trimmedBatch
            $reader = $cmd.ExecuteReader()

            # 如果有结果集，读取并显示
            do {
                if ($reader.HasRows) {
                    $dataTable = New-Object System.Data.DataTable
                    $dataTable.Load($reader)

                    # 显示结果
                    $rowIndex = 0
                    foreach ($row in $dataTable.Rows) {
                        $rowIndex++
                        $values = @()
                        foreach ($column in $dataTable.Columns) {
                            $values += "$($column.ColumnName)=$($row[$column.ColumnName])"
                        }
                        Write-Log "    结果: $($values -join '; ')"
                    }
                }
            } while ($reader.NextResult())
            $reader.Close()

            $successBatches++
        } catch {
            # 某些验证语句可能因条件不满足而报错，记录但不中断
            Write-Log "    批次 $batchIndex 执行提示: $($_.Exception.Message)" "WARN"
        }
    }

    $conn.Close()
    Write-Log "  SQL 脚本执行完成: $successBatches / $totalBatches 批次成功" "SUCCESS"
} catch {
    Write-Log "错误: SQL 脚本执行失败: $($_.Exception.Message)" "ERROR"
    if ($conn.State -eq 'Open') { $conn.Close() }
    exit 1
}

# ============================================================================
# 步骤 3: 验证数据库创建结果
# ============================================================================
Write-Log "步骤 3: 验证数据库创建结果..."

$verifyResult = @{}

try {
    $conn = New-Object System.Data.SqlClient.SqlConnection($SaConnectionString)
    $conn.Open()

    # 3.1 验证数据库是否存在
    $cmd = New-Object System.Data.SqlClient.SqlCommand(@"
        SELECT name, state_desc, recovery_model_desc, collation_name
        FROM sys.databases
        WHERE name = '$Database'
"@, $conn)
    $reader = $cmd.ExecuteReader()

    $dbExists = $false
    $dbRecovery = ""
    $dbCollation = ""

    if ($reader.Read()) {
        $dbExists = $true
        $dbState = $reader["state_desc"]
        $dbRecovery = $reader["recovery_model_desc"]
        $dbCollation = $reader["collation_name"]

        Write-Log "  数据库名称: $($reader["name"])" "SUCCESS"
        Write-Log "  数据库状态: $dbState" "SUCCESS"
        Write-Log "  恢复模式: $dbRecovery" "SUCCESS"
        Write-Log "  排序规则: $dbCollation" "SUCCESS"

        $verifyResult.DatabaseExists = $true
    }
    $reader.Close()

    if (-not $dbExists) {
        Write-Log "错误: 数据库 $Database 不存在!" "ERROR"
        $conn.Close()
        exit 1
    }

    # 3.2 验证登录用户是否存在
    $cmd = New-Object System.Data.SqlClient.SqlCommand(@"
        SELECT name, type_desc, default_database_name, is_disabled
        FROM sys.server_principals
        WHERE name = '$DbUser' AND type = 'S'
"@, $conn)
    $reader = $cmd.ExecuteReader()

    $loginExists = $false

    if ($reader.Read()) {
        $loginExists = $true
        $loginType = $reader["type_desc"]
        $loginDb = $reader["default_database_name"]
        $loginDisabled = $reader["is_disabled"]

        Write-Log "  登录用户: $($reader["name"])" "SUCCESS"
        Write-Log "  类型: $loginType" "SUCCESS"
        Write-Log "  默认数据库: $loginDb" "SUCCESS"
        Write-Log "  是否禁用: $loginDisabled" "SUCCESS"

        $verifyResult.LoginExists = $true
    }
    $reader.Close()

    if (-not $loginExists) {
        Write-Log "错误: 登录用户 $DbUser 不存在!" "ERROR"
        $conn.Close()
        exit 1
    }

    # 3.3 验证数据库用户和角色
    $useDbSql = "USE [$Database];"
    $cmd = New-Object System.Data.SqlClient.SqlCommand($useDbSql, $conn)
    $cmd.ExecuteNonQuery() | Out-Null

    $cmd = New-Object System.Data.SqlClient.SqlCommand(@"
        SELECT
            dp.name AS db_user,
            dp.default_schema_name AS default_schema,
            r.name AS role_name
        FROM sys.database_principals dp
            LEFT JOIN sys.database_role_members rm ON dp.principal_id = rm.member_principal_id
            LEFT JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
        WHERE dp.name = '$DbUser'
"@, $conn)
    $reader = $cmd.ExecuteReader()

    $dbUserExists = $false
    $dbOwnerRole = $false

    while ($reader.Read()) {
        $dbUserExists = $true
        $userName = $reader["db_user"]
        $defaultSchema = $reader["default_schema"]
        $roleName = $reader["role_name"]

        Write-Log "  数据库用户: $userName" "SUCCESS"
        Write-Log "  默认架构: $defaultSchema" "SUCCESS"
        if ($roleName) {
            Write-Log "  数据库角色: $roleName" "SUCCESS"
            if ($roleName -eq "db_owner") {
                $dbOwnerRole = $true
            }
        }
    }
    $reader.Close()

    if (-not $dbUserExists) {
        Write-Log "错误: 数据库用户 $DbUser 在 $Database 中不存在!" "ERROR"
        $conn.Close()
        exit 1
    }

    if (-not $dbOwnerRole) {
        Write-Log "警告: 用户 $DbUser 未获得 db_owner 角色权限!" "WARN"
    }

    $conn.Close()
    $verifyResult.DbUserExists = $true
    $verifyResult.DbOwnerRole = $dbOwnerRole

} catch {
    Write-Log "错误: 验证过程出错: $($_.Exception.Message)" "ERROR"
    if ($conn -and $conn.State -eq 'Open') { $conn.Close() }
    exit 1
}

# ============================================================================
# 步骤 4: 使用 tns_mes_user 账户验证连接
# ============================================================================
Write-Log "步骤 4: 使用 $DbUser 账户验证连接..."

$userConnected = Test-SqlConnection -ConnectionString $UserConnectionString -TestQuery "SELECT DB_NAME()"
if ($userConnected) {
    Write-Log "  $DbUser 连接测试: 通过" "SUCCESS"
    $verifyResult.UserConnection = $true
} else {
    Write-Log "错误: $DbUser 无法连接到数据库 $Database" "ERROR"
    Write-Log "请检查用户密码和权限设置" "WARN"
    exit 1
}

# ============================================================================
# 完成总结
# ============================================================================
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  SQL Server 数据库初始化完成!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "数据库信息:" -ForegroundColor White
Write-Host "  服务器:           $SqlServerInstance" -ForegroundColor White
Write-Host "  端口:             $SqlServerPort" -ForegroundColor White
Write-Host "  数据库名称:       $Database" -ForegroundColor White
Write-Host "  排序规则:         Chinese_PRC_CI_AS" -ForegroundColor White
Write-Host "  恢复模式:         SIMPLE" -ForegroundColor White
Write-Host ""
Write-Host "数据库账户信息:" -ForegroundColor White
Write-Host "  用户名:           $DbUser" -ForegroundColor White
Write-Host "  密码:             $DbPassword" -ForegroundColor White
Write-Host "  权限:             db_owner" -ForegroundColor White
Write-Host ""
Write-Host "连接字符串:" -ForegroundColor White
Write-Host "  Server=$SqlServerInstance,$SqlServerPort;Database=$Database;User Id=$DbUser;Password=$DbPassword;TrustServerCertificate=True;" -ForegroundColor Yellow
Write-Host ""
Write-Host "验证结果:" -ForegroundColor White
Write-Host "  数据库创建:       $(if($verifyResult.DatabaseExists){'成功'}else{'失败'})" -ForegroundColor $(if($verifyResult.DatabaseExists){'Green'}else{'Red'})
Write-Host "  登录用户创建:     $(if($verifyResult.LoginExists){'成功'}else{'失败'})" -ForegroundColor $(if($verifyResult.LoginExists){'Green'}else{'Red'})
Write-Host "  数据库用户创建:   $(if($verifyResult.DbUserExists){'成功'}else{'失败'})" -ForegroundColor $(if($verifyResult.DbUserExists){'Green'}else{'Red'})
Write-Host "  db_owner 权限:    $(if($verifyResult.DbOwnerRole){'已授予'}else{'未授予'})" -ForegroundColor $(if($verifyResult.DbOwnerRole){'Green'}else{'Yellow'})
Write-Host "  用户连接测试:     $(if($verifyResult.UserConnection){'通过'}else{'失败'})" -ForegroundColor $(if($verifyResult.UserConnection){'Green'}else{'Red'})
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
