package com.dark.roostergenics.gui;

import com.dark.roostergenics.items.HeldItemManager;
import com.dark.roostergenics.items.RoosterHeldItem;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz de equipamiento de objetos de combate de RoosterGenics para gallos.
 * Soporta hasta 21 objetos mostrados en cuadrícula expandida de 54 slots
 * y equipamiento directo al hacer clic en el inventario del jugador.
 * Renderizado MiniMessage completo — CERO legacy '§'.
 */
public class HeldItemGui {

    private static final int[] DISPLAY_SLOTS = {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public static void open(Player player, RoosterData rooster, RoosterRepository repository, Runnable backAction) {
        RoosterHolder holder = new RoosterHolder("rooster_held_items");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Mm.text("<gradient:#00CED1:#008B8B><bold>\uD83C\uDF92 Objetos de Combate:</bold> " + rooster.getName() + "</gradient>"));
        holder.setInventory(inv);

        ItemStack filler = createMmItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Slot 4: Info del Gallo
        String heldText = rooster.hasHeldItem()
                ? "<gradient:#FFD700:#FFA500>" + rooster.getHeldItem().getDisplayName() + "</gradient>"
                : "<#555555>[Ninguno]";
        ItemStack roosterInfo = createMmItem(Material.LEATHER_HORSE_ARMOR,
                "<bold><gradient:#FFD700:#FFA500>" + rooster.getName() + "</gradient></bold>",
                "<#DDDD00>\u2B50 Nivel: <#FFD700>" + rooster.getLevel(),
                "<#AAAAAA>Plumaje: <#FFFFFF>" + rooster.getGenetics().getPlumage(),
                "<#AAAAAA>Objeto Actual: " + heldText
        );
        inv.setItem(4, roosterInfo);

        // Slot 13: Objeto Actualmente Equipado
        if (rooster.hasHeldItem()) {
            RoosterHeldItem current = rooster.getHeldItem();
            List<Component> lore = new ArrayList<>();
            lore.add(Mm.noItalic("<gradient:#4ADE80:#22C55E>\u2714 Objeto actualmente equipado en este gallo</gradient>"));
            lore.add(Component.empty());
            lore.addAll(current.getLoreComponents("<gradient:#F87171:#DC2626>[Clic para Desequipar y devolver a tu inventario]</gradient>"));

            ItemStack currentStack = new ItemStack(current.getMaterial());
            ItemMeta meta = currentStack.getItemMeta();
            if (meta != null) {
                meta.displayName(current.getFormattedNameComponent());
                meta.lore(lore);
                currentStack.setItemMeta(meta);
            }
            inv.setItem(13, currentStack);

            holder.setAction(13, p -> {
                RoosterHeldItem removed = rooster.getHeldItem();
                rooster.setHeldItem(null);
                repository.saveRooster(rooster);

                ItemStack itemStack = HeldItemManager.createItem(removed, 1);
                var leftovers = p.getInventory().addItem(itemStack);
                if (!leftovers.isEmpty()) {
                    for (ItemStack drop : leftovers.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                }

                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                p.sendMessage(Mm.text("<gradient:#4ADE80:#22C55E>\u2714 Has desequipado:</gradient> <gradient:#FFD700:#FFA500>" + removed.getDisplayName() + "</gradient>"));

                open(p, rooster, repository, backAction);
            });
        } else {
            ItemStack emptySlot = createMmItem(Material.ITEM_FRAME,
                    "<#AAAAAA>Sin Objeto Equipado",
                    "<#555555>Este gallo no lleva ningún objeto de combate.",
                    "<#DDDD00>Haz clic abajo en cualquier objeto de tu",
                    "<#DDDD00>inventario para equipárselo."
            );
            inv.setItem(13, emptySlot);
        }

        // Limpiar ranuras de exhibición
        for (int slot : DISPLAY_SLOTS) {
            inv.setItem(slot, null);
        }

        // Fila 3, 4 y 5: Objetos detectados en el inventario del jugador
        int displayIdx = 0;
        int foundCount = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack playerItem = player.getInventory().getItem(i);
            if (playerItem != null && HeldItemManager.isHeldItem(playerItem)) {
                RoosterHeldItem held = HeldItemManager.getHeldItemFromStack(playerItem);
                if (held != null) {
                    foundCount++;
                    final int playerInventoryIndex = i;
                    final RoosterHeldItem itemToEquip = held;

                    GuiAction equipAction = p -> {
                        ItemStack currentInInv = p.getInventory().getItem(playerInventoryIndex);
                        if (currentInInv == null || HeldItemManager.getHeldItemFromStack(currentInInv) != itemToEquip) {
                            p.sendMessage(Mm.text("<#F87171>El objeto ya no está en esa ranura de tu inventario."));
                            open(p, rooster, repository, backAction);
                            return;
                        }

                        currentInInv.subtract(1);

                        if (rooster.hasHeldItem()) {
                            RoosterHeldItem oldItem = rooster.getHeldItem();
                            ItemStack oldStack = HeldItemManager.createItem(oldItem, 1);
                            var left = p.getInventory().addItem(oldStack);
                            for (ItemStack d : left.values()) {
                                p.getWorld().dropItemNaturally(p.getLocation(), d);
                            }
                        }

                        rooster.setHeldItem(itemToEquip);
                        repository.saveRooster(rooster);

                        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.2f);
                        p.sendMessage(Mm.text("<gradient:#4ADE80:#22C55E>\uD83C\uDF89 ¡Has equipado</gradient> <bold><gradient:#FFD700:#FFA500>" + itemToEquip.getDisplayName() + "</gradient></bold> <gradient:#4ADE80:#22C55E>a " + rooster.getName() + "!</gradient>"));

                        open(p, rooster, repository, backAction);
                    };

                    if (displayIdx < DISPLAY_SLOTS.length) {
                        int targetSlot = DISPLAY_SLOTS[displayIdx++];
                        List<Component> lore = new ArrayList<>();
                        lore.add(Mm.noItalic("<#AAAAAA>Disponible en tu inventario (Ranura " + (playerInventoryIndex + 1) + ")"));
                        lore.add(Component.empty());
                        lore.addAll(itemToEquip.getLoreComponents("<bold><gradient:#4ADE80:#22C55E>[CLIC PARA EQUIPAR A ESTE GALLO]</gradient></bold>"));

                        ItemStack display = new ItemStack(itemToEquip.getMaterial());
                        ItemMeta meta = display.getItemMeta();
                        if (meta != null) {
                            meta.displayName(itemToEquip.getFormattedNameComponent());
                            meta.lore(lore);
                            display.setItemMeta(meta);
                        }
                        inv.setItem(targetSlot, display);
                        holder.setAction(targetSlot, equipAction);
                    }

                    // Mapear también el clic directo en el inventario inferior del jugador
                    int rawSlot = (playerInventoryIndex < 9) ? (81 + playerInventoryIndex) : (45 + playerInventoryIndex);
                    holder.setAction(rawSlot, equipAction);
                }
            }
        }

        if (foundCount == 0) {
            ItemStack noItems = createMmItem(Material.BARRIER,
                    "<#F87171>No tienes objetos de combate",
                    "<#AAAAAA>Consigue objetos en el Mercader, torneos o",
                    "<#AAAAAA>mediante recompensas especiales para equiparlos."
            );
            inv.setItem(31, noItems);
        }

        // Slot 49: Botón Volver
        ItemStack back = createMmItem(Material.ARROW, "<bold><#F87171>\u00AB Regresar</bold>");
        inv.setItem(49, back);
        holder.setAction(49, p -> {
            if (backAction != null) backAction.run();
        });

        player.openInventory(inv);
    }

    /**
     * Crea un ItemStack con display name y lore renderizados por MiniMessage.
     */
    private static ItemStack createMmItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Mm.noItalic(name));
            List<Component> loreList = new ArrayList<>();
            for (String l : loreLines) {
                loreList.add(Mm.noItalic(l));
            }
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
