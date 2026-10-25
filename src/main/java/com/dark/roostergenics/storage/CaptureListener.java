package com.dark.roostergenics.storage;

import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.model.RoosterData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Escuchador de captura de gallos salvajes mediante jaulas con validación anti-dupes.
 */
public class CaptureListener implements Listener {

    private final RoosterRepository repository;
    private final SecureRandom rng = new SecureRandom();

    public CaptureListener(RoosterRepository repository) {
        this.repository = repository;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (CaptureCageItem.isCaptureCage(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getItem() != null && CaptureCageItem.isCaptureCage(event.getItem())) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Chicken chicken) || !chicken.isValid() || chicken.isDead()) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (!CaptureCageItem.isCaptureCage(itemInHand)) return;

        // Comprobar si es un gallo en combate activo en el palenque o ya está siendo capturado
        if (chicken.hasMetadata("roostergenics_arena") || chicken.hasMetadata("roostergenics_capturing") || chicken.getPersistentDataContainer().has(CaptureCageItem.CAGE_KEY)) {
            player.sendMessage(Component.text("¡No puedes atrapar este gallo en este momento!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Verificar límite de capacidad del corral (máximo 45 ejemplares)
        if (repository.getPlayerRoosters(player.getUniqueId()).size() >= 45) {
            player.sendMessage(Component.text("⚠ ¡Tu corral está lleno (45/45 gallos)! Libera algún ejemplar desde /gallos antes de capturar más.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            event.setCancelled(true);
            return;
        }

        // Marcar inmediatamente en el PDC para prevenir condiciones de carrera (Race Condition) en el mismo tick
        chicken.getPersistentDataContainer().set(CaptureCageItem.CAGE_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        event.setCancelled(true);

        // Consumir 1 jaula de forma segura (si no está en modo creativo)
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            itemInHand.subtract(1);
        }

        // Secuenciar genética silvestre vinculada al bioma del hábitat natural o variante forzada
        RoosterGenetics wildGenetics;
        if (chicken.hasMetadata("roostergenics_wild_plumage")) {
            String forcedPlu = chicken.getMetadata("roostergenics_wild_plumage").getFirst().asString();
            wildGenetics = RoosterGenetics.generateWildWithPlumage(rng, forcedPlu);
        } else {
            wildGenetics = RoosterGenetics.generateWild(rng, chicken.getLocation().getBlock().getBiome());
        }
        String customName = com.dark.roostergenics.model.RoosterNameGenerator.generateName(rng);
        RoosterData newRooster = new RoosterData(UUID.randomUUID(), player.getUniqueId(), customName, wildGenetics);

        repository.saveRooster(newRooster);

        if (repository.getActiveRooster(player.getUniqueId()) == null) {
            repository.setActiveRooster(player.getUniqueId(), newRooster.getRoosterId());
        }

        // Efectos visuales y sonoros
        Location loc = chicken.getLocation();
        chicken.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.05);
        chicken.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.02);
        chicken.getWorld().playSound(loc, Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.2f);
        chicken.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

        // Remover entidad física de forma segura
        chicken.remove();

        player.sendMessage(Component.text("✔ ¡Has capturado con éxito a ", NamedTextColor.GREEN)
                .append(Component.text(customName, NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD))
                .append(Component.text("! Plumaje: ", NamedTextColor.GREEN))
                .append(Component.text(wildGenetics.getPlumage(), NamedTextColor.AQUA))
                .append(Component.text(" | Cresta: ", NamedTextColor.GREEN))
                .append(Component.text(wildGenetics.getComb(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Accede a tu corral de gallos con ", NamedTextColor.GRAY)
                .append(Component.text("/gallos", NamedTextColor.YELLOW)));
    }
}
