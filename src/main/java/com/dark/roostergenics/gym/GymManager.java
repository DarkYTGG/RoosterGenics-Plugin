package com.dark.roostergenics.gym;

import com.dark.roostergenics.arena.DuelManager;
import com.dark.roostergenics.gui.RoosterHolder;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor central del Sistema de Gimnasios, Medallas y Liga de Gallos.
 */
public class GymManager implements Listener {

    private final JavaPlugin plugin;
    private final RoosterRepository repository;
    private final DuelManager duelManager;

    private final File progressFile;
    private YamlConfiguration progressConfig;
    private final Map<UUID, Set<GymLeader>> playerBadges = new ConcurrentHashMap<>();
    private final Map<GymLeader, GymLeaderNPC> gymNpcs = new ConcurrentHashMap<>();

    public GymManager(JavaPlugin plugin, RoosterRepository repository, DuelManager duelManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.duelManager = duelManager;
        this.progressFile = new File(plugin.getDataFolder(), "gym_progress.yml");
        loadProgress();
    }

    public void loadProgress() {
        playerBadges.clear();
        if (!progressFile.exists()) return;
        progressConfig = YamlConfiguration.loadConfiguration(progressFile);
        ConfigurationSection sec = progressConfig.getConfigurationSection("badges");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            try {
                UUID pId = UUID.fromString(key);
                List<String> list = sec.getStringList(key);
                Set<GymLeader> set = EnumSet.noneOf(GymLeader.class);
                for (String s : list) {
                    try {
                        set.add(GymLeader.valueOf(s));
                    } catch (Exception ignored) {}
                }
                playerBadges.put(pId, set);
            } catch (Exception ignored) {}
        }
    }

    public void saveProgress() {
        if (progressConfig == null) progressConfig = new YamlConfiguration();
        progressConfig.set("badges", null);

        for (Map.Entry<UUID, Set<GymLeader>> entry : playerBadges.entrySet()) {
            List<String> list = new ArrayList<>();
            for (GymLeader g : entry.getValue()) {
                list.add(g.name());
            }
            progressConfig.set("badges." + entry.getKey().toString(), list);
        }

        try {
            progressConfig.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Error guardando progreso de medallas: " + e.getMessage());
        }
    }

    public boolean hasBadge(UUID playerId, GymLeader gym) {
        Set<GymLeader> set = playerBadges.get(playerId);
        return set != null && set.contains(gym);
    }

    public int getBadgeCount(UUID playerId) {
        Set<GymLeader> set = playerBadges.get(playerId);
        return set != null ? set.size() : 0;
    }

    public boolean canEnterLeague(UUID playerId) {
        return getBadgeCount(playerId) >= 6;
    }

    public void awardBadge(Player player, GymLeader gym) {
        Set<GymLeader> set = playerBadges.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(GymLeader.class));
        if (set.add(gym)) {
            saveProgress();
            player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.sendMessage(Mm.noItalic("<gold>★ <yellow>¡FELICITACIONES! Has derrotado a <gradient:" + gym.getThemeHex() + ":#FFF>" + gym.getLeaderName() + "</gradient> y obtenido la <bold>" + gym.getBadgeName() + "</bold>!"));

            if (canEnterLeague(player.getUniqueId())) {
                player.sendMessage(Mm.noItalic("<gold>🏆 ¡HAS CONSEGUIDO LAS 6 MEDALLAS! Ahora tienes acceso a la <light_purple><bold>LIGA DE GALLOS SUPREMA</bold></light_purple> (<yellow>/rg liga</yellow>)."));
            }
        }
    }

    /**
     * Inicia el desafío contra un Líder de Gimnasio.
     */
    public boolean challengeGymLeader(Player player, GymLeader gym) {
        RoosterData playerRooster = repository.getActiveRooster(player.getUniqueId());
        if (playerRooster == null) {
            player.sendMessage(Mm.noItalic("<red>Debes tener un gallo activo seleccionado en tu corral para retar al gimnasio."));
            return false;
        }

        if (hasBadge(player.getUniqueId(), gym)) {
            player.sendMessage(Mm.noItalic("<gray>Ya has conquistado este gimnasio y posees la " + gym.getBadgeName() + "."));
        }

        RoosterData leaderRooster = gym.createGymRooster();
        player.sendMessage(Mm.noItalic("<gold>⚔ Entrando al <yellow>" + gym.getGymName() + "</yellow>... ¡Líder <bold>" + gym.getLeaderName() + "</bold> acepta tu desafío!"));

        // Iniciar duelo en arena más cercana o predeterminada
        duelManager.startPvEDuel(player, playerRooster, gym.getLeaderName(), leaderRooster, 0.0, (won) -> {
            if (won) {
                awardBadge(player, gym);
                double gymReward = gym.getRecommendedLevel() * 100.0;
                if (duelManager.getBettingHook() != null) {
                    duelManager.getBettingHook().depositReward(player, gymReward, "Victoria contra Líder " + gym.getLeaderName());
                }
                player.sendMessage(Mm.noItalic("<gold>💰 [Premio Oficial] ¡Se te han otorgado <green><bold>$" + String.format("%.2f", gymReward) + "</bold></green> por tu victoria en el gimnasio!"));
            } else {
                player.sendMessage(Mm.noItalic("<red>Has sido derrotado por el Líder " + gym.getLeaderName() + ". ¡Entrena a tus gallos y vuelve a intentarlo!"));
            }
        });
        return true;
    }

    /**
     * Inicia el combate contra la Gran Liga de Gallos (Campeón Supremo).
     */
    public void challengeLeague(Player player) {
        if (!canEnterLeague(player.getUniqueId())) {
            int remaining = 6 - getBadgeCount(player.getUniqueId());
            player.sendMessage(Mm.noItalic("<red>¡Aún no puedes desafiar la Liga de Gallos! Necesitas las 6 Medallas de Gimnasio (te faltan " + remaining + "). Usa /rg medallas."));
            return;
        }

        RoosterData playerRooster = repository.getActiveRooster(player.getUniqueId());
        if (playerRooster == null) {
            player.sendMessage(Mm.noItalic("<red>Debes tener un gallo activo seleccionado en tu corral para desafiar la Liga."));
            return;
        }

        player.sendMessage(Mm.noItalic("<gold>🏆 <bold>¡BIENVENIDO A LA GRAN LIGA DE GALLOS!</bold>"));
        player.sendMessage(Mm.noItalic("<yellow>Te enfrentas al Campeón Supremo del Palenque..."));

        RoosterData championRooster = com.dark.roostergenics.trainer.AdaptiveTrainer.createScaledOpponent(playerRooster, "Campeón Supremo");
        championRooster.getGenetics().addTrait(com.dark.roostergenics.genetics.RoosterTrait.GARRAS_AFILADAS_EVO);
        championRooster.getGenetics().addTrait(com.dark.roostergenics.genetics.RoosterTrait.PIEL_DE_HIERRO_EVO);
        championRooster.setLevel(Math.max(60, playerRooster.getLevel() + 2));

        duelManager.startPvEDuel(player, playerRooster, "Campeón Supremo", championRooster, 0.0, (won) -> {
            if (won) {
                player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                Bukkit.broadcast(Mm.noItalic("<gold>🏆 ¡<bold>" + player.getName() + "</bold> ha derrotado al Campeón Supremo y se ha coronado como el nuevo <gradient:#FFD700:#FFA500><bold>CAMPEÓN DE LA LIGA DE GALLOS</bold></gradient>! 🐓"));
                org.bukkit.inventory.ItemStack balm = com.dark.roostergenics.storage.LegendaryBalmItem.createBalm(1);
                player.getInventory().addItem(balm);
                player.sendMessage(Mm.noItalic("<gold>🎁 ¡Has recibido la recompensa máxima: <gradient:#FFD700:#FFA500><bold>Bálsamo Supremo del Campeón</bold></gradient>! Capaz de regenerar y sanar una secuela permanente en un gallo de combate."));
            } else {
                player.sendMessage(Mm.noItalic("<red>Has caído en la Gran Liga de Gallos. ¡Revisa tu genética y vuelve a intentarlo!"));
            }
        });
    }

    /**
     * Abre el estuche interactivo de 6 medallas del jugador.
     */
    public void openBadgeCaseGui(Player player) {
        Inventory inv = Bukkit.createInventory(new RoosterHolder("gym_badges"), 27, Mm.noItalic("<dark_gray>Medallero Oficial de Gallos"));

        // Rellenar bordes con paneles oscuros
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.displayName(Component.empty());
        border.setItemMeta(bm);
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        int[] slots = {10, 11, 12, 14, 15, 16};
        int index = 0;

        for (GymLeader gym : GymLeader.values()) {
            boolean owned = hasBadge(player.getUniqueId(), gym);
            ItemStack item;

            if (owned) {
                item = new ItemStack(gym.getBadgeMaterial());
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Mm.noItalic("<gradient:" + gym.getThemeHex() + ":#FFF><bold>" + gym.getBadgeName() + "</bold></gradient>"));
                meta.lore(List.of(
                        Mm.noItalic("<gray>Gimnasio: <white>" + gym.getGymName()),
                        Mm.noItalic("<gray>Líder: <yellow>" + gym.getLeaderName()),
                        Mm.noItalic("<gray>Especialidad: <white>" + gym.getSpecialty()),
                        Component.empty(),
                        Mm.noItalic("<green>✔ Medalla Conquistada")
                ));
                item.setItemMeta(meta);
            } else {
                item = new ItemStack(Material.GRAY_DYE);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Mm.noItalic("<dark_gray>Medalla Bloqueada (" + gym.getBadgeName() + ")"));
                meta.lore(List.of(
                        Mm.noItalic("<gray>Gimnasio: <white>" + gym.getGymName()),
                        Mm.noItalic("<gray>Líder: <yellow>" + gym.getLeaderName()),
                        Mm.noItalic("<gray>Nivel Recomendado: <yellow>Nv." + gym.getRecommendedLevel()),
                        Component.empty(),
                        Mm.noItalic("<red>✘ Pendiente de Conquistar"),
                        Mm.noItalic("<yellow>Clic para retar al gimnasio")
                ));
                item.setItemMeta(meta);
            }

            inv.setItem(slots[index++], item);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 0.8f, 1.2f);
    }

    @EventHandler
    public void onBadgeGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RoosterHolder holder)) return;
        if (!"gym_badges".equals(holder.getMenuId())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        int[] slots = {10, 11, 12, 14, 15, 16};
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                GymLeader gym = GymLeader.getByOrder(i + 1);
                if (gym != null) {
                    player.closeInventory();
                    challengeGymLeader(player, gym);
                }
                break;
            }
        }
    }

    public GymLeaderNPC spawnGymLeaderNPC(org.bukkit.Location loc, GymLeader leader) {
        return spawnGymLeaderNPC(loc, leader, UUID.randomUUID());
    }

    public GymLeaderNPC spawnGymLeaderNPC(org.bukkit.Location loc, GymLeader leader, UUID instanceId) {
        GymLeaderNPC existing = gymNpcs.remove(leader);
        if (existing != null) {
            existing.remove();
        }
        GymLeaderNPC npc = new GymLeaderNPC(plugin, leader, loc, instanceId);
        npc.spawnEntities();
        gymNpcs.put(leader, npc);
        return npc;
    }

    public Collection<GymLeaderNPC> getAllGymNPCs() {
        return gymNpcs.values();
    }

    public void removeAllGymNPCs() {
        for (GymLeaderNPC npc : gymNpcs.values()) {
            npc.remove();
        }
        gymNpcs.clear();
    }

    public void removeGymNPCNear(org.bukkit.Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return;
        double rSq = radius * radius;
        gymNpcs.entrySet().removeIf(entry -> {
            GymLeaderNPC npc = entry.getValue();
            if (npc != null && npc.getLocation().getWorld().equals(loc.getWorld()) && npc.getLocation().distanceSquared(loc) <= rSq) {
                npc.remove();
                return true;
            }
            return false;
        });
    }

    // =========================================================================
    // LISTENERS: Interacción con el Líder de Gimnasio (Desafío oficial)
    // =========================================================================

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onPlayerInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        handleGymLeaderClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        handleGymLeaderClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            handleGymLeaderClick(player, event.getEntity(), event);
        }
    }

    private void handleGymLeaderClick(Player player, org.bukkit.entity.Entity clicked, org.bukkit.event.Cancellable event) {
        if (clicked == null) return;
        GymLeader leader = null;

        if (clicked.getPersistentDataContainer().has(GymLeaderNPC.GYM_LEADER_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
            String lName = clicked.getPersistentDataContainer().get(GymLeaderNPC.GYM_LEADER_KEY, org.bukkit.persistence.PersistentDataType.STRING);
            try { leader = GymLeader.valueOf(lName); } catch (Exception ignored) {}
        } else if (clicked.hasMetadata("roostergenics_gym_leader")) {
            String lName = clicked.getMetadata("roostergenics_gym_leader").get(0).asString();
            try { leader = GymLeader.valueOf(lName); } catch (Exception ignored) {}
        }

        if (leader != null) {
            event.setCancelled(true);
            GymLeaderGui.open(player, leader, this, repository);
        }
    }
}
