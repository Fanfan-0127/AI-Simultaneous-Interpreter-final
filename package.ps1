# AI Simultaneous Interpreter — Windows packaging script
# Requires: JDK 21 with jpackage (C:\JAVA\jdk-21.0.8)
# Output:   dist\AI-Simultaneous-Interpreter\AI-Simultaneous-Interpreter.exe

param(
    [switch]$Rebuild,
    [switch]$SkipTests,
    [ValidateSet("app-image","exe")]
    [string]$Type = "app-image"
)

$ErrorActionPreference = "Stop"
# Suppress JAVA_TOOL_OPTIONS banner on stderr (set globally on this machine)
$env:JAVA_TOOL_OPTIONS = ""
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$JDK_HOME = "C:\JAVA\jdk-21.0.8"
$JPACKAGE = "$JDK_HOME\bin\jpackage.exe"
$JAVA    = "$JDK_HOME\bin\java.exe"
$JAVAC   = "$JDK_HOME\bin\javac.exe"
$JAR     = "target\ai-simultaneous-interpreter-final-0.1.0-SNAPSHOT.jar"
$ICON    = "package\windows\icon.ico"
$DIST    = "dist"

# ════════════════════════════════════════════════
# Phase 1: Pre-flight checks
# ════════════════════════════════════════════════

Write-Host "=== AI Simultaneous Interpreter Packaging ===" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $JPACKAGE)) {
    Write-Host "ERROR: jpackage not found at $JPACKAGE" -ForegroundColor Red
    Write-Host "Expected JDK 21 at $JDK_HOME"
    exit 1
}
Write-Host "[OK] jpackage found: $JPACKAGE" -ForegroundColor Green

# ════════════════════════════════════════════════
# Phase 2: Generate icon
# ════════════════════════════════════════════════

if (-not (Test-Path $ICON) -or $Rebuild) {
    Write-Host ""
    Write-Host "--- Generating app icon ---" -ForegroundColor Yellow

    $iconGenSrc = "package\IconGenerator.java"
    if (-not (Test-Path $iconGenSrc)) {
        Write-Host "ERROR: $iconGenSrc not found" -ForegroundColor Red
        exit 1
    }

    # Compile
    $compileOut = & $JAVAC --release 21 -d target\classes-icon $iconGenSrc 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host $compileOut -ForegroundColor Red
        Write-Host "ERROR: IconGenerator compilation failed" -ForegroundColor Red
        exit 1
    }

    # Run (working dir = project root, writes package/windows/icon.png)
    $runOut = & $JAVA -cp target\classes-icon IconGenerator 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host $runOut -ForegroundColor Red
        Write-Host "ERROR: IconGenerator failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Icon generated: $ICON" -ForegroundColor Green
}

# ════════════════════════════════════════════════
# Phase 3: Build fat JAR
# ════════════════════════════════════════════════

if (-not (Test-Path $JAR) -or $Rebuild) {
    Write-Host ""
    Write-Host "--- Building fat JAR ---" -ForegroundColor Yellow

    $mvnArgs = "package"
    if ($SkipTests) { $mvnArgs += " -DskipTests" }
    mvn $mvnArgs.Split(" ") 2>&1 | Write-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Maven build failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Fat JAR built: $JAR" -ForegroundColor Green
} else {
    Write-Host "[OK] Fat JAR exists: $JAR" -ForegroundColor Green
}

# ════════════════════════════════════════════════
# Phase 4: Stage JAR (input dir must contain ONLY the app JAR)
# ════════════════════════════════════════════════

$STAGE = "target\stage"
if (Test-Path $STAGE) { Remove-Item -Recurse -Force $STAGE }
New-Item -ItemType Directory -Path $STAGE -Force | Out-Null
Copy-Item $JAR $STAGE

# ════════════════════════════════════════════════
# Phase 5: jpackage
# ════════════════════════════════════════════════

Write-Host ""
Write-Host "--- Running jpackage ($Type) ---" -ForegroundColor Yellow

# Clean previous output
if (Test-Path $DIST) {
    Remove-Item -Recurse -Force $DIST
    Write-Host "Cleaned previous dist/"
}

$iconArg = if (Test-Path $ICON) { @("--icon", $ICON) } else { @() }

$jpackageArgs = @(
    "--type", $Type,
    "--name", "AI-Simultaneous-Interpreter",
    "--app-version", "0.1.0",
    "--vendor", "Fanfan Studio",
    "--description", "AI Simultaneous Interpreter - Real-time Translation Subtitles",
    "--input", $STAGE,
    "--main-jar", (Split-Path -Leaf $JAR),
    "--main-class", "com.fanfan.interpreter.App",
    "--dest", $DIST,
    "--java-options", "-Xmx512m",
    "--java-options", "-Dfile.encoding=UTF-8"
) + $iconArg

& $JPACKAGE $jpackageArgs 2>&1 | Write-Host
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: jpackage failed" -ForegroundColor Red
    exit 1
}

# ════════════════════════════════════════════════
# Done
# ════════════════════════════════════════════════

$appExe = "$DIST\AI-Simultaneous-Interpreter\AI-Simultaneous-Interpreter.exe"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Packaging complete!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Output: $appExe" -ForegroundColor White
Write-Host ""
Write-Host "  Double-click to launch, or run:" -ForegroundColor Gray
Write-Host "  .\$appExe" -ForegroundColor Gray
Write-Host ""
