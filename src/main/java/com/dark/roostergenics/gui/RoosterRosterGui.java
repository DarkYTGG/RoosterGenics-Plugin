package com.dark.roostergenics.gui;

import com.dark.roostergenics.genetics.BreedingManager;
import com.dark.roostergenics.model.RoosterCombatStats;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterInjury;
import com.dark.roostergenics.storage.CaptureCageItem;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.training.RoosterTrainingManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Vista central del corral virtual de gallos anti-lag (sin entidades físicas persistentes).
 */
public class RoosterRosterGui {

    public static void open(Player player, RoosterRepository repository, RoosterTrainingManager trainingManager, BreedingManager breedingManager) {
        RoosterHolder holder = new RoosterHolder("rooster_roster");
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("🐓 Corral de Gallos", NamedTextColor.GOLD));
        holder.setInventory(inv);

        List<RoosterData> roosters = repository.getPlayerRoosters(player.getUniqueId());
        RoosterData activeRooster = repository.getActiveRooster(player.getUniqueId());
        UUID activeId = activeRooster != null ? activeRooster.getRoosterId() : null;

        // Mostrar cada gallo
        int slot = 0;
        for (RoosterData rooster : roosters) {
            if (slot >= 45) break;

            boolean isActive = rooster.getRoosterId().equals(activeId);
            RoosterCombatStats stats = rooster.calculateCombatStats();

            List<String> lore = new ArrayList<>();
            if (isActive) {
                lore.add("§a§l[✔ GALLO ACTIVO PARA COMBATE]");
            } else {
                lore.add("§7[Clic Izquierdo para Seleccionar]");
            }
            String progressBar = getXpProgressBar(rooster.getExperience(), rooster.getXpForNextLevel(), rooster.getLevel());
            lore.add("§e⭐ Nivel: §6§l" + rooster.getLevel() + (rooster.getLevel() >= RoosterData.MAX_LEVEL ? " §c[NIVEL MÁXIMO]" : " §8[" + rooster.getExperience() + "/" + rooster.getXpForNextLevel() + " XP] " + progressBar));
            lore.add("§7Estado: " + rooster.getHealthStatus().getFormatted() + (rooster.isKnockedOut() ? " §8(" + (rooster.getRemainingKnockoutSeconds() / 60) + "m rest.)" : ""));
            lore.add("§7Generación: §f" + rooster.getGeneration() + " §8| Plumaje: §b" + rooster.getGenetics().getPlumage());
            lore.add("§7Récord: §a" + rooster.getWins() + "V §7- §c" + rooster.getLosses() + "D §7- §e" + rooster.getDraws() + "E §8(" + String.format("%.1f%%", rooster.getWinRate()) + ")");
            lore.add("§7HP: §a" + stats.getMaxHp() + " §8| ATK: §c" + stats.getAttack() + " §8| DEF: §9" + stats.getDefense());
            lore.add("§7Velocidad: §e" + stats.getAttackSpeedTicks() + " ticks §8| Evasión: §b" + String.format("%.1f%%", stats.getEvasion() * 100.0));
            lore.add("§7Objeto Equipado: " + (rooster.hasHeldItem() ? "§6" + rooster.getHeldItem().getDisplayName() : "§8Ninguno"));
            if (rooster.hasInjuries()) {
                lore.add("§c⚠ Lesiones: §e" + rooster.getTemporaryInjuries().size() + " activas §8(/rg salud)");
                for (RoosterInjury inj : rooster.getTemporaryInjuries()) {
                    lore.add("  §c✦ " + inj.getDisplayName());
                }
            }
            if (rooster.getConsecutiveLosses() >= 2) {
                lore.add("§4⚠ Racha Derrotas: §c" + rooster.getConsecutiveLosses() + " seguidas (¡Riesgo de Secuela!)");
            }
            lore.add(" ");
            lore.add("§e▶ Clic Izquierdo: §fElegir para combate");
            lore.add("§d▶ Clic Derecho: §fSecuenciador ADN RoosterGenics");
            lore.add("§b▶ Shift + Clic: §fGimnasio de Entrenamiento");

            Material iconMat = isActive ? Material.BLAZE_ROD : Material.FEATHER;
            ItemStack item = createItem(iconMat, (isActive ? "§6§l" : "§e") + rooster.getName(), lore.toArray(new String[0]));
            inv.setItem(slot, item);

            // Acciones por ranura
            final RoosterData r = rooster;
            holder.setAction(slot, p -> {
                repository.setActiveRooster(p.getUniqueId(), r.getRoosterId());
                p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_STEP, 1.0f, 1.5f);
                open(p, repository, trainingManager, breedingManager);
            });

            holder.setRightClickAction(slot, p -> {
                RoosterDnaGui.open(p, r, repository, () -> open(p, repository, trainingManager, breedingManager));
            });

            holder.setShiftClickAction(slot, p -> {
                RoosterTrainingGui.open(p, r, trainingManager, () -> open(p, repository, trainingManager, breedingManager));
            });

            slot++;
        }

        // Si el corral está vacío
        if (roosters.isEmpty()) {
            ItemStack emptyItem = createItem(Material.BARRIER,
                    "§c¡Tu corral está vacío!",
                    "§7Usa una §6Jaula de Captura §7sobre cualquier gallo salvaje",
                    "§7para atraparlo y secuenciar su genética."
            );
            inv.setItem(22, emptyItem);
        }

        // Barra inferior de navegación
        ItemStack barFiller = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, barFiller);
        }

        // 46: Jaula de Captura
        ItemStack cageBtn = createItem(Material.IRON_BARS,
                "§6§lJaula de Captura",
                "§7Crafteo: 7 Barrotes de Hierro + 1 Huevo + 1 Rienda.",
                "§e[Clic para obtener 1 Jaula (Modo Admin/Test)]"
        );
        inv.setItem(46, cageBtn);
        holder.setAction(46, p -> {
            p.getInventory().addItem(CaptureCageItem.createCage(1));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            p.sendMessage(Component.text("Has recibido 1 Jaula de Captura de Gallos.", NamedTextColor.GREEN));
        });

        // 47: Objetos de Combate (Held Items)
        ItemStack itemBtn = createItem(Material.CHEST,
                "§6§l🎒 Objetos de Combate",
                "§7Equipa o desequipa objetos de combate",
                "§7(Banda Focus, Restos, Vidasfera, etc.).",
                "§e[Clic para Abrir Bolsa de Objetos]"
        );
        inv.setItem(47, itemBtn);
        holder.setAction(47, p -> {
            RoosterData active = repository.getActiveRooster(p.getUniqueId());
            if (active == null) {
                p.sendMessage(Component.text("Selecciona primero un gallo en tu corral.", NamedTextColor.RED));
                return;
            }
            HeldItemGui.open(p, active, repository, () -> open(p, repository, trainingManager, breedingManager));
        });

        // 49: Laboratorio de Crianza RoosterGenics
        ItemStack breedBtn = createItem(Material.DRAGON_EGG,
                "§d§lLaboratorio de Crianza RoosterGenics",
                "§7Cruza dos gallos de tu corral y examina el COI.",
                "§e[Clic para Abrir Incubadora]"
        );
        inv.setItem(49, breedBtn);
        holder.setAction(49, p -> {
            BreedingGui.open(p, repository, breedingManager, () -> open(p, repository, trainingManager, breedingManager));
        });

        // 51: Gimnasio
        ItemStack gymBtn = createItem(Material.ANVIL,
                "§b§lGimnasio de Entrenamiento",
                "§7Entrena los EVs del gallo activo.",
                "§e[Clic para Abrir Gimnasio]"
        );
        inv.setItem(51, gymBtn);
        holder.setAction(51, p -> {
            RoosterData active = repository.getActiveRooster(p.getUniqueId());
            if (active == null) {
                p.sendMessage(Component.text("Selecciona primero un gallo en tu corral.", NamedTextColor.RED));
                return;
            }
            RoosterTrainingGui.open(p, active, trainingManager, () -> open(p, repository, trainingManager, breedingManager));
        });

        // 52: Devolver a la Libertad (Liberar Gallo)
        ItemStack releaseBtn = createItem(Material.CHICKEN,
                "§c§l🕊 Devolver a la Libertad",
                "§7Libera el gallo activo para hacer espacio",
                "§7si tu corral se ha llenado.",
                "§e[Clic para Confirmar Liberación]"
        );
        inv.setItem(52, releaseBtn);
        holder.setAction(52, p -> {
            RoosterData active = repository.getActiveRooster(p.getUniqueId());
            if (active == null) {
                p.sendMessage(Component.text("Selecciona primero un gallo en tu corral.", NamedTextColor.RED));
                return;
            }
            ReleaseConfirmationGui.open(p, active, repository, () -> open(p, repository, trainingManager, breedingManager));
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

    private static String getXpProgressBar(int current, int max, int level) {
        if (level >= RoosterData.MAX_LEVEL) {
            return "§6★★★★★";
        }
        if (max <= 0) max = 100;
        int totalBars = 8;
        int filled = Math.min(totalBars, Math.max(0, (int) Math.round(((double) current / max) * totalBars)));
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) sb.append("■");
        sb.append("§7");
        for (int i = filled; i < totalBars; i++) sb.append("■");
        return sb.toString();
    }
}
