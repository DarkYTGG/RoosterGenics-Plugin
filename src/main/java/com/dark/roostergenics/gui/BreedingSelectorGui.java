package com.dark.roostergenics.gui;

import com.dark.roostergenics.genetics.BreedingManager;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.RoosterRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Selector visual ágil para progenitores en el laboratorio de crianza.
 * Elimina la necesidad de ciclar toscamente clic a clic.
 */
public class BreedingSelectorGui {

    public static void open(Player player,
                            RoosterRepository repository,
                            BreedingManager breedingManager,
                            boolean selectingParentA,
                            RoosterData otherParent,
                            Consumer<RoosterData> onSelected,
                            Runnable onCancel) {

        List<RoosterData> roosters = repository.getPlayerRoosters(player.getUniqueId());
        String titleText = selectingParentA ? "🧬 Seleccionar Progenitor A" : "🧬 Seleccionar Progenitor B";

        RoosterHolder holder = new RoosterHolder("rooster_breeding_selector");
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(titleText, NamedTextColor.DARK_PURPLE));
        holder.setInventory(inv);

        // Decoración inferior
        ItemStack barFiller = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, barFiller);
        }

        // Llenar cuadrícula con gallos disponibles
        int slot = 0;
        for (RoosterData rooster : roosters) {
            if (slot >= 45) break;

            boolean isOtherParent = otherParent != null && rooster.getRoosterId().equals(otherParent.getRoosterId());
            boolean isSterile = rooster.isSterile();
            boolean canSelect = !isOtherParent && !isSterile;

            List<String> lore = new ArrayList<>();
            lore.add("§7Nivel: §6" + rooster.getLevel() + " §8| Gen: §f" + rooster.getGeneration());
            lore.add("§7Plumaje: §b" + rooster.getGenetics().getPlumage());
            lore.add("§7Cresta: §e" + rooster.getGenetics().getComb());

            if (isOtherParent) {
                lore.add(" ");
                lore.add("§c⚠ Ya seleccionado como el otro progenitor.");
            } else if (isSterile) {
                lore.add(" ");
                lore.add("§c⚠ ¡Este ejemplar presenta esterilidad genética!");
            } else {
                if (otherParent != null) {
                    double previewCoi = breedingManager.calculateInbreedingCoefficient(rooster, otherParent, repository::getRooster);
                    String coiColor = previewCoi > 0.125 ? "§c" : (previewCoi < 0.04 ? "§a" : "§e");
                    lore.add("§7Pronóstico COI con la pareja: " + coiColor + String.format("%.2f%%", previewCoi * 100.0));
                }
                lore.add(" ");
                lore.add("§a§l[CLIC PARA SELECCIONAR ESTE PROGENITOR]");
            }

            Material mat = isSterile ? Material.POISONOUS_POTATO : (isOtherParent ? Material.BARRIER : Material.FEATHER);
            ItemStack item = createItem(mat, (canSelect ? "§a§l" : "§c") + rooster.getName(), lore.toArray(new String[0]));
            inv.setItem(slot, item);

            final RoosterData selected = rooster;
            if (canSelect) {
                holder.setAction(slot, p -> {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.4f);
                    if (onSelected != null) {
                        onSelected.accept(selected);
                    }
                });
            } else {
                holder.setAction(slot, p -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                });
            }

            slot++;
        }

        // Slot 49: Cancelar / Volver
        ItemStack cancel = createItem(Material.ARROW, "§c§l« Volver a la Incubadora");
        inv.setItem(49, cancel);
        holder.setAction(49, p -> {
            if (onCancel != null) onCancel.run();
        });

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            List<Component> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(Component.text(l).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
