# Install Crystal Reports JARs from lib/ into local Maven repository
# Usage:  .\install-libs.ps1
# After running this once, pom.xml no longer needs scope=system / systemPath

$ErrorActionPreference = 'Stop'
$libDir = Join-Path $PSScriptRoot 'lib'
$groupId = 'crystal-reports'
$repoRoot = Join-Path $env:USERPROFILE '.m2\repository\crystal-reports'

if (-not (Test-Path $libDir)) {
  Write-Error "lib directory not found at $libDir"
  exit 1
}

# Clean any stale .lastUpdated markers left by previous failed resolutions
if (Test-Path $repoRoot) {
  Write-Host "Cleaning stale markers under $repoRoot ..." -ForegroundColor DarkGray
  Get-ChildItem $repoRoot -Recurse -Filter '*.lastUpdated' -File |
    ForEach-Object { Remove-Item $_.FullName -Force }
}

# (artifactId, version, jarFileName)
$libs = @(
  @{ name = 'com.azalea.ufl.barcode';     version = '1.0';              jar = 'com.azalea.ufl.barcode.1.0.jar' },
  @{ name = 'CrystalCommon2';             version = '12.2.233.5802';    jar = 'CrystalCommon2.jar' },
  @{ name = 'CrystalReportsRuntime';      version = '12.2.233.5802';    jar = 'CrystalReportsRuntime.jar' },
  @{ name = 'cvom';                       version = '12.2.233.5802';    jar = 'cvom.jar' },
  @{ name = 'DatabaseConnectors';         version = '12.2.233.5802';    jar = 'DatabaseConnectors.jar' },
  @{ name = 'JDBInterface';               version = '12.2.233.5802';    jar = 'JDBInterface.jar' },
  @{ name = 'keycodeDecoder';             version = '12.2.233.5802';    jar = 'keycodeDecoder.jar' },
  @{ name = 'logging';                    version = '12.2.233.5802';    jar = 'logging.jar' },
  @{ name = 'pfjgraphics';                version = '12.2.233.5802';    jar = 'pfjgraphics.jar' },
  @{ name = 'QueryBuilder';               version = '12.2.233.5802';    jar = 'QueryBuilder.jar' }
)

Write-Host "Installing Crystal Reports JARs to local Maven repository..." -ForegroundColor Cyan
Write-Host "lib dir: $libDir"
Write-Host ""

$installed = 0
$skipped = 0

foreach ($lib in $libs) {
  $jarPath = Join-Path $libDir $lib.jar
  if (-not (Test-Path $jarPath)) {
    Write-Warning "JAR not found, skipping: $($lib.jar)"
    $skipped++
    continue
  }

  # Skip only if the actual installed jar exists in the local repo
  $installedJar = Join-Path $repoRoot "$($lib.name)\$($lib.version)\$($lib.name)-$($lib.version).jar"
  if (Test-Path $installedJar) {
    Write-Host "[SKIP] $($lib.name):$($lib.version) (already installed)" -ForegroundColor Yellow
    $skipped++
    continue
  }

  Write-Host "[INSTALL] $($lib.name):$($lib.version) <- $($lib.jar)" -ForegroundColor Green
  & "$PSScriptRoot\mvnw.cmd" -f "$PSScriptRoot\pom.xml" install:install-file `
    -Dfile="$jarPath" `
    -DgroupId="$groupId" `
    -DartifactId="$($lib.name)" `
    -Dversion="$($lib.version)" `
    -Dpackaging=jar `
    -DgeneratePom=true

  if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to install $($lib.jar) (exit code $LASTEXITCODE)"
    exit 1
  }
  $installed++
}

Write-Host ""
Write-Host "Done. Installed: $installed, Skipped: $skipped" -ForegroundColor Cyan
Write-Host "You can now build with:  .\mvnw.cmd clean package"
