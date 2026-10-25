package com.dark.roostergenics.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Escuchador global de clics para los menús basados en RoosterHolder.
 * Incluye blindaje militar contra dupes, ghost items, drags y creative cloning.
 */
public class GuiListener implements Listener {

    private final Map<UUID, Long> lastClickNano = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_THRESHOLD_NANOS = 150_000_000L; // 150ms

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RoosterHolder 
                || (event.getView() != null && event.getView().getTopInventory().getHolder() instanceof RoosterHolder)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCreative(InventoryCreativeEvent event) {
        if (event.getInventory().getHolder() instanceof RoosterHolder 
                || (event.getView() != null && event.getView().getTopInventory().getHolder() instanceof RoosterHolder)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.setItemOnCursor(null);
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null) return;

        boolean isTopRooster = event.getView().getTopInventory().getHolder() instanceof RoosterHolder;
        boolean isHolder = event.getInventory().getHolder() instanceof RoosterHolder;

        if (!isTopRooster && !isHolder) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ClickType click = event.getClick();
        InventoryAction action = event.getAction();

        // Bloqueo total de atajos de exploit y duplicación de ítems
        if (click == ClickType.NUMBER_KEY 
                || click == ClickType.DROP 
                || click == ClickType.CONTROL_DROP 
                || click == ClickType.SWAP_OFFHAND 
                || click == ClickType.DOUBLE_CLICK 
                || click == ClickType.CREATIVE 
                || click == ClickType.MIDDLE) {
            player.setItemOnCursor(null);
            player.updateInventory();
            return;
        }

        if (action == InventoryAction.CLONE_STACK 
                || (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && !event.isShiftClick())
                || action == InventoryAction.HOTBAR_SWAP 
                || action == InventoryAction.COLLECT_TO_CURSOR 
                || action == InventoryAction.SWAP_WITH_CURSOR) {
            player.setItemOnCursor(null);
            player.updateInventory();
            return;
        }

        // Si se hace clic fuera o en el inventario inferior propio
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            player.updateInventory();
            return;
        }

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            player.updateInventory();
            return;
        }

        // Rate-limiting / debounce
        long now = System.nanoTime();
        Long last = lastClickNano.get(player.getUniqueId());
        if (last != null && (now - last) < DEBOUNCE_THRESHOLD_NANOS) {
            player.updateInventory();
            return;
        }
        lastClickNano.put(player.getUniqueId(), now);

        RoosterHolder holder = (RoosterHolder) event.getView().getTopInventory().getHolder();
        if (holder == null) return;

        int slot = event.getRawSlot();
        GuiAction guiAction = null;

        if (event.isShiftClick()) {
            guiAction = holder.getShiftClickAction(slot);
        } else if (event.isRightClick()) {
            guiAction = holder.getRightClickAction(slot);
        }

        if (guiAction == null) {
            guiAction = holder.getAction(slot);
        }

        if (guiAction != null) {
            guiAction.onClick(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof RoosterHolder) {
            if (event.getPlayer() instanceof Player player) {
                if (player.getItemOnCursor() != null && !player.getItemOnCursor().getType().isAir()) {
                    player.setItemOnCursor(null);
                }
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastClickNano.remove(event.getPlayer().getUniqueId());
    }
}
