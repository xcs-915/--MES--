# ==============================================================================
# git-push.ps1 - TNS-MES Git 快速推送脚本
# 用法: .\git-push.ps1 "提交信息"
# ==============================================================================
param(
    [string]$Message = "update: local changes"
)

$GitPath = "C:\Program Files\Git\bin\git.exe"
Set-Location $PSScriptRoot

Write-Host "[1/3] Adding files..." -ForegroundColor Cyan
& $GitPath add -A

Write-Host "[2/3] Committing..." -ForegroundColor Cyan
& $GitPath commit -m $Message

Write-Host "[3/3] Pushing to GitHub..." -ForegroundColor Cyan
$env:HTTPS_PROXY = "http://127.0.0.1:7897"
$env:HTTP_PROXY = "http://127.0.0.1:7897"
& $GitPath push origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[OK] Push successful!" -ForegroundColor Green
} else {
    Write-Host "`n[WARN] Push may have issues (warnings are normal with proxy)" -ForegroundColor Yellow
}

Write-Host "Repository: https://github.com/xcs-915/--MES--" -ForegroundColor White
