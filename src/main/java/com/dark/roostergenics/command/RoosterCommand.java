package com.dark.roostergenics.command;

import com.dark.roostergenics.RoostergenicsPlugin;
import com.dark.roostergenics.arena.ArenaBuilder;
import com.dark.roostergenics.arena.DuelManager;
import com.dark.roostergenics.genetics.BreedingManager;
import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.genetics.RoosterTrait;
import com.dark.roostergenics.gui.BreedingGui;
import com.dark.roostergenics.gui.HeldItemGui;
import com.dark.roostergenics.gui.ReleaseConfirmationGui;
import com.dark.roostergenics.gui.RoosterDnaGui;
import com.dark.roostergenics.gui.RoosterRosterGui;
import com.dark.roostergenics.gui.RoosterTrainingGui;
import com.dark.roostergenics.items.HeldItemManager;
import com.dark.roostergenics.items.RoosterHeldItem;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterInjury;
import com.dark.roostergenics.model.RoosterNameGenerator;
import com.dark.roostergenics.storage.CaptureCageItem;
import com.dark.roostergenics.storage.LegendaryBalmItem;
import com.dark.roostergenics.storage.RoosterRepository;
import com.dark.roostergenics.trainer.RoosterTrainer;
import com.dark.roostergenics.trainer.TrainerGui;
import com.dark.roostergenics.trainer.TrainerManager;
import com.dark.roostergenics.trainer.TrainerTier;
import com.dark.roostergenics.training.RoosterTrainingManager;
import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.security.SecureRandom;
import java.util.*;

/**
 * Enrutador integral de comandos para jugadores y administradores de Roostergenics.
 */
public class RoosterCommand implements CommandExecutor, TabCompleter {

    private final RoostergenicsPlugin plugin;
    private final RoosterRepository repository;
    private final DuelManager duelManager;
    private final RoosterTrainingManager trainingManager;
    private final BreedingManager breedingManager;
    private final TrainerManager trainerManager;
    private final com.dark.roostergenics.companion.RoosterCompanionManager companionManager;
    private final com.dark.roostergenics.gym.GymManager gymManager;
    private final com.dark.roostergenics.center.RoosterCenterManager centerManager;
    private final com.dark.roostergenics.structure.StructureManager structureManager;
    private final SecureRandom rng = new SecureRandom();

    public RoosterCommand(RoostergenicsPlugin plugin,
                          RoosterRepository repository,
                          DuelManager duelManager,
                          RoosterTrainingManager trainingManager,
                          BreedingManager breedingManager,
                          TrainerManager trainerManager,
                          com.dark.roostergenics.companion.RoosterCompanionManager companionManager,
                          com.dark.roostergenics.gym.GymManager gymManager,
                          com.dark.roostergenics.center.RoosterCenterManager centerManager,
                          com.dark.roostergenics.structure.StructureManager structureManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.duelManager = duelManager;
        this.trainingManager = trainingManager;
        this.breedingManager = breedingManager;
        this.trainerManager = trainerManager;
        this.companionManager = companionManager;
        this.gymManager = gymManager;
        this.centerManager = centerManager;
        this.structureManager = structureManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
                handleAdminCommand(sender, null, args);
                return true;
            }
            sender.sendMessage("Comandos interactivos de gallos requieren ser ejecutados por un jugador.");
            return true;
        }

        if (args.length == 0) {
            RoosterRosterGui.open(player, repository, trainingManager, breedingManager);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "jaula" -> {
                if (!player.hasPermission("roostergenics.admin")) {
                    sendRecipeInfo(player);
                    return true;
                }
                player.getInventory().addItem(CaptureCageItem.createCage(1));
                player.sendMessage(Component.text("✔ Has recibido 1 Jaula de Captura de Gallos.", NamedTextColor.GREEN));
                return true;
            }
            case "retar" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Uso: /rg retar <jugador> [apuesta]", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(Component.text("El jugador especificado no está en línea.", NamedTextColor.RED));
                    return true;
                }
                double wager = 0.0;
                if (args.length >= 3) {
                    try {
                        wager = Double.parseDouble(args[2]);
                        if (wager < 0 || Double.isNaN(wager) || Double.isInfinite(wager)) {
                            player.sendMessage(Component.text("El monto de la apuesta debe ser un número positivo.", NamedTextColor.RED));
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("El monto de la apuesta debe ser un número válido.", NamedTextColor.RED));
                        return true;
                    }
                }
                duelManager.sendChallenge(player, target, wager);
                return true;
            }
            case "aceptar" -> {
                duelManager.acceptChallenge(player);
                return true;
            }
            case "rechazar" -> {
                duelManager.declineChallenge(player);
                return true;
            }
            case "apostar" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Uso: /rg apostar <jugador> <monto>", NamedTextColor.RED));
                    return true;
                }
                String targetName = args[1];
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                    if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                        player.sendMessage(Component.text("El monto de la apuesta debe ser un número mayor a 0.", NamedTextColor.RED));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("El monto de la apuesta debe ser un número válido.", NamedTextColor.RED));
                    return true;
                }
                duelManager.placeSpectatorBet(player, targetName, amount);
                return true;
            }
            case "dna" -> {
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active == null) {
                    player.sendMessage(Component.text("No tienes ningún gallo activo seleccionado.", NamedTextColor.RED));
                    return true;
                }
                RoosterDnaGui.open(player, active, () -> RoosterRosterGui.open(player, repository, trainingManager, breedingManager));
                return true;
            }
            case "sacar" -> {
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active == null) {
                    player.sendMessage(Component.text("No tienes ningún gallo activo seleccionado en tu corral.", NamedTextColor.RED));
                    return true;
                }
                companionManager.summonCompanion(player, active);
                return true;
            }
            case "invocar", "spawn", "invocargallo", "testgallo" -> {
                if (args.length >= 2) {
                    handleSpawnRooster(player, args[1]);
                    return true;
                }
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active != null) {
                    companionManager.summonCompanion(player, active);
                    return true;
                }
                handleSpawnRooster(player, null);
                return true;
            }
            case "guardar", "descansar" -> {
                if (!companionManager.hasCompanionSummoned(player)) {
                    player.sendMessage(Component.text("No tienes a ningún gallo compañero invocado a tu lado actualmente.", NamedTextColor.RED));
                    return true;
                }
                companionManager.dismissCompanion(player, true);
                return true;
            }
            case "acariciar" -> {
                companionManager.petCompanion(player);
                return true;
            }
            case "salud", "diagnostico", "medico" -> {
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active == null) {
                    player.sendMessage(Component.text("No tienes ningún gallo activo seleccionado en tu corral.", NamedTextColor.RED));
                    return true;
                }
                showMedicalReport(player, active);
                return true;
            }
            case "medallas", "gimnasios" -> {
                gymManager.openBadgeCaseGui(player);
                return true;
            }
            case "liga" -> {
                gymManager.challengeLeague(player);
                return true;
            }
            case "entrenar" -> {
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active == null) {
                    player.sendMessage(Component.text("No tienes ningún gallo activo seleccionado.", NamedTextColor.RED));
                    return true;
                }
                if (companionManager.hasCompanionSummoned(player)) {
                    companionManager.executePhysicalTraining(player, active, () -> {
                        RoosterTrainingGui.open(player, active, trainingManager, () -> RoosterRosterGui.open(player, repository, trainingManager, breedingManager));
                    });
                    return true;
                }
                RoosterTrainingGui.open(player, active, trainingManager, () -> RoosterRosterGui.open(player, repository, trainingManager, breedingManager));
                return true;
            }
            case "criar" -> {
                BreedingGui.open(player, repository, breedingManager, () -> RoosterRosterGui.open(player, repository, trainingManager, breedingManager));
                return true;
            }
            case "tienda", "shop" -> {
                com.dark.roostergenics.gui.HeldItemShopGui.open(player);
                return true;
            }
            case "renombrar" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Uso: /rg renombrar <nuevo nombre>", NamedTextColor.RED));
                    return true;
                }
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active == null) {
                    player.sendMessage(Component.text("No tienes ningún gallo activo para renombrar.", NamedTextColor.RED));
                    return true;
                }
                String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (newName.length() > 24) {
                    player.sendMessage(Component.text("El nombre no puede superar los 24 caracteres.", NamedTextColor.RED));
                    return true;
                }
                active.setName(newName);
                repository.saveRooster(active);
                player.sendMessage(Component.text("✔ Tu gallo ha sido renombrado a: ", NamedTextColor.GREEN)
                        .append(Component.text(newName, NamedTextColor.GOLD, TextDecoration.BOLD)));
                return true;
            }
            case "desafiar" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Uso: /rg desafiar <nombre del entrenador>", NamedTextColor.RED));
                    return true;
                }
                String trainerName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                RoosterTrainer trainer = trainerManager.getTrainerByName(trainerName);
                if (trainer == null) {
                    player.sendMessage(Component.text("No se encontró ningún entrenador llamado: " + trainerName, NamedTextColor.RED));
                    return true;
                }
                TrainerGui.open(player, trainer, trainerManager, repository);
                return true;
            }
            case "npc", "entrenador" -> {
                if (!player.hasPermission("roostergenics.admin")) {
                    player.sendMessage(Component.text("No tienes permisos para usar comandos de NPCs.", NamedTextColor.RED));
                    return true;
                }
                handleTrainerNpcCommand(player, player, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
            case "arena" -> {
                if (!player.hasPermission("roostergenics.admin")) {
                    player.sendMessage(Component.text("No tienes permisos para configurar cuadriláteros.", NamedTextColor.RED));
                    return true;
                }
                handleArenaCommand(player, player, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
            case "admin" -> {
                if (!player.hasPermission("roostergenics.admin")) {
                    player.sendMessage(Component.text("No tienes permisos para usar comandos de administración.", NamedTextColor.RED));
                    return true;
                }
                handleAdminCommand(player, player, args);
                return true;
            }
            case "corral", "menu" -> {
                RoosterRosterGui.open(player, repository, trainingManager, breedingManager);
                return true;
            }
            case "objetos", "equipar", "bolsa" -> {
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active == null) {
                    player.sendMessage(Component.text("No tienes ningún gallo activo seleccionado.", NamedTextColor.RED));
                    return true;
                }
                HeldItemGui.open(player, active, repository, () -> RoosterRosterGui.open(player, repository, trainingManager, breedingManager));
                return true;
            }
            case "liberar" -> {
                RoosterData targetRooster = null;
                if (args.length >= 2) {
                    String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
                    for (RoosterData r : repository.getPlayerRoosters(player.getUniqueId())) {
                        if (r.getName().toLowerCase().contains(query)) {
                            targetRooster = r;
                            break;
                        }
                    }
                    if (targetRooster == null) {
                        player.sendMessage(Component.text("No se encontró ningún gallo en tu corral que coincida con: " + query, NamedTextColor.RED));
                        return true;
                    }
                } else {
                    targetRooster = repository.getActiveRooster(player.getUniqueId());
                    if (targetRooster == null) {
                        player.sendMessage(Component.text("No tienes ningún gallo activo seleccionado para liberar.", NamedTextColor.RED));
                        return true;
                    }
                }
                ReleaseConfirmationGui.open(player, targetRooster, repository, () -> RoosterRosterGui.open(player, repository, trainingManager, breedingManager));
                return true;
            }
            case "mutaciones", "rasgos" -> {
                sendMutationsList(player);
                return true;
            }
            case "listaobjetos", "items" -> {
                sendItemsList(player);
                return true;
            }
            case "crafteo", "receta" -> {
                sendRecipeInfo(player);
                return true;
            }
            case "ayuda", "help", "?" -> {
                sendPlayerHelp(player);
                return true;
            }
            default -> {
                player.sendMessage(Component.text("Comando desconocido. Usa /rg para abrir tu corral interactivo o /rg ayuda para ver los comandos.", NamedTextColor.RED));
                return true;
            }
        }
    }

    private void handleAdminCommand(CommandSender sender, Player player, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender);
            return;
        }

        String adminSub = args[1].toLowerCase();
        switch (adminSub) {
            case "setarena" -> handleArenaCommand(sender, player, new String[]{"crear"});
            case "arena" -> handleArenaCommand(sender, player, Arrays.copyOfRange(args, 2, args.length));
            case "entrenador", "trainer" -> handleTrainerNpcCommand(sender, player, Arrays.copyOfRange(args, 2, args.length));
            case "spawn", "invocar" -> {
                String variant = args.length >= 3 ? args[2] : null;
                Player target = (args.length >= 4) ? Bukkit.getPlayer(args[3]) : player;
                if (target == null) {
                    sender.sendMessage(Component.text("Jugador no encontrado o comando ejecutado desde consola sin especificar jugador objetivo.", NamedTextColor.RED));
                    return;
                }
                handleSpawnRooster(target, variant);
            }
            case "dar" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /gallos admin dar <jaula|gallo|objeto> <jugador> [cantidad/variante/tipo] [nombre]", NamedTextColor.RED));
                    return;
                }
                String giveType = args[2].toLowerCase();
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null || !target.isOnline()) {
                    sender.sendMessage(Component.text("El jugador especificado no está conectado.", NamedTextColor.RED));
                    return;
                }

                if (giveType.equalsIgnoreCase("jaula")) {
                    int amount = 1;
                    if (args.length >= 5) {
                        try { amount = Math.max(1, Integer.parseInt(args[4])); } catch (NumberFormatException ignored) {}
                    }
                    target.getInventory().addItem(CaptureCageItem.createCage(amount));
                    sender.sendMessage(Component.text("✔ Entregadas " + amount + " Jaulas de Captura a " + target.getName() + ".", NamedTextColor.GREEN));
                    target.sendMessage(Component.text("✔ Has recibido " + amount + " Jaula(s) de Captura de Gallos.", NamedTextColor.GREEN));
                } else if (giveType.equalsIgnoreCase("gallo")) {
                    String queryVariant = args.length >= 5 ? args[4] : null;
                    String canonicalPlu = RoosterGenetics.resolvePlumageAlias(queryVariant);
                    String roosterName;
                    RoosterGenetics gen;
                    if (queryVariant != null && (queryVariant.equalsIgnoreCase("arido") || queryVariant.equalsIgnoreCase("frio") || queryVariant.equalsIgnoreCase("cienaga") || queryVariant.equalsIgnoreCase("calido") || queryVariant.equalsIgnoreCase("salvaje") || queryVariant.equalsIgnoreCase("comun") || !canonicalPlu.equalsIgnoreCase("PLAINS") || queryVariant.equalsIgnoreCase("plains"))) {
                        gen = RoosterGenetics.generateWildWithPlumage(rng, canonicalPlu);
                        roosterName = args.length >= 6 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : RoosterNameGenerator.generateName(rng);
                    } else {
                        roosterName = args.length >= 5 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : RoosterNameGenerator.generateName(rng);
                        gen = target.isOnline() ? RoosterGenetics.generateWild(rng, target.getLocation().getBlock().getBiome()) : RoosterGenetics.generateWild(rng);
                    }
                    RoosterData data = new RoosterData(UUID.randomUUID(), target.getUniqueId(), roosterName, gen);
                    repository.saveRooster(data);
                    if (repository.getActiveRooster(target.getUniqueId()) == null) {
                        repository.setActiveRooster(target.getUniqueId(), data.getRoosterId());
                    }
                    sender.sendMessage(Component.text("✔ Se ha generado y añadido a " + target.getName() + " el gallo: " + roosterName + " (" + gen.getPlumage() + ").", NamedTextColor.GREEN));
                    target.sendMessage(Component.text("🎉 ¡Un nuevo gallo ha llegado a tu corral: ", NamedTextColor.GOLD)
                            .append(Component.text(roosterName, NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(Component.text("! Usa /gallos para verlo.", NamedTextColor.GOLD)));
                } else if (giveType.equalsIgnoreCase("objeto") || giveType.equalsIgnoreCase("item")) {
                    if (args.length < 5) {
                        sender.sendMessage(Component.text("Uso: /gallos admin dar objeto <jugador> <tipo|all> [cantidad]", NamedTextColor.RED));
                        return;
                    }
                    String itemQuery = args[4].toLowerCase();
                    int amount = 1;
                    if (args.length >= 6) {
                        try { amount = Math.max(1, Integer.parseInt(args[5])); } catch (NumberFormatException ignored) {}
                    }

                    if (itemQuery.equals("all") || itemQuery.equals("todos")) {
                        int count = 0;
                        for (RoosterHeldItem item : RoosterHeldItem.values()) {
                            target.getInventory().addItem(HeldItemManager.createItem(item, amount));
                            count++;
                        }
                        sender.sendMessage(Component.text("✔ Entregados " + count + " tipos de objetos de combate (" + amount + "x c/u) a " + target.getName() + ".", NamedTextColor.GREEN));
                        target.sendMessage(Component.text("✔ Has recibido todos los objetos de combate de RoosterGenics (" + amount + "x).", NamedTextColor.GREEN));
                        return;
                    }

                    RoosterHeldItem held = RoosterHeldItem.fromId(args[4]);
                    if (held == null) {
                        sender.sendMessage(Component.text("Objeto desconocido. Disponibles: BANDA_FOCUS, RESTOS, VIDASFERA, CASCO_DENTADO, PERISCOPIO, CHALECO_ASALTO, BAYA_ZIDRA, PLUMA_VELOZ, ALL", NamedTextColor.RED));
                        return;
                    }

                    target.getInventory().addItem(HeldItemManager.createItem(held, amount));
                    sender.sendMessage(Component.text("✔ Entregados " + amount + "x " + held.getDisplayName() + " a " + target.getName() + ".", NamedTextColor.GREEN));
                    target.sendMessage(Component.text("✔ Has recibido " + amount + "x ", NamedTextColor.GREEN)
                            .append(Component.text(held.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
                } else {
                    sender.sendMessage(Component.text("Tipo a dar desconocido. Usa 'jaula', 'gallo' u 'objeto'.", NamedTextColor.RED));
                }
            }
            case "curar" -> {
                String targetArg = args.length >= 3 ? args[2] : (player != null ? player.getName() : null);
                if (targetArg == null) {
                    sender.sendMessage(Component.text("Debes especificar el jugador: /gallos admin curar <jugador|all>", NamedTextColor.RED));
                    return;
                }

                if (targetArg.equalsIgnoreCase("all")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        for (RoosterData r : repository.getPlayerRoosters(p.getUniqueId())) {
                            r.reviveOrHeal();
                            repository.saveRooster(r);
                        }
                    }
                    sender.sendMessage(Component.text("✔ Todos los gallos de los jugadores conectados han sido curados y revitalizados.", NamedTextColor.GREEN));
                } else {
                    Player target = Bukkit.getPlayer(targetArg);
                    if (target == null) {
                        sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
                        return;
                    }
                    for (RoosterData r : repository.getPlayerRoosters(target.getUniqueId())) {
                        r.reviveOrHeal();
                        repository.saveRooster(r);
                    }
                    trainingManager.resetAllCooldowns();
                    sender.sendMessage(Component.text("✔ Todos los gallos de " + target.getName() + " han sido curados a Saludable.", NamedTextColor.GREEN));
                    target.sendMessage(Component.text("✔ Tus gallos han sido completamente curados y restaurados.", NamedTextColor.GREEN));
                }
            }
            case "resetcooldowns" -> {
                String targetArg = args.length >= 3 ? args[2] : (player != null ? player.getName() : null);
                if (targetArg == null) {
                    sender.sendMessage(Component.text("Uso: /gallos admin resetcooldowns <jugador>", NamedTextColor.RED));
                    return;
                }
                Player target = Bukkit.getPlayer(targetArg);
                if (target == null) {
                    sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
                    return;
                }
                for (RoosterData r : repository.getPlayerRoosters(target.getUniqueId())) {
                    trainingManager.resetCooldown(r.getRoosterId());
                }
                sender.sendMessage(Component.text("✔ Tiempos de fatiga de entrenamiento reseteados para " + target.getName() + ".", NamedTextColor.GREEN));
            }
            case "editar" -> {
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Uso: /gallos admin editar <jugador> <hp|ataque|defensa|velocidad|escala|coi|nombre|nivel|xp> <valor>", NamedTextColor.RED));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
                    return;
                }
                RoosterData active = repository.getActiveRooster(target.getUniqueId());
                if (active == null) {
                    sender.sendMessage(Component.text(target.getName() + " no tiene ningún gallo activo seleccionado.", NamedTextColor.RED));
                    return;
                }
                String stat = args[3].toLowerCase();
                String valStr = args[4];

                try {
                    switch (stat) {
                        case "hp" -> {
                            double val = Double.parseDouble(valStr);
                            active.getGenetics().setMaxHp(val);
                            sender.sendMessage(Component.text("✔ HP de " + active.getName() + " fijado en " + val, NamedTextColor.GREEN));
                        }
                        case "ataque", "atk" -> {
                            double val = Double.parseDouble(valStr);
                            active.getGenetics().setAttack(val);
                            sender.sendMessage(Component.text("✔ Ataque de " + active.getName() + " fijado en " + val, NamedTextColor.GREEN));
                        }
                        case "defensa", "def" -> {
                            double val = Double.parseDouble(valStr);
                            active.getGenetics().setDefense(val);
                            sender.sendMessage(Component.text("✔ Defensa de " + active.getName() + " fijada en " + val, NamedTextColor.GREEN));
                        }
                        case "velocidad", "spd" -> {
                            int val = Integer.parseInt(valStr);
                            active.getGenetics().setAttackSpeedTicks(val);
                            sender.sendMessage(Component.text("✔ Velocidad (ticks) de " + active.getName() + " fijada en " + val, NamedTextColor.GREEN));
                        }
                        case "escala", "size" -> {
                            double val = Double.parseDouble(valStr);
                            active.getGenetics().setScale(val);
                            sender.sendMessage(Component.text("✔ Escala visual de " + active.getName() + " fijada en " + val, NamedTextColor.GREEN));
                        }
                        case "coi" -> {
                            double val = Double.parseDouble(valStr);
                            active.setInbreedingCoefficient(val);
                            sender.sendMessage(Component.text("✔ COI de " + active.getName() + " fijado en " + val, NamedTextColor.GREEN));
                        }
                        case "nombre" -> {
                            String newName = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                            active.setName(newName);
                            sender.sendMessage(Component.text("✔ Nombre de " + active.getName() + " cambiado a: " + newName, NamedTextColor.GREEN));
                        }
                        case "nivel", "level" -> {
                            int val = Integer.parseInt(valStr);
                            active.setLevel(val);
                            active.setExperience(0);
                            sender.sendMessage(Component.text("✔ Nivel de " + active.getName() + " fijado en " + active.getLevel() + " (XP reiniciada a 0)", NamedTextColor.GREEN));
                        }
                        case "xp", "exp" -> {
                            int val = Integer.parseInt(valStr);
                            int lvlGained = active.addExperience(val);
                            sender.sendMessage(Component.text("✔ Añadidos " + val + " XP a " + active.getName() + " (Nivel actual: " + active.getLevel() + ", +" + lvlGained + " niveles)", NamedTextColor.GREEN));
                        }
                        default -> {
                            sender.sendMessage(Component.text("Estadística desconocida. Usa: hp, ataque, defensa, velocidad, escala, coi, nombre, nivel, xp", NamedTextColor.RED));
                            return;
                        }
                    }
                    repository.saveRooster(active);
                } catch (Exception e) {
                    sender.sendMessage(Component.text("Error en el formato del valor: " + e.getMessage(), NamedTextColor.RED));
                }
            }
            case "trait" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /gallos admin trait <add|remove|clear|evolve> <jugador> [rasgo|all|all_buenas|all_malas] [evolucionar: true|false]", NamedTextColor.RED));
                    return;
                }
                String action = args[2].toLowerCase();
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
                    return;
                }
                RoosterData active = repository.getActiveRooster(target.getUniqueId());
                if (active == null) {
                    sender.sendMessage(Component.text(target.getName() + " no tiene ningún gallo activo seleccionado.", NamedTextColor.RED));
                    return;
                }

                if (action.equals("clear")) {
                    active.getGenetics().clearTraits();
                    repository.saveRooster(active);
                    sender.sendMessage(Component.text("✔ Se han eliminado todos los rasgos de " + active.getName() + ".", NamedTextColor.GREEN));
                    return;
                }

                if (args.length < 5) {
                    sender.sendMessage(Component.text("Debes especificar un rasgo, 'all', 'all_buenas' o 'all_malas'.", NamedTextColor.RED));
                    return;
                }

                String traitQuery = args[4].toUpperCase();
                boolean evolveFlag = args.length >= 6 && Boolean.parseBoolean(args[5]);

                if (action.equals("evolve")) {
                    if (traitQuery.equalsIgnoreCase("ALL")) {
                        int evolvedCount = 0;
                        for (RoosterTrait t : RoosterTrait.getAllBaseTraits()) {
                            if (active.getGenetics().evolveTrait(t, true)) {
                                evolvedCount++;
                            }
                        }
                        repository.saveRooster(active);
                        sender.sendMessage(Component.text("✔ Se evolucionaron " + evolvedCount + " mutaciones de " + active.getName() + " (bypass admin).", NamedTextColor.GREEN));
                        return;
                    }
                    try {
                        RoosterTrait t = RoosterTrait.valueOf(traitQuery);
                        if (active.getGenetics().evolveTrait(t, true)) {
                            repository.saveRooster(active);
                            sender.sendMessage(Component.text("✔ Mutación " + t.getDisplayName() + " evolucionada con éxito en " + active.getName() + ".", NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("No se pudo evolucionar " + t.getDisplayName() + " (¿no la posee o ya está en su fase final?).", NamedTextColor.RED));
                        }
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(Component.text("Rasgo inválido para evolucionar.", NamedTextColor.RED));
                    }
                    return;
                }

                if (action.equals("add")) {
                    List<RoosterTrait> toAdd = new ArrayList<>();
                    switch (traitQuery) {
                        case "ALL", "TODAS" -> toAdd.addAll(Arrays.asList(RoosterTrait.values()));
                        case "ALL_BUENAS", "ALL_POSITIVAS" -> toAdd.addAll(Arrays.asList(RoosterTrait.getPositiveTraits()));
                        case "ALL_MALAS", "ALL_NEGATIVAS" -> toAdd.addAll(Arrays.asList(RoosterTrait.getNegativeTraits()));
                        default -> {
                            try {
                                toAdd.add(RoosterTrait.valueOf(traitQuery));
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(Component.text("Rasgo inválido: " + traitQuery + ". Usa un rasgo válido, 'all', 'all_buenas' o 'all_malas'.", NamedTextColor.RED));
                                return;
                            }
                        }
                    }

                    int addedCount = 0;
                    for (RoosterTrait t : toAdd) {
                        active.getGenetics().addTrait(t);
                        if (evolveFlag) {
                            active.getGenetics().evolveTrait(t, true);
                        }
                        addedCount++;
                    }
                    repository.saveRooster(active);
                    String stateMsg = evolveFlag ? " (EVOLUCIONADAS)" : " (BASE)";
                    sender.sendMessage(Component.text("✔ Añadida(s) " + addedCount + " mutación(es)" + stateMsg + " a " + active.getName() + ".", NamedTextColor.GREEN));
                    return;
                }

                if (action.equals("remove")) {
                    try {
                        RoosterTrait t = RoosterTrait.valueOf(traitQuery);
                        active.getGenetics().removeTrait(t);
                        repository.saveRooster(active);
                        sender.sendMessage(Component.text("✔ Removido rasgo " + t.getDisplayName() + " de " + active.getName() + ".", NamedTextColor.GREEN));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(Component.text("Rasgo inválido a remover.", NamedTextColor.RED));
                    }
                    return;
                }

                sender.sendMessage(Component.text("Acción de trait desconocida. Usa: add, remove, clear, evolve", NamedTextColor.RED));
            }
            case "reload" -> {
                plugin.reloadConfig();
                duelManager.loadConfiguredArena();
                trainerManager.loadTrainers();
                boolean nuz = plugin.getConfig().getBoolean("settings.nuzlocke-mode", false);
                long koDur = plugin.getConfig().getLong("settings.knockout-recovery-minutes", 20L) * 60L * 1000L;
                duelManager.setNuzlockeEnabled(nuz);
                duelManager.setKnockoutDurationMillis(koDur);
                sender.sendMessage(Component.text("✔ Configuración y Entrenadores de Roostergenics recargados con éxito.", NamedTextColor.GREEN));
            }
            case "nuzlocke" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /gallos admin nuzlocke <true|false>", NamedTextColor.RED));
                    return;
                }
                boolean enabled = Boolean.parseBoolean(args[2]);
                duelManager.setNuzlockeEnabled(enabled);
                plugin.getConfig().set("settings.nuzlocke-mode", enabled);
                plugin.saveConfig();
                sender.sendMessage(Component.text("✔ Modo Nuzlocke (Muerte Permanente) configurado en: " + enabled, NamedTextColor.GREEN));
            }
            case "estructura" -> {
                if (player == null) {
                    sender.sendMessage(Component.text("Debes ser un jugador para generar estructuras procedurales en tu ubicación.", NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /gallos admin estructura <gimnasio|centrogallo|tienda|arena> [1-6]", NamedTextColor.RED));
                    return;
                }
                String estType = args[2].toLowerCase();
                Location loc = player.getLocation();
                switch (estType) {
                    case "gimnasio", "gym" -> {
                        int gymIndex = 1;
                        if (args.length >= 4) {
                            try { gymIndex = Math.max(1, Math.min(6, Integer.parseInt(args[3]))); } catch (NumberFormatException ignored) {}
                        }
                        com.dark.roostergenics.gym.GymLeader leader = com.dark.roostergenics.gym.GymLeader.values()[gymIndex - 1];
                        structureManager.buildGym(player, loc, leader);
                    }
                    case "centrogallo", "centro", "clinica" -> {
                        structureManager.buildRoosterCenter(player, loc);
                    }
                    case "tienda", "mercader", "shop" -> {
                        structureManager.buildMerchantShop(player, loc);
                    }
                    case "arena", "palenque", "coliseo" -> {
                        structureManager.buildArena(player, loc);
                    }
                    case "remover", "eliminar", "limpiar", "clear", "remove", "delete" -> {
                        int radius = 10;
                        if (args.length >= 4) {
                            try {
                                radius = Math.max(3, Math.min(30, Integer.parseInt(args[3])));
                            } catch (NumberFormatException ignored) {}
                        }
                        int cleaned = structureManager.removeStructure(loc, radius);
                        sender.sendMessage(Component.text("✔ Estructura y entidades removidas con éxito (" + cleaned + " bloques restaurados en radio de " + radius + " bloques).", NamedTextColor.GREEN));
                    }
                    default -> {
                        sender.sendMessage(Component.text("Estructura desconocida. Disponibles: gimnasio <1-6>, centrogallo, tienda, arena, remover [radio]", NamedTextColor.RED));
                    }
                }
            }
            case "removerestructura", "limpiarestructura" -> {
                if (player == null) {
                    sender.sendMessage(Component.text("Debes ser un jugador para remover estructuras.", NamedTextColor.RED));
                    return;
                }
                int radius = 10;
                if (args.length >= 3) {
                    try {
                        radius = Math.max(3, Math.min(30, Integer.parseInt(args[2])));
                    } catch (NumberFormatException ignored) {}
                }
                int cleaned = structureManager.removeStructure(player.getLocation(), radius);
                sender.sendMessage(Component.text("✔ Estructura y entidades removidas con éxito (" + cleaned + " bloques restaurados en radio de " + radius + " bloques).", NamedTextColor.GREEN));
            }
            case "npc" -> {
                if (player == null) {
                    sender.sendMessage(Component.text("Debes ser un jugador para spawnear NPCs en tu ubicación.", NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /gallos admin npc <enfermera|tendero|lider> [1-6]", NamedTextColor.RED));
                    return;
                }
                String npcType = args[2].toLowerCase();
                Location loc = player.getLocation();
                switch (npcType) {
                    case "enfermera", "nurse" -> {
                        centerManager.spawnNurseNpc(loc);
                        sender.sendMessage(Component.text("✔ Enfermera del Centro de Gallos creada con éxito en tu ubicación.", NamedTextColor.GREEN));
                    }
                    case "tendero", "mercader", "tienda" -> {
                        structureManager.spawnMerchantNpc(loc);
                        sender.sendMessage(Component.text("✔ Mercader de Held Items creado con éxito en tu ubicación.", NamedTextColor.GREEN));
                    }
                    case "lider", "leader", "gymleader" -> {
                        int gymIndex = 1;
                        if (args.length >= 4) {
                            try { gymIndex = Math.max(1, Math.min(6, Integer.parseInt(args[3]))); } catch (NumberFormatException ignored) {}
                        }
                        com.dark.roostergenics.gym.GymLeader leader = com.dark.roostergenics.gym.GymLeader.values()[gymIndex - 1];
                        structureManager.spawnGymLeaderNpc(loc, leader);
                        sender.sendMessage(Component.text("✔ Líder de Gimnasio " + leader.getLeaderName() + " (" + leader.getBadgeName() + ") creado en tu ubicación.", NamedTextColor.GREEN));
                    }
                    default -> {
                        sender.sendMessage(Component.text("Tipo de NPC desconocido. Opciones: enfermera, tendero, lider <1-6>", NamedTextColor.RED));
                    }
                }
            }
            case "bloque" -> {
                if (player == null) {
                    sender.sendMessage(Component.text("Debes ser un jugador para colocar bloques interactivos.", NamedTextColor.RED));
                    return;
                }
                if (args.length >= 3 && args[2].equalsIgnoreCase("curacion")) {
                    centerManager.placeHealingBlock(player.getLocation());
                    sender.sendMessage(Component.text("✔ Bloque de Curación interactivo fijado en tu posición.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Uso: /gallos admin bloque curacion", NamedTextColor.RED));
                }
            }
            case "balsamo" -> {
                if (player == null) {
                    sender.sendMessage(Component.text("Debes ser un jugador para recibir el bálsamo legendario.", NamedTextColor.RED));
                    return;
                }
                player.getInventory().addItem(LegendaryBalmItem.createBalm(1));
                player.sendMessage(Component.text("✔ Has recibido 1 Bálsamo Supremo del Campeón.", NamedTextColor.GREEN));
            }
            case "lesion" -> {
                if (player == null) {
                    sender.sendMessage(Component.text("Debes ser un jugador para gestionar lesiones de tu gallo.", NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /gallos admin lesion <dar|curar|limpiar> [tipo]", NamedTextColor.RED));
                    return;
                }
                RoosterData active = repository.getActiveRooster(player.getUniqueId());
                if (active == null) {
                    sender.sendMessage(Component.text("No tienes ningún gallo activo seleccionado.", NamedTextColor.RED));
                    return;
                }
                String action = args[2].toLowerCase();
                if (action.equals("limpiar") || action.equals("curar_todo")) {
                    active.clearInjuries();
                    repository.saveRooster(active);
                    sender.sendMessage(Component.text("✔ Se han limpiado todas las lesiones de " + active.getName(), NamedTextColor.GREEN));
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /gallos admin lesion <dar|curar> <ALA_LASTIMADA|OJO_HINCHADO|...>", NamedTextColor.RED));
                    return;
                }
                try {
                    RoosterInjury inj = RoosterInjury.valueOf(args[3].toUpperCase());
                    if (action.equals("dar")) {
                        active.addInjury(inj);
                        repository.saveRooster(active);
                        sender.sendMessage(Component.text("✔ Se ha añadido la lesión " + inj.getDisplayName() + " a " + active.getName(), NamedTextColor.GREEN));
                    } else if (action.equals("curar")) {
                        active.removeInjury(inj);
                        repository.saveRooster(active);
                        sender.sendMessage(Component.text("✔ Se ha curado la lesión " + inj.getDisplayName() + " de " + active.getName(), NamedTextColor.GREEN));
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Tipo de lesión inválida. Valores: ALA_LASTIMADA, OJO_HINCHADO, ESPOLON_AGRIETADO, SHOCK_DE_COMBATE, FATIGA_MUSCULAR, HERIDA_ABIERTA", NamedTextColor.RED));
                }
            }
            default -> sendAdminHelp(sender);
        }
    }

    private void handleArenaCommand(CommandSender sender, Player player, String[] args) {
        if (args.length < 1) {
            sendArenaHelp(sender);
            return;
        }

        String arenaAction = args[0].toLowerCase();
        switch (arenaAction) {
            case "fijar", "set" -> {
                if (player == null) {
                    sender.sendMessage("Debes ser un jugador para fijar la arena en tu posición.");
                    return;
                }
                Location loc = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
                duelManager.saveConfiguredArena(loc);
                player.sendMessage(Component.text("✔ ¡Cuadrilátero fijado exitosamente en tus coordenadas! (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")", NamedTextColor.GREEN));
                player.sendMessage(Component.text("ℹ Guardado permanente en arena.yml. Tus batallas y retos se disputarán aquí.", NamedTextColor.GRAY));
            }
            case "crear", "build" -> {
                if (player == null) {
                    sender.sendMessage("Debes ser un jugador para crear la arena en tu posición.");
                    return;
                }
                int radius = ArenaBuilder.DEFAULT_RADIUS;
                if (args.length >= 2) {
                    try { radius = Math.max(3, Math.min(8, Integer.parseInt(args[1]))); } catch (NumberFormatException ignored) {}
                }
                Location loc = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
                ArenaBuilder.buildArena(loc, radius);
                duelManager.saveConfiguredArena(loc);
                player.sendMessage(Component.text("✔ ¡Cuadrilátero de Palenque construido físicamente y guardado! (Radio: " + radius + ")", NamedTextColor.GREEN));
            }
            case "detectar", "scan" -> {
                if (player == null) {
                    sender.sendMessage("Debes ser un jugador para detectar la arena.");
                    return;
                }
                Location detected = duelManager.findNearbyPhysicalArena(player.getLocation(), 25);
                if (detected != null) {
                    duelManager.saveConfiguredArena(detected);
                    player.sendMessage(Component.text("✔ ¡Cuadrilátero físico detectado y fijado en (" + detected.getBlockX() + ", " + detected.getBlockY() + ", " + detected.getBlockZ() + ")!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("❌ No se detectó ninguna estructura de cuadrilátero en 25 bloques a la redonda.", NamedTextColor.RED));
                    player.sendMessage(Component.text("Colócate en el centro de tu arena y usa: /gallos arena fijar", NamedTextColor.YELLOW));
                }
            }
            case "remover" -> {
                Location curr = duelManager.getConfiguredArena();
                if (curr == null) {
                    sender.sendMessage(Component.text("No hay ninguna arena fija configurada actualmente.", NamedTextColor.YELLOW));
                    return;
                }
                int radius = ArenaBuilder.DEFAULT_RADIUS;
                if (args.length >= 2) {
                    try { radius = Math.max(3, Math.min(8, Integer.parseInt(args[1]))); } catch (NumberFormatException ignored) {}
                }
                ArenaBuilder.removeArena(curr, radius);
                duelManager.saveConfiguredArena(null);
                sender.sendMessage(Component.text("✔ Cuadrilátero removido físicamente y arena fija deshabilitada.", NamedTextColor.GREEN));
            }
            case "tp" -> {
                if (player == null) {
                    sender.sendMessage("Solo jugadores pueden teletransportarse.");
                    return;
                }
                Location curr = duelManager.getConfiguredArena();
                if (curr == null) {
                    player.sendMessage(Component.text("No hay ninguna arena fija configurada.", NamedTextColor.RED));
                    return;
                }
                player.teleportAsync(curr.clone().add(0, 1.0, 0));
                player.sendMessage(Component.text("✔ Teletransportado al centro de la arena del palenque.", NamedTextColor.GREEN));
            }
            case "info" -> {
                Location curr = duelManager.getConfiguredArena();
                if (curr == null) {
                    sender.sendMessage(Component.text("Estado de Arena: Sin arena fija (Combates intentarán autodetección física o punto medio).", NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("Estado de Arena: Fija y Activa en " + curr.getWorld().getName() + " (" + curr.getBlockX() + ", " + curr.getBlockY() + ", " + curr.getBlockZ() + ") [arena.yml]", NamedTextColor.GREEN));
                }
            }
            default -> sendArenaHelp(sender);
        }
    }

    private void sendArenaHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("╔══════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("      ⚔ GESTIÓN DE ARENA (PALENQUE) ⚔", NamedTextColor.YELLOW, TextDecoration.BOLD));
        sender.sendMessage(Component.text("╚══════════════════════════════════════════╝", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("✦ /rg admin arena fijar §7- Fija tu posición actual como centro del ring"));
        sender.sendMessage(Component.text("✦ /rg admin arena crear [radio] §7- Construye físicamente el palenque aquí"));
        sender.sendMessage(Component.text("✦ /rg admin arena detectar §7- Auto-detecta el cuadrilátero físico"));
        sender.sendMessage(Component.text("✦ /rg admin arena tp §7- Teletransporta al centro de la arena"));
        sender.sendMessage(Component.text("✦ /rg admin arena info §7- Ver coordenadas y estado"));
        sender.sendMessage(Component.text("✦ /rg admin arena remover [radio] §7- Remueve la arena físicamente"));
        sender.sendMessage(Component.empty());
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("╔══════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("      ⚔ COMANDOS DE ADMIN ROOSTERGENICS ⚔", NamedTextColor.YELLOW, TextDecoration.BOLD));
        sender.sendMessage(Component.text("╚══════════════════════════════════════════╝", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("✦ /rg admin estructura <gimnasio [1-6]|centrogallo|tienda|arena> §7- Generación procedural adaptada"));
        sender.sendMessage(Component.text("✦ /rg admin npc <enfermera|tendero|lider [1-6]> §7- Spawn de NPCs especiales"));
        sender.sendMessage(Component.text("✦ /rg admin bloque curacion §7- Colocar estación de curación"));
        sender.sendMessage(Component.text("✦ /rg admin arena <fijar|crear|detectar|tp|info> §7- Arena de combate"));
        sender.sendMessage(Component.text("✦ /rg admin entrenador <crear|remover|listar|tp> §7- Entrenadores sparring"));
        sender.sendMessage(Component.text("✦ /rg admin dar <jaula|gallo|objeto> <jugador> [cant/item] §7- Entregar ítems/gallos"));
        sender.sendMessage(Component.text("✦ /rg admin curar <jugador|all> §7- Revivir y curar gallos"));
        sender.sendMessage(Component.text("✦ /rg admin resetcooldowns <jugador> §7- Quitar fatiga de entreno"));
        sender.sendMessage(Component.text("✦ /rg admin editar <jugador> <stat> <valor> §7- Modificar atributos"));
        sender.sendMessage(Component.text("✦ /rg admin trait <add|remove|clear|evolve> <jugador> [rasgo|all] §7- Mutaciones"));
        sender.sendMessage(Component.text("✦ /rg admin balsamo §7- Otorga 1 Bálsamo Supremo del Campeón"));
        sender.sendMessage(Component.text("✦ /rg admin lesion <dar|curar|limpiar> [tipo] §7- Gestionar lesiones"));
        sender.sendMessage(Component.text("✦ /rg admin nuzlocke <true|false> §7- Muerte permanente"));
        sender.sendMessage(Component.text("✦ /rg admin reload §7- Recargar configuraciones"));
        sender.sendMessage(Component.empty());
    }

    private void sendPlayerHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("╔══════════════════════════════════════════╗", NamedTextColor.GOLD));
        player.sendMessage(Component.text("        ⚔ ROOSTERGENICS: GUÍA RÁPIDA ⚔", NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("╚══════════════════════════════════════════╝", NamedTextColor.GOLD));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" §7- ¡Menú Principal! Abre corral, ADN, crianza, gym y objetos en 1 clic.")));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg sacar §7/ §f/rg guardar", NamedTextColor.YELLOW))
                .append(Component.text(" §7- Invoca o guarda a tu gallo compañero para que te siga.")));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg acariciar", NamedTextColor.YELLOW))
                .append(Component.text(" §7- Acaricia a tu gallo invocado para subir vínculo o calmar Shock.")));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg salud", NamedTextColor.YELLOW))
                .append(Component.text(" §7- Diagnóstico médico veterinario (lesiones, rachas y secuelas).")));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg medallas §7/ §f/rg liga", NamedTextColor.YELLOW))
                .append(Component.text(" §7- Consulta tu medallero de gimnasios y desafía la Gran Liga.")));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg retar <jugador> [apuesta]", NamedTextColor.YELLOW))
                .append(Component.text(" §7- Desafía a otro jugador a un duelo 1v1 en el palenque.")));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg aceptar §7/ §f/rg rechazar", NamedTextColor.YELLOW))
                .append(Component.text(" §7- Responde a un desafío entrante.")));
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("/rg apostar <jugador> <monto>", NamedTextColor.YELLOW))
                .append(Component.text(" §7- Apuesta como espectador durante la previa del combate.")));
        player.sendMessage(Component.empty());
    }

    private void handleTrainerNpcCommand(CommandSender sender, Player player, String[] subArgs) {
        if (subArgs.length == 0 || subArgs[0].equalsIgnoreCase("ayuda") || subArgs[0].equalsIgnoreCase("help")) {
            sendTrainerNpcHelp(sender);
            return;
        }

        String action = subArgs[0].toLowerCase();
        switch (action) {
            case "crear", "spawn", "nuevo" -> {
                if (player == null) {
                    sender.sendMessage("Debes ser un jugador para spawnear un entrenador en tu ubicación.");
                    return;
                }
                if (subArgs.length < 2) {
                    player.sendMessage(Component.text("Uso: /gallos npc crear <nombre> [tier] [skin/random]", NamedTextColor.RED));
                    return;
                }
                String trainerName = subArgs[1].replace("_", " ");
                TrainerTier tier = TrainerTier.APRENDIZ;
                String skin = "random";

                if (subArgs.length >= 3) {
                    String param1 = subArgs[2];
                    TrainerTier parsedTier = parseTierOrNull(param1);
                    if (parsedTier != null) {
                        tier = parsedTier;
                        if (subArgs.length >= 4) {
                            skin = subArgs[3];
                        }
                    } else {
                        skin = param1;
                    }
                }

                Location loc = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
                loc.setYaw(player.getLocation().getYaw());
                loc.setPitch(player.getLocation().getPitch());

                RoosterTrainer created = trainerManager.spawnTrainer(trainerName, tier, loc, skin);
                player.sendMessage(Component.text("✔ Entrenador Gallero creado con éxito: ", NamedTextColor.GREEN)
                        .append(Component.text(created.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(" (" + tier.getTitle() + ")", tier.getColor()))
                        .append(Component.text(" | Skin: ", NamedTextColor.GRAY))
                        .append(Component.text(created.getSkinName(), NamedTextColor.AQUA)));
            }
            case "remover", "eliminar", "borrar", "delete" -> {
                RoosterTrainer target = null;
                if (subArgs.length >= 2) {
                    String targetName = String.join(" ", Arrays.copyOfRange(subArgs, 1, subArgs.length)).replace("_", " ");
                    target = trainerManager.getTrainerByName(targetName);
                } else if (player != null) {
                    target = trainerManager.findNearestTrainer(player.getLocation(), 6.0);
                }

                if (target != null && trainerManager.removeTrainer(target.getTrainerId())) {
                    sender.sendMessage(Component.text("✔ Entrenador " + target.getName() + " eliminado con éxito.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("No se encontró ningún entrenador cercano (dentro de 6 bloques) o con ese nombre.", NamedTextColor.RED));
                }
            }
            case "skin" -> {
                if (subArgs.length < 2) {
                    sender.sendMessage(Component.text("Uso: /gallos npc skin [nombre] <cuenta_mc|random>", NamedTextColor.RED));
                    return;
                }
                RoosterTrainer target = null;
                String newSkin;

                if (subArgs.length == 2) {
                    if (player != null) {
                        target = trainerManager.findNearestTrainer(player.getLocation(), 6.0);
                    }
                    newSkin = subArgs[1];
                } else {
                    String targetName = subArgs[1].replace("_", " ");
                    target = trainerManager.getTrainerByName(targetName);
                    newSkin = subArgs[2];
                }

                if (target == null) {
                    sender.sendMessage(Component.text("No se encontró ningún entrenador cercano o con ese nombre.", NamedTextColor.RED));
                    return;
                }

                trainerManager.updateTrainerSkin(target.getTrainerId(), newSkin);
                sender.sendMessage(Component.text("✔ Skin de " + target.getName() + " actualizada a: ", NamedTextColor.GREEN)
                        .append(Component.text(target.getSkinName(), NamedTextColor.AQUA, TextDecoration.BOLD)));
            }
            case "renombrar", "rename" -> {
                if (subArgs.length < 2) {
                    sender.sendMessage(Component.text("Uso: /gallos npc renombrar [nombre_actual] <nuevo_nombre>", NamedTextColor.RED));
                    return;
                }
                RoosterTrainer target = null;
                String newName;

                if (subArgs.length == 2) {
                    if (player != null) {
                        target = trainerManager.findNearestTrainer(player.getLocation(), 6.0);
                    }
                    newName = subArgs[1].replace("_", " ");
                } else {
                    String targetName = subArgs[1].replace("_", " ");
                    target = trainerManager.getTrainerByName(targetName);
                    newName = String.join(" ", Arrays.copyOfRange(subArgs, 2, subArgs.length)).replace("_", " ");
                }

                if (target == null) {
                    sender.sendMessage(Component.text("No se encontró ningún entrenador cercano o con ese nombre.", NamedTextColor.RED));
                    return;
                }

                trainerManager.renameTrainer(target.getTrainerId(), newName);
                sender.sendMessage(Component.text("✔ Entrenador renombrado a: ", NamedTextColor.GREEN)
                        .append(Component.text(target.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)));
            }
            case "listar", "list" -> {
                var list = trainerManager.getTrainers();
                if (list.isEmpty()) {
                    sender.sendMessage(Component.text("No hay ningún Entrenador Gallero NPC activo.", NamedTextColor.YELLOW));
                    return;
                }
                sender.sendMessage(Component.text("\n=== ENTRENADORES GALLEROS ACTIVOS (" + list.size() + ") ===", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (var t : list) {
                    Location l = t.getLocation();
                    String locStr = l != null ? l.getWorld().getName() + " (" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")" : "Sin ubicación";

                    Component line = Component.text("• ", NamedTextColor.GRAY)
                            .append(Component.text(t.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(Component.text(" [" + t.getTier().name() + "]", t.getTier().getColor()))
                            .append(Component.text(" Skin: ", NamedTextColor.GRAY))
                            .append(Component.text(t.getSkinName(), NamedTextColor.AQUA))
                            .append(Component.text(" en " + locStr, NamedTextColor.DARK_GRAY))
                            .append(Component.text(" "));

                    Component tpBtn = Component.text("[TP]", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("Clic para teletransportarte", NamedTextColor.GRAY)))
                            .clickEvent(ClickEvent.runCommand("/gallos npc tp " + t.getName().replace(" ", "_")));

                    Component delBtn = Component.text(" [Eliminar]", NamedTextColor.RED, TextDecoration.BOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("Clic para eliminar este NPC", NamedTextColor.RED)))
                            .clickEvent(ClickEvent.suggestCommand("/gallos npc remover " + t.getName().replace(" ", "_")));

                    sender.sendMessage(line.append(tpBtn).append(delBtn));
                }
                sender.sendMessage(Component.empty());
            }
            case "tp" -> {
                if (player == null) {
                    sender.sendMessage("Solo jugadores pueden teletransportarse.");
                    return;
                }
                if (subArgs.length < 2) {
                    player.sendMessage(Component.text("Uso: /gallos npc tp <nombre>", NamedTextColor.RED));
                    return;
                }
                String targetName = String.join(" ", Arrays.copyOfRange(subArgs, 1, subArgs.length)).replace("_", " ");
                var tr = trainerManager.getTrainerByName(targetName);
                if (tr == null || tr.getLocation() == null) {
                    player.sendMessage(Component.text("Entrenador no encontrado.", NamedTextColor.RED));
                    return;
                }
                player.teleportAsync(tr.getLocation());
                player.sendMessage(Component.text("✔ Teletransportado hacia " + tr.getName() + ".", NamedTextColor.GREEN));
            }
            case "desafiar" -> {
                if (player == null) {
                    sender.sendMessage("Solo jugadores pueden desafiar entrenadores.");
                    return;
                }
                if (subArgs.length < 2) {
                    player.sendMessage(Component.text("Uso: /gallos npc desafiar <nombre>", NamedTextColor.RED));
                    return;
                }
                String targetName = String.join(" ", Arrays.copyOfRange(subArgs, 1, subArgs.length)).replace("_", " ");
                var tr = trainerManager.getTrainerByName(targetName);
                if (tr == null) {
                    player.sendMessage(Component.text("Entrenador no encontrado.", NamedTextColor.RED));
                    return;
                }
                TrainerGui.open(player, tr, trainerManager, repository);
            }
            default -> sendTrainerNpcHelp(sender);
        }
    }

    private TrainerTier parseTierOrNull(String str) {
        if (str == null) return null;
        return switch (str.toUpperCase()) {
            case "APRENDIZ", "VETERANO", "MAESTRO", "LEYENDA" -> TrainerTier.valueOf(str.toUpperCase());
            default -> null;
        };
    }

    private void sendTrainerNpcHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("╔══════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("      ⚔ GESTIÓN DE ENTRENADORES NPC ⚔", NamedTextColor.YELLOW, TextDecoration.BOLD));
        sender.sendMessage(Component.text("╚══════════════════════════════════════════╝", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("✦ /rg admin npc crear <nombre> [tier] [skin] §7- Spawnea un entrenador"));
        sender.sendMessage(Component.text("✦ /rg admin npc remover [nombre] §7- Elimina el entrenador"));
        sender.sendMessage(Component.text("✦ /rg admin npc skin [nombre] <skin> §7- Cambia la skin"));
        sender.sendMessage(Component.text("✦ /rg admin npc renombrar [actual] <nuevo> §7- Renombra"));
        sender.sendMessage(Component.text("✦ /rg admin npc listar §7- Lista con botones [TP] y [Eliminar]"));
        sender.sendMessage(Component.text("✦ /rg admin npc tp <nombre> §7- Teletransportarse a un NPC"));
        sender.sendMessage(Component.empty());
    }

    private void sendMutationsList(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("🧬 CATÁLOGO DE MUTACIONES ROOSTERGENICS 🧬", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        player.sendMessage(Component.text("✦ Rasgos Favorables (Mutaciones Positivas):", NamedTextColor.GREEN, TextDecoration.BOLD));
        for (RoosterTrait t : RoosterTrait.values()) {
            if (t.isPositive()) {
                player.sendMessage(Component.text(" • ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(t.getDisplayName(), NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text(": §7" + t.getDescription())));
            }
        }
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ Defectos Genéticos (Endogamia / COI Alto):", NamedTextColor.RED, TextDecoration.BOLD));
        for (RoosterTrait t : RoosterTrait.values()) {
            if (!t.isPositive()) {
                player.sendMessage(Component.text(" • ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(t.getDisplayName(), NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text(": §7" + t.getDescription())));
            }
        }
        player.sendMessage(Component.empty());
    }

    private void sendItemsList(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("🎒 OBJETOS DE COMBATE ROOSTERGENICS 🎒", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Equípalos desde /rg objetos o en el secuenciador ADN:", NamedTextColor.GRAY));
        for (RoosterHeldItem item : RoosterHeldItem.values()) {
            player.sendMessage(Component.text(" • ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(item.getDisplayName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" - §f" + String.join(" ", item.getDescription()))));
        }
        player.sendMessage(Component.empty());
    }

    private void sendRecipeInfo(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("🪤 RECETA: JAULA DE CAPTURA DE GALLOS 🪤", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Coloca en la mesa de crafteo 3x3:", NamedTextColor.YELLOW));

        Component bars = Component.text("[Barrotes]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl("https://minecraft.fandom.com/wiki/Iron_Bars"))
                .hoverEvent(HoverEvent.showText(Component.text("Clic para ver Barrotes de Hierro en la Minecraft Wiki", NamedTextColor.YELLOW)));

        Component egg = Component.text("[Huevo]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.openUrl("https://minecraft.fandom.com/es/wiki/Huevo"))
                .hoverEvent(HoverEvent.showText(Component.text("Clic para ver Huevo en la Minecraft Wiki", NamedTextColor.YELLOW)));

        Component lead = Component.text("[Rienda]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.openUrl("https://es.minecraft.wiki/w/Rienda"))
                .hoverEvent(HoverEvent.showText(Component.text("Clic para ver Rienda en la Minecraft Wiki", NamedTextColor.YELLOW)));

        player.sendMessage(Component.text("  ").append(bars).append(Component.text(" ")).append(bars).append(Component.text(" ")).append(bars));
        player.sendMessage(Component.text("  ").append(bars).append(Component.text("  ")).append(egg).append(Component.text("   ")).append(bars));
        player.sendMessage(Component.text("  ").append(bars).append(Component.text(" ")).append(lead).append(Component.text("  ")).append(bars));

        player.sendMessage(Component.text("Uso: Haz clic derecho con la jaula sobre un gallo salvaje.", NamedTextColor.WHITE));
        player.sendMessage(Component.text("✦ Clic en cualquier ingrediente para abrir su guía oficial.", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> primary = new ArrayList<>(Arrays.asList("retar", "apostar", "aceptar", "rechazar", "ayuda"));
            if (sender.hasPermission("roostergenics.admin")) {
                primary.add("admin");
            }

            List<String> matches = filter(primary, args[0]);
            if (!matches.isEmpty()) {
                return matches;
            }

            List<String> shortcuts = Arrays.asList(
                    "menu", "corral", "criar", "entrenar", "dna", "objetos", "liberar", "mutaciones", "crafteo", "renombrar",
                    "sacar", "guardar", "acariciar", "salud", "diagnostico", "medallas", "liga", "tienda", "spawn", "invocar"
            );
            return filter(shortcuts, args[0]);
        }

        if ((args[0].equalsIgnoreCase("npc") || args[0].equalsIgnoreCase("entrenador")) && sender.hasPermission("roostergenics.admin")) {
            return tabCompleteTrainerNpc(args, 1);
        }

        if (args[0].equalsIgnoreCase("arena") && sender.hasPermission("roostergenics.admin")) {
            if (args.length == 2) {
                return filter(Arrays.asList("fijar", "set", "crear", "detectar", "tp", "info", "remover"), args[1]);
            }
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("spawn") || sub.equals("invocar") || sub.equals("testgallo")) {
                return filter(Arrays.asList("arido", "frio", "pino", "cienaga", "calido", "salvaje", "comun", "all", "arid", "cold", "pine", "pinar", "taiga", "mire", "warm", "wild", "default"), args[1]);
            }
            if (sub.equals("retar") || sub.equals("apostar")) {
                return onlinePlayerNames(sender, args[1]);
            }
            if (sub.equals("desafiar")) {
                List<String> trainerNames = new ArrayList<>();
                for (var t : trainerManager.getTrainers()) {
                    trainerNames.add(t.getName());
                }
                return filter(trainerNames, args[1]);
            }
            if (sub.equals("admin") && sender.hasPermission("roostergenics.admin")) {
                return filter(Arrays.asList("arena", "entrenador", "dar", "curar", "resetcooldowns", "editar", "trait", "nuzlocke", "reload", "estructura", "removerestructura", "npc", "bloque", "balsamo", "lesion", "spawn"), args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("retar")) {
                return filter(Arrays.asList("0", "50", "100", "250", "500", "1000"), args[2]);
            }
            if (sub.equals("apostar")) {
                return filter(Arrays.asList("10", "50", "100", "250", "500", "1000"), args[2]);
            }
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("roostergenics.admin")) {
            String adminSub = args[1].toLowerCase();
            if (adminSub.equals("entrenador") || adminSub.equals("trainer")) {
                return tabCompleteTrainerNpc(args, 2);
            }
            if (args.length == 3) {
                switch (adminSub) {
                    case "estructura" -> { return filter(Arrays.asList("gimnasio", "centrogallo", "tienda", "arena", "remover", "limpiar"), args[2]); }
                    case "removerestructura", "limpiarestructura" -> { return filter(Arrays.asList("5", "10", "15", "20"), args[2]); }
                    case "npc" -> { return filter(Arrays.asList("enfermera", "tendero", "lider", "crear", "remover", "skin", "renombrar", "listar", "tp"), args[2]); }
                    case "bloque" -> { return filter(Arrays.asList("curacion"), args[2]); }
                    case "lesion" -> { return filter(Arrays.asList("dar", "curar", "limpiar"), args[2]); }
                    case "arena" -> { return filter(Arrays.asList("fijar", "set", "crear", "detectar", "tp", "info", "remover"), args[2]); }
                    case "spawn" -> { return filter(Arrays.asList("arido", "frio", "pino", "cienaga", "calido", "salvaje", "comun", "all"), args[2]); }
                    case "dar" -> { return filter(Arrays.asList("jaula", "gallo", "objeto"), args[2]); }
                    case "curar" -> {
                        List<String> curarOptions = new ArrayList<>(onlinePlayerNames(null, args[2]));
                        curarOptions.add("all");
                        return filter(curarOptions, args[2]);
                    }
                    case "resetcooldowns", "editar" -> { return onlinePlayerNames(null, args[2]); }
                    case "trait" -> { return filter(Arrays.asList("add", "remove", "clear", "evolve"), args[2]); }
                    case "nuzlocke" -> { return filter(Arrays.asList("true", "false"), args[2]); }
                }
            }
            if (args.length == 4) {
                switch (adminSub) {
                    case "spawn" -> { return onlinePlayerNames(null, args[3]); }
                    case "estructura" -> {
                        if (args[2].equalsIgnoreCase("gimnasio")) {
                            return filter(Arrays.asList("1", "2", "3", "4", "5", "6"), args[3]);
                        }
                        if (args[2].equalsIgnoreCase("remover") || args[2].equalsIgnoreCase("limpiar")) {
                            return filter(Arrays.asList("5", "10", "15", "20"), args[3]);
                        }
                    }
                    case "npc" -> {
                        if (args[2].equalsIgnoreCase("lider")) {
                            return filter(Arrays.asList("1", "2", "3", "4", "5", "6"), args[3]);
                        }
                    }
                    case "dar" -> { return onlinePlayerNames(null, args[3]); }
                    case "editar" -> { return filter(Arrays.asList("hp", "ataque", "defensa", "velocidad", "escala", "coi", "nombre", "nivel", "xp"), args[3]); }
                    case "trait" -> { return onlinePlayerNames(null, args[3]); }
                }
            }
            if (args.length == 5) {
                if (adminSub.equals("dar")) {
                    if (args[2].equalsIgnoreCase("objeto")) {
                        List<String> itemNames = new ArrayList<>();
                        itemNames.add("all");
                        for (RoosterHeldItem i : RoosterHeldItem.values()) itemNames.add(i.name());
                        return filter(itemNames, args[4]);
                    }
                    if (args[2].equalsIgnoreCase("jaula")) {
                        return filter(Arrays.asList("1", "5", "10", "16", "64"), args[4]);
                    }
                    if (args[2].equalsIgnoreCase("gallo")) {
                        return filter(Arrays.asList("arido", "frio", "pino", "cienaga", "calido", "salvaje", "comun"), args[4]);
                    }
                }
                if (adminSub.equals("trait")) {
                    String traitAction = args[2].toLowerCase();
                    if (traitAction.equals("add")) {
                        List<String> traits = new ArrayList<>(Arrays.asList("all", "all_buenas", "all_malas"));
                        for (RoosterTrait t : RoosterTrait.values()) {
                            traits.add(t.name());
                        }
                        return filter(traits, args[4]);
                    } else if (traitAction.equals("evolve")) {
                        List<String> traits = new ArrayList<>();
                        traits.add("all");
                        for (RoosterTrait t : RoosterTrait.getAllBaseTraits()) {
                            traits.add(t.name());
                        }
                        return filter(traits, args[4]);
                    } else if (traitAction.equals("remove")) {
                        List<String> traits = new ArrayList<>();
                        for (RoosterTrait t : RoosterTrait.values()) {
                            traits.add(t.name());
                        }
                        return filter(traits, args[4]);
                    }
                }
            }
            if (args.length == 6) {
                if (adminSub.equals("dar") && args[2].equalsIgnoreCase("objeto")) {
                    return filter(Arrays.asList("1", "5", "16", "64"), args[5]);
                }
                if (adminSub.equals("trait") && args[2].equalsIgnoreCase("add")) {
                    return filter(Arrays.asList("true", "false"), args[5]);
                }
            }
        }

        return List.of();
    }

    private List<String> tabCompleteTrainerNpc(String[] args, int offset) {
        int index = args.length - 1 - offset;
        if (index == 0) {
            return filter(Arrays.asList("crear", "remover", "skin", "renombrar", "listar", "tp", "desafiar"), args[offset]);
        }
        String action = args[offset].toLowerCase();
        if (index == 1) {
            switch (action) {
                case "crear" -> {
                    return Arrays.asList("Don_Ramon", "El_Patron", "Capitan_Plumas", "Viejo_Pancho", "El_Carnicero");
                }
                case "remover", "eliminar", "borrar", "tp", "desafiar", "renombrar" -> {
                    List<String> names = new ArrayList<>();
                    for (var t : trainerManager.getTrainers()) names.add(t.getName().replace(" ", "_"));
                    return filter(names, args[offset + 1]);
                }
                case "skin" -> {
                    List<String> options = new ArrayList<>();
                    for (var t : trainerManager.getTrainers()) options.add(t.getName().replace(" ", "_"));
                    options.addAll(Arrays.asList("random", "ElRichMC", "iDarkt", "Technoblade", "FitMC"));
                    return filter(options, args[offset + 1]);
                }
            }
        }
        if (index == 2) {
            switch (action) {
                case "crear" -> {
                    List<String> opts = new ArrayList<>(Arrays.asList("aprendiz", "veterano", "maestro", "leyenda", "random", "ElRichMC", "iDarkt", "Technoblade"));
                    return filter(opts, args[offset + 2]);
                }
                case "skin" -> {
                    return filter(Arrays.asList("random", "ElRichMC", "iDarkt", "Technoblade", "FitMC", "Vegetta777", "Willyrex", "AuronPlay"), args[offset + 2]);
                }
            }
        }
        if (index == 3) {
            if (action.equals("crear")) {
                return filter(Arrays.asList("random", "ElRichMC", "iDarkt", "Technoblade", "FitMC", "Vegetta777", "Willyrex", "AuronPlay"), args[offset + 3]);
            }
        }
        return List.of();
    }

    public void handleSpawnRooster(Player player, String variantQuery) {
        if (player == null || !player.isOnline()) return;

        if (variantQuery == null || variantQuery.isBlank()) {
            sendSpawnMenu(player);
            return;
        }

        String q = variantQuery.toLowerCase();
        if (q.equals("all") || q.equals("todos")) {
            spawnAllRoosterVariants(player);
            return;
        }

        String plumage = RoosterGenetics.resolvePlumageAlias(variantQuery);
        Location spawnLoc = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(1.8));
        if (!spawnLoc.getBlock().isPassable()) {
            spawnLoc = player.getLocation().clone();
        }
        spawnSingleRoosterVariant(player, plumage, spawnLoc);
    }

    private void spawnSingleRoosterVariant(Player player, String plumage, Location loc) {
        Location spawnLoc = loc.clone();
        Vector toPlayer = player.getLocation().toVector().subtract(spawnLoc.toVector()).setY(0);
        if (toPlayer.lengthSquared() > 0.001) {
            spawnLoc.setDirection(toPlayer.normalize());
        }

        Chicken chicken = (Chicken) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.CHICKEN);
        chicken.setAdult();
        chicken.setBreed(false);
        chicken.setRemoveWhenFarAway(false);
        chicken.setCustomNameVisible(true);

        try {
            chicken.setVariant(Chicken.Variant.TEMPERATE);
        } catch (Throwable ignored) {}

        String variantName = RoosterGenetics.getVariantTextureName(plumage);
        String biomeSubtitle = switch (plumage.toUpperCase()) {
            case "DESIERTO", "SAVANNA", "MESETA" -> "Desierto";
            case "ARTICO" -> "Ártico";
            case "TAIGA", "PINAR", "PINO" -> "Pinar";
            case "PANTANO" -> "Pantano";
            case "JUNGLA" -> "Jungla";
            case "DAPPLED_FOREST", "PALE_GARDEN" -> "Bosque";
            default -> "Llanura";
        };

        String nameFormat = switch (plumage.toUpperCase()) {
            case "SAVANNA" -> "§6★ §eGallo Árido";
            case "DESIERTO", "MESETA" -> "§6★ §6Gallo Cálido";
            case "JUNGLA" -> "§6★ §cGallo Salvaje";
            case "ARTICO" -> "§6★ §bGallo Frío";
            case "TAIGA", "PINAR", "PINO" -> "§6★ §2Gallo de Pinar";
            case "PANTANO" -> "§6★ §2Gallo Ciénaga";
            default -> "§6★ §fGallo Común";
        };

        String tag = RoosterGenetics.getInvisibleVariantTag(plumage);
        chicken.customName(Component.text(nameFormat + tag));
        chicken.setMetadata("roostergenics_wild_plumage", new FixedMetadataValue(plugin, plumage));
        chicken.setMetadata("roostergenics_preview", new FixedMetadataValue(plugin, true));

        try {
            java.lang.reflect.Field field = org.bukkit.attribute.Attribute.class.getField("GENERIC_SCALE");
            org.bukkit.attribute.Attribute scaleAttr = (org.bukkit.attribute.Attribute) field.get(null);
            org.bukkit.attribute.AttributeInstance instance = chicken.getAttribute(scaleAttr);
            if (instance != null) {
                instance.setBaseValue(1.10);
            }
        } catch (Throwable ignored) {}

        Particle particle = switch (plumage.toUpperCase()) {
            case "SAVANNA" -> Particle.COMPOSTER;
            case "DESIERTO", "MESETA" -> Particle.FLAME;
            case "JUNGLA" -> Particle.HAPPY_VILLAGER;
            case "ARTICO" -> Particle.SNOWFLAKE;
            case "TAIGA", "PINAR", "PINO" -> Particle.ITEM_SLIME;
            case "PANTANO" -> Particle.FALLING_SPORE_BLOSSOM;
            default -> Particle.HEART;
        };

        chicken.getWorld().spawnParticle(particle, spawnLoc.clone().add(0, 0.5, 0), 12, 0.25, 0.25, 0.25, 0.05);
        chicken.getWorld().playSound(spawnLoc, Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.2f);
        chicken.getWorld().playSound(spawnLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);

        player.sendMessage(Mm.noItalic("<green>✔ ¡Has invocado al <gold>" + variantName + "</gold>! <gray>(Modelo CEM & Textura ETF activos)"));
    }

    private void spawnAllRoosterVariants(Player player) {
        String[] allVariants = {"SAVANNA", "DESIERTO", "JUNGLA", "ARTICO", "TAIGA", "PANTANO", "PLAINS"};
        Location baseLoc = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(2.5));
        Vector rightDir = new Vector(-player.getLocation().getDirection().getZ(), 0, player.getLocation().getDirection().getX()).normalize();

        double startOffset = -((allVariants.length - 1) * 1.5) / 2.0;
        for (int i = 0; i < allVariants.length; i++) {
            Location spawnLoc = baseLoc.clone().add(rightDir.clone().multiply(startOffset + (i * 1.5)));
            spawnSingleRoosterVariant(player, allVariants[i], spawnLoc);
        }
        player.sendMessage(Mm.noItalic("<gold>★ ¡Se han invocado las <yellow>7 variantes de gallos</yellow> con texturas nuevas!"));
    }

    private void sendSpawnMenu(Player player) {
        player.sendMessage(Mm.noItalic("<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Mm.noItalic("  <yellow><bold>ROOSTERGENICS • INVOCADOR DE GALLOS</bold></yellow>"));
        player.sendMessage(Mm.noItalic("  <gray>Haz clic en una variante para invocarla con su modelo CEM:</gray>"));
        player.sendMessage(Component.empty());
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn arido'><hover:show_text:'<gray>Clic para invocar'><gold>▶ [Gallo Árido]</gold> <gray>(Savanna, Sabana)</hover></click>"));
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn calido'><hover:show_text:'<gray>Clic para invocar'><gold>▶ [Gallo Cálido]</gold> <gray>(Desierto, Badlands, Meseta)</hover></click>"));
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn salvaje'><hover:show_text:'<gray>Clic para invocar'><red>▶ [Gallo Salvaje]</red> <gray>(Jungla, Bambú)</hover></click>"));
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn frio'><hover:show_text:'<gray>Clic para invocar'><aqua>▶ [Gallo Frío]</aqua> <gray>(Nieve, Hielo, Polar)</hover></click>"));
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn pino'><hover:show_text:'<gray>Clic para invocar'><dark_green>▶ [Gallo de Pinar]</dark_green> <gray>(Pino, Taiga, Abeto)</hover></click>"));
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn cienaga'><hover:show_text:'<gray>Clic para invocar'><green>▶ [Gallo Ciénaga]</green> <gray>(Pantano, Manglar)</hover></click>"));
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn comun'><hover:show_text:'<gray>Clic para invocar'><white>▶ [Gallo Común]</white> <gray>(Llanura, Bosque)</hover></click>"));
        player.sendMessage(Component.empty());
        player.sendMessage(Mm.noItalic("  <click:run_command:'/rg spawn all'><hover:show_text:'<gold>Invoca a los 7 juntos en formación'><yellow>★ [INVOCAR TODOS (7)]</yellow></hover></click>"));
        player.sendMessage(Mm.noItalic("<gold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void showMedicalReport(Player player, RoosterData rooster) {
        player.sendMessage(Component.empty()
                .append(Component.text("\n═══════════ [🩺 DIAGNÓSTICO VETERINARIO] ═══════════\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("✦ Paciente: ", NamedTextColor.GRAY)).append(Component.text(rooster.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" | Estado: ", NamedTextColor.GRAY)).append(Component.text(rooster.getHealthStatus().getFormatted() + "\n", NamedTextColor.WHITE))
                .append(Component.text("✦ Derrotas Consecutivas: ", NamedTextColor.GRAY))
                .append(Component.text(rooster.getConsecutiveLosses() + (rooster.getConsecutiveLosses() >= 2 ? " ⚠ (¡Riesgo inminente de secuela!)" : " (Bajo control)"), rooster.getConsecutiveLosses() >= 2 ? NamedTextColor.RED : NamedTextColor.GREEN))
                .append(Component.text("\n")));

        // Lesiones Temporales
        if (rooster.hasInjuries()) {
            player.sendMessage(Component.text("▶ LESIONES TEMPORALES ACTIVAS (" + rooster.getTemporaryInjuries().size() + "):", NamedTextColor.RED, TextDecoration.BOLD));
            for (RoosterInjury inj : rooster.getTemporaryInjuries()) {
                player.sendMessage(Component.empty()
                        .append(Component.text(" ✖ ", NamedTextColor.RED))
                        .append(Component.text(inj.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(" - " + inj.getDescription(), NamedTextColor.GRAY))
                        .append(Component.text("\n   ➥ Tratamiento: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text(inj.getCureHint(), NamedTextColor.WHITE)));
            }
        } else {
            player.sendMessage(Component.text("✔ No presenta lesiones temporales activas. Físicamente en óptimo estado.", NamedTextColor.GREEN));
        }

        // Rasgos Negativos Permanentes (Defectos)
        List<RoosterTrait> defects = new ArrayList<>();
        for (RoosterTrait t : rooster.getGenetics().getTraits()) {
            if (!t.isPositive()) {
                defects.add(t);
            }
        }
        if (!defects.isEmpty()) {
            player.sendMessage(Component.text("\n▶ SECUELAS Y DEFECTOS PERMANENTES (" + defects.size() + "):", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
            for (RoosterTrait t : defects) {
                player.sendMessage(Component.empty()
                        .append(Component.text(" ☠ ", NamedTextColor.DARK_RED))
                        .append(Component.text(t.getDisplayName(), NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text(" - " + t.getDescription(), NamedTextColor.GRAY)));
            }
            player.sendMessage(Component.text("   ➥ Tratamiento: Requiere ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("Bálsamo Supremo de la Gran Liga", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" para extirpar.\n", NamedTextColor.DARK_GRAY)));
        } else {
            player.sendMessage(Component.text("✔ Genética limpia: sin defectos ni secuelas permanentes.", NamedTextColor.GREEN));
        }

        player.sendMessage(Component.text("\n════════════════════════════════════════════════════\n", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_CLERIC, 1.0f, 1.2f);
    }

    private List<String> onlinePlayerNames(CommandSender exclude, String prefix) {
        List<String> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (exclude == null || !p.getName().equalsIgnoreCase(exclude.getName())) {
                if (p.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                    players.add(p.getName());
                }
            }
        }
        return players;
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}