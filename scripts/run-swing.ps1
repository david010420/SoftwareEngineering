$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot "compile.ps1")
& (Join-Path $PSScriptRoot "ensure-sqlite-jdbc.ps1")
java -cp "$($root)\out;$($root)\lib\*" ui.swing.SwingIssueApp
