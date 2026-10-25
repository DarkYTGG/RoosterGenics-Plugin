# 🐓 RoosterGenics - Plugin de Minecraft

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Folia-blue.svg)](https://papermc.io/)
[![Version](https://img.shields.io/badge/Minecraft-1.21.11-green.svg)](https://papermc.io/)
[![Architecture](https://img.shields.io/badge/Threading-Folia--Aware-brightgreen.svg)]()

**RoosterGenics** es un plugin integral y avanzado para Minecraft (Paper / Purpur / Folia 1.21.11) que introduce un sistema completo de **Gallos de Combate con Genética Profunda**, crianza con herencia alélica mendeliana, entrenamientos, arenas de combate (palenques) 1v1 con apuestas en tiempo real, gimnasios con líderes NPC y compatibilidad nativa multihilo.

---

## 🌟 Características Principales

### 🧬 Genética Profunda (RoosterGenics)
- **Sistema Alélico Real**: Genes dominantes y recesivos que determinan tanto estadísticas de combate como fenotipos visuales.
- **Estadísticas de Combate Heredables**: Fuerza, Velocidad, Resistencia, Agresividad y Precisión.
- **Árbol Genealógico y Linaje**: Trazabilidad completa de progenitores y generaciones en cada gallo.

### 🐣 Crianza e Incubación
- **Emparejamiento Genético**: Combina reproductores seleccionados evaluando compatibilidad y probabilidad de mutaciones genéticas.
- **Incubación y Cuidado**: Proceso de eclosión con cuidados especiales para maximizar los atributos del polluelo.

### ⚔️ Duelos 1v1, Arenas y Palenques
- **Arenas Protegidas (Palenques)**: Generación y protección estricta contra griefing e interferencia externa durante el combate.
- **Aislamiento Concurrente de Sesiones**: Motor de combate determinista basado en leases de ejecución física, garantizando cero desincronizaciones (`CRIT-02`).
- **Sistema de Apuestas (Spectator Betting)**: Liquidación atómica y segura de apuestas para espectadores durante las peleas.

### 🥊 Gimnasios y Líderes NPC
- **Líderes de Gimnasio**: Desafíos con dificultad escalonada y recompensas exclusivas.
- **Entrenadores Adaptativos**: NPCs que ajustan sus tácticas y gallos al nivel del jugador.
- **Gestión de Entidades Robusta**: Sistema de reconciliación de entidades y claims (`CRIT-04`) inmune a reinicios, unloads de chunks y cambios de dimensión.

### 🏥 Centro de Recuperación y Enfermería
- **Tratamiento de Lesiones**: Mecánica de salud, curaciones tras combates intensos con Enfermeras NPC.
- **Objetos Sostenibles (Held Items) y Bálsamos**: Equipamiento que potencia estadísticas y otorga habilidades pasivas especiales durante el combate.

### ⚡ Arquitectura Multihilo (Folia-Ready)
- Diseñado desde cero para entornos masivos (200-300+ jugadores).
- `TaskSchedulerAdapter` inteligente: usa `RegionScheduler` y `EntityScheduler` en Folia, y los programadores equivalentes en Paper.
- Cero accesos inseguros a la Bukkit API desde hilos asíncronos.

---

## 📦 Contenido del Repositorio

- **`src/main/java/`**: Código fuente modular y optimizado en Java 21.
- **`src/main/resources/`**: Configuraciones por defecto (`config.yml`, `plugin.yml`).
- **`resourcepack/`**: Modelos JSON, texturas personalizadas y configuraciones para Minecraft.
- **`Roostergenics-ResourcePack.zip`**: Pack de recursos listo para distribuir o cargar en el servidor.
- **`Roostergenics-1.0.0.jar`**: Artefacto binario precompilado listo para su despliegue en la carpeta `plugins/`.
- **`pom.xml`**: Definición de dependencias Maven (Paper API 1.21.11-R0.1-SNAPSHOT).
- **`build.ps1` / `run_tests.ps1`**: Scripts de compilación y suites de validación formal de concurrencia y persistencia.

---

## 🎮 Comandos y Permisos

### Comandos
| Comando Principal | Aliases | Descripción |
| :--- | :--- | :--- |
| `/gallos` | `/rg`, `/roostergenics`, `/peleagallos`, `/roosters`, `/palenque` | Abre el menú principal de RoosterGenics (corral, crianza, entrenamiento, duelos). |

### Permisos
| Permiso | Descripción | Por Defecto |
| :--- | :--- | :--- |
| `roostergenics.use` | Permite acceder a las funciones de jugador (corral, crianza, entrenar, combatir). | `true` (Todos) |
| `roostergenics.admin` | Permite acceso a comandos administrativos y de configuración. | `op` |

---

## ⚙️ Requisitos e Instalación

### Requisitos
- **Servidor**: Paper, Purpur o Folia `1.21.11` (o compatible).
- **Java**: Java 21 (LTS) o superior.

### Instalación
1. Descarga el archivo `Roostergenics-1.0.0.jar` desde los releases o la raíz de este repositorio.
2. Colócalo en la carpeta `plugins/` de tu servidor.
3. Coloca el pack de recursos `Roostergenics-ResourcePack.zip` en tu cliente o configúralo en `server.properties`.
4. Inicia o reinicia el servidor.
5. Edita `plugins/Roostergenics/config.yml` a tu preferencia.

---

## 🛠️ Compilación y Tests

Para compilar el proyecto manualmente o ejecutar la suite de verificación formal:

### Compilación rápida (PowerShell)
```powershell
.\build.ps1
```

### Ejecución de Pruebas Formales (Gates CRIT-02 & CRIT-04)
```powershell
.\run_tests.ps1
```

---

## 📄 Licencia

Desarrollado por **Dark** (@DarkYTGG). Todos los derechos reservados.
