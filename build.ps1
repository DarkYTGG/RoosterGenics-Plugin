$ErrorActionPreference = "Stop"

$java21Home = "C:\Users\Dark\.jdks\ms-21.0.9"
$javac21 = "$java21Home\bin\javac.exe"
$jar21 = "$java21Home\bin\jar.exe"

if (-not (Test-Path $javac21)) {
    Write-Error "Java 21 javac no encontrado en: $javac21"
    exit 1
}

$paperApi11 = "C:\Users\Dark\Desktop\servermc2\libraries\io\papermc\paper\paper-api\1.21.11-R0.1-SNAPSHOT\paper-api-1.21.11-R0.1-SNAPSHOT.jar"
if (-not (Test-Path $paperApi11)) {
    Write-Error "Paper API 1.21.11 no encontrada en: $paperApi11"
    exit 1
}

$javaFiles = (Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java").FullName
$serverApiJar = @((Get-ChildItem -Path "C:\Users\Dark\Desktop\servermc2\versions\1.21.11\paper-1.21.11.jar" -ErrorAction SilentlyContinue).FullName) + @((Get-ChildItem -Path "C:\Users\Dark\Desktop\servermc2\cache\mojang_*.jar" -ErrorAction SilentlyContinue).FullName)
# Filtrar explícitamente paper-api-1.21.8 para garantizar alineación 100% con 1.21.11
$serverJars = (Get-ChildItem -Path "C:\Users\Dark\Desktop\servermc2\libraries" -Recurse -Filter "*.jar").FullName | Where-Object { $_ -notlike "*paper-api-1.21.8*" }
$allJars = @($paperApi11) + @($serverApiJar) + $serverJars
$cp = ($allJars | Select-Object -Unique) -join ";"

Write-Host "=== CONFIGURACION DE COMPILACION ==="
Write-Host "Java 21 Home: $java21Home"
& $javac21 -version
Write-Host "Paper API Target: $paperApi11"
Write-Host "Paper API SHA256: $((Get-FileHash -Path $paperApi11 -Algorithm SHA256).Hash)"

if (Test-Path "build") {
    Remove-Item -Recurse -Force "build"
}
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$lines = @("--release", "21", "-encoding", "UTF-8", "-cp", $cp, "-d", "build/classes") + $javaFiles
[System.IO.File]::WriteAllLines("$PWD\compile_args.txt", $lines, $utf8NoBom)

Write-Host "Compilando Roostergenics con javac 21 contra Paper API 1.21.11..."
& $javac21 "@$PWD\compile_args.txt"
$javacStatus = $LASTEXITCODE

Remove-Item "$PWD\compile_args.txt" -Force -ErrorAction SilentlyContinue

if ($javacStatus -eq 0) {
    Write-Host "Compilación exitosa! Empaquetando JAR..."
    Copy-Item -Path "src\main\resources\*" -Destination "build\classes" -Recurse -Force
    
    $jarFile = "$PWD\Roostergenics-1.0.0.jar"
    & $jar21 --create --file="$jarFile" -C "build/classes" .
    
    $targetServerJar = "C:\Users\Dark\Desktop\servermc2\plugins\Roostergenics-1.0.0.jar"
    Copy-Item $jarFile $targetServerJar -Force
    Write-Host "Desplegado exitosamente en: $targetServerJar"

    $serverConfigDir = "C:\Users\Dark\Desktop\servermc2\plugins\Roostergenics"
    if (-not (Test-Path $serverConfigDir)) {
        New-Item -ItemType Directory -Force -Path $serverConfigDir | Out-Null
    }
    if (-not (Test-Path "$serverConfigDir\config.yml")) {
        Copy-Item "src\main\resources\config.yml" "$serverConfigDir\config.yml" -Force
        Write-Host "Configuración inicial creada en: $serverConfigDir"
    } else {
        Write-Host "Configuración existente preservada en: $serverConfigDir"
    }
} else {
    Write-Error "Fallo la compilación con código de salida $javacStatus"
}
