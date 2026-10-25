package com.dark.roostergenics.npc;

import com.dark.roostergenics.gui.HeldItemShopGui;
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
 * Entidad física del Mercader de Objetos de Combate:
 * - Fake Player NPC nativo por paquetes NMS con skin completa de comerciante y capas 3D.
 * - Holograma TextDisplay flotante superior.
 * - Entidad Interaction para capturar clics derecho/izquierdo y abrir HeldItemShopGui.
 */
public class MerchantNPC {

    public static final NamespacedKey MERCHANT_NPC_KEY = new NamespacedKey("roostergenics", "merchant_npc");
    public static final String SKIN_NAME = "Trader";

    private final JavaPlugin plugin;
    private final Location location;
    private final UUID instanceId;

    private PlayerNpc playerNpc;
    private TextDisplay hologram;
    private Interaction interaction;

    public MerchantNPC(JavaPlugin plugin, Location location) {
        this(plugin, location, UUID.randomUUID());
    }

    public MerchantNPC(JavaPlugin plugin, Location location, UUID instanceId) {
        this.plugin = plugin;
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

        // 1. Fake Player NPC con skin 3D de Mercader
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
            td.getPersistentDataContainer().set(MERCHANT_NPC_KEY, PersistentDataType.STRING, "merchant");
            com.dark.roostergenics.structure.StructureEntityTracker.tagEntityPdc(
                    td, instanceId, com.dark.roostergenics.structure.EntityRole.HOLOGRAM,
                    com.dark.roostergenics.structure.StructureType.MERCHANT_SHOP);
        });

        // 3. Entidad Interaction para capturar clics
        this.interaction = world.spawn(location, Interaction.class, inter -> {
            inter.setInteractionHeight(1.95f);
            inter.setInteractionWidth(0.75f);
            inter.setResponsive(true);
            inter.setInvulnerable(true);
            inter.setPersistent(true);
            inter.getPersistentDataContainer().set(MERCHANT_NPC_KEY, PersistentDataType.STRING, "merchant");
            inter.setMetadata("roostergenics_shop_npc", new FixedMetadataValue(plugin, true));
            com.dark.roostergenics.structure.StructureEntityTracker.tagEntityPdc(
                    inter, instanceId, com.dark.roostergenics.structure.EntityRole.INTERACTION,
                    com.dark.roostergenics.structure.StructureType.MERCHANT_SHOP);
        });
    }

    public static Component buildHologramText() {
        String line1 = "<bold><gradient:#FFD700:#FFA500>\uD83C\uDF92 MERCADER DEL PALENQUE \uD83C\uDF92</gradient></bold>";
        String line2 = "<gold><bold>Garrick el Proveedor</bold></gold> <gray>(Held Items)</gray>";
        String line3 = "<yellow>« Clic para Comprar Objetos »</yellow>";
        return Mm.text(line1 + "\n" + line2 + "\n" + line3);
    }

    public void handleClick(Player player) {
        HeldItemShopGui.open(player);
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
