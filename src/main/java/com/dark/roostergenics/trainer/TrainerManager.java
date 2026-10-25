package com.dark.roostergenics.trainer;

import com.dark.roostergenics.api.events.RoosterDuelVictoryEvent;
import com.dark.roostergenics.arena.DuelArenaSession;
import com.dark.roostergenics.arena.DuelManager;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.CaptureCageItem;
import com.dark.roostergenics.storage.RoosterRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor central de Entrenadores Galleros NPC, persistencia en trainers.yml y batallas PvE.
 */
public class TrainerManager implements Listener {

    private final JavaPlugin plugin;
    private final RoosterRepository repository;
    private final DuelManager duelManager;

    private final Map<UUID, RoosterTrainer> trainers = new ConcurrentHashMap<>();
    private final Map<UUID, RoosterTrainerNPC> npcs = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activePlayerBattles = new ConcurrentHashMap<>(); // PlayerUUID -> TrainerUUID

    private final File trainersFile;
    private YamlConfiguration trainersConfig;

    public TrainerManager(JavaPlugin plugin, RoosterRepository repository, DuelManager duelManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.duelManager = duelManager;
        this.trainersFile = new File(plugin.getDataFolder(), "trainers.yml");
    }

    public void loadTrainers() {
        // Limpiar entidades previas si existían
        removeAllNPCs();
        trainers.clear();

        if (!trainersFile.exists()) {
            return;
        }

        this.trainersConfig = YamlConfiguration.loadConfiguration(trainersFile);
        ConfigurationSection sec = trainersConfig.getConfigurationSection("trainers");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            try {
                ConfigurationSection tSec = sec.getConfigurationSection(key);
                if (tSec != null) {
                    RoosterTrainer trainer = RoosterTrainer.deserializeFrom(tSec);
                    trainers.put(trainer.getTrainerId(), trainer);

                    if (trainer.getLocation() != null && trainer.getLocation().getWorld() != null) {
                        RoosterTrainerNPC npc = new RoosterTrainerNPC(plugin, trainer);
                        npc.spawnEntities();
                        npcs.put(trainer.getTrainerId(), npc);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error cargando entrenador " + key + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("✔ Se cargaron " + trainers.size() + " Entrenadores Galleros NPC.");
    }

    public void saveTrainers() {
        if (trainersConfig == null) {
            trainersConfig = new YamlConfiguration();
        }
        trainersConfig.set("trainers", null);

        for (RoosterTrainer t : trainers.values()) {
            ConfigurationSection sec = trainersConfig.createSection("trainers." + t.getTrainerId().toString());
            t.serializeTo(sec);
        }

        try {
            trainersConfig.save(trainersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar trainers.yml: " + e.getMessage());
        }
    }

    public RoosterTrainer spawnTrainer(String name, TrainerTier tier, Location loc, String skinName) {
        RoosterTrainer trainer = new RoosterTrainer(UUID.randomUUID(), name, tier, skinName, loc);
        trainers.put(trainer.getTrainerId(), trainer);

        RoosterTrainerNPC npc = new RoosterTrainerNPC(plugin, trainer);
        npc.spawnEntities();
        npcs.put(trainer.getTrainerId(), npc);

        saveTrainers();
        return trainer;
    }

    public boolean removeTrainer(UUID trainerId) {
        RoosterTrainer trainer = trainers.remove(trainerId);
        RoosterTrainerNPC npc = npcs.remove(trainerId);
        if (npc != null) {
            npc.removeEntities();
        }
        if (trainer != null) {
            saveTrainers();
            return true;
        }
        return false;
    }

    public RoosterTrainer getTrainer(UUID id) {
        return trainers.get(id);
    }

    public RoosterTrainer getTrainerByName(String name) {
        for (RoosterTrainer t : trainers.values()) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public Collection<RoosterTrainer> getTrainers() {
        return Collections.unmodifiableCollection(trainers.values());
    }

    public Collection<RoosterTrainerNPC> getAllNPCs() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    public RoosterTrainerNPC getNpc(UUID id) {
        return npcs.get(id);
    }

    public RoosterTrainer findNearestTrainer(Location loc, double maxRadius) {
        if (loc == null || loc.getWorld() == null) return null;
        RoosterTrainer nearest = null;
        double minDistSq = maxRadius * maxRadius;
        for (RoosterTrainer t : trainers.values()) {
            Location tLoc = t.getLocation();
            if (tLoc != null && tLoc.getWorld() != null && tLoc.getWorld().equals(loc.getWorld())) {
                double distSq = tLoc.distanceSquared(loc);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    nearest = t;
                }
            }
        }
        return nearest;
    }

    public boolean updateTrainerSkin(UUID trainerId, String newSkin) {
        RoosterTrainer trainer = trainers.get(trainerId);
        if (trainer == null) return false;

        trainer.setSkinName(newSkin);

        RoosterTrainerNPC oldNpc = npcs.remove(trainerId);
        if (oldNpc != null) {
            oldNpc.removeEntities();
        }

        if (trainer.getLocation() != null && trainer.getLocation().getWorld() != null) {
            RoosterTrainerNPC newNpc = new RoosterTrainerNPC(plugin, trainer);
            newNpc.spawnEntities();
            npcs.put(trainerId, newNpc);
        }

        saveTrainers();
        return true;
    }

    public boolean renameTrainer(UUID trainerId, String newName) {
        RoosterTrainer trainer = trainers.get(trainerId);
        if (trainer == null || newName == null || newName.isBlank()) return false;

        trainer.setName(newName);
        RoosterTrainerNPC npc = npcs.get(trainerId);
        if (npc != null) {
            npc.updateHologram();
        }

        saveTrainers();
        return true;
    }

    public boolean startTrainerBattle(Player player, RoosterTrainer trainer) {
        if (duelManager.findActiveSessionForPlayer(player.getUniqueId()) != null) {
            player.sendMessage(Component.text("¡Ya te encuentras en medio de un combate de palenque!", NamedTextColor.RED));
            return false;
        }

        RoosterData playerRooster = repository.getActiveRooster(player.getUniqueId());
        if (playerRooster == null || !playerRooster.isUsableForCombat()) {
            player.sendMessage(Component.text("Tu gallo activo no está disponible o está noqueado.", NamedTextColor.RED));
            return false;
        }

        Location arenaLoc = duelManager.resolveArena(trainer.getLocation(), player.getLocation());
        if (arenaLoc == null || arenaLoc.getWorld() == null) {
            player.sendMessage(Component.text("❌ [Palenque] No se encontró ningún cuadrilátero configurado cerca.", NamedTextColor.RED));
            player.sendMessage(Component.text("Párate en el centro de tu arena y escribe: ", NamedTextColor.GRAY)
                    .append(Component.text("/gallos arena fijar", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" (o ", NamedTextColor.GRAY))
                    .append(Component.text("/gallos arena crear", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(")", NamedTextColor.GRAY)));
            return false;
        }

        // Teletransportar al jugador a su esquina de entrenador (Oeste, fuera de las vallas del cuadrilátero)
        Location coachSpot = arenaLoc.clone().add(-5.2, 0, 0);
        coachSpot.setDirection(arenaLoc.toVector().subtract(coachSpot.toVector()));
        if (!coachSpot.getBlock().isPassable()) {
            coachSpot.add(0, 1, 0);
        }
        player.teleportAsync(coachSpot);

        // Crear sesión de combate en la arena
        DuelArenaSession session = new DuelArenaSession(
                plugin,
                repository,
                duelManager.getBettingHook(),
                false, // Combates de entrenador no matan en nuzlocke por defecto
                duelManager.getKnockoutDurationMillis(),
                player,
                null,
                trainer.getName(),
                playerRooster,
                trainer.getRooster(),
                0.0,
                arenaLoc
        );

        activePlayerBattles.put(player.getUniqueId(), trainer.getTrainerId());
        duelManager.getActiveSessions().add(session);
        session.start();

        player.sendMessage(Component.empty()
                .append(Component.text("\n⚔ [DESAFÍO] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(trainer.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" ha aceptado tu reto!\n", NamedTextColor.WHITE))
                .append(Component.text("Rango: ", NamedTextColor.GRAY))
                .append(Component.text(trainer.getTier().getTitle() + " (Nv. " + trainer.getTier().getLevel() + ")\n", trainer.getTier().getColor()))
                .append(Component.text("¡Comienza la batalla en el cuadrilátero!\n", NamedTextColor.GREEN)));

        return true;
    }

    @EventHandler
    public void onDuelVictory(RoosterDuelVictoryEvent event) {
        DuelArenaSession session = event.getSession();
        Player winner = event.getWinner();
        Player loser = event.getLoser();

        if (winner != null && activePlayerBattles.containsKey(winner.getUniqueId())) {
            UUID trainerId = activePlayerBattles.remove(winner.getUniqueId());
            RoosterTrainer trainer = trainers.get(trainerId);
            if (trainer != null) {
                trainer.addLoss();
                saveTrainers();

                TrainerTier tier = trainer.getTier();

                // Recompensa de jaulas
                if (tier.getRewardCages() > 0) {
                    winner.getInventory().addItem(CaptureCageItem.createCage(tier.getRewardCages()));
                }

                // Recompensa monetaria
                if (tier.getRewardCoins() > 0) {
                    depositCoins(winner, tier.getRewardCoins(), "Victoria contra " + trainer.getName());
                }

                // Efectos de victoria
                winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                spawnVictoryFireworks(winner.getLocation());

                winner.sendMessage(Component.empty()
                        .append(Component.text("\n🎉 ¡VICTORIA CONTRA ENTRENADOR GALLERO! 🎉\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text("Has vencido al gallo de ", NamedTextColor.GREEN))
                        .append(Component.text(trainer.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .append(Component.text("!\nRecompensas recibidas:\n", NamedTextColor.GREEN))
                        .append(Component.text("✦ " + tier.getRewardCages() + " Jaula(s) de Captura de Gallos\n", NamedTextColor.AQUA))
                        .append(Component.text("✦ $" + String.format("%.2f", tier.getRewardCoins()) + " en monedas/fichas acreditadas a tu cuenta\n", NamedTextColor.GOLD)));
            }
        }

        if (loser != null && activePlayerBattles.containsKey(loser.getUniqueId())) {
            UUID trainerId = activePlayerBattles.remove(loser.getUniqueId());
            RoosterTrainer trainer = trainers.get(trainerId);
            if (trainer != null) {
                trainer.addWin();
                saveTrainers();

                loser.sendMessage(Component.empty()
                        .append(Component.text("\n☠ [DERROTA] ", NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text(trainer.getName() + ": ", NamedTextColor.GOLD))
                        .append(Component.text("\"Buen combate muchacho, pero tu gallo aún necesita más entrenamiento en el gimnasio.\"\n", NamedTextColor.GRAY)));
            }
        }
    }

    private void spawnVictoryFireworks(Location loc) {
        if (loc.getWorld() == null) return;
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc.clone().add(0, 1, 0), EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(org.bukkit.Color.fromRGB(255, 215, 0), org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW)
                .withFade(org.bukkit.Color.RED)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    public void removeAllNPCs() {
        for (RoosterTrainerNPC npc : npcs.values()) {
            npc.removeEntities();
        }
        npcs.clear();
    }

    public boolean depositCoins(Player player, double amount, String reason) {
        if (player == null || amount <= 0) return false;
        com.dark.roostergenics.api.betting.DuelBettingHook hook = duelManager.getBettingHook();
        if (hook != null && hook.depositReward(player, amount, reason)) {
            return true;
        }
        try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                Class<?> ecoClass = Class.forName("net.milkbowl.vault.economy.Economy");
                var rsp = org.bukkit.Bukkit.getServicesManager().getRegistration(ecoClass);
                if (rsp != null && rsp.getProvider() != null) {
                    var method = ecoClass.getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
                    method.invoke(rsp.getProvider(), player, amount);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
