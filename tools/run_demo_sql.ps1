param(
  [ValidateSet("users", "privacy", "tickets", "change", "all")]
  [string]$Demo = "all",
  [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe",
  [string]$User = "root",
  [string]$Password = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$scripts = @{
  users = Join-Path $repoRoot "database\demo_user_table.sql"
  privacy = Join-Path $repoRoot "database\demo_privacy_security.sql"
  tickets = Join-Path $repoRoot "database\demo_special_ticket_orders.sql"
  change = Join-Path $repoRoot "database\demo_change_chain.sql"
  all = Join-Path $repoRoot "database\demo_all.sql"
}

$scriptPath = $scripts[$Demo]

if (-not (Test-Path -LiteralPath $MysqlExe)) {
  throw "Cannot find mysql.exe at: $MysqlExe"
}

if (-not (Test-Path -LiteralPath $scriptPath)) {
  throw "Cannot find SQL script: $scriptPath"
}

Write-Host "Running demo SQL: $Demo"
Write-Host "SQL file: $scriptPath"
if ([string]::IsNullOrEmpty($Password)) {
  Write-Host "When prompted, enter the MySQL password for user '$User'."
}

$passwordArg = if ([string]::IsNullOrEmpty($Password)) { "-p" } else { "-p$Password" }
$command = '"' + $MysqlExe + '" -u ' + $User + ' ' + $passwordArg + ' --default-character-set=utf8mb4 < "' + $scriptPath + '"'
cmd.exe /c $command
