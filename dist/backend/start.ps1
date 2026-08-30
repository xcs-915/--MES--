# MES 启动脚本 (SQL Server Local)
# 用法: .\start.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$dllPath = Join-Path $root 'tools\mssql-jdbc-auth\extracted\x64'
$jar = Join-Path $root 'target\tns-mes-server.jar'

if (-not (Test-Path $jar)) {
    Write-Host '[BUILD] 未找到 jar, 正在打包...' -ForegroundColor Yellow
    Set-Location $root
    & mvn package -DskipTests -q
}

Write-Host '[START] 启动 MES (sqlserver-local)...' -ForegroundColor Cyan
Write-Host "  DLL 路径: $dllPath"
Write-Host "  JAR 路径:  $jar"
Write-Host "  访问地址:  http://127.0.0.1:8080/tns-mes/"
Write-Host ''

& java "-Djava.library.path=$dllPath" -jar $jar '--spring.profiles.active=sqlserver-local'
