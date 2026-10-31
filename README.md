<div align="center">

  # 🐓 RoosterGenics — Advanced Minecraft Plugin
  ### Sistema Integral de Gallos de Combate · Genética Mendeliana · Arenas 1v1 · Folia-Aware
  ### Advanced Fighting Roosters Plugin · Deep Genetics · 1v1 Arenas · Folia Multithreading

  <p align="center">
    <a href="#-versión-en-español"><b>🇪🇸 Versión en Español</b></a>
    &nbsp;•&nbsp;
    <a href="#-english-version"><b>🇬🇧 English Version</b></a>
  </p>

  <p align="center">
    <a href="https://adoptium.net/"><img src="https://img.shields.io/badge/Java-21_(LTS)-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"></a>
    <a href="https://papermc.io/"><img src="https://img.shields.io/badge/Platform-Paper_%7C_Purpur_%7C_Folia-02303A?style=for-the-badge" alt="Platform"></a>
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-10B981?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.21">
    <img src="https://img.shields.io/badge/Architecture-Folia--Aware-brightgreen?style=for-the-badge" alt="Folia-Aware">
  </p>

  <p align="center">
    <a href="https://darkytgg.github.io/Wiki-RoosterGenics/"><b>🌐 Documentación & Wiki Oficial / Official Wiki</b></a>
    &nbsp;•&nbsp;
    <a href="https://github.com/DarkYTGG/Wiki-RoosterGenics"><b>📖 Repositorio Wiki</b></a>
  </p>

</div>

---

<a id="-versión-en-español"></a>
## 🇪🇸 Versión en Español

### 🌟 Descripción General

**RoosterGenics** es un plugin integral y avanzado para servidores de Minecraft (**Paper / Purpur / Folia 1.21.1**) que introduce un sistema completo de **Gallos de Combate con Genética Profunda**, crianza con herencia alélica mendeliana, entrenamientos, arenas de combate (palenques) 1v1 con apuestas en tiempo real, gimnasios con líderes NPC y compatibilidad nativa multihilo.

---

### ✨ Características Principales

- 🧬 **Genética Profunda:** Genes dominantes y recesivos que determinan estadísticas de combate (Fuerza, Velocidad, Resistencia, Agresividad, Precisión), fenotipos de plumaje y árboles genealógicos completos.
- 🥚 **Crianza e Incubación:** Emparejamiento genético, coeficiente de consanguinidad (COI) y probabilidad de mutaciones genéticas o defectos por endogamia.
- ⚔️ **Duelos 1v1 y Palenques:** Arenas protegidas contra griefing con techo de vuelo de 4.5 bloques, emparejamiento determinista y sistema de apuestas atómicas para espectadores (`/rg apostar`).
- 🏛️ **6 Gimnasios Temáticos & Gran Liga:** Desafíos contra líderes NPC con dificultad progresiva, medallas coleccionables y recompensas de alto nivel.
- 🏥 **Centro Médico & Held Items:** Tratamiento de heridas, curaciones con la Enfermera del Palenque y 14 objetos sostenibles de combate (Banda Focus, Vidasfera, Chaleco Asalto, etc.).
- ⚡ **Arquitectura Multihilo (Folia-Ready):** Totalmente optimizado para servidores masivos con `TaskSchedulerAdapter`, interactuando de forma segura con `RegionScheduler` y `EntityScheduler`.

---

### 📦 Contenido del Repositorio

- `src/main/java/`: Código fuente modular y fuertemente tipado en Java 21.
- `src/main/resources/`: Archivos de configuración por defecto (`config.yml`, `plugin.yml`).
- `resourcepack/`: Modelos JSON 3D, texturas y sonidos personalizados.
- `Roostergenics-ResourcePack.zip`: Pack de recursos listo para clientes o `server.properties`.
- `Roostergenics-1.0.0.jar`: Binario precompilado listo para la carpeta `plugins/`.
- `pom.xml`: Definición de dependencias Maven (Paper API 1.21.1-R0.1-SNAPSHOT).

---

### ⌨️ Comandos y Permisos

| Comando Principal | Aliases | Descripción |
| :--- | :--- | :--- |
| `/gallos` | `/rg`, `/roostergenics`, `/peleagallos`, `/roosters`, `/palenque` | Abre el menú principal de RoosterGenics (corral, crianza, entrenamiento, duelos). |

| Permiso | Descripción | Por Defecto |
| :--- | :--- | :--- |
| `roostergenics.use` | Permite acceder a las funciones de jugador (corral, crianza, entrenar, combatir). | `true` (Todos) |
| `roostergenics.admin` | Permite acceso a comandos administrativos y de configuración. | `op` |

---

### 🚀 Instalación

1. Descarga el archivo **`Roostergenics-1.0.0.jar`** de este repositorio.
2. Cópialo en la carpeta `plugins/` de tu servidor Paper, Purpur o Folia 1.21.
3. Instala el resourcepack `Roostergenics-ResourcePack.zip`.
4. Inicia o reinicia el servidor.

---

<a id="-english-version"></a>
## 🇬🇧 English Version

### 🌟 Overview

**RoosterGenics** is a comprehensive, production-grade Minecraft plugin (**Paper / Purpur / Folia 1.21.1**) that introduces a complete **Fighting Roosters system with Deep Mendelian Genetics**, allelic inheritance, physical training, 1v1 battle arenas with real-time spectator betting, gym leaders, and native Folia multithreading support.

---

### ✨ Key Features

- 🧬 **Deep Genetics Engine:** Dominant and recessive alleles controlling combat stats (Strength, Speed, Defense, Aggressiveness, Accuracy), visual plumage variations, and genealogical bloodlines.
- 🥚 **Breeding & Incubation:** Selective pairing, Inbreeding Coefficient (COI) calculations, hybrid vigor, and genetic mutation tables.
- ⚔️ **1v1 Arenas & Palenques:** Grief-protected arenas with strict 4.5-block flight caps, deterministic combat isolation, and atomic spectator betting mechanics (`/rg apostar`).
- 🏛️ **6 Themed Gyms & Championship League:** Progressive NPC gym leader challenges with badges, held items, and exclusive tournament rewards.
- 🏥 **Medical Center & Held Items:** Post-combat injury mechanics, treatment with the Nurse NPC, and 14 competitive held items (Focus Sash, Life Orb, Assault Vest).
- ⚡ **Folia-Aware Multithreading:** Built from the ground up for high-capacity servers (200-300+ players) leveraging `RegionScheduler` and `EntityScheduler` safely.

---

### 📦 Repository Structure

- `src/main/java/`: High-performance, modular Java 21 codebase.
- `src/main/resources/`: Default configurations (`config.yml`, `plugin.yml`).
- `resourcepack/`: Custom 3D JSON models, sounds, and UI textures.
- `Roostergenics-ResourcePack.zip`: Pack ready for distribution or server-resource-pack configuration.
- `Roostergenics-1.0.0.jar`: Precompiled artifact ready for your server's `plugins/` folder.
- `pom.xml`: Maven dependency configuration (Paper API 1.21.1).

---

### ⌨️ Commands & Permissions

| Command | Aliases | Description |
| :--- | :--- | :--- |
| `/gallos` | `/rg`, `/roostergenics`, `/peleagallos`, `/roosters`, `/palenque` | Opens the main GUI menu (coop, breeding, training, duels). |

| Permission | Description | Default |
| :--- | :--- | :--- |
| `roostergenics.use` | Grants player access to core gameplay features. | `true` (Everyone) |
| `roostergenics.admin` | Access to administrative commands and reloading. | `op` |

---

### 🚀 Installation

1. Download **`Roostergenics-1.0.0.jar`**.
2. Drop it into your server's `plugins/` directory (Paper, Purpur, or Folia 1.21).
3. Distribute or link `Roostergenics-ResourcePack.zip`.
4. Start or restart your server.

---

## 👨‍💻 Autor / Author

Desarrollado por / Built by **DarkYTGG** — Desarrollador de Software & Estudiante de TSU en Informática.
- 🌐 GitHub: [@DarkYTGG](https://github.com/DarkYTGG)
- 📖 Wiki Oficial: [darkytgg.github.io/Wiki-RoosterGenics](https://darkytgg.github.io/Wiki-RoosterGenics/)
