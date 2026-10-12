#Requires -Version 5.1
<#
.SYNOPSIS
  Chạy kiểm thử backend.

.DESCRIPTION
  unit         Unit + WebMvc (*Test)                       -> mvnw test
  it           Tích hợp Spring/DB (*IT)                    -> mvnw verify (chỉ failsafe)
  verify       Toàn bộ: unit + *IT                         -> mvnw verify
  e2e          Replication E2E trên 2 PostgreSQL           -> dựng docker-compose, tự dọn
  smoke        Smoke nhóm 2 trên stack đang chạy           -> system/smoke-group2.sh (cần Git Bash)
  replication  Replication 2 chiều trên stack đang chạy    -> system/replication-e2e.sh (cần Git Bash)

  Tham số còn lại được chuyển nguyên cho Maven, vd: .\scripts\test.ps1 unit -Dtest=AuthServiceTest

.EXAMPLE
  .\scripts\test.ps1 unit
.EXAMPLE
  .\scripts\test.ps1 e2e
#>
param(
    [Parameter(Position = 0)]
    [ValidateSet('unit', 'it', 'verify', 'e2e', 'smoke', 'replication')]
    [string]$Mode = 'unit',

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs = @()
)

$ErrorActionPreference = 'Stop'
$BackendDir = Split-Path -Parent $PSScriptRoot
Set-Location $BackendDir

$E2ECompose = 'src/test/resources/replication-e2e/docker-compose.yml'
# Không chạy test nào ở surefire khi chỉ muốn chạy nhóm failsafe (*IT)
$NoUnitTests = @('-Dtest=__none__', '-Dsurefire.failIfNoSpecifiedTests=false')

function Invoke-Maven([string[]]$Goals) {
    & .\mvnw.cmd @Goals
    if ($LASTEXITCODE -ne 0) { throw "Maven thất bại (mã $LASTEXITCODE)" }
}

function Invoke-Compose {
    # Không chuyển stderr của docker thành lỗi: docker ghi tiến trình ra stderr
    & docker compose -f $E2ECompose @args
    if ($LASTEXITCODE -ne 0) { throw "docker compose lỗi (mã $LASTEXITCODE): $args" }
}

function Wait-Subscription {
    $state = ''
    for ($i = 0; $i -lt 60; $i++) {
        $state = (& docker compose -f $E2ECompose exec -T pg-replica psql -U erp_user -d erp_db -At `
            -c "SELECT count(*) FILTER (WHERE srsubstate = 'r')::text || '|' || count(*)::text FROM pg_subscription_rel").Trim()
        if ($state -eq '1|1') { Write-Host '==> Subscription sẵn sàng'; return }
        Start-Sleep -Seconds 1
    }
    throw "Subscription chưa sẵn sàng sau 60s (trạng thái: $state)"
}

function Invoke-E2E {
    try {
        Invoke-Compose down -v
        Invoke-Compose up -d --wait

        Write-Host '==> Tạo bảng và logical replication (master -> replica)'
        $ddl = 'CREATE TABLE IF NOT EXISTS category (id serial PRIMARY KEY, code varchar(50) UNIQUE NOT NULL, name varchar(200) NOT NULL, is_active boolean NOT NULL)'
        Invoke-Compose exec -T pg-master psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q -c $ddl
        Invoke-Compose exec -T pg-replica psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q -c $ddl
        Invoke-Compose exec -T pg-master psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q `
            -c 'DROP PUBLICATION IF EXISTS erp_pub' -c 'CREATE PUBLICATION erp_pub FOR TABLE category'
        Invoke-Compose exec -T pg-replica psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q `
            -c 'DROP SUBSCRIPTION IF EXISTS erp_sub' `
            -c "CREATE SUBSCRIPTION erp_sub CONNECTION 'host=pg-master port=5432 user=erp_user password=erp_password dbname=erp_db' PUBLICATION erp_pub"
        Wait-Subscription

        Write-Host '==> Chạy ReplicationE2EIT'
        Invoke-Maven (@('verify', '-Pe2e') + $NoUnitTests + @('-Dit.test=ReplicationE2EIT', '-Dfailsafe.failIfNoSpecifiedTests=false') + $MavenArgs)
    }
    finally {
        Write-Host '==> Dọn container E2E'
        & docker compose -f $E2ECompose down -v | Out-Null
    }
}

switch ($Mode) {
    'unit'        { Invoke-Maven (@('test') + $MavenArgs) }
    'it'          { Invoke-Maven (@('verify') + $NoUnitTests + $MavenArgs) }
    'verify'      { Invoke-Maven (@('verify') + $MavenArgs) }
    'e2e'         { Invoke-E2E }
    'smoke'       { & bash (Join-Path $PSScriptRoot 'system/smoke-group2.sh') @MavenArgs; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
    'replication' { & bash (Join-Path $PSScriptRoot 'system/replication-e2e.sh') @MavenArgs; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
}
