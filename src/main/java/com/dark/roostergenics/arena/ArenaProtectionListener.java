package com.dark.roostergenics.arena;

import org.bukkit.entity.Chicken;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Protege a los gallos y el cuadrilátero contra interferencias de jugadores o el entorno durante el combate.
 */
public class ArenaProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Chicken victim)) return;

        if (victim.hasMetadata("roostergenics_arena")) {
            // Permitir solo daño provocado por otro gallo del palenque
            if (!(event.getDamager() instanceof Chicken damager) || !damager.hasMetadata("roostergenics_arena")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Chicken chicken)) return;

        if (chicken.hasMetadata("roostergenics_arena")) {
            // Cancelar daños ambientales ajenos a los espolones del combate
            if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
                    event.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
                    event.getCause() == EntityDamageEvent.DamageCause.FALL ||
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Chicken chicken && chicken.hasMetadata("roostergenics_arena")) {
            event.setCancelled(true);
        }
    }
}
