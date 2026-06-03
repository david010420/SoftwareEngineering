$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$lib = Join-Path $root "lib"
New-Item -ItemType Directory -Force $lib | Out-Null

$dependencies = @(
    @{
        Name = "sqlite-jdbc-3.45.3.0.jar"
        Url = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar"
    },
    @{
        Name = "slf4j-api-2.0.9.jar"
        Url = "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"
    },
    @{
        Name = "slf4j-nop-2.0.9.jar"
        Url = "https://repo1.maven.org/maven2/org/slf4j/slf4j-nop/2.0.9/slf4j-nop-2.0.9.jar"
    }
)

foreach ($dependency in $dependencies) {
    $target = Join-Path $lib $dependency.Name
    if (-not (Test-Path $target)) {
        Write-Host "Downloading $($dependency.Name)..."
        Invoke-WebRequest -Uri $dependency.Url -OutFile $target
    }
}
