# Setup script for MSM Private Server
# Downloads H2 database and JSON library Java

param(
    [string]$jdkPath = ""
)

$ErrorActionPreference = "Continue"

# Use JDK if provided, otherwise try JAVA_HOME or PATH
if (-not $jdkPath) {
    $jdkPath = $env:JAVA_HOME
}
if (-not $jdkPath) {
    # Try to find javac
    $javac = Get-Command "javac" -ErrorAction SilentlyContinue
    if ($javac) {
        $jdkPath = Split-Path -Parent (Split-Path -Parent $javac.Source)
    }
}

$libDir = Join-Path $PSScriptRoot "lib"
New-Item -ItemType Directory -Path $libDir -Force | Out-Null

# --- H2 Database ---
$h2Url = "https://github.com/h2database/h2database/releases/download/version-2.2.224/h2-2023-09-17.zip"
$h2Zip = "$env:TEMP\h2.zip"
$h2Jar = Join-Path $libDir "h2.jar"

if (-not (Test-Path $h2Jar)) {
    Write-Output "Downloading H2 database..."
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $h2Url -OutFile $h2Zip -UseBasicParsing
        Expand-Archive -Path $h2Zip -DestinationPath "$env:TEMP\h2-extract" -Force
        Get-ChildItem "$env:TEMP\h2-extract\h2\bin\h2-*.jar" | ForEach-Object {
            Copy-Item $_.FullName $h2Jar -Force
        }
        Remove-Item -Path $h2Zip -Force -ErrorAction SilentlyContinue
        Remove-Item -Path "$env:TEMP\h2-extract" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Output "H2: OK"
    } catch {
        Write-Output "H2 download failed: $($_.Exception.Message)"
    }
} else {
    Write-Output "H2: already present"
}

# --- JSON Library ---
$jsonJar = Join-Path $libDir "json-20250107.jar"

if (-not (Test-Path $jsonJar)) {
    Write-Output "Downloading JSON-java source..."
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $jsonZipUrl = "https://github.com/stleary/JSON-java/archive/refs/tags/20250107.zip"
        $jsonZip = "$env:TEMP\json-src.zip"
        Invoke-WebRequest -Uri $jsonZipUrl -OutFile $jsonZip -UseBasicParsing
        Expand-Archive -Path $jsonZip -DestinationPath "$env:TEMP\json-extract" -Force

        if ($jdkPath) {
            Write-Output "Compiling JSON-java..."
            $javac = Join-Path $jdkPath "bin\javac"
            $jar = Join-Path $jdkPath "bin\jar"
            $jsonSrc = "$env:TEMP\json-extract\JSON-java-20250107\src\main\java"
            $jsonClasses = "$env:TEMP\json-classes"
            New-Item -ItemType Directory -Path $jsonClasses -Force | Out-Null
            & $javac -d $jsonClasses "$jsonSrc\org\json\*.java"
            & $jar cf $jsonJar -C $jsonClasses .
            Remove-Item $jsonClasses -Recurse -Force -ErrorAction SilentlyContinue
            Write-Output "JSON: OK (compiled from source)"
        } else {
            Write-Output "JDK not found. JSON source downloaded but not compiled."
            Write-Output "To compile manually: javac -d classes src/main/java/org/json/*.java && jar cf lib/json-20250107.jar -C classes ."
        }
        Remove-Item -Path $jsonZip -Force -ErrorAction SilentlyContinue
        Remove-Item -Path "$env:TEMP\json-extract" -Recurse -Force -ErrorAction SilentlyContinue
    } catch {
        Write-Output "JSON download failed: $($_.Exception.Message)"
    }
} else {
    Write-Output "JSON: already present"
}

Write-Output ""
Write-Output "=== Setup complete ==="
Write-Output "Dependencies in: $libDir"
Write-Output ""
Write-Output "Next steps:"
Write-Output "  1. Copy SFS2X jars into MSMSandbox/lib/"
Write-Output "  2. Copy config.example.properties to config.properties and edit"
Write-Output "  3. Build: gradlew build"
Write-Output ""
