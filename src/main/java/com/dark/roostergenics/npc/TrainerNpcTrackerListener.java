package com.dark.roostergenics.npc;

import com.dark.roostergenics.RoostergenicsPlugin;
import com.dark.roostergenics.trainer.RoosterTrainerNPC;
import com.dark.roostergenics.trainer.TrainerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Mantiene la sincronización de visibilidad y nametag de los Player NPCs para jugadores
 * que entran al servidor, cambian de mundo, se teletransportan o se mueven.
 */
public class TrainerNpcTrackerListener implements Listener {

    private final RoostergenicsPlugin plugin;
    private final TrainerManager trainerManager;

    public TrainerNpcTrackerListener(RoostergenicsPlugin plugin, TrainerManager trainerManager) {
        this.plugin = plugin;
        this.trainerManager = trainerManager;

        // Tarea periódica de proximidad cada 20 ticks (1s) para que los NPCs siempre sean visibles
        plugin.getSchedulerAdapter().runRepeatingGlobal(handle -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayer(player);
            }
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            Scoreboard sb = player.getScoreboard();
            for (String skinName : PlayerNpc.RANDOM_SKINS) {
                PlayerNpc.applyHideTeam(sb, skinName);
            }
            for (var tr : trainerManager.getTrainers()) {
                PlayerNpc.applyHideTeam(sb, tr.getSkinName());
            }
            for (com.dark.roostergenics.gym.GymLeader gl : com.dark.roostergenics.gym.GymLeader.values()) {
                PlayerNpc.applyHideTeam(sb, gl.getSkinName());
            }
            PlayerNpc.applyHideTeam(sb, com.dark.roostergenics.npc.MerchantNPC.SKIN_NAME);
            PlayerNpc.applyHideTeam(sb, com.dark.roostergenics.npc.NurseNPC.SKIN_NAME);
        } catch (Exception ignored) {}

        plugin.getSchedulerAdapter().runDelayedAtEntity(player, handle -> {
            updatePlayer(player);
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (RoosterTrainerNPC npc : trainerManager.getAllNPCs()) {
            if (npc.getPlayerNpc() != null) {
                npc.getPlayerNpc().hideFrom(player);
            }
        }
        if (plugin.getGymManager() != null) {
            for (com.dark.roostergenics.gym.GymLeaderNPC npc : plugin.getGymManager().getAllGymNPCs()) {
                if (npc.getPlayerNpc() != null) {
                    npc.getPlayerNpc().hideFrom(player);
                }
            }
        }
        if (plugin.getStructureManager() != null) {
            for (com.dark.roostergenics.npc.MerchantNPC npc : plugin.getStructureManager().getAllMerchantNpcs()) {
                if (npc.getPlayerNpc() != null) {
                    npc.getPlayerNpc().hideFrom(player);
                }
            }
        }
        if (plugin.getCenterManager() != null) {
            for (com.dark.roostergenics.npc.NurseNPC npc : plugin.getCenterManager().getAllNurseNpcs()) {
                if (npc.getPlayerNpc() != null) {
                    npc.getPlayerNpc().hideFrom(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        plugin.getSchedulerAdapter().runDelayedAtEntity(event.getPlayer(), handle -> {
            updatePlayer(event.getPlayer());
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.getSchedulerAdapter().runDelayedAtEntity(event.getPlayer(), handle -> {
            updatePlayer(event.getPlayer());
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getSchedulerAdapter().runDelayedAtEntity(event.getPlayer(), handle -> {
            updatePlayer(event.getPlayer());
        }, 10L);
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        for (RoosterTrainerNPC npc : trainerManager.getAllNPCs()) {
            PlayerNpc playerNpc = npc.getPlayerNpc();
            if (playerNpc != null) {
                if (player.getWorld().equals(playerNpc.getLocation().getWorld())
                        && player.getLocation().distanceSquared(playerNpc.getLocation()) <= 64.0 * 64.0) {
                    playerNpc.showTo(player);
                } else {
                    playerNpc.hideFrom(player);
                }
            }
        }

        if (plugin.getGymManager() != null) {
            for (com.dark.roostergenics.gym.GymLeaderNPC npc : plugin.getGymManager().getAllGymNPCs()) {
                PlayerNpc playerNpc = npc.getPlayerNpc();
                if (playerNpc != null) {
                    if (player.getWorld().equals(playerNpc.getLocation().getWorld())
                            && player.getLocation().distanceSquared(playerNpc.getLocation()) <= 64.0 * 64.0) {
                        playerNpc.showTo(player);
                    } else {
                        playerNpc.hideFrom(player);
                    }
                }
            }
        }

        if (plugin.getStructureManager() != null) {
            for (com.dark.roostergenics.npc.MerchantNPC npc : plugin.getStructureManager().getAllMerchantNpcs()) {
                PlayerNpc playerNpc = npc.getPlayerNpc();
                if (playerNpc != null) {
                    if (player.getWorld().equals(playerNpc.getLocation().getWorld())
                            && player.getLocation().distanceSquared(playerNpc.getLocation()) <= 64.0 * 64.0) {
                        playerNpc.showTo(player);
                    } else {
                        playerNpc.hideFrom(player);
                    }
                }
            }
        }

        if (plugin.getCenterManager() != null) {
            for (com.dark.roostergenics.npc.NurseNPC npc : plugin.getCenterManager().getAllNurseNpcs()) {
                PlayerNpc playerNpc = npc.getPlayerNpc();
                if (playerNpc != null) {
                    if (player.getWorld().equals(playerNpc.getLocation().getWorld())
                            && player.getLocation().distanceSquared(playerNpc.getLocation()) <= 64.0 * 64.0) {
                        playerNpc.showTo(player);
                    } else {
                        playerNpc.hideFrom(player);
                    }
                }
            }
        }
    }
}
