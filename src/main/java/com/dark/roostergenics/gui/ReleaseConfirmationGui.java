package com.dark.roostergenics.gui;

import com.dark.roostergenics.items.HeldItemManager;
import com.dark.roostergenics.items.RoosterHeldItem;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo de confirmación seguro e irreversible para liberar gallos del corral.
 */
public class ReleaseConfirmationGui {

    public static void open(Player player, RoosterData rooster, RoosterRepository repository, Runnable onCancel) {
        open(player, rooster, repository, onCancel, onCancel);
    }

    public static void open(Player player, RoosterData rooster, RoosterRepository repository, Runnable onCancel, Runnable onReleased) {
        RoosterHolder holder = new RoosterHolder("rooster_release_confirm");
        Inventory inv = Bukkit.createInventory(holder, 27, Mm.noItalic("<red>🕊 ¿Liberar a " + rooster.getName() + "?</red>"));
        holder.setInventory(inv);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 4: Info del ejemplar
        ItemStack info = createItem(Material.FEATHER,
                "<yellow><bold>" + rooster.getName() + "</bold></yellow>",
                "<gray>Nivel: <gold>" + rooster.getLevel() + "</gold></gray>",
                "<gray>Generación: <white>" + rooster.getGeneration() + "</white></gray>",
                "<gray>Plumaje: <aqua>" + rooster.getGenetics().getPlumage() + "</aqua></gray>",
                "<gray>Objeto Equipado: " + (rooster.hasHeldItem() ? "<gold>" + rooster.getHeldItem().getDisplayName() + "</gold>" : "<dark_gray>Ninguno</dark_gray>"),
                " ",
                "<red><bold>¡ADVERTENCIA DE SEGURIDAD!</bold></red>",
                "<red>Esta acción es permanente e irreversible.</red>",
                "<red>El ejemplar abandonará tu corral para siempre.</red>"
        );
        inv.setItem(4, info);

        // Slot 11: Confirmar
        ItemStack confirm = createItem(Material.LIME_WOOL,
                "<green><bold>✔ SÍ, LIBERAR EN LA NATURALEZA</bold></green>",
                "<gray>El gallo será devuelto al campo libremente.</gray>",
                "<gray>Si tiene un objeto equipado, se devolverá a tu inventario.</gray>",
                "<gray>Liberarás 1 espacio en tu corral.</gray>"
        );
        inv.setItem(11, confirm);

        holder.setAction(11, p -> {
            // Devolver objeto si tenía uno equipado
            if (rooster.hasHeldItem()) {
                RoosterHeldItem held = rooster.getHeldItem();
                ItemStack stack = HeldItemManager.createItem(held, 1);
                var leftovers = p.getInventory().addItem(stack);
                for (ItemStack d : leftovers.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), d);
                }
                p.sendMessage(Mm.noItalic("<green>✔ Se te ha devuelto el objeto equipado: <yellow>" + held.getDisplayName() + "</yellow></green>"));
            }

            // Eliminar del repositorio
            repository.deleteRooster(rooster.getRoosterId());

            // Efectos
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1.0, 0), 25, 0.4, 0.4, 0.4, 0.05);
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.02);
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_STEP, 1.0f, 1.4f);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);

            p.sendMessage(Mm.noItalic("<green>🕊 ¡Has liberado a <gold><bold>" + rooster.getName() + "</bold></gold> en la naturaleza! Ahora tienes espacio libre en tu corral.</green>"));

            if (onReleased != null) {
                onReleased.run();
            } else {
                p.closeInventory();
            }
        });

        // Slot 15: Cancelar
        ItemStack cancel = createItem(Material.RED_WOOL,
                "<red><bold>✖ CANCELAR Y CONSERVAR</bold></red>",
                "<gray>Regresar al menú sin realizar ningún cambio.</gray>"
        );
        inv.setItem(15, cancel);

        holder.setAction(15, p -> {
            if (onCancel != null) onCancel.run();
        });

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Mm.noItalic(name));
            List<Component> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(Mm.noItalic(l));
            }
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
