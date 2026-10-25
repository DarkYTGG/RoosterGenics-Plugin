package com.dark.roostergenics.gui;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface GuiAction {
    void onClick(Player player);
}
