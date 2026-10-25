package com.dark.roostergenics.storage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * Ítem legendario exclusivo otorgado tras coronarse Campeón de la Gran Liga de Gallos.
 * Es el único remedio capaz de sanar quirúrgicamente una secuela permanente en un gallo de combate.
 */
public class LegendaryBalmItem {

    public static final NamespacedKey BALM_KEY = new NamespacedKey("roostergenics", "legendary_balm");

    public static ItemStack createBalm(int amount) {
        ItemStack item = new ItemStack(Material.DRAGON_BREATH, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(mm.deserialize("<!italic><gradient:#FFD700:#FFA500><bold>Bálsamo Supremo del Campeón</bold></gradient>"));
            meta.lore(Arrays.asList(
                    mm.deserialize("<!italic><gray><i>\"Elixir legendario destilado con esencias fénix y fórmulas maestras.\"</i></gray>"),
                    Component.empty(),
                    mm.deserialize("<!italic><gold>★ Trofeo de la Gran Liga de Gallos</gold>"),
                    mm.deserialize("<!italic><yellow>Efecto Milagroso:</yellow>"),
                    mm.deserialize("<!italic><white>Sana y elimina <red><bold>1 Secuela Permanente</bold></red> en un gallo.</white>"),
                    Component.empty(),
                    mm.deserialize("<!italic><gray>Uso: Haz clic derecho con el bálsamo sobre tu</gray>"),
                    mm.deserialize("<!italic><gray>gallo compañero invocado (/rg sacar) o usa /rg curar.</gray>"),
                    Component.empty(),
                    mm.deserialize("<!italic><dark_purple>Objeto Mítico de Campeón</dark_purple>")
            ));

            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(BALM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isBalm(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(BALM_KEY, PersistentDataType.BYTE);
    }
}
