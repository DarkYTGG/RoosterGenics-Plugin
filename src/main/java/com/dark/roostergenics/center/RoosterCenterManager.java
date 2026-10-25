package com.dark.roostergenics.center;

import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterHealthStatus;
import com.dark.roostergenics.scheduler.TaskSchedulerAdapter;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.util.Mm;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor del Centro de Gallos (Clínica Médica de Gallos):
 * - Soporta tanto Bloque Interactivo (Incubadora) como NPC (Enfermera del Palenque).
 * - Cura a todos los gallos del jugador y reproduce la fanfarria melódica de recuperación.
 */
public class RoosterCenterManager implements Listener {

    public static final String METADATA_HEAL_BLOCK = "roostergenics_heal_block";
    public static final String METADATA_NURSE_NPC = "roostergenics_nurse_npc";

    private final JavaPlugin plugin;
    private final RoosterRepository repository;
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();

    private final List<com.dark.roostergenics.npc.NurseNPC> nurseNpcs = new java.util.concurrent.CopyOnWriteArrayList<>();

    public RoosterCenterManager(JavaPlugin plugin, RoosterRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public java.util.Collection<com.dark.roostergenics.npc.NurseNPC> getAllNurseNpcs() {
        return nurseNpcs;
    }

    public void removeNurseNPCNear(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return;
        double rSq = radius * radius;
        nurseNpcs.removeIf(npc -> {
            if (npc != null && npc.getLocation().getWorld().equals(loc.getWorld()) && npc.getLocation().distanceSquared(loc) <= rSq) {
                npc.remove();
                return true;
            }
            return false;
        });
    }

    public void cleanupAll() {
        for (com.dark.roostergenics.npc.NurseNPC npc : nurseNpcs) {
            if (npc != null) {
                npc.remove();
            }
        }
        nurseNpcs.clear();
    }

    /**
     * Coloca un Bloque de Curación interactivo en la ubicación especificada.
     */
    public void placeHealingBlock(Location loc) {
        Block block = loc.getBlock();
        block.setType(Material.LODESTONE);
        block.setMetadata(METADATA_HEAL_BLOCK, new FixedMetadataValue(plugin, true));
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 1.2, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }

    /**
     * Spawnea una Enfermera NPC del Palenque (Player NPC con Skin 3D) en la ubicación.
     */
    public com.dark.roostergenics.npc.NurseNPC spawnNurseNpc(Location loc) {
        return spawnNurseNpc(loc, UUID.randomUUID());
    }

    public com.dark.roostergenics.npc.NurseNPC spawnNurseNpc(Location loc, UUID instanceId) {
        com.dark.roostergenics.npc.NurseNPC npc = new com.dark.roostergenics.npc.NurseNPC(plugin, this, loc, instanceId);
        npc.spawnEntities();
        nurseNpcs.add(npc);

        loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1.5, 0), 10, 0.4, 0.4, 0.4, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
        return npc;
    }

    /**
     * Ejecuta el proceso de curación completa para el jugador.
     */
    public boolean performHealingProcess(Player player, Location sourceLoc) {
        long now = System.currentTimeMillis();
        long last = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 5000L) {
            player.sendMessage(Mm.noItalic("<gray>Tus gallos ya han sido atendidos recientemente. Espera unos segundos."));
            return false;
        }
        playerCooldowns.put(player.getUniqueId(), now);

        List<RoosterData> playerRoosters = repository.getPlayerRoosters(player.getUniqueId());
        if (playerRoosters.isEmpty()) {
            player.sendMessage(Mm.noItalic("<yellow>No tienes ningún gallo registrado en tu corral para curar."));
            return false;
        }

        // 1. Sanar salud básica y K.O. (siempre garantizado como emergencia)
        for (RoosterData r : playerRoosters) {
            r.setHealthStatus(RoosterHealthStatus.HEALTHY);
            r.setKnockedOutUntilMillis(0L);
        }

        // 2. Contar lesiones de combate temporales en el equipo
        int totalInjuries = 0;
        for (RoosterData r : playerRoosters) {
            totalInjuries += r.getTemporaryInjuries().size();
        }

        int seedsCost = totalInjuries * 5;
        int availableSeeds = countSeeds(player);
        boolean injuriesTreated = false;

        if (totalInjuries > 0) {
            if (availableSeeds >= seedsCost || player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    removeSeeds(player, seedsCost);
                }
                for (RoosterData r : playerRoosters) {
                    r.clearInjuries();
                }
                injuriesTreated = true;
            }
        }

        for (RoosterData r : playerRoosters) {
            repository.saveRooster(r);
        }

        // Partículas sanadoras
        Location effectLoc = sourceLoc != null ? sourceLoc.clone().add(0, 1.0, 0) : player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.HEART, effectLoc, 14, 0.5, 0.5, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, effectLoc, 24, 0.6, 0.6, 0.6, 0.05);

        // Fanfarria clásica de recuperación en 6 notas musicales
        playHealingFanfare(player, effectLoc);

        if (totalInjuries == 0) {
            player.sendMessage(Mm.noItalic("<green>✔ <bold>¡CENTRO DE GALLOS!</bold> Todos tus gallos han sido restaurados al 100% de salud y vitalidad. <gray>(Sin lesiones activas).</gray>"));
        } else if (injuriesTreated) {
            player.sendMessage(Mm.noItalic("<green>✔ <bold>¡TRATAMIENTO MÉDICO COMPLETO!</bold> La Enfermera ha aplicado vendajes y sanado <yellow>" + totalInjuries + "</yellow> lesión(es) en tu equipo. <gray>(Tarifa pagada: " + seedsCost + " semillas).</gray>"));
        } else {
            player.sendMessage(Mm.noItalic("<green>✔ Salud básica restaurada, <yellow>pero tu equipo tiene <red><bold>" + totalInjuries + " lesión(es) de combate</bold></red> sin curar."));
            player.sendMessage(Mm.noItalic("<gray>La Enfermera requiere una tarifa de <yellow>" + seedsCost + " semillas</yellow> de insumos médicos para tratarlas (tienes " + availableSeeds + ")."));
        }
        return true;
    }

    private int countSeeds(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSeed(item.getType())) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeSeeds(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && isSeed(item.getType())) {
                int toTake = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - toTake);
                remaining -= toTake;
                if (item.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setContents(contents);
    }

    private boolean isSeed(Material mat) {
        return mat == Material.WHEAT_SEEDS || mat == Material.BEETROOT_SEEDS ||
                mat == Material.MELON_SEEDS || mat == Material.PUMPKIN_SEEDS ||
                mat == Material.TORCHFLOWER_SEEDS || mat == Material.PITCHER_POD;
    }

    private void playHealingFanfare(Player player, Location loc) {
        float[] pitches = {1.0f, 1.189f, 1.335f, 1.498f, 1.782f, 2.0f};
        long delay = 1L;

        for (int i = 0; i < pitches.length; i++) {
            final float pitch = pitches[i];
            player.getScheduler().runDelayed(plugin, t -> {
                if (player.isOnline()) {
                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, pitch);
                }
            }, null, delay);
            delay += 3L;
        }
    }

    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.LODESTONE || block.hasMetadata(METADATA_HEAL_BLOCK)) {
            event.setCancelled(true);
            performHealingProcess(event.getPlayer(), block.getLocation().add(0.5, 1.0, 0.5));
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity target = event.getRightClicked();

        if (target.hasMetadata(METADATA_NURSE_NPC) 
                || target.getPersistentDataContainer().has(com.dark.roostergenics.npc.NurseNPC.NURSE_NPC_KEY, org.bukkit.persistence.PersistentDataType.STRING)
                || (target instanceof Villager v && v.getCustomName() != null && v.getCustomName().contains("Enfermera"))) {
            event.setCancelled(true);
            performHealingProcess(event.getPlayer(), target.getLocation());
        }
    }

    @EventHandler
    public void onDamageEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if (target.hasMetadata(METADATA_NURSE_NPC) 
                || target.getPersistentDataContainer().has(com.dark.roostergenics.npc.NurseNPC.NURSE_NPC_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                performHealingProcess(player, target.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        playerCooldowns.remove(event.getPlayer().getUniqueId());
    }
}
