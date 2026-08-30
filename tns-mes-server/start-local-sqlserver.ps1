$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$authDirectory = Join-Path $projectRoot 'tools\mssql-jdbc-auth\extracted\x64'
$applicationJar = Join-Path $projectRoot 'target\tns-mes-server.jar'

if (-not (Test-Path -LiteralPath $applicationJar)) {
    throw "Application package not found: $applicationJar"
}

if (-not (Test-Path -LiteralPath $authDirectory)) {
    throw "SQL Server JDBC authentication library not found: $authDirectory"
}

Set-Location -LiteralPath $projectRoot
& java "-Djava.library.path=$authDirectory" -jar $applicationJar '--spring.profiles.active=sqlserver-local'
