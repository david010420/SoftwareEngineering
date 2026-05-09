$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot "compile.ps1")

$testOut = Join-Path $root "out-test"
if (Test-Path $testOut) {
    Remove-Item -Recurse -Force $testOut
}
New-Item -ItemType Directory -Force $testOut | Out-Null

$tests = Get-ChildItem -Path (Join-Path $root "src\test\java") -Recurse -Filter *.java |
    Select-Object -ExpandProperty FullName

javac -encoding UTF-8 -cp (Join-Path $root "out") -d $testOut $tests
java -cp "$($root)\out;$testOut" its.IssueServiceTest
