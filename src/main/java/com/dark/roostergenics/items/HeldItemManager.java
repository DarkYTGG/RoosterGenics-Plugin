package com.dark.roostergenics.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gestor criptográfico de objetos equipables con sellado PDC anti-falsificación y anti-dupes.
 * Anula atributos vanilla de armadura e impide que los jugadores los equipen en su cuerpo.
 */
public class HeldItemManager implements Listener {

    public static final NamespacedKey HELD_ITEM_KEY = new NamespacedKey("roostergenics", "held_item");

    public static ItemStack createItem(RoosterHeldItem item, int amount) {
        ItemStack stack = new ItemStack(item.getMaterial(), Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(item.getFormattedNameComponent());
            meta.lore(item.getLoreComponents());

            // Anular atributos nativos de armadura/daño
            try {
                meta.setAttributeModifiers(com.google.common.collect.ImmutableMultimap.of());
            } catch (Throwable ignored) {}

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
            } catch (Throwable ignored) {}
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ARMOR_TRIM"));
            } catch (Throwable ignored) {}

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(HELD_ITEM_KEY, PersistentDataType.STRING, item.getId());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static RoosterHeldItem getHeldItemFromStack(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(HELD_ITEM_KEY, PersistentDataType.STRING);
        if (id == null) return null;
        return RoosterHeldItem.fromId(id);
    }

    public static boolean isHeldItem(ItemStack stack) {
        return getHeldItemFromStack(stack) != null;
    }

    // =========================================================================
    // LISTENERS: Evitar que el jugador equipe los Held Items como armadura propia
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasItem() && isHeldItem(event.getItem())) {
            // Cancelar clic derecho con el held item para evitar que Minecraft lo equipe como armadura
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // 1. Colocar directamente en la ranura de armadura con el cursor
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (isHeldItem(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
        }

        // 2. Intercambio por tecla numérica (1-9) hacia una ranura de armadura
        if (event.getClick() == ClickType.NUMBER_KEY && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (isHeldItem(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 3. Shift-click sobre un Held Item que sea material de armadura
        if (event.isShiftClick()) {
            ItemStack current = event.getCurrentItem();
            if (isHeldItem(current)) {
                Material mat = current.getType();
                String name = mat.name();
                if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                        name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
                        mat == Material.ELYTRA || mat == Material.SHIELD) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 4. Intercambio por tecla F / SWAP_OFFHAND hacia o desde ranura de armadura
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
                return;
            }
            if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (isHeldItem(offhand) && event.getSlotType() == InventoryType.SlotType.ARMOR) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isHeldItem(event.getOldCursor())) {
            for (int slot : event.getRawSlots()) {
                var slotType = event.getView().getSlotType(slot);
                if (slotType == InventoryType.SlotType.ARMOR) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (isHeldItem(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
