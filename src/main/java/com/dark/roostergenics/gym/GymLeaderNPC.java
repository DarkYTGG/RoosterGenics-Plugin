package com.dark.roostergenics.gym;

import com.dark.roostergenics.npc.PlayerNpc;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Entidad física del Líder de Gimnasio en el mundo:
 * - Fake Player NPC nativo por paquetes NMS con skin completa de Minecraft y capas 3D.
 * - Holograma TextDisplay flotante superior con nombre custom y título de gimnasio.
 * - Entidad Interaction para capturar clics derecho/izquierdo e iniciar el diálogo de combate.
 */
public class GymLeaderNPC {

    public static final NamespacedKey GYM_LEADER_KEY = new NamespacedKey("roostergenics", "gym_leader_id");

    private final JavaPlugin plugin;
    private final GymLeader leader;
    private final Location location;
    private final UUID instanceId;

    private PlayerNpc playerNpc;
    private TextDisplay hologram;
    private Interaction interaction;

    public GymLeaderNPC(JavaPlugin plugin, GymLeader leader, Location location) {
        this(plugin, leader, location, UUID.randomUUID());
    }

    public GymLeaderNPC(JavaPlugin plugin, GymLeader leader, Location location, UUID instanceId) {
        this.plugin = plugin;
        this.leader = leader;
        this.location = location.clone();
        this.instanceId = instanceId != null ? instanceId : UUID.randomUUID();
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public GymLeader getLeader() {
        return leader;
    }

    public Location getLocation() {
        return location;
    }

    public PlayerNpc getPlayerNpc() {
        return playerNpc;
    }

    public void spawnEntities() {
        if (location == null || location.getWorld() == null) return;
        World world = location.getWorld();

        // 1. Fake Player NPC nativo con skin temática y capas 3D
        this.playerNpc = new PlayerNpc(UUID.randomUUID(), leader.getSkinName(), location);
        this.playerNpc.spawn();

        // 2. Holograma flotante superior informativo
        Location holoLoc = location.clone().add(0, 2.25, 0);
        this.hologram = world.spawn(holoLoc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setDefaultBackground(true);
            td.text(buildHologramText());
            td.setInvulnerable(true);
            td.setPersistent(true);
            td.getPersistentDataContainer().set(GYM_LEADER_KEY, PersistentDataType.STRING, leader.name());
            com.dark.roostergenics.structure.StructureEntityTracker.tagEntityPdc(
                    td, instanceId, com.dark.roostergenics.structure.EntityRole.HOLOGRAM,
                    com.dark.roostergenics.structure.StructureType.GYM);
        });

        // 3. Entidad Interaction para capturar clics derecho e izquierdo
        this.interaction = world.spawn(location, Interaction.class, inter -> {
            inter.setInteractionHeight(1.95f);
            inter.setInteractionWidth(0.75f);
            inter.setResponsive(true);
            inter.setInvulnerable(true);
            inter.setPersistent(true);
            inter.getPersistentDataContainer().set(GYM_LEADER_KEY, PersistentDataType.STRING, leader.name());
            inter.setMetadata("roostergenics_gym_leader", new FixedMetadataValue(plugin, leader.name()));
            com.dark.roostergenics.structure.StructureEntityTracker.tagEntityPdc(
                    inter, instanceId, com.dark.roostergenics.structure.EntityRole.INTERACTION,
                    com.dark.roostergenics.structure.StructureType.GYM);
        });
    }

    public static Component buildHologramText(GymLeader leader) {
        String line1 = "<bold><gradient:" + leader.getThemeHex() + ":#FFFFFF>⚔ LÍDER DE GIMNASIO ⚔</gradient></bold>";
        String line2 = "<color:" + leader.getThemeHex() + "><bold>" + leader.getLeaderName() + "</bold></color> <gray>(" + leader.getGymName() + ")</gray>";
        String line3 = "<yellow>Medalla: </yellow><gold>" + leader.getBadgeName() + "</gold> <dark_gray>(Nv. " + leader.getRecommendedLevel() + "+)</dark_gray>";
        String line4 = "<green>« Clic para Desafiar »</green>";
        return Mm.text(line1 + "\n" + line2 + "\n" + line3 + "\n" + line4);
    }

    public Component buildHologramText() {
        return buildHologramText(this.leader);
    }

    public void remove() {
        if (playerNpc != null) {
            playerNpc.destroy();
        }
        if (hologram != null && hologram.isValid()) {
            hologram.remove();
        }
        if (interaction != null && interaction.isValid()) {
            interaction.remove();
        }
    }
}
