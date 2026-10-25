package com.dark.roostergenics.trainer;

import com.dark.roostergenics.npc.PlayerNpc;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entidad física del Entrenador Gallero en el mundo:
 * - Fake Player NPC nativo por paquetes NMS con skin completa de Minecraft y capas 3D.
 * - Holograma TextDisplay flotante superior con nombre custom y estadísticas de su gallo.
 * - Entidad Interaction para capturar clics derecho/izquierdo e interactuar fluidamente.
 */
public class RoosterTrainerNPC {

    public static final NamespacedKey TRAINER_KEY = new NamespacedKey("roostergenics", "trainer_id");

    private final JavaPlugin plugin;
    private final RoosterTrainer trainer;

    private PlayerNpc playerNpc;
    private TextDisplay hologram;
    private Interaction interaction;

    public RoosterTrainerNPC(JavaPlugin plugin, RoosterTrainer trainer) {
        this.plugin = plugin;
        this.trainer = trainer;
    }

    public RoosterTrainer getTrainer() {
        return trainer;
    }

    public PlayerNpc getPlayerNpc() {
        return playerNpc;
    }

    public void spawnEntities() {
        Location loc = trainer.getLocation();
        if (loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();

        // Limpiar posibles entidades huérfanas con la misma clave en la posición (por ejemplo tras reinicios abruptos)
        for (org.bukkit.entity.Entity e : world.getNearbyEntities(loc, 2.5, 3.5, 2.5)) {
            if (e.getPersistentDataContainer().has(TRAINER_KEY, PersistentDataType.STRING)) {
                String tid = e.getPersistentDataContainer().get(TRAINER_KEY, PersistentDataType.STRING);
                if (trainer.getTrainerId().toString().equals(tid)) {
                    e.remove();
                }
            }
        }

        // 1. Fake Player NPC nativo con skin de cuenta de Minecraft y capas 3D
        this.playerNpc = new PlayerNpc(trainer.getTrainerId(), trainer.getSkinName(), loc);
        this.playerNpc.spawn();

        // 2. Holograma flotante superior informativo
        Location holoLoc = loc.clone().add(0, 2.20, 0);
        this.hologram = world.spawn(holoLoc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setDefaultBackground(true);
            td.text(buildHologramText());
            td.getPersistentDataContainer().set(TRAINER_KEY, PersistentDataType.STRING, trainer.getTrainerId().toString());
        });

        // 3. Entidad Interaction para clics precisos (tanto derecho como izquierdo)
        this.interaction = world.spawn(loc, Interaction.class, inter -> {
            inter.setInteractionHeight(1.9f);
            inter.setInteractionWidth(0.7f);
            inter.setResponsive(true);
            inter.getPersistentDataContainer().set(TRAINER_KEY, PersistentDataType.STRING, trainer.getTrainerId().toString());
        });
    }

    public void updateHologram() {
        if (hologram != null && hologram.isValid()) {
            hologram.text(buildHologramText());
        }
    }

    public Component buildHologramText() {
        TrainerTier tier = trainer.getTier();
        MiniMessage mm = MiniMessage.miniMessage();

        String line1 = "<gold><bold>⚔ " + trainer.getName() + " ⚔</bold></gold>";
        String line2 = "<color:" + tier.getColor().asHexString() + ">" + tier.getTitle() + "</color> <gray>(Nv. " + tier.getLevel() + ")</gray>";
        String line3 = "<yellow>Gallo: </yellow><white>" + (trainer.getRooster() != null ? trainer.getRooster().getName() : "Gallo de Combate") + "</white> " +
                "<dark_gray>(" + (int) tier.getBaseHp() + " HP | " + (int) tier.getBaseAttack() + " ATK)</dark_gray>";
        String line4 = "<green>« Clic para Desafiar »</green>";

        return mm.deserialize(line1 + "\n" + line2 + "\n" + line3 + "\n" + line4);
    }

    public void removeEntities() {
        if (playerNpc != null) {
            playerNpc.destroy();
            playerNpc = null;
        }
        if (hologram != null && hologram.isValid()) {
            hologram.remove();
            hologram = null;
        }
        if (interaction != null && interaction.isValid()) {
            interaction.remove();
            interaction = null;
        }
    }
}
