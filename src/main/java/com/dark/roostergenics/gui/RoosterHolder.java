package com.dark.roostergenics.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class RoosterHolder implements InventoryHolder {

    private final String menuId;
    private final Map<Integer, GuiAction> actions = new HashMap<>();
    private final Map<Integer, GuiAction> rightClickActions = new HashMap<>();
    private final Map<Integer, GuiAction> shiftClickActions = new HashMap<>();
    private Inventory inventory;

    public RoosterHolder(String menuId) {
        this.menuId = menuId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setAction(int slot, GuiAction action) {
        actions.put(slot, action);
    }

    public void setRightClickAction(int slot, GuiAction action) {
        rightClickActions.put(slot, action);
    }

    public void setShiftClickAction(int slot, GuiAction action) {
        shiftClickActions.put(slot, action);
    }

    public GuiAction getAction(int slot) {
        return actions.get(slot);
    }

    public GuiAction getRightClickAction(int slot) {
        return rightClickActions.get(slot);
    }

    public GuiAction getShiftClickAction(int slot) {
        return shiftClickActions.get(slot);
    }
}
