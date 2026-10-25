package com.dark.roostergenics.gui;

import com.dark.roostergenics.items.HeldItemManager;
import com.dark.roostergenics.items.RoosterHeldItem;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Tienda oficial del Palenque para adquisición de Objetos de Combate (Held Items).
 * Admite moneda de economía de servidor (Vault) y esmeraldas vanilla como respaldo.
 * CERO legacy ChatColor '§' — MiniMessage puro.
 */
public class HeldItemShopGui {

    private static volatile boolean vaultInitialized = false;
    private static Class<?> vaultEcoClass = null;
    private static Method vaultHasMethod = null;
    private static Method vaultWithdrawMethod = null;
    private static Method vaultGetBalanceMethod = null;

    private static synchronized void initVaultReflection() {
        if (vaultInitialized) return;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                vaultEcoClass = Class.forName("net.milkbowl.vault.economy.Economy");
                vaultHasMethod = vaultEcoClass.getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
                vaultWithdrawMethod = vaultEcoClass.getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
                vaultGetBalanceMethod = vaultEcoClass.getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            }
        } catch (Throwable ignored) {
            vaultEcoClass = null;
            vaultHasMethod = null;
            vaultWithdrawMethod = null;
            vaultGetBalanceMethod = null;
        } finally {
            vaultInitialized = true;
        }
    }

    private static final int[] ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25
    };

    public static void open(Player player) {
        RoosterHolder holder = new RoosterHolder("rooster_held_shop");
        Inventory inv = Bukkit.createInventory(holder, 36,
                Mm.text("<gradient:#FFD700:#FFA500><bold>\uD83C\uDF92 Tienda del Palenque: Objetos</bold></gradient>"));
        holder.setInventory(inv);

        ItemStack border = createMmItem(Material.ORANGE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, border);
        }

        // Limpiar ranuras de ítems
        for (int slot : ITEM_SLOTS) {
            inv.setItem(slot, null);
        }

        // Info superior: Slot 4
        double balance = getPlayerBalance(player);
        int emeralds = countEmeralds(player);

        String balanceText = balance >= 0
                ? "<green>$" + String.format("%.2f", balance) + " monedas</green>"
                : "<green>" + emeralds + " esmeraldas</green>";

        ItemStack banner = createMmItem(Material.GOLD_BLOCK,
                "<bold><gradient:#FFD700:#FFA500>\uD83D\uDCB0 Tu Billetera del Palenque</gradient></bold>",
                "<#AAAAAA>Fondos disponibles: " + balanceText,
                "<#DDDD00>¡Compra equipamiento táctico para tus gallos!",
                "<#555555>Usa /rg objeto para equiparlos tras adquirirlos."
        );
        inv.setItem(4, banner);

        // Desplegar catálogo de Held Items (hasta 14)
        RoosterHeldItem[] catalog = RoosterHeldItem.values();
        int slotIndex = 0;

        for (RoosterHeldItem item : catalog) {
            if (slotIndex >= ITEM_SLOTS.length) break;
            int targetSlot = ITEM_SLOTS[slotIndex++];

            int coinPrice = getItemPrice(item);
            int emeraldPrice = Math.max(5, coinPrice / 25);

            List<Component> lore = new ArrayList<>();
            lore.add(Mm.noItalic("<gray>──────────────────────────────</gray>"));
            lore.addAll(item.getLoreComponents());
            lore.add(Mm.noItalic("<gray>──────────────────────────────</gray>"));

            if (balance >= 0) {
                lore.add(Mm.noItalic("<yellow>Precio oficial: <gold><bold>" + coinPrice + " monedas</bold></gold> <gray>(o " + emeraldPrice + " esmeraldas)</gray>"));
            } else {
                lore.add(Mm.noItalic("<yellow>Precio en trueque: <green><bold>" + emeraldPrice + " esmeraldas</bold></green>"));
            }

            lore.add(Component.empty());
            lore.add(Mm.noItalic("<gradient:#4ADE80:#22C55E><bold>[CLIC PARA COMPRAR 1 UNIDAD]</bold></gradient>"));

            ItemStack displayStack = new ItemStack(item.getMaterial());
            ItemMeta meta = displayStack.getItemMeta();
            if (meta != null) {
                meta.displayName(item.getFormattedNameComponent());
                meta.lore(lore);
                displayStack.setItemMeta(meta);
            }
            inv.setItem(targetSlot, displayStack);

            holder.setAction(targetSlot, p -> {
                boolean success = tryPurchase(p, item, coinPrice, emeraldPrice);
                if (success) {
                    ItemStack purchased = HeldItemManager.createItem(item, 1);
                    var leftover = p.getInventory().addItem(purchased);
                    for (ItemStack drop : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }

                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
                    p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.2f);
                    p.sendMessage(Mm.noItalic("<green>✔ ¡Has adquirido <bold>" + item.getDisplayName() + "</bold>!</green>"));

                    // Refrescar tienda con fondos actualizados
                    open(p);
                }
            });
        }

        // Slot 31: Cerrar
        ItemStack closeBtn = createMmItem(Material.BARRIER, "<bold><red>\u00AB Salir de la Tienda</red></bold>");
        inv.setItem(31, closeBtn);
        holder.setAction(31, Player::closeInventory);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.7f, 1.1f);
    }

    private static int getItemPrice(RoosterHeldItem item) {
        return switch (item) {
            case BANDA_FOCUS, VIDASFERA, CINTA_ELEGIDA -> 350;
            case RESTOS, CHALECO_ASALTO, CASCABEL_CONCHA -> 300;
            case CASCO_DENTADO, PERISCOPIO, POLVORA_FENIX, AMULETO_PLOMO -> 250;
            case PLUMA_VELOZ, SEMILLA_MILAGROSA -> 200;
            case BAYA_ZIDRA, BAYA_MELOC -> 150;
            default -> 250;
        };
    }

    private static boolean tryPurchase(Player player, RoosterHeldItem item, int coinPrice, int emeraldPrice) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }

        // 1. Intentar con Vault Economy si está presente
        if (!vaultInitialized) {
            initVaultReflection();
        }
        if (vaultEcoClass != null && vaultHasMethod != null && vaultWithdrawMethod != null) {
            try {
                var rsp = Bukkit.getServicesManager().getRegistration(vaultEcoClass);
                if (rsp != null && rsp.getProvider() != null) {
                    Object provider = rsp.getProvider();
                    boolean hasFunds = (boolean) vaultHasMethod.invoke(provider, player, (double) coinPrice);
                    if (hasFunds) {
                        vaultWithdrawMethod.invoke(provider, player, (double) coinPrice);
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Respaldo: Cobro con Esmeraldas del inventario
        int playerEmeralds = countEmeralds(player);
        if (playerEmeralds >= emeraldPrice) {
            removeEmeralds(player, emeraldPrice);
            return true;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
        player.sendMessage(Mm.noItalic("<red>✖ Fondos insuficientes. Se requieren <yellow>" + coinPrice + " monedas</yellow> o <green>" + emeraldPrice + " esmeraldas</green>."));
        return false;
    }

    private static double getPlayerBalance(Player player) {
        if (!vaultInitialized) {
            initVaultReflection();
        }
        if (vaultEcoClass != null && vaultGetBalanceMethod != null) {
            try {
                var rsp = Bukkit.getServicesManager().getRegistration(vaultEcoClass);
                if (rsp != null && rsp.getProvider() != null) {
                    return (double) vaultGetBalanceMethod.invoke(rsp.getProvider(), player);
                }
            } catch (Throwable ignored) {}
        }
        return -1.0;
    }

    private static int countEmeralds(Player player) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.EMERALD) {
                count += is.getAmount();
            }
        }
        return count;
    }

    private static void removeEmeralds(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() == Material.EMERALD) {
                int take = Math.min(remaining, is.getAmount());
                is.setAmount(is.getAmount() - take);
                remaining -= take;
                if (is.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setContents(contents);
    }

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
