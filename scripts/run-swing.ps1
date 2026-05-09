$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot "compile.ps1")
java -cp (Join-Path $root "out") its.ui.swing.SwingIssueApp
