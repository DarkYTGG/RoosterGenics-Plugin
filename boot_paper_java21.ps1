$ErrorActionPreference = "Stop"

$java21 = "C:\Users\Dark\.jdks\ms-21.0.9\bin\java.exe"
$serverDir = "C:\Users\Dark\Desktop\servermc2"
$latestLog = "$serverDir\logs\latest.log"

Write-Host "=== VERIFYING JAVA 21 EXECUTABLE ==="
Write-Host "Executable: $java21"
& $java21 -version

# Truncate or mark latest.log
$startPosition = 0
if (Test-Path $latestLog) {
    $startPosition = (Get-Item $latestLog).Length
}

Write-Host "`n=== STARTING PAPER 1.21.11-127 WITH JAVA 21 ==="
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $java21
$psi.Arguments = "-Dfile.encoding=UTF-8 -Dterminal.jline=false -Dterminal.ansi=false -Xms2G -Xmx4G -jar paper.jar --nogui --nojline"
$psi.WorkingDirectory = $serverDir
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $false
$psi.RedirectStandardError = $false
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true

$proc = [System.Diagnostics.Process]::Start($psi)
Write-Host "Proceso Paper iniciado con PID: $($proc.Id)"

$fs = $null
$reader = $null

# Wait up to 5 seconds for log file
$swInit = [System.Diagnostics.Stopwatch]::StartNew()
while ($swInit.Elapsed.TotalSeconds -lt 10) {
    if (Test-Path $latestLog) {
        try {
            $fs = New-Object System.IO.FileStream($latestLog, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $fs.Seek($startPosition, [System.IO.SeekOrigin]::Begin) | Out-Null
            $reader = New-Object System.IO.StreamReader($fs, [System.Text.Encoding]::UTF8)
            break
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
}

$booted = $false
$timeoutSeconds = 90
$sw = [System.Diagnostics.Stopwatch]::StartNew()

while ($sw.Elapsed.TotalSeconds -lt $timeoutSeconds -and -not $proc.HasExited) {
    if ($reader -ne $null) {
        while (-not $reader.EndOfStream) {
            $line = $reader.ReadLine()
            if ($line -ne $null) {
                Write-Host $line
                if ($line -match "Done \(" -or $line -match 'For help, type "help"') {
                    $booted = $true
                    break
                }
            }
        }
    }
    if ($booted) {
        Write-Host "`n>>> [DETECTADO]: Servidor completó arranque ('Done'). Enviando comando 'stop'... <<<"
        break
    }
    Start-Sleep -Milliseconds 500
}

# Enviar comando stop
try {
    $proc.StandardInput.WriteLine("stop")
    $proc.StandardInput.Flush()
    $proc.StandardInput.Close()
} catch {
    Write-Warning "No se pudo enviar 'stop': $_"
}

Write-Host "`nEsperando finalización limpia del servidor (hasta 45s)..."
$cleanExit = $proc.WaitForExit(45000)

if ($reader -ne $null) {
    while (-not $reader.EndOfStream) {
        $line = $reader.ReadLine()
        if ($line -ne $null) {
            Write-Host $line
        }
    }
    $reader.Close()
    $fs.Close()
}

if (-not $cleanExit) {
    Write-Warning "El servidor no finalizó limpiamente. Forzando terminación..."
    try { $proc.Kill() } catch {}
}

Write-Host "`n=== PROCESO FINALIZADO. EXIT CODE: $($proc.ExitCode) ==="
Write-Host "Boot status detectado: $booted"
