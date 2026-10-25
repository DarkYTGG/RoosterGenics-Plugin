package com.dark.roostergenics.trainer;

import com.dark.roostergenics.gui.RoosterHolder;
import com.dark.roostergenics.model.RoosterCombatStats;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.RoosterRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menú interactivo de desafío previo al combate contra un Entrenador Gallero NPC.
 */
public class TrainerGui {

    public static void open(Player player, RoosterTrainer trainer, TrainerManager trainerManager, RoosterRepository repository) {
        RoosterHolder holder = new RoosterHolder("trainer_challenge");
        Inventory inv = Bukkit.createInventory(holder, 45, Component.text("⚔ Desafío: " + trainer.getName(), NamedTextColor.GOLD));
        holder.setInventory(inv);

        TrainerTier tier = trainer.getTier();
        RoosterData tRooster = trainer.getRooster();
        RoosterCombatStats tStats = tRooster != null ? tRooster.calculateCombatStats() : null;

        // 1. Tarjeta del Entrenador (Slot 11)
        List<String> tLore = new ArrayList<>();
        tLore.add("§7Rango: " + tier.getLegacyColor() + tier.getTitle() + " §8(Nv. " + tier.getLevel() + ")");
        tLore.add("§7Récord: §a" + trainer.getWins() + "V §7- §c" + trainer.getLosses() + "D");
        tLore.add(" ");
        tLore.add("§6Gallo Asignado: §f" + (tRooster != null ? tRooster.getName() : "Sin Gallo"));
        if (tStats != null) {
            tLore.add("§7HP: §a" + tStats.getMaxHp() + " §8| ATK: §c" + tStats.getAttack() + " §8| DEF: §9" + tStats.getDefense());
            tLore.add("§7Velocidad: §e" + tStats.getAttackSpeedTicks() + " ticks §8| Evasión: §b" + String.format("%.1f%%", tStats.getEvasion() * 100.0));
            if (!tRooster.getGenetics().getTraits().isEmpty()) {
                tLore.add("§dRasgos Genéticos:");
                for (var trait : tRooster.getGenetics().getTraits()) {
                    tLore.add(" §5✦ " + trait.getDisplayName());
                }
            }
        }
        ItemStack trainerCard = createItem(Material.PLAYER_HEAD, "§6§l" + trainer.getName(), tLore);
        if (trainerCard.getItemMeta() instanceof SkullMeta sm) {
            sm.setOwner(trainer.getSkinName());
            trainerCard.setItemMeta(sm);
        }
        inv.setItem(11, trainerCard);

        // 2. Icono central de VS y Reglas (Slot 13)
        List<String> vsLore = new ArrayList<>();
        vsLore.add("§7Duelo oficial contra un gallero profesional.");
        vsLore.add(" ");
        vsLore.add("§eRecompensas por Victoria:");
        vsLore.add("§a✦ $" + String.format("%.2f", tier.getRewardCoins()) + " en monedas");
        vsLore.add("§b✦ " + tier.getRewardCages() + " Jaula(s) de Captura");
        vsLore.add("§6✦ +1 Victoria al récord de tu gallo");
        vsLore.add(" ");
        vsLore.add("§8Si pierdes, tu gallo entrará en recuperación normal.");
        ItemStack vsItem = createItem(Material.NETHERITE_SWORD, "§e§l⚔ Combate de Palenque ⚔", vsLore);
        inv.setItem(13, vsItem);

        // 3. Tarjeta del Gallo del Jugador (Slot 15)
        RoosterData pRooster = repository.getActiveRooster(player.getUniqueId());
        boolean canFight = false;

        if (pRooster == null) {
            ItemStack noRooster = createItem(Material.BARRIER,
                    "§c¡No tienes ningún gallo activo!",
                    "§7Usa §e/gallos §7para abrir tu corral",
                    "§7y seleccionar un gallo con clic izquierdo."
            );
            inv.setItem(15, noRooster);
        } else {
            RoosterCombatStats pStats = pRooster.calculateCombatStats();
            List<String> pLore = new ArrayList<>();
            pLore.add("§7Estado: " + pRooster.getHealthStatus().getFormatted());
            pLore.add("§7HP: §a" + pStats.getMaxHp() + " §8| ATK: §c" + pStats.getAttack() + " §8| DEF: §9" + pStats.getDefense());
            pLore.add("§7Velocidad: §e" + pStats.getAttackSpeedTicks() + " ticks §8| Evasión: §b" + String.format("%.1f%%", pStats.getEvasion() * 100.0));
            pLore.add("§7Récord: §a" + pRooster.getWins() + "V §7- §c" + pRooster.getLosses() + "D");

            if (!pRooster.isUsableForCombat()) {
                pLore.add(" ");
                pLore.add("§c⚠ ¡Este gallo está noqueado o indispuesto!");
                pLore.add("§7Espera su recuperación o cúralo con /gallos admin curar.");
            } else {
                canFight = true;
                pLore.add(" ");
                pLore.add("§a✔ Listo para el combate.");
            }

            ItemStack playerCard = createItem(Material.BLAZE_ROD, "§a§lTu Gallo: §f" + pRooster.getName(), pLore);
            inv.setItem(15, playerCard);
        }

        // 4. Botón Iniciar Combate (Slot 31)
        if (canFight) {
            ItemStack startBtn = createItem(Material.EMERALD_BLOCK,
                    "§a§l[¡ENTRAR AL CUADRILÁTERO!]",
                    "§7Comienza la batalla inmediatamente en el palenque.",
                    "§e¡Que gane el mejor gallero!"
            );
            inv.setItem(31, startBtn);

            holder.setAction(31, p -> {
                p.closeInventory();
                trainerManager.startTrainerBattle(p, trainer);
            });
        } else {
            ItemStack cantBtn = createItem(Material.GRAY_DYE,
                    "§7[Combate No Disponible]",
                    "§cSelecciona un gallo activo y saludable",
                    "§cpara poder retar a " + trainer.getName() + "."
            );
            inv.setItem(31, cantBtn);
        }

        // Botón Cerrar (Slot 40)
        ItemStack closeBtn = createItem(Material.BARRIER, "§c§l« Cerrar »", List.of());
        inv.setItem(40, closeBtn);
        holder.setAction(40, Player::closeInventory);

        // Decoración de fondo
        ItemStack pane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 45; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.2f);
    }

    private static ItemStack createItem(Material mat, String name, String... loreLines) {
        return createItem(mat, name, Arrays.asList(loreLines));
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> compLore = new ArrayList<>();
            for (String l : lore) {
                compLore.add(Component.text(l));
            }
            meta.lore(compLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
