<#
.SYNOPSIS
    Script generico de compilacion y despliegue para plugins de Minecraft (Paper/Spigot 1.21.x).
.DESCRIPTION
    Permite compilar cualquier plugin seleccionandolo de un menu interactivo o por comando.
    La primera vez que se ejecuta, solicita configurar las rutas de la carpeta de plugins y del
    servidor de Minecraft, guardandolas en 'build_config.json'.
.PARAMETER Plugin
    Nombre o parte del nombre de la carpeta del plugin a compilar (o 'all' / 'todos').
.PARAMETER All
    Compila todos los plugins disponibles secuencialmente.
.PARAMETER Configure
    Abre el asistente interactivo para reconfigurar las rutas de plugins y servidor.
.PARAMETER ForceConfig
    Sobrescribe los archivos de configuracion (.yml) existentes en el servidor.
.PARAMETER ServerPath
    Sobrescribe temporalmente la ruta del servidor para esta ejecucion.
.PARAMETER PluginsRoot
    Sobrescribe temporalmente la ruta de plugins para esta ejecucion.
.EXAMPLE
    .\build.ps1
    Ejecuta el menu interactivo (pide configurar las carpetas la primera vez).
.EXAMPLE
    .\build.ps1 -Configure
    Permite cambiar las carpetas de plugins o del servidor guardadas.
.EXAMPLE
    .\build.ps1 Roostergenics
    Compila directamente el plugin Roostergenics.
.EXAMPLE
    .\build.ps1 -All
    Compila todos los plugins en orden.
#>

param(
    [Parameter(Position = 0, Mandatory = $false, HelpMessage = "Nombre o carpeta del plugin a compilar")]
    [string]$Plugin,

    [Parameter(Mandatory = $false)]
    [switch]$All,

    [Parameter(Mandatory = $false)]
    [switch]$Configure,

    [Parameter(Mandatory = $false)]
    [switch]$ForceConfig,

    [Parameter(Mandatory = $false)]
    [string]$ServerPath = "",

    [Parameter(Mandatory = $false)]
    [string]$PluginsRoot = ""
)

$ErrorActionPreference = "Stop"

# Objeto FSO para convertir rutas con espacios a formato 8.3 compatible con javac
$global:fso = New-Object -ComObject Scripting.FileSystemObject

function Get-SafePath([string]$path) {
    if ($path -match '\s' -and (Test-Path -LiteralPath $path)) {
        try {
            if (Test-Path -LiteralPath $path -PathType Leaf) {
                return $global:fso.GetFile($path).ShortPath
            } elseif (Test-Path -LiteralPath $path -PathType Container) {
                return $global:fso.GetFolder($path).ShortPath
            }
        } catch {
            return $path
        }
    }
    return $path
}

# Dialogo de seleccion de carpeta en Windows
function Select-FolderDialog([string]$title, [string]$initialPath = "") {
    Add-Type -AssemblyName System.Windows.Forms
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = $title
    $dialog.ShowNewFolderButton = $true
    if ($initialPath -and (Test-Path $initialPath)) {
        $dialog.SelectedPath = $initialPath
    }
    $result = $dialog.ShowDialog()
    if ($result -eq [System.Windows.Forms.DialogResult]::OK -and -not [string]::IsNullOrWhiteSpace($dialog.SelectedPath)) {
        return $dialog.SelectedPath
    }
    return $null
}

# Ubicacion del archivo de configuracion (procura guardarlo junto al script o en la raiz de plugins)
function Get-ConfigFilePath {
    $scriptDir = $PSScriptRoot
    if (-not $scriptDir) {
        $scriptDir = (Get-Location).Path
    }
    # Si se ejecuta dentro de una subcarpeta de un plugin, buscar en la carpeta superior
    if (Test-Path (Join-Path $scriptDir "src\main\java")) {
        $parent = Split-Path $scriptDir -Parent
        if ($parent -and (Test-Path $parent)) {
            $parentConfig = Join-Path $parent "build_config.json"
            if (Test-Path $parentConfig) {
                return $parentConfig
            }
            return (Join-Path $parent "build_config.json")
        }
    }
    return (Join-Path $scriptDir "build_config.json")
}

function Save-Configuration([string]$filePath, [string]$pluginsPath, [string]$serverPath) {
    $cfg = [PSCustomObject]@{
        PluginsRoot = $pluginsPath
        ServerPath  = $serverPath
    }
    $json = $cfg | ConvertTo-Json -Depth 2
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($filePath, $json, $utf8NoBom)
}

function Load-Configuration([string]$filePath) {
    if (Test-Path $filePath) {
        try {
            $json = Get-Content -Path $filePath -Raw -Encoding UTF8
            return ($json | ConvertFrom-Json)
        } catch {
            Write-Warning "No se pudo leer el archivo de configuracion en: $filePath"
        }
    }
    return $null
}

function Prompt-Path([string]$title, [string]$dialogTitle, [string]$initialBrowsePath = "") {
    Write-Host "`n$title" -ForegroundColor Cyan
    Write-Host "  -> Escribe o pega la ruta, o presiona [F] o [ENTER] para abrir el explorador." -ForegroundColor Gray

    $val = Read-Host "Ruta"
    $trimmed = if ($val) { $val.Trim().Trim('"').Trim("'") } else { "" }

    # Si presiono Enter vacio o 'F', abrir el explorador de Windows
    if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.ToUpper() -eq "F") {
        Write-Host "  Abriendo ventana de seleccion de carpeta..." -ForegroundColor DarkGray
        $chosen = Select-FolderDialog -title $dialogTitle -initialPath $initialBrowsePath
        if ($chosen) {
            Write-Host "  Carpeta seleccionada: $chosen" -ForegroundColor Green
            return $chosen
        } else {
            Write-Host "  No se selecciono ninguna carpeta." -ForegroundColor Yellow
            return ""
        }
    }
    return $trimmed
}

function Setup-Environment([string]$configFile, [bool]$forceSetup) {
    $existing = Load-Configuration $configFile

    if ($existing -and -not $forceSetup) {
        if ((Test-Path $existing.PluginsRoot) -and (Test-Path $existing.ServerPath)) {
            return $existing
        }
        Write-Host "`n[!] Una o mas rutas guardadas en build_config.json no son validas o cambiaron de ubicacion." -ForegroundColor Yellow
    }

    Write-Host "`n==========================================================" -ForegroundColor Cyan
    Write-Host "      CONFIGURACION DEL ENTORNO DE COMPILACION (MC 1.21)  " -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host " Configura las carpetas de trabajo para compilar y desplegar." -ForegroundColor White
    Write-Host " Esta configuracion se guardara en: build_config.json" -ForegroundColor DarkGray
    Write-Host " (Podras cambiarla en cualquier momento desde el menu con la opcion [C])" -ForegroundColor DarkGray
    Write-Host "----------------------------------------------------------" -ForegroundColor Cyan

    $initialBrowse = if ($PSScriptRoot -and (Test-Path $PSScriptRoot)) { $PSScriptRoot } else { (Get-Location).Path }

    # 1. Configurar carpeta de plugins
    $chosenPlugins = ""
    while ([string]::IsNullOrWhiteSpace($chosenPlugins) -or -not (Test-Path $chosenPlugins)) {
        $chosenPlugins = Prompt-Path `
            -title "1. Carpeta raiz donde se encuentran todos tus proyectos de PLUGINS:" `
            -dialogTitle "Selecciona la carpeta principal de PLUGINS" `
            -initialBrowsePath $initialBrowse

        if ([string]::IsNullOrWhiteSpace($chosenPlugins)) {
            Write-Host "  Debes indicar una carpeta de plugins para continuar." -ForegroundColor Red
            continue
        }

        if (-not (Test-Path $chosenPlugins)) {
            Write-Host "La ruta '$chosenPlugins' no existe." -ForegroundColor Yellow
            $resp = Read-Host "Deseas crear esta carpeta ahora? (S/N) o 'F' para elegir otra"
            if ($resp.ToUpper() -eq "S") {
                New-Item -ItemType Directory -Force -Path $chosenPlugins | Out-Null
                break
            } elseif ($resp.ToUpper() -eq "F") {
                $chosenPlugins = Select-FolderDialog "Selecciona la carpeta principal de PLUGINS" $initialBrowse
            }
        }
    }

    # 2. Configurar carpeta del servidor
    $chosenServer = ""
    while ([string]::IsNullOrWhiteSpace($chosenServer) -or -not (Test-Path $chosenServer)) {
        $chosenServer = Prompt-Path `
            -title "2. Carpeta raiz de tu SERVIDOR de Minecraft (donde estan 'plugins', 'versions', etc.):" `
            -dialogTitle "Selecciona la carpeta raiz de tu SERVIDOR de Minecraft" `
            -initialBrowsePath $chosenPlugins

        if ([string]::IsNullOrWhiteSpace($chosenServer)) {
            Write-Host "  Debes indicar la carpeta del servidor para que el script pueda desplegar los plugins." -ForegroundColor Red
            continue
        }

        if (-not (Test-Path $chosenServer)) {
            Write-Host "La ruta del servidor '$chosenServer' no existe." -ForegroundColor Yellow
            $resp = Read-Host "Deseas continuar con esta ruta de todas formas? (S/N) o 'F' para elegir otra"
            if ($resp.ToUpper() -eq "S") {
                break
            } elseif ($resp.ToUpper() -eq "F") {
                $chosenServer = Select-FolderDialog "Selecciona la carpeta raiz de tu servidor" $chosenPlugins
            }
        }
    }

    Save-Configuration -filePath $configFile -pluginsPath $chosenPlugins -serverPath $chosenServer

    Write-Host "`n[v] Configuracion guardada exitosamente en:" -ForegroundColor Green
    Write-Host "    $configFile" -ForegroundColor Gray
    Start-Sleep -Milliseconds 600

    return [PSCustomObject]@{
        PluginsRoot = $chosenPlugins
        ServerPath  = $chosenServer
    }
}

# --- Cargar o Configurar Entorno ---
$configFile = Get-ConfigFilePath
$envConfig = Setup-Environment -configFile $configFile -forceSetup $Configure

$activePluginsRoot = if ($PluginsRoot) { $PluginsRoot } else { $envConfig.PluginsRoot }
$activeServerPath  = if ($ServerPath)  { $ServerPath }  else { $envConfig.ServerPath }

# Funcion para verificar si un directorio es un proyecto de plugin
function Test-IsPluginProject([System.IO.DirectoryInfo]$dir) {
    if ($dir.Name.StartsWith(".")) { return $false }
    $javaDir = Join-Path $dir.FullName "src\main\java"
    $pomFile = Join-Path $dir.FullName "pom.xml"
    $pluginYml = Join-Path $dir.FullName "src\main\resources\plugin.yml"
    return (Test-Path $javaDir) -or (Test-Path $pomFile) -or (Test-Path $pluginYml)
}

# Obtener lista de plugins disponibles ordenados
function Get-AvailablePlugins([string]$root) {
    if (-not (Test-Path $root)) { return @() }
    return @(Get-ChildItem -Path $root -Directory | Where-Object { Test-IsPluginProject $_ } | Sort-Object Name)
}

# Funcion para compilar un unico plugin
function Build-SinglePlugin([System.IO.DirectoryInfo]$pluginDir, [string]$serverRoot, [bool]$overwriteConfig) {
    $originalLocation = Get-Location
    try {
        Set-Location $pluginDir.FullName

        Write-Host "`n========================================================" -ForegroundColor Cyan
        Write-Host " Iniciando compilacion de: $($pluginDir.Name)" -ForegroundColor Cyan
        Write-Host " Directorio: $($pluginDir.FullName)" -ForegroundColor DarkGray
        Write-Host "========================================================" -ForegroundColor Cyan

        # 1. Obtener nombre y version del plugin desde plugin.yml
        $pluginName = $pluginDir.Name
        $pluginVersion = "1.0.0"
        $pluginYmlPath = Join-Path $pluginDir.FullName "src\main\resources\plugin.yml"

        if (Test-Path $pluginYmlPath) {
            $ymlContent = Get-Content -Path $pluginYmlPath -Raw -Encoding UTF8
            if ($ymlContent -match '(?m)^name:\s*["'']?([^"''\r\n#]+)["'']?') {
                $pluginName = $matches[1].Trim()
            }
            if ($ymlContent -match '(?m)^version:\s*["'']?([^"''\r\n#]+)["'']?') {
                $pluginVersion = $matches[1].Trim()
            }
        }

        Write-Host " [i] Plugin detectado: " -NoNewline
        Write-Host "$pluginName (v$pluginVersion)" -ForegroundColor Yellow

        # 2. Localizar archivos fuente .java
        $javaSrcPath = Join-Path $pluginDir.FullName "src\main\java"
        if (-not (Test-Path $javaSrcPath)) {
            Write-Error "No se encontro la carpeta de codigo: $javaSrcPath"
            return $false
        }

        $javaFiles = (Get-ChildItem -Path $javaSrcPath -Recurse -Filter "*.java" -ErrorAction SilentlyContinue).FullName
        if (-not $javaFiles -or $javaFiles.Count -eq 0) {
            Write-Warning "No se encontraron archivos .java en $javaSrcPath. Omitiendo..."
            return $false
        }
        Write-Host " [i] Total de archivos Java: $($javaFiles.Count)" -ForegroundColor Gray

        # 3. Construir Classpath (Paper API, Cache Mojang, Bibliotecas y otros Plugins)
        $serverApiJar = @(
            (Get-ChildItem -Path "$serverRoot\versions\*\*.jar" -ErrorAction SilentlyContinue).FullName
        ) + @(
            (Get-ChildItem -Path "$serverRoot\cache\mojang_*.jar" -ErrorAction SilentlyContinue).FullName
        ) + @(
            (Get-ChildItem -Path "$serverRoot\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "*paper*" -or $_.Name -like "*spigot*" -or $_.Name -like "*purpur*" }).FullName
        )
        $serverJars = (Get-ChildItem -Path "$serverRoot\libraries" -Recurse -Filter "*.jar" -ErrorAction SilentlyContinue).FullName | Sort-Object -Descending

        # Otros plugins en el servidor (para resolver dependencias cruzadas entre plugins)
        $otherPluginsJars = (Get-ChildItem -Path "$serverRoot\plugins\*.jar" -ErrorAction SilentlyContinue | Where-Object { 
            $_.BaseName -notlike "$pluginName*" -and $_.BaseName -notlike "$($pluginDir.Name)*"
        }).FullName

        $allJars = @($serverApiJar) + $serverJars + @($otherPluginsJars)
        $safeJars = $allJars | ForEach-Object { Get-SafePath $_ }
        $cp = ($safeJars | Select-Object -Unique) -join ";"

        # 4. Limpiar y recrear carpeta build/classes
        if (Test-Path "build") {
            Remove-Item -Recurse -Force "build"
        }
        New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null

        # 5. Generar archivo de argumentos para javac en UTF-8 sin BOM
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        $compileArgsPath = Join-Path $PWD.Path "compile_args.txt"
        $lines = @("--release", "21", "-encoding", "UTF-8", "-cp", $cp, "-d", "build/classes") + $javaFiles
        [System.IO.File]::WriteAllLines($compileArgsPath, $lines, $utf8NoBom)

        Write-Host " [*] Compilando con javac (Java 21)..." -ForegroundColor Gray
        & javac "@$compileArgsPath"
        $javacStatus = $LASTEXITCODE

        Remove-Item $compileArgsPath -Force -ErrorAction SilentlyContinue

        if ($javacStatus -ne 0) {
            Write-Error "Fallo la compilacion de $pluginName con codigo de salida $javacStatus"
            return $false
        }

        Write-Host " [v] Compilacion exitosa! Empaquetando JAR..." -ForegroundColor Green

        # 6. Copiar recursos a build/classes
        $resourcesPath = Join-Path $pluginDir.FullName "src\main\resources"
        if (Test-Path $resourcesPath) {
            Copy-Item -Path "$resourcesPath\*" -Destination "build\classes" -Recurse -Force
        }

        # 7. Crear JAR
        $jarFile = Join-Path $PWD.Path "$pluginName-$pluginVersion.jar"
        & jar --create --file="$jarFile" -C "build/classes" .
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Error empaquetando JAR con la herramienta 'jar'."
            return $false
        }

        # 8. Desplegar al servidor
        $pluginsServerDir = Join-Path $serverRoot "plugins"
        if (-not (Test-Path $pluginsServerDir)) {
            New-Item -ItemType Directory -Force -Path $pluginsServerDir | Out-Null
        }

        $targetServerJar = Join-Path $pluginsServerDir "$pluginName-$pluginVersion.jar"
        Copy-Item $jarFile $targetServerJar -Force
        Write-Host " [v] Desplegado exitosamente en: $targetServerJar" -ForegroundColor Green

        # 9. Gestionar carpeta de configuracion en el servidor (excluyendo descriptores internos)
        $serverConfigDir = Join-Path $pluginsServerDir $pluginName
        if (-not (Test-Path $serverConfigDir)) {
            New-Item -ItemType Directory -Force -Path $serverConfigDir | Out-Null
        }

        if (Test-Path $resourcesPath) {
            $ymlFiles = Get-ChildItem -Path $resourcesPath -Filter "*.yml" -ErrorAction SilentlyContinue | Where-Object { 
                $_.Name -notin @("plugin.yml", "paper-plugin.yml") 
            }
            foreach ($yml in $ymlFiles) {
                $targetConfig = Join-Path $serverConfigDir $yml.Name
                if (-not (Test-Path $targetConfig)) {
                    Copy-Item $yml.FullName $targetConfig -Force
                    Write-Host "   [+] Configuracion inicial creada: $($yml.Name)" -ForegroundColor Yellow
                } elseif ($overwriteConfig) {
                    Copy-Item $yml.FullName $targetConfig -Force
                    Write-Host "   [!] Configuracion sobrescrita (-ForceConfig): $($yml.Name)" -ForegroundColor Cyan
                } else {
                    Write-Host "   [=] Configuracion existente preservada: $($yml.Name)" -ForegroundColor DarkGray
                }
            }
        }

        Write-Host " [OK] Proceso completado con exito para: $pluginName`n" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Error "Error durante la compilacion de $($pluginDir.Name): $_"
        return $false
    }
    finally {
        Set-Location $originalLocation
    }
}

# --- Flujo de Seleccion y Ejecucion ---

$availablePlugins = @(Get-AvailablePlugins -root $activePluginsRoot)
$targetsToBuild = @()

if ($All -or ($Plugin -and $Plugin.ToLower() -in @("all", "todos", "*"))) {
    $targetsToBuild = $availablePlugins
}
elseif ($Plugin) {
    if (Test-Path $Plugin) {
        $customDir = Get-Item $Plugin
        $targetsToBuild = @($customDir)
    } else {
        $matched = $availablePlugins | Where-Object { 
            $_.Name -eq $Plugin -or $_.Name -like "*$Plugin*"
        }
        if (-not $matched) {
            Write-Host "No se encontro ningun plugin que coincida con '$Plugin'." -ForegroundColor Red
            Write-Host "Plugins disponibles en $($activePluginsRoot):" -ForegroundColor Yellow
            $availablePlugins | ForEach-Object { Write-Host " - $($_.Name)" }
            exit 1
        }
        $targetsToBuild = @($matched)
    }
}
else {
    # Bucle del Menu Interactivo
    while ($true) {
        Write-Host "`n==========================================================" -ForegroundColor Cyan
        Write-Host "      SELECTOR DE COMPILACION DE PLUGINS (MC 1.21)        " -ForegroundColor Yellow
        Write-Host "==========================================================" -ForegroundColor Cyan
        Write-Host " Directorio de Plugins: $activePluginsRoot" -ForegroundColor DarkGray
        Write-Host " Directorio Servidor:   $activeServerPath" -ForegroundColor DarkGray
        Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
        Write-Host " Plugins detectados:" -ForegroundColor White

        $availablePlugins = @(Get-AvailablePlugins -root $activePluginsRoot)

        for ($i = 0; $i -lt $availablePlugins.Count; $i++) {
            $p = $availablePlugins[$i]
            $pNum = ($i + 1).ToString().PadLeft(2)
            Write-Host "   [$pNum] " -NoNewline -ForegroundColor Green
            Write-Host "$($p.Name)" -ForegroundColor White
        }

        Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
        Write-Host "   [ A] " -NoNewline -ForegroundColor Magenta
        Write-Host "Compilar TODOS los plugins" -ForegroundColor White
        Write-Host "   [ F] " -NoNewline -ForegroundColor Yellow
        Write-Host "Elegir carpeta especifica con Explorador de Windows" -ForegroundColor White
        Write-Host "   [ C] " -NoNewline -ForegroundColor Cyan
        Write-Host "Cambiar configuracion de carpetas (Plugins / Servidor)" -ForegroundColor White
        Write-Host "   [ Q] " -NoNewline -ForegroundColor Red
        Write-Host "Salir" -ForegroundColor White
        Write-Host "==========================================================" -ForegroundColor Cyan

        $selection = Read-Host "Elige una opcion (numero, nombre, 'A', 'F', 'C' o 'Q')"
        if ([string]::IsNullOrWhiteSpace($selection) -or $selection.Trim().ToUpper() -eq "Q") {
            Write-Host "Operacion cancelada." -ForegroundColor Yellow
            exit 0
        }

        $trimmed = $selection.Trim()

        if ($trimmed.ToUpper() -eq "C") {
            $envConfig = Setup-Environment -configFile $configFile -forceSetup $true
            $activePluginsRoot = $envConfig.PluginsRoot
            $activeServerPath  = $envConfig.ServerPath
            continue
        }
        elseif ($trimmed.ToUpper() -eq "A") {
            $targetsToBuild = $availablePlugins
            break
        }
        elseif ($trimmed.ToUpper() -eq "F") {
            $chosen = Select-FolderDialog "Selecciona la carpeta del plugin que deseas compilar" $activePluginsRoot
            if ($chosen -and (Test-Path $chosen)) {
                $targetsToBuild = @(Get-Item $chosen)
                break
            } else {
                Write-Host "No se selecciono ninguna carpeta." -ForegroundColor Yellow
                continue
            }
        }
        else {
            $parts = $trimmed -split "[,;\s]+"
            foreach ($part in $parts) {
                if ($part -match '^\d+$') {
                    $idx = [int]$part - 1
                    if ($idx -ge 0 -and $idx -lt $availablePlugins.Count) {
                        $targetsToBuild += $availablePlugins[$idx]
                    } else {
                        Write-Warning "Numero fuera de rango: $part"
                    }
                } else {
                    $found = $availablePlugins | Where-Object { $_.Name -like "*$part*" }
                    if ($found) {
                        $targetsToBuild += $found
                    } else {
                        Write-Warning "No se encontro plugin para: $part"
                    }
                }
            }
            if ($targetsToBuild.Count -gt 0) {
                break
            }
        }
    }
}

$targetsToBuild = $targetsToBuild | Select-Object -Unique

if ($targetsToBuild.Count -eq 0) {
    Write-Host "No se selecciono ningun plugin para compilar." -ForegroundColor Yellow
    exit 0
}

Write-Host "`nPlugins listos para compilar ($($targetsToBuild.Count)):" -ForegroundColor Cyan
$targetsToBuild | ForEach-Object { Write-Host " -> $($_.Name)" -ForegroundColor Yellow }

$successCount = 0
$failCount = 0

foreach ($target in $targetsToBuild) {
    $ok = Build-SinglePlugin -pluginDir $target -serverRoot $activeServerPath -overwriteConfig $ForceConfig
    if ($ok) { $successCount++ } else { $failCount++ }
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host " Resumen de Compilacion:" -ForegroundColor Cyan
Write-Host "  Exito:  $successCount" -ForegroundColor Green
if ($failCount -gt 0) {
    Write-Host "  Fallos: $failCount" -ForegroundColor Red
}
Write-Host "========================================================" -ForegroundColor Cyan
