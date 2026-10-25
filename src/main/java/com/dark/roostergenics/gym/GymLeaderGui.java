package com.dark.roostergenics.gym;

import com.dark.roostergenics.gui.RoosterHolder;
import com.dark.roostergenics.model.RoosterCombatStats;
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
 * Menú interactivo de combate y presentación del Líder de Gimnasio.
 */
public class GymLeaderGui {

    public static void open(Player player, GymLeader leader, GymManager gymManager, RoosterRepository repository) {
        RoosterHolder holder = new RoosterHolder("gym_leader_challenge");
        Inventory inv = Bukkit.createInventory(holder, 27, Mm.noItalic("<dark_gray>Desafío: <gold>" + leader.getGymName()));
        holder.setInventory(inv);

        // 1. Relleno decorativo con cristales oscuros
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        boolean hasBadge = gymManager.hasBadge(player.getUniqueId(), leader);
        RoosterData playerRooster = repository.getActiveRooster(player.getUniqueId());

        // 2. Ranura 13: Ficha del Líder de Gimnasio
        List<String> leaderLore = new ArrayList<>();
        leaderLore.add("<gray>Gimnasio: <yellow>" + leader.getGymName());
        leaderLore.add("<gray>Especialidad: <color:" + leader.getThemeHex() + ">" + leader.getSpecialty() + "</color>");
        leaderLore.add("<gray>Medalla en disputa: <gold>" + leader.getBadgeName());
        leaderLore.add("<gray>Nivel Recomendado: <aqua>Nv. " + leader.getRecommendedLevel() + "+");
        leaderLore.add("<gray>Objeto Equipado: <light_purple>" + (leader.getSignatureItem() != null ? leader.getSignatureItem().getDisplayName() : "Ninguno"));
        leaderLore.add(" ");
        if (hasBadge) {
            leaderLore.add("<green>✔ ¡Ya posees esta Medalla Oficial en tu estuche!");
        } else {
            leaderLore.add("<red>✖ Medalla aún no conquistada.");
        }

        ItemStack leaderItem = createItem(
                leader.getBadgeMaterial(),
                "<bold><gradient:" + leader.getThemeHex() + ":#FFFFFF>" + leader.getLeaderName() + "</gradient></bold> <gray>(" + leader.getGymName() + ")",
                leaderLore.toArray(new String[0])
        );
        inv.setItem(13, leaderItem);

        // 3. Ranura 11: Ficha de tu Gallo de Combate Activo
        List<String> roosterLore = new ArrayList<>();
        if (playerRooster == null) {
            roosterLore.add("<red>No tienes ningún gallo activo seleccionado.");
            roosterLore.add("<yellow>Abre tu corral con /rg y selecciona uno.");
            ItemStack emptyItem = createItem(Material.BARRIER, "<red>Sin Gallo Seleccionado", roosterLore.toArray(new String[0]));
            inv.setItem(11, emptyItem);
        } else {
            RoosterCombatStats stats = playerRooster.calculateCombatStats();
            roosterLore.add("<gray>Nivel: <gold>Nv. " + playerRooster.getLevel());
            roosterLore.add("<gray>Estado: " + playerRooster.getHealthStatus().getFormatted());
            roosterLore.add("<gray>HP: <green>" + stats.getMaxHp() + " <gray>| ATK: <red>" + stats.getAttack() + " <gray>| DEF: <blue>" + stats.getDefense());
            roosterLore.add("<gray>Objeto: <gold>" + (playerRooster.hasHeldItem() ? playerRooster.getHeldItem().getDisplayName() : "Ninguno"));
            roosterLore.add(" ");
            if (playerRooster.getLevel() < leader.getRecommendedLevel()) {
                roosterLore.add("<gold>⚠ Nivel inferior al recomendado (¡Desafío Difícil!)");
            } else {
                roosterLore.add("<green>✔ Nivel adecuado para disputar la medalla.");
            }
            ItemStack roosterItem = createItem(Material.FEATHER, "<yellow><bold>" + playerRooster.getName() + "</bold></yellow>", roosterLore.toArray(new String[0]));
            inv.setItem(11, roosterItem);
        }

        // 4. Ranura 15: Botón de Desafío al Líder
        List<String> challengeLore = new ArrayList<>();
        challengeLore.add("<yellow>Inicia un combate oficial 1v1 en el cuadrilátero.</yellow>");
        challengeLore.add("<gray>Vence al Líder para conquistar la <gold>" + leader.getBadgeName() + "<gray>.</gray>");
        challengeLore.add(" ");
        challengeLore.add("<green>▶ Haz clic para iniciar el combate</green>");

        ItemStack challengeBtn = createItem(Material.DIAMOND_SWORD, "<gold><bold>⚔ ¡DESAFIAR AL LÍDER! ⚔</bold></gold>", challengeLore.toArray(new String[0]));
        inv.setItem(15, challengeBtn);

        holder.setAction(15, p -> {
            p.closeInventory();
            if (playerRooster == null) {
                p.sendMessage(Mm.noItalic("<red>Debes seleccionar un gallo activo en tu corral (/rg) antes de retar al Líder."));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            if (playerRooster.isKnockedOut()) {
                p.sendMessage(Mm.noItalic("<red>Tu gallo activo está K.O. Visita el Centro de Gallos o usa /rg salud antes de combatir."));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            p.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.2f);
            gymManager.challengeGymLeader(p, leader);
        });

        // 5. Ranura 26: Salir
        ItemStack exitBtn = createItem(Material.BARRIER, "<red>Cerrar");
        inv.setItem(26, exitBtn);
        holder.setAction(26, Player::closeInventory);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.3f);
    }

    private static ItemStack createItem(Material mat, String nameMM, String... loreMM) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Mm.noItalic(nameMM));
            List<Component> lore = new ArrayList<>();
            for (String l : loreMM) {
                lore.add(Mm.noItalic(l));
            }
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
