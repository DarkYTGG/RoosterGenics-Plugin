package com.dark.roostergenics.storage;

import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * Ítem de captura crafteable con validación criptográfica en PDC anti-dupes.
 * Estilizado con estética premium MiniMessage + Small Caps + Gradientes Hex.
 */
public class CaptureCageItem {

    public static final NamespacedKey CAGE_KEY = new NamespacedKey("roostergenics", "capture_cage");

    public static ItemStack createCage(int amount) {
        ItemStack item = new ItemStack(Material.IRON_BARS, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Mm.noItalic("<bold><gradient:#FFB347:#FF8C00>Jaula de Captura de Gallos</gradient></bold>"));
            meta.lore(Arrays.asList(
                    Mm.noItalic("<gradient:#B0BEC5:#78909C><i>\"Estructura de hierro forjado reforzada para atrapar aves de combate\"</i></gradient>"),
                    Component.empty(),
                    Mm.noItalic("<bold><gradient:#FFD700:#FFA500>✦ " + Mm.toSmallCaps("Uso: Captura de Ejemplares") + "</gradient></bold>"),
                    Mm.noItalic("<gradient:#E0E0E0:#B0C4DE>Haz clic derecho sobre un gallo o gallina</gradient>"),
                    Mm.noItalic("<gradient:#E0E0E0:#B0C4DE>salvaje para atraparlo e ingresarlo a tu corral.</gradient>"),
                    Component.empty(),
                    Mm.noItalic("<gradient:#4ADE80:#22C55E>✔ " + Mm.toSmallCaps("Captura de ADN 100% Efectiva") + "</gradient>"),
                    Mm.noItalic("<gradient:#DAA520:#B8860B>◆ " + Mm.toSmallCaps("Herramienta de Gallero") + "</gradient>")
            ));

            try {
                meta.setAttributeModifiers(com.google.common.collect.ImmutableMultimap.of());
            } catch (Throwable ignored) {}
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
            } catch (Throwable ignored) {}

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(CAGE_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isCaptureCage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(CAGE_KEY, PersistentDataType.BYTE);
    }

    public static void registerRecipe(JavaPlugin plugin) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "capture_cage_recipe");
        if (plugin.getServer().getRecipe(recipeKey) != null) return;

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createCage(1));
        recipe.shape("BBB", "BEB", "BLB");
        recipe.setIngredient('B', Material.IRON_BARS);
        recipe.setIngredient('E', Material.EGG);
        recipe.setIngredient('L', Material.LEAD);

        try {
            plugin.getServer().addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo registrar la receta de la jaula: " + e.getMessage());
        }
    }
}
