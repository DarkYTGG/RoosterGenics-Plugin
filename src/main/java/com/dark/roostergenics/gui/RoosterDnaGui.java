package com.dark.roostergenics.gui;

import com.dark.roostergenics.genetics.Allele;
import com.dark.roostergenics.genetics.GenePair;
import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.genetics.RoosterTrait;
import com.dark.roostergenics.model.RoosterData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz de secuenciación y visualización del genoma diploide de RoosterGenics.
 */
public class RoosterDnaGui {

    public static void open(Player player, RoosterData rooster, Runnable backAction) {
        open(player, rooster, null, backAction);
    }

    public static void open(Player player, RoosterData rooster, com.dark.roostergenics.storage.RoosterRepository repository, Runnable backAction) {
        RoosterHolder holder = new RoosterHolder("rooster_dna");
        Inventory inv = Bukkit.createInventory(holder, 45, Component.text("🧬 ADN RoosterGenics: " + rooster.getName(), NamedTextColor.DARK_PURPLE));
        holder.setInventory(inv);

        // Decoración de fondo
        ItemStack filler = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        RoosterGenetics g = rooster.getGenetics();

        // Resumen General
        ItemStack infoItem = createItem(Material.NETHER_STAR,
                "§d§l" + rooster.getName(),
                "§e⭐ Nivel: §6" + rooster.getLevel() + (rooster.getLevel() >= RoosterData.MAX_LEVEL ? " §c[MÁXIMO]" : " §8[" + rooster.getExperience() + "/" + rooster.getXpForNextLevel() + " XP]"),
                "§7Generación: §f" + rooster.getGeneration(),
                "§7Coeficiente de Endogamia (COI): " + formatCoi(rooster.getInbreedingCoefficient()),
                "§7Plumaje Expresado: §b" + g.getExpressedPlumage(),
                "§7Tipo de Cresta: §e" + g.getExpressedComb(),
                "§7Escala Morfológica: §6" + String.format("%.2f", g.getExpressedScale()) + "x",
                "§7Objeto Equipado: " + (rooster.hasHeldItem() ? "§6" + rooster.getHeldItem().getDisplayName() : "§8[Ninguno]")
        );
        inv.setItem(4, infoItem);

        // Loci de Combate
        inv.setItem(10, createLocusItem(Material.REDSTONE, "Locus de Salud (HP)", g.getMaxHpLocus(), "%.1f HP"));
        inv.setItem(11, createLocusItem(Material.IRON_SWORD, "Locus de Ataque", g.getAttackLocus(), "%.1f ATK"));
        inv.setItem(12, createLocusItem(Material.SHIELD, "Locus de Defensa", g.getDefenseLocus(), "%.1f DEF"));
        inv.setItem(13, createLocusIntItem(Material.FEATHER, "Locus de Velocidad de Picotazo", g.getAttackSpeedTicksLocus(), "%d ticks"));
        inv.setItem(14, createLocusItem(Material.ENDER_PEARL, "Locus de Evasión", g.getEvasionLocus(), "%.2f%%"));
        inv.setItem(15, createLocusItem(Material.ARROW, "Locus de Precisión", g.getAccuracyLocus(), "%.2f%%"));
        inv.setItem(16, createLocusIntItem(Material.BLAZE_POWDER, "Locus de Agresividad", g.getAggressionLocus(), "Nivel %d"));

        // Rasgos y Mutaciones Fenotípicas (Soporte dinámico hasta 14 rasgos en 2 filas)
        if (g.getTraits().isEmpty()) {
            inv.setItem(31, createItem(Material.GRAY_DYE, "§7Sin Mutaciones Activas", "§8Este ejemplar no posee rasgos mutacionales."));
        } else {
            List<Integer> slots = new ArrayList<>();
            if (g.getTraits().size() <= 7) {
                for (int s = 28; s <= 34; s++) slots.add(s);
            } else {
                for (int s = 19; s <= 25; s++) slots.add(s);
                for (int s = 28; s <= 34; s++) slots.add(s);
            }
            int idx = 0;
            for (RoosterTrait trait : g.getTraits()) {
                if (idx >= slots.size()) break;
                Material mat = trait.isPositive() ? Material.EMERALD : Material.POISONOUS_POTATO;
                String color = trait.isPositive() ? "§a" : "§c";
                inv.setItem(slots.get(idx++), createItem(mat,
                        color + "§l" + trait.getDisplayName(),
                        "§7Tipo: " + (trait.isPositive() ? "§aMutación Favorable" : "§cDefecto Genético (Endogamia)"),
                        "§f" + trait.getDescription()
                ));
            }
        }

        // Slot 36: Objeto Equipado (Held Item)
        Material itemMat = rooster.hasHeldItem() ? rooster.getHeldItem().getMaterial() : Material.CHEST;
        ItemStack heldBtn = createItem(itemMat,
                "§6§l🎒 Objeto Equipado",
                "§7Actual: " + (rooster.hasHeldItem() ? "§6" + rooster.getHeldItem().getDisplayName() : "§8Ninguno"),
                "§e[Clic para Gestionar / Equipar Objeto]"
        );
        inv.setItem(36, heldBtn);
        holder.setAction(36, p -> {
            if (repository != null) {
                HeldItemGui.open(p, rooster, repository, () -> open(p, rooster, repository, backAction));
            } else {
                p.sendMessage(Component.text("Usa /gallos objetos para gestionar el equipo.", NamedTextColor.YELLOW));
            }
        });

        // Slot 44: Devolver a la Libertad (Liberar)
        ItemStack releaseBtn = createItem(Material.CHICKEN,
                "§c§l🕊 Devolver a la Libertad",
                "§7Libera este ejemplar para desocupar tu corral.",
                "§e[Clic para Abrir Confirmación]"
        );
        inv.setItem(44, releaseBtn);
        holder.setAction(44, p -> {
            if (repository != null) {
                ReleaseConfirmationGui.open(p, rooster, repository, 
                        () -> open(p, rooster, repository, backAction), 
                        () -> { if (backAction != null) backAction.run(); });
            } else {
                p.sendMessage(Component.text("Usa /gallos liberar para liberar ejemplares.", NamedTextColor.YELLOW));
            }
        });

        // Botón Regresar
        ItemStack back = createItem(Material.BARRIER, "§c§l« Volver al Corral");
        inv.setItem(40, back);
        holder.setAction(40, p -> {
            if (backAction != null) backAction.run();
        });

        player.openInventory(inv);
    }

    private static <T> ItemStack createLocusItem(Material mat, String locusName, GenePair<T> pair, String format) {
        String pVal = formatVal(pair.getPaternal().getValue(), format);
        String mVal = formatVal(pair.getMaternal().getValue(), format);
        String expVal = formatVal(pair.getExpressedValue(), format);

        return createItem(mat,
                "§e§l" + locusName,
                "§7Alelo Paterno: §f" + pVal + " §8(" + formatDom(pair.getPaternal().getDominance()) + ")",
                "§7Alelo Materno: §f" + mVal + " §8(" + formatDom(pair.getMaternal().getDominance()) + ")",
                "§6Expresado Fenotípicamente: §a§l" + expVal
        );
    }

    private static ItemStack createLocusIntItem(Material mat, String locusName, GenePair<Integer> pair, String format) {
        return createItem(mat,
                "§e§l" + locusName,
                "§7Alelo Paterno: §f" + pair.getPaternal().getValue() + " §8(" + formatDom(pair.getPaternal().getDominance()) + ")",
                "§7Alelo Materno: §f" + pair.getMaternal().getValue() + " §8(" + formatDom(pair.getMaternal().getDominance()) + ")",
                "§6Expresado Fenotípicamente: §a§l" + pair.getExpressedValue()
        );
    }

    private static String formatVal(Object val, String format) {
        if (val instanceof Double d) {
            return String.format(format, d);
        }
        return val.toString();
    }

    private static String formatDom(Allele.Dominance dom) {
        return switch (dom) {
            case DOMINANT -> "§cDominante";
            case RECESSIVE -> "§7Recesivo";
            case CODOMINANT -> "§bCodominante";
        };
    }

    private static String formatCoi(double coi) {
        double pct = coi * 100.0;
        if (pct < 5.0) return "§a" + String.format("%.2f", pct) + "% (Bajo / Vigor Híbrido)";
        if (pct < 12.5) return "§e" + String.format("%.2f", pct) + "% (Moderado)";
        return "§c" + String.format("%.2f", pct) + "% (¡Alto Riesgo de Malformaciones!)";
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
