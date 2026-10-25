package com.dark.roostergenics.gui;

import com.dark.roostergenics.genetics.BreedingManager;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz de incubación y cruza genética estilo RoosterGenics con selector visual y pronóstico de consanguinidad.
 */
public class BreedingGui {

    public static void open(Player player, RoosterRepository repository, BreedingManager breedingManager, Runnable backAction) {
        List<RoosterData> roosters = repository.getPlayerRoosters(player.getUniqueId());

        if (roosters.size() < 2) {
            player.sendMessage(Component.text("Necesitas al menos 2 gallos en tu corral para iniciar la cruza.", NamedTextColor.RED));
            if (backAction != null) backAction.run();
            return;
        }

        UUID lastF = breedingManager.getLastFather(player.getUniqueId());
        UUID lastM = breedingManager.getLastMother(player.getUniqueId());

        RoosterData first = null;
        RoosterData second = null;

        if (lastF != null) {
            RoosterData candidate = repository.getRooster(lastF);
            if (candidate != null && player.getUniqueId().equals(candidate.getOwnerId())) {
                first = candidate;
            }
        }
        if (lastM != null) {
            RoosterData candidate = repository.getRooster(lastM);
            if (candidate != null && player.getUniqueId().equals(candidate.getOwnerId())) {
                second = candidate;
            }
        }

        if (first == null) {
            first = roosters.get(0);
        }
        if (second == null || second.getRoosterId().equals(first.getRoosterId())) {
            for (RoosterData r : roosters) {
                if (!r.getRoosterId().equals(first.getRoosterId())) {
                    second = r;
                    break;
                }
            }
        }
        if (second == null && roosters.size() > 1) {
            second = roosters.get(1);
        }

        breedingManager.recordSelectedParents(player.getUniqueId(), first.getRoosterId(), second.getRoosterId());
        openWithParents(player, repository, breedingManager, first, second, backAction);
    }

    private static void openWithParents(Player player,
                                        RoosterRepository repository,
                                        BreedingManager breedingManager,
                                        RoosterData father,
                                        RoosterData mother,
                                        Runnable backAction) {

        RoosterHolder holder = new RoosterHolder("rooster_breeding");
        Inventory inv = Bukkit.createInventory(holder, 36, Mm.noItalic("<dark_purple>🥚 Laboratorio de Crianza RoosterGenics</dark_purple>"));
        holder.setInventory(inv);

        ItemStack filler = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        // Slot 11: Padre (Progenitor A)
        ItemStack fatherItem = createItem(Material.FEATHER,
                "<gold><bold>Progenitor A: " + father.getName() + "</bold></gold>",
                "<gray>Plumaje: <white>" + father.getGenetics().getPlumage() + "</white></gray>",
                "<gray>Generación: <white>" + father.getGeneration() + "</white></gray>",
                "<gray>Nivel: <gold>" + father.getLevel() + "</gold></gray>",
                "<gray>Esterilidad: " + (father.isSterile() ? "<red>¡ESTÉRIL!</red>" : "<green>Fértil</green>"),
                " ",
                "<yellow>[Clic para Elegir Progenitor A de la Lista]</yellow>"
        );
        inv.setItem(11, fatherItem);
        holder.setAction(11, p -> {
            BreedingSelectorGui.open(p, repository, breedingManager, true, mother, selectedA -> {
                breedingManager.recordSelectedParents(p.getUniqueId(), selectedA.getRoosterId(), mother.getRoosterId());
                openWithParents(p, repository, breedingManager, selectedA, mother, backAction);
            }, () -> openWithParents(p, repository, breedingManager, father, mother, backAction));
        });

        // Slot 15: Madre (Progenitor B)
        ItemStack motherItem = createItem(Material.FEATHER,
                "<light_purple><bold>Progenitor B: " + mother.getName() + "</bold></light_purple>",
                "<gray>Plumaje: <white>" + mother.getGenetics().getPlumage() + "</white></gray>",
                "<gray>Generación: <white>" + mother.getGeneration() + "</white></gray>",
                "<gray>Nivel: <gold>" + mother.getLevel() + "</gold></gray>",
                "<gray>Esterilidad: " + (mother.isSterile() ? "<red>¡ESTÉRIL!</red>" : "<green>Fértil</green>"),
                " ",
                "<yellow>[Clic para Elegir Progenitor B de la Lista]</yellow>"
        );
        inv.setItem(15, motherItem);
        holder.setAction(15, p -> {
            BreedingSelectorGui.open(p, repository, breedingManager, false, father, selectedB -> {
                breedingManager.recordSelectedParents(p.getUniqueId(), father.getRoosterId(), selectedB.getRoosterId());
                openWithParents(p, repository, breedingManager, father, selectedB, backAction);
            }, () -> openWithParents(p, repository, breedingManager, father, mother, backAction));
        });

        // Pronóstico Genético Central
        boolean sameParents = father.getRoosterId().equals(mother.getRoosterId());
        boolean canBreed = !sameParents && !father.isSterile() && !mother.isSterile();

        double coi = canBreed ? breedingManager.calculateInbreedingCoefficient(father, mother, repository::getRooster) : 1.0;
        String coiStr = String.format("%.2f%%", coi * 100.0);

        List<String> eggLore = new ArrayList<>();
        eggLore.add("<gray>Pronóstico de Consanguinidad (COI): <yellow>" + coiStr + "</yellow></gray>");
        if (sameParents) {
            eggLore.add("<red>¡No puedes cruzar un ejemplar consigo mismo!</red>");
        } else if (father.isSterile() || mother.isSterile()) {
            eggLore.add("<red>¡Uno de los ejemplares presenta esterilidad genética!</red>");
        } else {
            if (coi > 0.125) {
                eggLore.add("<red>⚠ ¡Alto riesgo de depresión endogámica y malformaciones!</red>");
            } else if (coi < 0.04) {
                eggLore.add("<green>✨ Gran potencial de vigor híbrido (Mutaciones de élite).</green>");
            } else {
                eggLore.add("<green>✔ Cruce genéticamente estable.</green>");
            }
            eggLore.add(" ");
            eggLore.add("<yellow><bold>[CLIC PARA INICIAR CRUZA]</bold></yellow>");
        }

        ItemStack eggItem = createItem(Material.DRAGON_EGG, "<gold><bold>Huevo de Incubación RoosterGenics</bold></gold>", eggLore.toArray(new String[0]));
        inv.setItem(13, eggItem);

        if (canBreed) {
            holder.setAction(13, p -> {
                try {
                    RoosterData child = breedingManager.breed(father, mother, p.getUniqueId(), repository::getRooster);
                    repository.saveRooster(child);

                    p.closeInventory();
                    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1.0, 0), 25, 0.5, 0.5, 0.5, 0.1);
                    p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation().add(0, 1.0, 0), 15, 0.3, 0.3, 0.3, 0.05);
                    p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.5f);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);

                    p.sendMessage(Mm.noItalic("<green>🎉 ¡Ha nacido <gold><bold>" + child.getName() + "</bold></gold> (Gen " + child.getGeneration() + ") con COI de " + String.format("%.2f%%", child.getInbreedingCoefficient() * 100.0) + "!</green>"));

                    RoosterDnaGui.open(p, child, repository, () -> open(p, repository, breedingManager, backAction));
                } catch (Exception e) {
                    p.sendMessage(Component.text("Error al cruzar ejemplares: " + e.getMessage(), NamedTextColor.RED));
                }
            });
        }

        // Botón Regresar
        ItemStack back = createItem(Material.BARRIER, "<red><bold>« Volver al Corral</bold></red>");
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
            meta.displayName(Mm.noItalic(name));
            List<Component> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(Mm.noItalic(l));
            }
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
