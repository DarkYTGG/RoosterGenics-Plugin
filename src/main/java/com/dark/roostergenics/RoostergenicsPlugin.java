package com.dark.roostergenics;

import com.dark.roostergenics.api.betting.DuelBettingHook;
import com.dark.roostergenics.arena.ArenaProtectionListener;
import com.dark.roostergenics.arena.DuelManager;
import com.dark.roostergenics.command.RoosterCommand;
import com.dark.roostergenics.genetics.BreedingManager;
import com.dark.roostergenics.gui.GuiListener;
import com.dark.roostergenics.storage.CaptureCageItem;
import com.dark.roostergenics.storage.CaptureListener;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.training.RoosterTrainingManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Plugin principal Roostergenics: Simulación genética profunda,
 * corral virtual anti-lag, crianza por consanguinidad y combate físico 1v1 en
 * palenque.
 */
public class RoostergenicsPlugin extends JavaPlugin {

    private static RoostergenicsPlugin instance;

    private RoosterRepository repository;
    private BreedingManager breedingManager;
    private RoosterTrainingManager trainingManager;
    private DuelManager duelManager;
    private com.dark.roostergenics.trainer.TrainerManager trainerManager;
    private com.dark.roostergenics.scheduler.TaskSchedulerAdapter schedulerAdapter;
    private com.dark.roostergenics.companion.RoosterCompanionManager companionManager;
    private com.dark.roostergenics.gym.GymManager gymManager;
    private com.dark.roostergenics.center.RoosterCenterManager centerManager;
    private com.dark.roostergenics.structure.StructureManager structureManager;
    private com.dark.roostergenics.structure.StructureRegistry structureRegistry;
    private com.dark.roostergenics.structure.StructureEntityTracker structureEntityTracker;

    public com.dark.roostergenics.structure.StructureRegistry getStructureRegistry() {
        return structureRegistry;
    }

    public com.dark.roostergenics.structure.StructureEntityTracker getStructureEntityTracker() {
        return structureEntityTracker;
    }

    public com.dark.roostergenics.trainer.TrainerManager getTrainerManager() {
        return trainerManager;
    }

    public com.dark.roostergenics.scheduler.TaskSchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }

    public com.dark.roostergenics.companion.RoosterCompanionManager getCompanionManager() {
        return companionManager;
    }

    public com.dark.roostergenics.gym.GymManager getGymManager() {
        return gymManager;
    }

    public com.dark.roostergenics.center.RoosterCenterManager getCenterManager() {
        return centerManager;
    }

    public com.dark.roostergenics.structure.StructureManager getStructureManager() {
        return structureManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        printStartupBanner();

        // 0. Adaptador de Planificación Universal
        this.schedulerAdapter = new com.dark.roostergenics.scheduler.TaskSchedulerAdapter(this);

        // 1. Inicializar Repositorio y Cargar Datos
        this.repository = new RoosterRepository(this);
        this.repository.loadAll();

        // 2. Motores de Genética y Entrenamiento
        this.breedingManager = new BreedingManager();
        this.trainingManager = new RoosterTrainingManager(this.repository);

        // 3. Gestor de Cuadriláteros y Palenque 1v1
        this.duelManager = new DuelManager(this, this.repository);
        this.duelManager.loadConfiguredArena();
        boolean nuzlocke = getConfig().getBoolean("settings.nuzlocke-mode", false);
        long knockoutDuration = getConfig().getLong("settings.knockout-recovery-minutes", 20L) * 60L * 1000L;
        this.duelManager.setNuzlockeEnabled(nuzlocke);
        this.duelManager.setKnockoutDurationMillis(knockoutDuration);

        // 4. Registro de Receta de Crafteo
        CaptureCageItem.registerRecipe(this);

        // 5. Gestor de Entrenadores Galleros NPC (PvE)
        this.trainerManager = new com.dark.roostergenics.trainer.TrainerManager(this, this.repository, this.duelManager);
        this.trainerManager.loadTrainers();

        // 6. Sistemas de Compañero Fiel, Gimnasios, Centro de Gallos y Estructuras
        this.companionManager = new com.dark.roostergenics.companion.RoosterCompanionManager(this, this.repository);
        this.gymManager = new com.dark.roostergenics.gym.GymManager(this, this.repository, this.duelManager);
        this.centerManager = new com.dark.roostergenics.center.RoosterCenterManager(this, this.repository);
        this.structureManager = new com.dark.roostergenics.structure.StructureManager(this, this.centerManager, this.gymManager);
        this.structureRegistry = new com.dark.roostergenics.structure.StructureRegistry(this);
        this.structureRegistry.load();
        this.structureEntityTracker = new com.dark.roostergenics.structure.StructureEntityTracker(this, this.structureRegistry, this.structureManager);
        this.structureManager.setRegistryAndTracker(this.structureRegistry, this.structureEntityTracker);

        // 7. Escuchadores de Eventos
        getServer().getPluginManager().registerEvents(new CaptureListener(this.repository), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(this.trainerManager, this);
        getServer().getPluginManager().registerEvents(new com.dark.roostergenics.trainer.TrainerListener(this.trainerManager, this.repository), this);
        getServer().getPluginManager().registerEvents(new com.dark.roostergenics.npc.TrainerNpcTrackerListener(this, this.trainerManager), this);
        getServer().getPluginManager().registerEvents(this.companionManager, this);
        getServer().getPluginManager().registerEvents(this.gymManager, this);
        getServer().getPluginManager().registerEvents(this.centerManager, this);
        getServer().getPluginManager().registerEvents(this.structureManager, this);
        getServer().getPluginManager().registerEvents(this.structureEntityTracker, this);
        getServer().getPluginManager().registerEvents(new com.dark.roostergenics.items.HeldItemManager(), this);
        getServer().getPluginManager().registerEvents(this.duelManager, this);

        // 8. Registro de Comandos
        RoosterCommand commandHandler = new RoosterCommand(this, this.repository, this.duelManager, this.trainingManager,
                this.breedingManager, this.trainerManager, this.companionManager, this.gymManager, this.centerManager, this.structureManager);
        PluginCommand gallosCmd = getCommand("gallos");
        if (gallosCmd != null) {
            gallosCmd.setExecutor(commandHandler);
            gallosCmd.setTabCompleter(commandHandler);
        }

        // 9. Pre-carga asíncrona de skins para prevenir congelamiento del hilo principal
        List<String> skinsToPreload = new ArrayList<>(com.dark.roostergenics.npc.PlayerNpc.RANDOM_SKINS);
        skinsToPreload.add("NurseJoy");
        skinsToPreload.add("Merchant");
        com.dark.roostergenics.npc.PlayerNpc.preloadSkins(this, skinsToPreload);

        getLogger().info("✔ Roostergenics cargado exitosamente. Listo para los combates.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Guardando datos de gallos y cerrando cuadriláteros...");
        if (companionManager != null) {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                companionManager.dismissCompanion(p, false);
            }
        }
        if (trainerManager != null) {
            trainerManager.removeAllNPCs();
            trainerManager.saveTrainers();
        }
        if (gymManager != null) {
            gymManager.removeAllGymNPCs();
        }
        if (structureManager != null) {
            structureManager.cleanupAll();
        }
        if (structureRegistry != null) {
            structureRegistry.shutdown();
        }
        if (centerManager != null) {
            centerManager.cleanupAll();
        }
        if (duelManager != null) {
            duelManager.abortAll();
        }
        if (repository != null) {
            repository.saveAll();
        }
        getLogger().info("Roostergenics deshabilitado correctamente.");
    }

    private void printStartupBanner() {
        Logger log = getLogger();
        log.info("===============================================================");
        log.info("  ____                 _                            _          ");
        log.info(" |  _ \\ ___   ___  ___| |_ ___ _ __ __ _  ___ _ __ (_) ___ ___ ");
        log.info(" | |_) / _ \\ / _ \\/ __| __/ _ \\ '__/ _` |/ _ \\ '_ \\| |/ __/ __|");
        log.info(" |  _ < (_) | (_) \\__ \\ ||  __/ | | (_| |  __/ | | | | (__\\__ \\");
        log.info(" |_| \\_\\___/ \\___/|___/\\__\\___|_|  \\__, |\\___|_| |_|_|\\___|___/");
        log.info("                                   |___/                       ");
        log.info("            ROOSTERGENICS COCKFIGHTING SYSTEM v" + getDescription().getVersion() + " 🐓           ");
        log.info("                   [Paper 1.21] [By: iDarkt]                   ");
        log.info("===============================================================");
    }

    /**
     * Registra un hook externo de apuestas (por ejemplo desde DarkCasino).
     */
    public void registerBettingHook(DuelBettingHook hook) {
        if (duelManager != null) {
            duelManager.setBettingHook(hook);
            getLogger()
                    .info("✔ Hook de apuestas externo registrado con éxito (" + hook.getClass().getSimpleName() + ").");
        }
    }

    public static RoostergenicsPlugin getInstance() {
        return instance;
    }

    public RoosterRepository getRepository() {
        return repository;
    }

    public BreedingManager getBreedingManager() {
        return breedingManager;
    }

    public RoosterTrainingManager getTrainingManager() {
        return trainingManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }
}
