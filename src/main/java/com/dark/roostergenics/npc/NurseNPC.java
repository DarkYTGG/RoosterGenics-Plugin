package com.dark.roostergenics.npc;

import com.dark.roostergenics.center.RoosterCenterManager;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Entidad física de la Enfermera del Palenque:
 * - Fake Player NPC nativo por paquetes NMS con skin completa de enfermera y capas 3D.
 * - Holograma TextDisplay flotante superior.
 * - Entidad Interaction para capturar clics e iniciar la atención médica/curación.
 */
public class NurseNPC {

    public static final NamespacedKey NURSE_NPC_KEY = new NamespacedKey("roostergenics", "nurse_npc");
    public static final String SKIN_NAME = "NurseJoy";

    private final JavaPlugin plugin;
    private final RoosterCenterManager centerManager;
    private final Location location;
    private final UUID instanceId;

    private PlayerNpc playerNpc;
    private TextDisplay hologram;
    private Interaction interaction;

    public NurseNPC(JavaPlugin plugin, RoosterCenterManager centerManager, Location location) {
        this(plugin, centerManager, location, UUID.randomUUID());
    }

    public NurseNPC(JavaPlugin plugin, RoosterCenterManager centerManager, Location location, UUID instanceId) {
        this.plugin = plugin;
        this.centerManager = centerManager;
        this.location = location.clone();
        this.instanceId = instanceId != null ? instanceId : UUID.randomUUID();
    }

    public UUID getInstanceId() {
        return instanceId;
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

        // 1. Fake Player NPC con skin de Enfermera
        this.playerNpc = new PlayerNpc(UUID.randomUUID(), SKIN_NAME, location);
        this.playerNpc.spawn();

        // 2. Holograma TextDisplay flotante
        Location holoLoc = location.clone().add(0, 2.25, 0);
        this.hologram = world.spawn(holoLoc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setDefaultBackground(true);
            td.text(buildHologramText());
            td.setInvulnerable(true);
            td.setPersistent(true);
            td.getPersistentDataContainer().set(NURSE_NPC_KEY, PersistentDataType.STRING, "nurse");
            com.dark.roostergenics.structure.StructureEntityTracker.tagEntityPdc(
                    td, instanceId, com.dark.roostergenics.structure.EntityRole.HOLOGRAM,
                    com.dark.roostergenics.structure.StructureType.ROOSTER_CENTER);
        });

        // 3. Entidad Interaction para capturar clics
        this.interaction = world.spawn(location, Interaction.class, inter -> {
            inter.setInteractionHeight(1.95f);
            inter.setInteractionWidth(0.75f);
            inter.setResponsive(true);
            inter.setInvulnerable(true);
            inter.setPersistent(true);
            inter.getPersistentDataContainer().set(NURSE_NPC_KEY, PersistentDataType.STRING, "nurse");
            inter.setMetadata(RoosterCenterManager.METADATA_NURSE_NPC, new FixedMetadataValue(plugin, true));
            com.dark.roostergenics.structure.StructureEntityTracker.tagEntityPdc(
                    inter, instanceId, com.dark.roostergenics.structure.EntityRole.INTERACTION,
                    com.dark.roostergenics.structure.StructureType.ROOSTER_CENTER);
        });
    }

    public static Component buildHologramText() {
        String line1 = "<bold><gradient:#FF69B4:#FFFFFF>❤ ENFERMERA DEL PALENQUE ❤</gradient></bold>";
        String line2 = "<light_purple><bold>Hermana Joy</bold></light_purple> <gray>(Centro de Gallos)</gray>";
        String line3 = "<yellow>« Clic para Atender a tus Gallos »</yellow>";
        return Mm.text(line1 + "\n" + line2 + "\n" + line3);
    }

    public void handleClick(Player player) {
        if (centerManager != null) {
            centerManager.performHealingProcess(player, location);
        }
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
