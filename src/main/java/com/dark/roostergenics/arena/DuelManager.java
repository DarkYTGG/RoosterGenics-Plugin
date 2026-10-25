package com.dark.roostergenics.arena;

import com.dark.roostergenics.api.betting.DuelBettingHook;
import com.dark.roostergenics.api.events.RoosterDuelStartEvent;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.storage.RoosterRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestor central de retos, cuadriláteros y sesiones de palenque 1v1.
 */
public class DuelManager implements org.bukkit.event.Listener {

    private final JavaPlugin plugin;
    private final RoosterRepository repository;
    private DuelBettingHook bettingHook;

    private boolean nuzlockeEnabled = false;
    private long knockoutDurationMillis = 20 * 60 * 1000L; // 20 minutos por defecto

    private final Map<UUID, DuelChallenge> pendingChallenges = new ConcurrentHashMap<>();
    private final List<DuelArenaSession> activeSessions = new CopyOnWriteArrayList<>();
    private final List<Location> knownArenas = new CopyOnWriteArrayList<>();

    private Location configuredArena = null;

    public DuelManager(JavaPlugin plugin, RoosterRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void setBettingHook(DuelBettingHook bettingHook) {
        this.bettingHook = bettingHook;
    }

    public DuelBettingHook getBettingHook() {
        return bettingHook;
    }

    public void setNuzlockeEnabled(boolean enabled) {
        this.nuzlockeEnabled = enabled;
    }

    public void setKnockoutDurationMillis(long millis) {
        this.knockoutDurationMillis = millis;
    }

    public long getKnockoutDurationMillis() {
        return knockoutDurationMillis;
    }

    public List<DuelArenaSession> getActiveSessions() {
        return activeSessions;
    }

    public void setConfiguredArena(Location loc) {
        saveConfiguredArena(loc);
    }

    public Location getConfiguredArena() {
        return configuredArena;
    }

    public void addKnownArena(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        for (Location existing : knownArenas) {
            if (existing.getWorld().equals(loc.getWorld()) && existing.distanceSquared(loc) < 9.0) {
                return;
            }
        }
        knownArenas.add(loc);
        saveKnownArenasToFile();
    }

    public List<Location> getKnownArenas() {
        return Collections.unmodifiableList(knownArenas);
    }

    public void removeArenasNear(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return;
        double rSq = radius * radius;
        boolean removed = knownArenas.removeIf(a -> a != null && a.getWorld().equals(loc.getWorld()) && a.distanceSquared(loc) <= rSq);
        if (configuredArena != null && configuredArena.getWorld().equals(loc.getWorld()) && configuredArena.distanceSquared(loc) <= rSq) {
            saveConfiguredArena(null);
            removed = true;
        }
        if (removed) {
            saveKnownArenasToFile();
        }
    }

    private File getArenaFile() {
        return new File(plugin.getDataFolder(), "arena.yml");
    }

    public void loadConfiguredArena() {
        File arenaFile = getArenaFile();
        if (arenaFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arenaFile);
            if (yaml.getBoolean("arena.enabled", false)) {
                String worldName = yaml.getString("arena.world");
                if (worldName != null) {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        double x = yaml.getDouble("arena.x");
                        double y = yaml.getDouble("arena.y");
                        double z = yaml.getDouble("arena.z");
                        float yaw = (float) yaml.getDouble("arena.yaw", 0.0);
                        float pitch = (float) yaml.getDouble("arena.pitch", 0.0);
                        this.configuredArena = new Location(w, x, y, z, yaw, pitch);
                        addKnownArena(this.configuredArena);
                    }
                }
            }

            List<Map<?, ?>> list = yaml.getMapList("known_arenas");
            for (Map<?, ?> entry : list) {
                try {
                    String wName = (String) entry.get("world");
                    World w = Bukkit.getWorld(wName);
                    if (w != null) {
                        double x = ((Number) entry.get("x")).doubleValue();
                        double y = ((Number) entry.get("y")).doubleValue();
                        double z = ((Number) entry.get("z")).doubleValue();
                        float yaw = entry.containsKey("yaw") ? ((Number) entry.get("yaw")).floatValue() : 0f;
                        float pitch = entry.containsKey("pitch") ? ((Number) entry.get("pitch")).floatValue() : 0f;
                        Location loc = new Location(w, x, y, z, yaw, pitch);
                        addKnownArena(loc);
                    }
                } catch (Exception ignored) {}
            }
            return;
        }

        // Fallback a config.yml para retrocompatibilidad
        if (plugin.getConfig().getBoolean("arena.enabled", false)) {
            String worldName = plugin.getConfig().getString("arena.world");
            if (worldName != null) {
                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    double x = plugin.getConfig().getDouble("arena.x");
                    double y = plugin.getConfig().getDouble("arena.y");
                    double z = plugin.getConfig().getDouble("arena.z");
                    float yaw = (float) plugin.getConfig().getDouble("arena.yaw", 0.0);
                    float pitch = (float) plugin.getConfig().getDouble("arena.pitch", 0.0);
                    this.configuredArena = new Location(w, x, y, z, yaw, pitch);
                    saveConfiguredArena(this.configuredArena);
                }
            }
        }
    }

    private void saveKnownArenasToFile() {
        File arenaFile = getArenaFile();
        YamlConfiguration yaml = arenaFile.exists() ? YamlConfiguration.loadConfiguration(arenaFile) : new YamlConfiguration();
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Location loc : knownArenas) {
            if (loc != null && loc.getWorld() != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("world", loc.getWorld().getName());
                map.put("x", loc.getX());
                map.put("y", loc.getY());
                map.put("z", loc.getZ());
                map.put("yaw", loc.getYaw());
                map.put("pitch", loc.getPitch());
                serialized.add(map);
            }
        }
        yaml.set("known_arenas", serialized);
        try {
            if (!arenaFile.getParentFile().exists()) {
                arenaFile.getParentFile().mkdirs();
            }
            yaml.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar known_arenas en arena.yml: " + e.getMessage());
        }
    }

    public void saveConfiguredArena(Location loc) {
        this.configuredArena = loc;
        if (loc != null) {
            addKnownArena(loc);
        }
        File arenaFile = getArenaFile();
        YamlConfiguration yaml = arenaFile.exists() ? YamlConfiguration.loadConfiguration(arenaFile) : new YamlConfiguration();

        if (loc != null && loc.getWorld() != null) {
            yaml.set("arena.enabled", true);
            yaml.set("arena.world", loc.getWorld().getName());
            yaml.set("arena.x", loc.getX());
            yaml.set("arena.y", loc.getY());
            yaml.set("arena.z", loc.getZ());
            yaml.set("arena.yaw", loc.getYaw());
            yaml.set("arena.pitch", loc.getPitch());

            // También en config.yml para compatibilidad
            plugin.getConfig().set("arena.enabled", true);
            plugin.getConfig().set("arena.world", loc.getWorld().getName());
            plugin.getConfig().set("arena.x", loc.getX());
            plugin.getConfig().set("arena.y", loc.getY());
            plugin.getConfig().set("arena.z", loc.getZ());
            plugin.getConfig().set("arena.yaw", loc.getYaw());
            plugin.getConfig().set("arena.pitch", loc.getPitch());
            plugin.saveConfig();
        } else {
            yaml.set("arena.enabled", false);
            plugin.getConfig().set("arena.enabled", false);
            plugin.saveConfig();
        }

        try {
            if (!arenaFile.getParentFile().exists()) {
                arenaFile.getParentFile().mkdirs();
            }
            yaml.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar arena.yml: " + e.getMessage());
        }
    }

    public Location resolveArena(Location... contexts) {
        List<Location> candidates = new ArrayList<>(knownArenas);
        if (configuredArena != null && !candidates.contains(configuredArena)) {
            candidates.add(configuredArena);
        }

        // 1. Prioridad: Comprobar si alguna arena conocida o configurada está en las proximidades (<= 45 bloques)
        for (Location ctx : contexts) {
            if (ctx == null || ctx.getWorld() == null) continue;
            for (Location candidate : candidates) {
                if (candidate != null && candidate.getWorld() != null && candidate.getWorld().equals(ctx.getWorld())) {
                    if (ctx.distanceSquared(candidate) <= 45.0 * 45.0) {
                        return candidate;
                    }
                }
            }
        }

        // 2. Si no hay arena conocida cerca, buscar arena física en un radio controlado (8 bloques) sin saturar el tick
        for (Location ctx : contexts) {
            if (ctx != null && ctx.getWorld() != null) {
                Location detected = findNearbyPhysicalArena(ctx, 8);
                if (detected != null) {
                    addKnownArena(detected);
                    return detected;
                }
            }
        }

        // 3. Fallback: buscar la arena conocida más cercana en el mismo mundo
        Location closest = null;
        double minDistanceSq = Double.MAX_VALUE;
        for (Location ctx : contexts) {
            if (ctx == null || ctx.getWorld() == null) continue;
            for (Location candidate : candidates) {
                if (candidate != null && candidate.getWorld() != null && candidate.getWorld().equals(ctx.getWorld())) {
                    double distSq = ctx.distanceSquared(candidate);
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        closest = candidate;
                    }
                }
            }
        }

        if (closest != null) {
            return closest;
        }

        if (configuredArena != null && configuredArena.getWorld() != null) {
            return configuredArena;
        }

        return null;
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 1. Cancelar cualquier reto pendiente
        pendingChallenges.remove(playerId);
        pendingChallenges.values().removeIf(ch -> ch.getChallengedId().equals(playerId));

        // 2. Comprobar si el jugador está en un combate activo y aplicar forfeit
        for (DuelArenaSession session : activeSessions) {
            if (session == null || session.getState().isTerminal()) continue;

            if (session.getChallengerId().equals(playerId)) {
                session.handlePlayerForfeit(playerId, "El retador " + player.getName() + " se desconectó del servidor.");
                break;
            } else if (session.getChallengedId().equals(playerId)) {
                session.handlePlayerForfeit(playerId, "El retado " + player.getName() + " se desconectó del servidor.");
                break;
            }
        }
    }

    public Location findNearbyPhysicalArena(Location searchCenter, int searchRadius) {
        World world = searchCenter.getWorld();
        if (world == null) return null;

        int cx = searchCenter.getBlockX();
        int cy = searchCenter.getBlockY();
        int cz = searchCenter.getBlockZ();

        // 1. Escanear alfombra roja y azul
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -5; y <= 5; y++) {
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    Material mat = b.getType();
                    if (mat == Material.RED_CARPET || mat == Material.RED_WOOL || mat == Material.RED_TERRACOTTA) {
                        for (int dx = -14; dx <= 14; dx++) {
                            for (int dz = -14; dz <= 14; dz++) {
                                int dist = Math.abs(dx) + Math.abs(dz);
                                if (dist < 4 || dist > 14) continue;
                                for (int dy = -1; dy <= 1; dy++) {
                                    Block blueCandidate = world.getBlockAt(b.getX() + dx, b.getY() + dy, b.getZ() + dz);
                                    Material blueMat = blueCandidate.getType();
                                    if (blueMat == Material.BLUE_CARPET || blueMat == Material.BLUE_WOOL || blueMat == Material.BLUE_TERRACOTTA) {
                                        double centerX = (b.getX() + blueCandidate.getX()) / 2.0 + 0.5;
                                        double centerZ = (b.getZ() + blueCandidate.getZ()) / 2.0 + 0.5;
                                        double centerY = Math.max(b.getY(), blueCandidate.getY());
                                        return new Location(world, centerX, centerY, centerZ);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Escanear centro de madera pulida (STRIPPED_OAK_WOOD)
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -5; y <= 5; y++) {
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (b.getType() == Material.STRIPPED_OAK_WOOD) {
                        if (world.getBlockAt(b.getX() + 1, b.getY(), b.getZ()).getType() == Material.STRIPPED_OAK_WOOD &&
                            world.getBlockAt(b.getX() - 1, b.getY(), b.getZ()).getType() == Material.STRIPPED_OAK_WOOD &&
                            world.getBlockAt(b.getX(), b.getY(), b.getZ() + 1).getType() == Material.STRIPPED_OAK_WOOD &&
                            world.getBlockAt(b.getX(), b.getY(), b.getZ() - 1).getType() == Material.STRIPPED_OAK_WOOD) {
                            return new Location(world, b.getX() + 0.5, b.getY() + 1.0, b.getZ() + 0.5);
                        }
                    }
                }
            }
        }

        return null;
    }

    public boolean sendChallenge(Player challenger, Player challenged, double wager) {
        if (challenger.getUniqueId().equals(challenged.getUniqueId())) {
            challenger.sendMessage(Component.text("No puedes retarte a ti mismo.", NamedTextColor.RED));
            return false;
        }

        if (findActiveSessionForPlayer(challenger.getUniqueId()) != null || findActiveSessionForPlayer(challenged.getUniqueId()) != null) {
            challenger.sendMessage(Component.text("Uno de los jugadores ya está combatiendo en el palenque.", NamedTextColor.RED));
            return false;
        }

        RoosterData rChallenger = repository.getActiveRooster(challenger.getUniqueId());
        RoosterData rChallenged = repository.getActiveRooster(challenged.getUniqueId());

        if (rChallenger == null) {
            challenger.sendMessage(Component.text("No tienes ningún gallo activo. Usa /gallos para seleccionar uno.", NamedTextColor.RED));
            return false;
        }
        if (rChallenged == null) {
            challenger.sendMessage(Component.text(challenged.getName() + " no tiene ningún gallo activo.", NamedTextColor.RED));
            return false;
        }

        if (!rChallenger.isUsableForCombat()) {
            challenger.sendMessage(Component.text("Tu gallo activo (" + rChallenger.getName() + ") está noqueado o indispuesto.", NamedTextColor.RED));
            return false;
        }
        if (!rChallenged.isUsableForCombat()) {
            challenger.sendMessage(Component.text("El gallo de " + challenged.getName() + " no está en condiciones de pelear.", NamedTextColor.RED));
            return false;
        }

        if (wager > 0 && bettingHook != null) {
            if (!bettingHook.canChallengeWithBet(challenger, challenged, wager)) {
                challenger.sendMessage(Component.text("No cumples los requisitos de balance o límites para apostar $" + String.format("%.2f", wager), NamedTextColor.RED));
                return false;
            }
        }

        DuelChallenge challenge = new DuelChallenge(challenger.getUniqueId(), challenged.getUniqueId(), wager);
        pendingChallenges.put(challenged.getUniqueId(), challenge);

        challenger.sendMessage(Component.text("Has retado a ", NamedTextColor.GREEN)
                .append(Component.text(challenged.getName(), NamedTextColor.GOLD))
                .append(Component.text(" con tu gallo ", NamedTextColor.GREEN))
                .append(Component.text(rChallenger.getName(), NamedTextColor.YELLOW))
                .append(Component.text(wager > 0 ? " por $" + String.format("%.2f", wager) : " (Duelo amistoso)", NamedTextColor.AQUA)));

        Component acceptBtn = Component.text("[ACEPTAR RETO]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Clic para aceptar el combate de palenque", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/gallos aceptar"));

        Component declineBtn = Component.text("[RECHAZAR]", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Clic para rechazar el reto", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/gallos rechazar"));

        challenged.sendMessage(Component.empty()
                .append(Component.text("\n⚔ [PALENQUE] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(challenger.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" te ha retado a una pelea de gallos 1v1!\n", NamedTextColor.WHITE))
                .append(Component.text("Gallo rival: ", NamedTextColor.GRAY))
                .append(Component.text(rChallenger.getName() + " (" + rChallenger.getGenetics().getPlumage() + ")\n", NamedTextColor.GOLD))
                .append(Component.text(wager > 0 ? "Apuesta en juego: $" + String.format("%.2f", wager) + "\n" : "Combate de honor (Sin apuesta)\n", NamedTextColor.AQUA))
                .append(acceptBtn).append(Component.text("  ")).append(declineBtn).append(Component.text("\n")));

        challenged.playSound(challenged.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
        return true;
    }

    public boolean acceptChallenge(Player challenged) {
        DuelChallenge challenge = pendingChallenges.remove(challenged.getUniqueId());
        if (challenge == null || challenge.isExpired()) {
            challenged.sendMessage(Component.text("No tienes ningún reto pendiente o ha expirado.", NamedTextColor.RED));
            return false;
        }

        Player challenger = Bukkit.getPlayer(challenge.getChallengerId());
        if (challenger == null || !challenger.isOnline()) {
            challenged.sendMessage(Component.text("El retador ya no está conectado.", NamedTextColor.RED));
            return false;
        }

        RoosterData rA = repository.getActiveRooster(challenger.getUniqueId());
        RoosterData rB = repository.getActiveRooster(challenged.getUniqueId());

        if (rA == null || !rA.isUsableForCombat() || rB == null || !rB.isUsableForCombat()) {
            challenged.sendMessage(Component.text("Uno de los gallos ya no está en condiciones de combatir.", NamedTextColor.RED));
            return false;
        }

        double wager = challenge.getWagerAmount();
        if (wager > 0 && bettingHook != null) {
            if (!bettingHook.lockDuelEscrow(challenger, challenged, wager)) {
                challenged.sendMessage(Component.text("No se pudo procesar el bloqueo de fondos en escrow para ambos jugadores.", NamedTextColor.RED));
                return false;
            }
        }

        Location arenaLoc = resolveArena(challenger.getLocation(), challenged.getLocation());
        if (arenaLoc != null && arenaLoc.getWorld() != null) {
            // Teletransportar a ambos entrenadores a sus esquinas respectivas fuera del cuadrilátero
            Location coachA = arenaLoc.clone().add(-5.2, 0, 0);
            coachA.setDirection(arenaLoc.toVector().subtract(coachA.toVector()));
            Location coachB = arenaLoc.clone().add(5.2, 0, 0);
            coachB.setDirection(arenaLoc.toVector().subtract(coachB.toVector()));

            challenger.teleportAsync(coachA);
            challenged.teleportAsync(coachB);
        } else {
            // Cuadrilátero dinámico en el punto medio si no hay arena fija configurada
            arenaLoc = challenger.getLocation().add(challenged.getLocation()).multiply(0.5);
            arenaLoc.setY(challenger.getLocation().getY());
        }

        DuelArenaSession session = new DuelArenaSession(
                plugin,
                repository,
                bettingHook,
                nuzlockeEnabled,
                knockoutDurationMillis,
                challenger,
                challenged,
                rA,
                rB,
                wager,
                arenaLoc
        );

        activeSessions.add(session);
        session.start();

        RoosterDuelStartEvent startEvent = new RoosterDuelStartEvent(session);
        plugin.getServer().getPluginManager().callEvent(startEvent);

        if (bettingHook != null) {
            try {
                bettingHook.onDuelStart(session);
            } catch (Exception ignored) {}
        }

        return true;
    }

    public boolean startPvEDuel(Player player, RoosterData playerRooster, String opponentName, RoosterData opponentRooster, double wager, java.util.function.Consumer<Boolean> onComplete) {
        if (findActiveSessionForPlayer(player.getUniqueId()) != null) {
            player.sendMessage(Component.text("¡Ya te encuentras en medio de un combate de palenque!", NamedTextColor.RED));
            return false;
        }

        Location arenaLoc = resolveArena(player.getLocation());
        if (arenaLoc == null || arenaLoc.getWorld() == null) {
            player.sendMessage(Component.text("❌ [Palenque] No se encontró ningún cuadrilátero configurado cerca.", NamedTextColor.RED));
            player.sendMessage(Component.text("Colócate en tu arena y escribe: /rg admin arena fijar (o /rg admin estructura arena)", NamedTextColor.YELLOW));
            return false;
        }

        Location coachSpot = arenaLoc.clone().add(-5.2, 0, 0);
        coachSpot.setDirection(arenaLoc.toVector().subtract(coachSpot.toVector()));
        if (!coachSpot.getBlock().isPassable()) {
            coachSpot.add(0, 1, 0);
        }
        player.teleportAsync(coachSpot);

        DuelArenaSession session = new DuelArenaSession(
                plugin,
                repository,
                bettingHook,
                false,
                knockoutDurationMillis,
                player,
                null,
                opponentName,
                playerRooster,
                opponentRooster,
                wager,
                arenaLoc
        );

        if (onComplete != null) {
            session.setOnDuelEndCallback(onComplete);
        }

        activeSessions.add(session);
        session.start();
        return true;
    }

    public boolean declineChallenge(Player challenged) {
        DuelChallenge challenge = pendingChallenges.remove(challenged.getUniqueId());
        if (challenge == null) {
            challenged.sendMessage(Component.text("No tienes ningún reto pendiente.", NamedTextColor.RED));
            return false;
        }

        Player challenger = Bukkit.getPlayer(challenge.getChallengerId());
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(Component.text(challenged.getName() + " ha rechazado tu reto de palenque.", NamedTextColor.YELLOW));
        }
        challenged.sendMessage(Component.text("Has rechazado el reto.", NamedTextColor.GRAY));
        return true;
    }

    public boolean placeSpectatorBet(Player bettor, String targetPlayerName, double amount) {
        if (amount <= 0) {
            bettor.sendMessage(Component.text("El monto de la apuesta debe ser mayor a 0.", NamedTextColor.RED));
            return false;
        }

        DuelArenaSession session = findActiveSessionNear(bettor.getLocation(), 35.0);
        if (session == null) {
            bettor.sendMessage(Component.text("No hay ningún combate en ventana de apuestas cerca de ti.", NamedTextColor.RED));
            return false;
        }

        if (session.getState() != DuelArenaSession.State.BETTING_WINDOW) {
            bettor.sendMessage(Component.text("La ventana de apuestas de este combate ya está cerrada.", NamedTextColor.RED));
            return false;
        }

        UUID targetId = null;
        if (session.getChallengerName().equalsIgnoreCase(targetPlayerName)) {
            targetId = session.getChallengerId();
        } else if (session.getChallengedName().equalsIgnoreCase(targetPlayerName)) {
            targetId = session.getChallengedId();
        } else {
            bettor.sendMessage(Component.text("Ese jugador no participa en este combate (" + session.getChallengerName() + " vs " + session.getChallengedName() + ").", NamedTextColor.RED));
            return false;
        }

        if (bettingHook == null) {
            bettor.sendMessage(Component.text("El módulo de economía/apuestas externas no está activo.", NamedTextColor.RED));
            return false;
        }

        if (!bettingHook.placeSpectatorBet(bettor, targetPlayerName, amount)) {
            bettor.sendMessage(Component.text("No se pudo registrar la apuesta de espectador.", NamedTextColor.RED));
            return false;
        }

        SpectatorBet bet = new SpectatorBet(bettor.getUniqueId(), targetId, amount);
        session.addSpectatorBet(bet);

        bettor.sendMessage(Component.text("✔ ¡Apuesta de $", NamedTextColor.GREEN)
                .append(Component.text(String.format("%.2f", amount), NamedTextColor.GOLD))
                .append(Component.text(" registrada a favor de ", NamedTextColor.GREEN))
                .append(Component.text(targetPlayerName, NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.GREEN)));

        bettor.playSound(bettor.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        return true;
    }

    public DuelArenaSession findActiveSessionNear(Location loc, double maxDistance) {
        double maxSq = maxDistance * maxDistance;
        for (DuelArenaSession session : activeSessions) {
            if (!session.getState().isTerminal() &&
                    session.getArenaCenter().getWorld().equals(loc.getWorld()) &&
                    session.getArenaCenter().distanceSquared(loc) <= maxSq) {
                return session;
            }
        }
        return null;
    }

    public DuelArenaSession findActiveSessionForPlayer(UUID playerId) {
        for (DuelArenaSession session : activeSessions) {
            if (!session.getState().isTerminal()) {
                if (session.getChallengerId().equals(playerId) || session.getChallengedId().equals(playerId)) {
                    return session;
                }
            }
        }
        return null;
    }

    public void abortAll() {
        for (DuelArenaSession session : activeSessions) {
            if (!session.getState().isTerminal()) {
                session.abort("Servidor apagándose o recargándose.");
            }
        }
        activeSessions.clear();
        pendingChallenges.clear();
    }
}
