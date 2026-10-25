package com.dark.roostergenics.gui;

import com.dark.roostergenics.model.RoosterCombatStats;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.training.RoosterTrainingManager;
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
 * Gimnasio de entrenamiento de EVs para el gallo seleccionado.
 */
public class RoosterTrainingGui {

    public static void open(Player player, RoosterData rooster, RoosterTrainingManager trainingManager, Runnable backAction) {
        RoosterHolder holder = new RoosterHolder("rooster_training");
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("🏋 Palenque: Gimnasio de Gallos", NamedTextColor.GOLD));
        holder.setInventory(inv);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        RoosterCombatStats stats = rooster.calculateCombatStats();

        // Info del ejemplar en entrenamiento
        ItemStack info = createItem(Material.LEATHER_HORSE_ARMOR,
                "§6§l" + rooster.getName(),
                "§e⭐ Nivel: §6" + rooster.getLevel() + (rooster.getLevel() >= RoosterData.MAX_LEVEL ? " §c[MÁXIMO]" : " §8[" + rooster.getExperience() + "/" + rooster.getXpForNextLevel() + " XP]"),
                "§7Estado de Salud: " + rooster.getHealthStatus().getFormatted(),
                "§7Esfuerzo Total: §e" + rooster.getTotalEvs() + " / " + RoosterTrainingManager.MAX_TOTAL_EVS + " EVs",
                "§7HP: §a" + stats.getMaxHp() + " §7(+" + rooster.getEvHp() + " EVs)",
                "§7Ataque: §c" + stats.getAttack() + " §7(+" + rooster.getEvAttack() + " EVs)",
                "§7Defensa: §9" + stats.getDefense() + " §7(+" + rooster.getEvDefense() + " EVs)",
                "§7Velocidad: §e" + stats.getAttackSpeedTicks() + " ticks §7(+" + rooster.getEvSpeed() + " EVs)"
        );
        inv.setItem(4, info);

        // Módulos de Entrenamiento
        // 1. HP
        ItemStack itemHp = createItem(Material.GOLDEN_CARROT,
                "§a§lNutrición de Resistencia (HP)",
                "§7Aumenta la vitalidad y puntos de vida máximos.",
                "§7Puntos actuales: §a" + rooster.getEvHp() + " / " + RoosterTrainingManager.MAX_SINGLE_EV + " EVs",
                "§e[Clic para Entrenar +3 EVs]"
        );
        inv.setItem(11, itemHp);
        holder.setAction(11, p -> {
            if (trainingManager.train(p, rooster, RoosterTrainingManager.TrainingType.HP)) {
                open(p, rooster, trainingManager, backAction);
            }
        });

        // 2. Ataque
        ItemStack itemAtk = createItem(Material.IRON_SWORD,
                "§c§lGolpeo a Maniquí (Ataque)",
                "§7Aumenta la fuerza de picotazos y desgarres.",
                "§7Puntos actuales: §c" + rooster.getEvAttack() + " / " + RoosterTrainingManager.MAX_SINGLE_EV + " EVs",
                "§e[Clic para Entrenar +3 EVs]"
        );
        inv.setItem(13, itemAtk);
        holder.setAction(13, p -> {
            if (trainingManager.train(p, rooster, RoosterTrainingManager.TrainingType.ATTACK)) {
                open(p, rooster, trainingManager, backAction);
            }
        });

        // 3. Defensa
        ItemStack itemDef = createItem(Material.SHIELD,
                "§9§lCoraza de Espolón (Defensa)",
                "§7Endurece la resistencia a los golpes del rival.",
                "§7Puntos actuales: §9" + rooster.getEvDefense() + " / " + RoosterTrainingManager.MAX_SINGLE_EV + " EVs",
                "§e[Clic para Entrenar +3 EVs]"
        );
        inv.setItem(15, itemDef);
        holder.setAction(15, p -> {
            if (trainingManager.train(p, rooster, RoosterTrainingManager.TrainingType.DEFENSE)) {
                open(p, rooster, trainingManager, backAction);
            }
        });

        // 4. Velocidad
        ItemStack itemSpd = createItem(Material.FEATHER,
                "§e§lCardio y Reflejos (Velocidad)",
                "§7Disminuye los ticks necesarios para conectar cada golpe.",
                "§7Puntos actuales: §e" + rooster.getEvSpeed() + " / " + RoosterTrainingManager.MAX_SINGLE_EV + " EVs",
                "§e[Clic para Entrenar +3 EVs]"
        );
        inv.setItem(22, itemSpd);
        holder.setAction(22, p -> {
            if (trainingManager.train(p, rooster, RoosterTrainingManager.TrainingType.SPEED)) {
                open(p, rooster, trainingManager, backAction);
            }
        });

        // Botón Regresar
        ItemStack back = createItem(Material.BARRIER, "§c§l« Volver al Corral");
        inv.setItem(31, back);
        holder.setAction(31, p -> {
            if (backAction != null) backAction.run();
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
