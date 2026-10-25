$ErrorActionPreference = "Stop"

$java21Home = "C:\Users\Dark\.jdks\ms-21.0.9"
$javac21 = "$java21Home\bin\javac.exe"
$java21 = "$java21Home\bin\java.exe"

if (-not (Test-Path $javac21)) {
    Write-Error "Java 21 javac no encontrado en: $javac21"
    exit 1
}

$paperApi11 = "C:\Users\Dark\Desktop\servermc2\libraries\io\papermc\paper\paper-api\1.21.11-R0.1-SNAPSHOT\paper-api-1.21.11-R0.1-SNAPSHOT.jar"
if (-not (Test-Path $paperApi11)) {
    Write-Error "Paper API 1.21.11 no encontrada en: $paperApi11"
    exit 1
}

$serverApiJar = @((Get-ChildItem -Path "C:\Users\Dark\Desktop\servermc2\versions\1.21.11\paper-1.21.11.jar" -ErrorAction SilentlyContinue).FullName) + @((Get-ChildItem -Path "C:\Users\Dark\Desktop\servermc2\cache\mojang_*.jar" -ErrorAction SilentlyContinue).FullName)
# Excluir paper-api-1.21.8 para garantizar coherencia
$serverJars = (Get-ChildItem -Path "C:\Users\Dark\Desktop\servermc2\libraries" -Recurse -Filter "*.jar").FullName | Where-Object { $_ -notlike "*paper-api-1.21.8*" }
$allJars = @($paperApi11) + @($serverApiJar) + $serverJars + @("build\classes")
$cp = ($allJars | Select-Object -Unique) -join ";"

if (Test-Path "build\test-classes") {
    Remove-Item -Recurse -Force "build\test-classes"
}
New-Item -ItemType Directory -Force -Path "build\test-classes" | Out-Null

$testFiles = (Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java").FullName
$lines = @("--release", "21", "-encoding", "UTF-8", "-cp", $cp, "-d", "build/test-classes") + $testFiles
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines("$PWD\test_compile_args.txt", $lines, $utf8NoBom)

Write-Host "=== TEST ENVIRONMENT ==="
Write-Host "Java 21 Executable: $java21"
& $java21 -version
Write-Host "Paper API 1.21.11: $paperApi11"

Write-Host "Compilando tests con javac 21 contra Paper API 1.21.11..."
& $javac21 "@$PWD\test_compile_args.txt"
$javacStatus = $LASTEXITCODE
Remove-Item "$PWD\test_compile_args.txt" -Force -ErrorAction SilentlyContinue

if ($javacStatus -ne 0) {
    Write-Error "Fallo la compilación de tests con código de salida $javacStatus"
    exit $javacStatus
}

$runCp = "build\test-classes;build\classes;" + $cp
Write-Host "Ejecutando StructurePersistenceTest (CRIT-04) con Java 21..."
& $java21 -cp $runCp com.dark.roostergenics.structure.StructurePersistenceTest
$runStatus = $LASTEXITCODE

if ($runStatus -ne 0) {
    Write-Error "StructurePersistenceTest fallo con código de salida $runStatus"
    exit $runStatus
}

Write-Host "`nEjecutando DuelArenaSessionTest (CRIT-02) con Java 21..."
& $java21 -cp $runCp com.dark.roostergenics.arena.DuelArenaSessionTest
$runStatus2 = $LASTEXITCODE

if ($runStatus2 -ne 0) {
    Write-Error "DuelArenaSessionTest fallo con código de salida $runStatus2"
    exit $runStatus2
}

