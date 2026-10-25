package com.dark.roostergenics.trainer;

import com.dark.roostergenics.storage.RoosterRepository;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Escuchador de interacciones y protección de Entrenadores Galleros NPC.
 * Permite abrir el menú de desafío tanto con clic derecho como con golpe/clic izquierdo.
 */
public class TrainerListener implements Listener {

    private final TrainerManager trainerManager;
    private final RoosterRepository repository;

    public TrainerListener(TrainerManager trainerManager, RoosterRepository repository) {
        this.trainerManager = trainerManager;
        this.repository = repository;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handleTrainerClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        handleTrainerClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrainerAttack(EntityDamageByEntityEvent event) {
        Entity clicked = event.getEntity();
        if (clicked != null && clicked.getPersistentDataContainer().has(RoosterTrainerNPC.TRAINER_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                handleTrainerClick(player, clicked, event);
            }
        }
    }

    private void handleTrainerClick(Player player, Entity clicked, org.bukkit.event.Cancellable event) {
        if (clicked == null) return;
        if (!clicked.getPersistentDataContainer().has(RoosterTrainerNPC.TRAINER_KEY, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);
        String idStr = clicked.getPersistentDataContainer().get(RoosterTrainerNPC.TRAINER_KEY, PersistentDataType.STRING);
        if (idStr == null) return;

        try {
            UUID trainerId = UUID.fromString(idStr);
            RoosterTrainer trainer = trainerManager.getTrainer(trainerId);
            if (trainer != null) {
                TrainerGui.open(player, trainer, trainerManager, repository);
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTrainerDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(RoosterTrainerNPC.TRAINER_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }
}
