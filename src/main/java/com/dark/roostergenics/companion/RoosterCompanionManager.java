package com.dark.roostergenics.companion;

import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterHealthStatus;
import com.dark.roostergenics.scheduler.TaskSchedulerAdapter;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor del Sistema de Compañero Fiel:
 * - Invocación en el mundo físico y seguimiento fiel.
 * - Caricias con la mano vacía (aumenta vínculo afectivo y emite corazones).
 * - Alimentación con semillas (nutrición y curación).
 * - Rutina visible de entrenamiento físico en el mundo.
 */
public class RoosterCompanionManager implements Listener {

    public static final String METADATA_COMPANION = "roostergenics_companion";
    public static final String METADATA_OWNER = "roostergenics_companion_owner";

    private final JavaPlugin plugin;
    private final RoosterRepository repository;
    private final Map<UUID, UUID> activeCompanions = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isTrainingPhysically = new ConcurrentHashMap<>();
    private final Map<UUID, Long> petCooldowns = new ConcurrentHashMap<>();

    public RoosterCompanionManager(JavaPlugin plugin, RoosterRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        startFollowTask();
    }

    /**
     * Invoca al gallo activo en el mundo exterior para que acompañe al jugador.
     */
    public boolean summonCompanion(Player player, RoosterData rooster) {
        if (player == null || !player.isOnline() || rooster == null) return false;

        // Desinvocar acompañante previo si existía
        dismissCompanion(player, false);

        Location spawnLoc = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(1.2));
        if (!spawnLoc.getBlock().isPassable()) {
            spawnLoc = player.getLocation().clone();
        }

        Chicken chicken = player.getWorld().spawn(spawnLoc, Chicken.class, c -> {
            c.setAdult();
            c.setBreed(false);
            c.setPersistent(false);
            c.setRemoveWhenFarAway(false);
            c.setCustomNameVisible(true);

            try {
                c.setVariant(Chicken.Variant.TEMPERATE);
            } catch (Throwable ignored) {}

            updateCompanionName(c, rooster);

            c.setMetadata(METADATA_COMPANION, new FixedMetadataValue(plugin, rooster.getRoosterId().toString()));
            c.setMetadata(METADATA_OWNER, new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            // Aplicar escala genética
            try {
                Field field = Attribute.class.getField("GENERIC_SCALE");
                Attribute scaleAttr = (Attribute) field.get(null);
                AttributeInstance instance = c.getAttribute(scaleAttr);
                if (instance != null) {
                    double scale = rooster.calculateCombatStats().getScale();
                    instance.setBaseValue(scale);
                }
            } catch (Throwable ignored) {}
        });

        activeCompanions.put(player.getUniqueId(), chicken.getUniqueId());

        player.getWorld().spawnParticle(Particle.HEART, spawnLoc.clone().add(0, 0.6, 0), 6, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(spawnLoc, Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.3f);
        player.sendMessage(Mm.noItalic("<gold>★ ¡Has invocado a <yellow>" + rooster.getName() + "</yellow> como tu compañero fiel!"));
        return true;
    }

    public Chicken getCompanion(UUID playerId) {
        if (playerId == null) return null;
        UUID chickenId = activeCompanions.get(playerId);
        if (chickenId == null) return null;
        org.bukkit.entity.Entity entity = Bukkit.getEntity(chickenId);
        if (entity instanceof Chicken chicken && chicken.isValid()) {
            return chicken;
        }
        activeCompanions.remove(playerId);
        return null;
    }

    public Chicken getCompanion(Player player) {
        return player != null ? getCompanion(player.getUniqueId()) : null;
    }

    /**
     * Guarda / desinvoca al compañero del jugador.
     */
    public boolean dismissCompanion(Player player, boolean notify) {
        if (player == null) return false;
        UUID chickenId = activeCompanions.remove(player.getUniqueId());
        if (chickenId != null) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(chickenId);
            if (entity instanceof Chicken chicken && chicken.isValid()) {
                Location loc = chicken.getLocation();
                loc.getWorld().spawnParticle(Particle.POOF, loc.clone().add(0, 0.4, 0), 8, 0.2, 0.2, 0.2, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_CHICKEN_STEP, 0.8f, 1.5f);
                chicken.remove();
                if (notify) {
                    player.sendMessage(Mm.noItalic("<gray>Tu gallo compañero ha regresado a descansar a su corral."));
                }
                return true;
            }
        }
        return false;
    }

    public boolean hasCompanionSummoned(Player player) {
        return getCompanion(player) != null;
    }

    public void petCompanion(Player player) {
        Chicken chicken = getCompanion(player);
        RoosterData rooster = repository.getActiveRooster(player.getUniqueId());
        if (chicken == null || rooster == null) {
            player.sendMessage(Mm.noItalic("<red>No tienes a ningún gallo compañero invocado a tu lado. Usa /rg sacar."));
            return;
        }

        long now = System.currentTimeMillis();
        Long lastPet = petCooldowns.get(player.getUniqueId());
        if (lastPet != null && now - lastPet < 8000L) {
            long remaining = (8000L - (now - lastPet) + 999L) / 1000L;
            player.sendActionBar(Mm.noItalic("<yellow>⏳ Espera <aqua>" + remaining + "s</aqua> antes de volver a acariciar a <gold>" + rooster.getName() + "</gold>"));
            return;
        }
        petCooldowns.put(player.getUniqueId(), now);

        boolean curedShock = false;
        if (rooster.hasInjury(com.dark.roostergenics.model.RoosterInjury.SHOCK_DE_COMBATE)) {
            rooster.removeInjury(com.dark.roostergenics.model.RoosterInjury.SHOCK_DE_COMBATE);
            curedShock = true;
        }

        boolean isMax = rooster.getBondLevel() >= 100;
        if (!isMax) {
            rooster.setBondLevel(Math.min(100, rooster.getBondLevel() + 2));
        }

        repository.saveRooster(rooster);
        updateCompanionName(chicken, rooster);

        chicken.getWorld().spawnParticle(Particle.HEART, chicken.getLocation().add(0, 0.55, 0), 6, 0.2, 0.2, 0.2, 0.05);
        chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.4f);

        if (curedShock) {
            player.sendActionBar(Mm.noItalic("<gold>✨ ¡Tus caricias han calmado a <yellow>" + rooster.getName() + "</yellow>! Recuperado de <green>Shock de Combate</green>"));
        } else if (isMax) {
            player.sendActionBar(Mm.noItalic("<light_purple>❤ Acaricias a <gold>" + rooster.getName() + "</gold>. ¡Vínculo al máximo nivel! <yellow>(100% ❤)"));
        } else {
            player.sendActionBar(Mm.noItalic("<light_purple>❤ Acaricias a <gold>" + rooster.getName() + "</gold>. ¡Cacarea alegremente! <yellow>(Vínculo: " + rooster.getBondLevel() + "%)"));
        }
    }

    public Chicken getCompanionEntity(Player player) {
        return getCompanion(player);
    }

    private void updateCompanionName(Chicken chicken, RoosterData rooster) {
        String plumage = rooster.getGenetics() != null ? rooster.getGenetics().getExpressedPlumage() : "PLAINS";
        String tag = com.dark.roostergenics.genetics.RoosterGenetics.getInvisibleVariantTag(plumage);
        String shortPlumage = com.dark.roostergenics.genetics.RoosterGenetics.getShortPlumageDisplay(plumage);
        String name = "§6★ §e" + rooster.getName() + " §7(Nv." + rooster.getLevel() + " • " + shortPlumage + " §7• §d❤" + rooster.getBondLevel() + "%§7)" + tag;
        chicken.customName(Component.text(name));
    }

    /**
     * Ejecuta una rutina física visible de entrenamiento en el mundo real.
     */
    public void executePhysicalTraining(Player player, RoosterData rooster, Runnable onComplete) {
        Chicken chicken = getCompanion(player);
        if (chicken == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        isTrainingPhysically.put(player.getUniqueId(), true);
        Location loc = chicken.getLocation();
        player.sendMessage(Mm.noItalic("<gold>★ ¡<yellow>" + rooster.getName() + "</yellow> comienza su entrenamiento físico en el campo!"));

        // Segundo 1: Salto y aleteo de calentamiento (usando EntityScheduler nativo)
        chicken.getScheduler().runDelayed(plugin, t -> {
            if (chicken.isValid()) {
                chicken.setVelocity(new Vector(0, 0.38, 0));
                chicken.getWorld().spawnParticle(Particle.CRIT, chicken.getLocation().add(0, 0.4, 0), 10, 0.2, 0.2, 0.2, 0.1);
                chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_CHICKEN_STEP, 1.0f, 1.8f);
            }
        }, null, 10L);

        // Segundo 2: Picotazos veloces hacia el suelo
        chicken.getScheduler().runDelayed(plugin, t -> {
            if (chicken.isValid()) {
                chicken.getWorld().spawnParticle(Particle.BLOCK, chicken.getLocation().add(0, 0.1, 0), 15, 0.2, 0.1, 0.2, 0.1, Material.HAY_BLOCK.createBlockData());
                chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 1.6f);
            }
        }, null, 30L);

        // Segundo 3: Giro acrobático
        chicken.getScheduler().runDelayed(plugin, t -> {
            if (chicken.isValid()) {
                chicken.setVelocity(new Vector(0, 0.25, 0));
                chicken.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, chicken.getLocation().add(0, 0.5, 0), 12, 0.3, 0.3, 0.3, 0.05);
                chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.5f);
            }
        }, null, 50L);

        // Segundo 4: Finalización triunfal y recompensas
        chicken.getScheduler().runDelayed(plugin, t -> {
            isTrainingPhysically.remove(player.getUniqueId());
            if (chicken.isValid()) {
                chicken.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, chicken.getLocation().add(0, 0.6, 0), 16, 0.3, 0.4, 0.3, 0.1);
                chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
                updateCompanionName(chicken, rooster);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }, null, 70L);
    }

    /**
     * Tarea periódica de seguimiento hacia el dueño.
     */
    private void startFollowTask() {
        TaskSchedulerAdapter.runTimer(plugin, () -> {
            for (Map.Entry<UUID, UUID> entry : activeCompanions.entrySet()) {
                UUID pId = entry.getKey();
                UUID chickenId = entry.getValue();

                org.bukkit.entity.Entity entity = Bukkit.getEntity(chickenId);
                if (!(entity instanceof Chicken chicken) || !chicken.isValid()) {
                    activeCompanions.remove(pId);
                    continue;
                }

                Player player = plugin.getServer().getPlayer(pId);
                if (player == null || !player.isOnline()) {
                    chicken.remove();
                    activeCompanions.remove(pId);
                    continue;
                }

                // Si está en entrenamiento físico, no interrumpir con movimiento
                if (Boolean.TRUE.equals(isTrainingPhysically.get(pId))) {
                    continue;
                }

                Location pLoc = player.getLocation();
                Location cLoc = chicken.getLocation();

                if (!cLoc.getWorld().equals(pLoc.getWorld())) {
                    chicken.teleport(pLoc);
                    continue;
                }

                double distSq = cLoc.distanceSquared(pLoc);

                // Teletransporte de seguridad si el jugador se aleja demasiado
                if (distSq > 22 * 22) {
                    chicken.teleport(pLoc);
                    chicken.getWorld().spawnParticle(Particle.POOF, pLoc.clone().add(0, 0.3, 0), 6, 0.2, 0.2, 0.2, 0.02);
                } else if (distSq > 2.8 * 2.8) {
                    // Seguimiento nativo Paper: navegación A* real que camina y salta naturalmente
                    chicken.getPathfinder().moveTo(player, 1.25);
                } else {
                    // Cuando ya está al lado del dueño, detener la marcha y mirarlo con afecto
                    chicken.getPathfinder().stopPathfinding();
                    Vector lookDir = player.getEyeLocation().toVector().subtract(cLoc.toVector().add(new Vector(0, 0.4, 0)));
                    if (lookDir.lengthSquared() > 0.01) {
                        cLoc.setDirection(lookDir);
                    }
                }
            }
        }, 20L, 10L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractCompanion(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity target = event.getRightClicked();
        if (!(target instanceof Chicken chicken)) return;
        if (!chicken.hasMetadata(METADATA_COMPANION)) return;

        Player player = event.getPlayer();
        String ownerStr = chicken.hasMetadata(METADATA_OWNER) ? chicken.getMetadata(METADATA_OWNER).get(0).asString() : "";

        if (!player.getUniqueId().toString().equals(ownerStr)) {
            player.sendMessage(Mm.noItalic("<red>Este gallo de combate le pertenece a otro entrenador."));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        RoosterData rooster = repository.getActiveRooster(player.getUniqueId());
        if (rooster == null) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();

        // 1. USO DE BÁLSAMO SUPREMO DEL CAMPEÓN (Sana secuelas permanentes)
        if (com.dark.roostergenics.storage.LegendaryBalmItem.isBalm(handItem)) {
            java.util.List<com.dark.roostergenics.genetics.RoosterTrait> negTraits = new java.util.ArrayList<>();
            for (com.dark.roostergenics.genetics.RoosterTrait t : rooster.getGenetics().getTraits()) {
                if (!t.isPositive()) {
                    negTraits.add(t);
                }
            }
            if (negTraits.isEmpty()) {
                player.sendMessage(Mm.noItalic("<yellow>" + rooster.getName() + " no sufre de ninguna secuela permanente que requiera el Bálsamo Supremo."));
                return;
            }

            com.dark.roostergenics.genetics.RoosterTrait cured = negTraits.get(0);
            rooster.getGenetics().removeTrait(cured);
            repository.saveRooster(rooster);

            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                handItem.subtract(1);
            }

            chicken.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, chicken.getLocation().add(0, 0.6, 0), 35, 0.4, 0.5, 0.4, 0.1);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.sendMessage(Mm.noItalic("\n<gold>✨ <bold>¡MILAGRO VITAL!</bold> El Bálsamo Supremo del Campeón ha regenerado los tejidos y sanado la secuela permanente <red><bold>" + cured.getDisplayName() + "</bold></red> en <yellow>" + rooster.getName() + "</yellow>!\n"));
            return;
        }

        // 2. ALIMENTACIÓN (Semillas de cultivo)
        if (isSeedMaterial(handItem.getType())) {
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                handItem.setAmount(handItem.getAmount() - 1);
            }

            rooster.setHealthStatus(RoosterHealthStatus.HEALTHY);
            rooster.setBondLevel(rooster.getBondLevel() + 3);

            // Alivio de Fatiga Muscular con semillas
            if (rooster.hasInjury(com.dark.roostergenics.model.RoosterInjury.FATIGA_MUSCULAR)) {
                rooster.removeInjury(com.dark.roostergenics.model.RoosterInjury.FATIGA_MUSCULAR);
                player.sendMessage(Mm.noItalic("<green>✔ Las semillas han restaurado la energía de <gold>" + rooster.getName() + "</gold>, aliviando su Fatiga Muscular."));
            }

            repository.saveRooster(rooster);
            updateCompanionName(chicken, rooster);

            chicken.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, chicken.getLocation().add(0, 0.4, 0), 8, 0.2, 0.2, 0.2, 0.02);
            chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.4f);
            player.sendMessage(Mm.noItalic("<green>✔ Has alimentado a <gold>" + rooster.getName() + "<green> con semillas. ¡Su vigor aumenta! <yellow>(Vínculo: " + rooster.getBondLevel() + "%)"));
            return;
        }

        // 3. CARICIAS (Mano vacía)
        if (handItem.getType().isAir()) {
            petCompanion(player);
        }
    }

    private boolean isSeedMaterial(Material mat) {
        return mat == Material.WHEAT_SEEDS || mat == Material.BEETROOT_SEEDS ||
                mat == Material.MELON_SEEDS || mat == Material.PUMPKIN_SEEDS ||
                mat == Material.NETHER_WART || mat == Material.TORCHFLOWER_SEEDS;
    }

    @EventHandler
    public void onCompanionDamage(EntityDamageEvent event) {
        if (event.getEntity().hasMetadata(METADATA_COMPANION)) {
            // El compañero fiel es invulnerable en el mundo exterior fuera de la arena
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dismissCompanion(event.getPlayer(), false);
        petCooldowns.remove(event.getPlayer().getUniqueId());
        isTrainingPhysically.remove(event.getPlayer().getUniqueId());
    }
}
