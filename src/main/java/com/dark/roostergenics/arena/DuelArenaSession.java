package com.dark.roostergenics.arena;

import com.dark.roostergenics.api.betting.DuelBettingHook;
import com.dark.roostergenics.api.events.RoosterDuelAbortEvent;
import com.dark.roostergenics.api.events.RoosterDuelDrawEvent;
import com.dark.roostergenics.api.events.RoosterDuelVictoryEvent;
import com.dark.roostergenics.arena.combat.CombatMessage;
import com.dark.roostergenics.arena.combat.CombatState;
import com.dark.roostergenics.arena.combat.PhysicalCombatCommand;
import com.dark.roostergenics.arena.combat.PhysicalCommandType;
import com.dark.roostergenics.arena.combat.PhysicalExecutionLease;
import com.dark.roostergenics.genetics.RoosterTrait;
import com.dark.roostergenics.items.RoosterHeldItem;
import com.dark.roostergenics.model.RoosterCombatStats;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterHealthStatus;
import com.dark.roostergenics.model.RoosterInjury;
import com.dark.roostergenics.storage.RoosterRepository;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Sesión de combate físico 1v1 en el palenque con arquitectura estricta para Folia:
 * 1. Simulación lógica poseída exclusivamente por RegionScheduler(arenaCenter).
 * 2. Actuación física delegada a EntityScheduler(chicken) mediante comandos inmutables con sessionEpoch.
 * 3. Notificaciones a jugadores despachadas a Player#getScheduler().
 * 4. Recepción de eventos externos vía buzón concurrente inmutable (CombatMessage).
 * 5. Liquidación atómica garantizada (exactamente una liquidación por sesión).
 */
public class DuelArenaSession {

    public enum State {
        CREATED,
        BETTING_WINDOW,
        FIGHTING,
        ENDING,
        FINISHED,
        CANCELLED;

        public boolean isTerminal() {
            return this == ENDING || this == FINISHED || this == CANCELLED;
        }

        public boolean isFighting() {
            return this == FIGHTING;
        }

        public boolean canAcceptBets() {
            return this == BETTING_WINDOW;
        }
    }

    private final JavaPlugin plugin;
    private final RoosterRepository repository;
    private final DuelBettingHook bettingHook;
    private final boolean nuzlockeEnabled;
    private final long knockoutDurationMillis;

    private final UUID challengerId;
    private final UUID challengedId;
    private final String challengerName;
    private final String challengedName;

    private final RoosterData roosterA;
    private final RoosterData roosterB;
    private final RoosterCombatStats statsA;
    private final RoosterCombatStats statsB;

    private final double wagerAmount;
    private final List<SpectatorBet> spectatorBets = new CopyOnWriteArrayList<>();

    private final Location arenaCenter;
    private final Location spawnA;
    private final Location spawnB;

    // --- AUTORIDAD LÓGICA (RegionScheduler arenaCenter) ---
    private volatile State state = State.CREATED;
    private int countdownSeconds = 15;
    private ScheduledTask task;
    private final SecureRandom rng = new SecureRandom();

    private final PhysicalExecutionLease executionLease = new PhysicalExecutionLease();
    private final ConcurrentLinkedQueue<CombatMessage> inbox = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean settlementGuarded = new AtomicBoolean(false);

    private Chicken entityA;
    private Chicken entityB;
    private UUID entityAUuid;
    private UUID entityBUuid;

    private double currentHpA;
    private double currentHpB;
    private int cooldownTicksA = 0;
    private int cooldownTicksB = 0;
    private boolean fenixUsedA = false;
    private boolean fenixUsedB = false;
    private int fightElapsedTicks = 0;

    private BossBar bossBarRed;
    private BossBar bossBarBlue;
    private final Set<UUID> bossBarViewers = ConcurrentHashMap.newKeySet();

    private boolean focusUsedA = false;
    private boolean focusUsedB = false;
    private boolean sitrusUsedA = false;
    private boolean sitrusUsedB = false;
    private int leftoversTicks = 0;
    private Consumer<Boolean> onDuelEndCallback;

    public void setOnDuelEndCallback(Consumer<Boolean> callback) {
        this.onDuelEndCallback = callback;
    }

    public DuelArenaSession(JavaPlugin plugin,
                            RoosterRepository repository,
                            DuelBettingHook bettingHook,
                            boolean nuzlockeEnabled,
                            long knockoutDurationMillis,
                            Player playerA,
                            Player playerB,
                            RoosterData roosterA,
                            RoosterData roosterB,
                            double wagerAmount,
                            Location arenaCenter) {
        this(plugin, repository, bettingHook, nuzlockeEnabled, knockoutDurationMillis, playerA, playerB, null, roosterA, roosterB, wagerAmount, arenaCenter);
    }

    public DuelArenaSession(JavaPlugin plugin,
                            RoosterRepository repository,
                            DuelBettingHook bettingHook,
                            boolean nuzlockeEnabled,
                            long knockoutDurationMillis,
                            Player playerA,
                            Player playerB,
                            String challengedNameOverride,
                            RoosterData roosterA,
                            RoosterData roosterB,
                            double wagerAmount,
                            Location arenaCenter) {
        this.plugin = plugin;
        this.repository = repository;
        this.bettingHook = bettingHook;
        this.nuzlockeEnabled = nuzlockeEnabled;
        this.knockoutDurationMillis = knockoutDurationMillis;

        this.challengerId = playerA.getUniqueId();
        this.challengedId = playerB != null ? playerB.getUniqueId() : (roosterB.getOwnerId() != null ? roosterB.getOwnerId() : UUID.randomUUID());
        this.challengerName = playerA.getName();
        this.challengedName = challengedNameOverride != null ? challengedNameOverride : (playerB != null ? playerB.getName() : "Entrenador Gallero");

        this.roosterA = roosterA;
        this.roosterB = roosterB;
        this.statsA = roosterA.calculateCombatStats();
        this.statsB = roosterB.calculateCombatStats();

        this.wagerAmount = wagerAmount;
        this.arenaCenter = arenaCenter.clone();

        Vector offset = new Vector(2.5, 0.05, 0);
        this.spawnA = arenaCenter.clone().subtract(offset);
        this.spawnB = arenaCenter.clone().add(offset);
        this.spawnA.setDirection(spawnB.toVector().subtract(spawnA.toVector()));
        this.spawnB.setDirection(spawnA.toVector().subtract(spawnB.toVector()));

        this.currentHpA = statsA.getMaxHp();
        this.currentHpB = statsB.getMaxHp();
    }

    public PhysicalExecutionLease getExecutionLease() {
        return executionLease;
    }

    public void start() {
        plugin.getServer().getRegionScheduler().run(plugin, arenaCenter, scheduledTask -> {
            if (this.state != State.CREATED) {
                return;
            }
            this.state = State.BETTING_WINDOW;
            broadcastLocalAnnouncement();

            // El ciclo de apuestas y cuenta regresiva corre en el RegionScheduler de la arena
            this.task = plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, arenaCenter, t -> {
                processPendingMessages();
                if (state == State.BETTING_WINDOW) {
                    tickBettingWindow();
                } else if (state == State.FIGHTING) {
                    tickFighting();
                }
            }, 1L, 20L);
        });
    }

    // =========================================================================
    // BUZÓN DE MENSAJES INMUTABLES (Cross-Thread Safe Message Ingestion)
    // =========================================================================

    public void enqueueMessage(CombatMessage message) {
        if (state.isTerminal()) {
            return;
        }
        inbox.add(message);
        // Despachar drenado asíncrono ordenado en el RegionScheduler de la arena
        try {
            plugin.getServer().getRegionScheduler().run(plugin, arenaCenter, task -> processPendingMessages());
        } catch (Throwable ignored) {}
    }

    private void processPendingMessages() {
        CombatMessage msg;
        while ((msg = inbox.poll()) != null) {
            if (state.isTerminal() && !(msg instanceof CombatMessage.AdminAbort)) {
                continue;
            }
            switch (msg) {
                case CombatMessage.PlayerQuit quit -> handleQuitMessage(quit);
                case CombatMessage.EntityRetired retired -> handleEntityRetiredMessage(retired);
                case CombatMessage.FighterDied died -> handleFighterDiedMessage(died);
                case CombatMessage.Forfeit forfeit -> handleForfeitMessage(forfeit);
                case CombatMessage.SpectatorBetMsg betMsg -> handleSpectatorBetMessage(betMsg);
                case CombatMessage.AdminAbort abort -> handleAdminAbortMessage(abort);
                case CombatMessage.Timeout timeout -> handleTimeoutMessage(timeout);
                case CombatMessage.ExternalDamage dmg -> handleExternalDamageMessage(dmg);
            }
        }
    }

    private void handleQuitMessage(CombatMessage.PlayerQuit quit) {
        if (state.isTerminal()) return;
        if (state == State.BETTING_WINDOW) {
            abort("Combate abortado por desconexión: " + quit.reason());
            return;
        }
        if (state == State.FIGHTING) {
            boolean forfeitedIsA = (quit.playerId() != null && quit.playerId().equals(challengerId));
            if (forfeitedIsA) {
                currentHpA = 0;
                if (currentHpB <= 0) currentHpB = 10.0;
            } else {
                currentHpB = 0;
                if (currentHpA <= 0) currentHpA = 10.0;
            }
            broadcastMessageToNearby(Component.text("⚠ [PALENQUE] " + quit.reason(), NamedTextColor.RED, TextDecoration.BOLD));
            declareWinner();
        }
    }

    private void handleEntityRetiredMessage(CombatMessage.EntityRetired retired) {
        if (state.isTerminal()) return;
        if (retired.sessionEpoch() != executionLease.getActiveEpoch()) {
            return; // Notificación de una época anterior
        }
        if (state == State.FIGHTING) {
            endFightInDraw("Uno de los ejemplares abandonó o fue retirado del cuadrilátero.");
        }
    }

    private void handleFighterDiedMessage(CombatMessage.FighterDied died) {
        if (state.isTerminal()) return;
        if (died.sessionEpoch() != executionLease.getActiveEpoch()) {
            return;
        }
        if (state == State.FIGHTING) {
            if (died.entityId().equals(entityAUuid)) {
                currentHpA = 0;
            } else if (died.entityId().equals(entityBUuid)) {
                currentHpB = 0;
            }
            declareWinner();
        }
    }

    private void handleForfeitMessage(CombatMessage.Forfeit forfeit) {
        if (state.isTerminal()) return;
        boolean forfeitedIsA = (forfeit.playerId() != null && forfeit.playerId().equals(challengerId));
        if (forfeitedIsA) {
            currentHpA = 0;
            if (currentHpB <= 0) currentHpB = 10.0;
        } else {
            currentHpB = 0;
            if (currentHpA <= 0) currentHpA = 10.0;
        }
        broadcastMessageToNearby(Component.text("⚠ [PALENQUE] " + forfeit.reason(), NamedTextColor.RED, TextDecoration.BOLD));
        declareWinner();
    }

    private void handleSpectatorBetMessage(CombatMessage.SpectatorBetMsg betMsg) {
        if (state == State.BETTING_WINDOW) {
            spectatorBets.add(betMsg.bet());
        }
    }

    private void handleAdminAbortMessage(CombatMessage.AdminAbort abort) {
        executeAbort(abort.reason());
    }

    private void handleTimeoutMessage(CombatMessage.Timeout timeout) {
        if (state == State.FIGHTING) {
            endFightInDraw(timeout.reason());
        }
    }

    private void handleExternalDamageMessage(CombatMessage.ExternalDamage dmg) {
        if (state == State.FIGHTING) {
            if (dmg.victimId().equals(entityAUuid)) {
                currentHpA = Math.max(0.0, currentHpA - dmg.damage());
            } else if (dmg.victimId().equals(entityBUuid)) {
                currentHpB = Math.max(0.0, currentHpB - dmg.damage());
            }
            if (currentHpA <= 0 || currentHpB <= 0) {
                declareWinner();
            }
        }
    }

    // =========================================================================
    // COMUNICACIÓN CON JUGADORES Y ACTUADORES FÍSICOS (EntityScheduler / PlayerScheduler)
    // =========================================================================

    private void dispatchPlayerAction(UUID playerId, Consumer<Player> action) {
        if (playerId == null) return;
        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) {
            p.getScheduler().run(plugin, task -> {
                if (p.isOnline()) {
                    action.accept(p);
                }
            }, null);
        }
    }

    private void broadcastMessageToNearby(Component msg) {
        for (Player p : arenaCenter.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arenaCenter) <= 900) {
                dispatchPlayerAction(p.getUniqueId(), player -> player.sendMessage(msg));
            }
        }
    }

    private void dispatchEntityCommand(Chicken chicken,
                                       PhysicalCommandType type,
                                       Vector vectorParam,
                                       String textParam,
                                       double doubleParam,
                                       int intParam,
                                       Object extraPayload) {
        if (chicken == null || !chicken.isValid()) return;
        long epoch = executionLease.getActiveEpoch();
        long cmdId = executionLease.generateNextCommandId();
        UUID entityId = chicken.getUniqueId();

        PhysicalCombatCommand cmd = new PhysicalCombatCommand(
                epoch, cmdId, entityId, type, vectorParam, textParam, doubleParam, intParam, extraPayload
        );

        chicken.getScheduler().run(plugin, scheduledTask -> {
            if (!executionLease.validateAndApply(entityId, cmd.sessionEpoch(), cmd.commandId())) {
                return; // Stale o duplicado
            }
            applyPhysicalCommand(chicken, cmd);
        }, () -> {
            // RETIRED CALLBACK: Mínimo, jamás muta estado ni carga chunks
            enqueueMessage(new CombatMessage.EntityRetired(entityId, epoch));
        });
    }

    private void applyPhysicalCommand(Chicken chicken, PhysicalCombatCommand cmd) {
        if (!chicken.isValid()) return;

        switch (cmd.type()) {
            case FACE_DIRECTION -> {
                if (cmd.vectorParam() != null && cmd.vectorParam().lengthSquared() > 0.001) {
                    Location loc = chicken.getLocation();
                    loc.setDirection(cmd.vectorParam());
                    chicken.teleport(loc);
                }
            }
            case SET_VELOCITY -> {
                if (cmd.vectorParam() != null) {
                    chicken.setVelocity(cmd.vectorParam());
                }
            }
            case UPDATE_NAMETAG -> {
                if (cmd.textParam() != null) {
                    chicken.setCustomName(cmd.textParam());
                }
            }
            case APPLY_DAMAGE_EFFECT -> {
                if (cmd.vectorParam() != null) {
                    chicken.setVelocity(cmd.vectorParam());
                }
            }
            case SET_FIRE -> {
                chicken.setFireTicks(cmd.intParam());
            }
            case APPLY_POTION -> {
                if (cmd.extraPayload() instanceof PotionEffect effect) {
                    chicken.addPotionEffect(effect);
                }
            }
            case REMOVE_ENTITY -> {
                chicken.remove();
            }
            case SPAWN_PARTICLES, PLAY_SOUND -> {
                // Eventos de entorno
            }
        }
    }

    // =========================================================================
    // CICLO DE SIMULACIÓN DE COMBATE (Logical Arena Simulation)
    // =========================================================================

    private void broadcastLocalAnnouncement() {
        Component hoverA = buildRoosterHover(roosterA, challengerName);
        Component hoverB = buildRoosterHover(roosterB, challengedName);

        Component header = com.dark.roostergenics.util.Mm.text("<gold>⚔ <bold>[PALENQUE]</bold> ¡Combate 1v1 Inminente! ⚔ <yellow>⏰ Apuestas: 15s</yellow>\n");

        Component fighterA = Component.text(" [Gallo Rojo: " + roosterA.getName() + " (" + challengerName + ")]", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(hoverA));

        Component vs = Component.text(" vs ", NamedTextColor.GRAY);

        Component fighterB = Component.text("[Gallo Azul: " + roosterB.getName() + " (" + challengedName + ")]\n", NamedTextColor.BLUE, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(hoverB));

        Component clickableBetA = Component.text(" [Apostar Rojo] ", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Haz clic para apostar $100 por " + challengerName, NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.suggestCommand("/rg apostar " + challengerName + " 100"));

        Component clickableBetB = Component.text(" [Apostar Azul] ", NamedTextColor.BLUE, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Haz clic para apostar $100 por " + challengedName, NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.suggestCommand("/rg apostar " + challengedName + " 100"));

        Component actionLine = Component.text("  ➥ ", NamedTextColor.DARK_GRAY)
                .append(clickableBetA)
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(clickableBetB);

        Component fullMsg = header.append(fighterA).append(vs).append(fighterB).append(actionLine);

        for (Player p : arenaCenter.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arenaCenter) <= 900) {
                dispatchPlayerAction(p.getUniqueId(), player -> {
                    player.sendMessage(fullMsg);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                });
            }
        }
    }

    private Component buildRoosterHover(RoosterData r, String trainerName) {
        var stats = r.calculateCombatStats();
        String held = r.hasHeldItem() ? r.getHeldItem().getDisplayName() : "Ninguno";
        StringBuilder sb = new StringBuilder();
        sb.append("<bold><gold>★ ").append(r.getName()).append("</gold></bold> <gray>(").append(trainerName).append(")</gray>\n");
        sb.append("<gray>Nivel: <yellow>Nv.").append(r.getLevel()).append("</yellow> | Plumaje: <aqua>").append(r.getGenetics().getPlumage()).append("</aqua>\n");
        sb.append("<red>❤ Vida: ").append(String.format("%.1f", stats.getMaxHp())).append("</red> | ");
        sb.append("<gold>⚔ ATK: ").append(String.format("%.1f", stats.getAttack())).append("</gold> | ");
        sb.append("<blue>🛡 DEF: ").append(String.format("%.1f", stats.getDefense())).append("</blue>\n");
        sb.append("<gray>🎒 Objeto: <yellow>").append(held).append("</yellow>\n");
        if (!r.getGenetics().getTraits().isEmpty()) {
            sb.append("<gray>Rasgos: <light_purple>");
            int count = 0;
            for (var t : r.getGenetics().getTraits()) {
                if (count > 0) sb.append(", ");
                sb.append(t.getDisplayName());
                count++;
            }
            sb.append("</light_purple>\n");
        }
        sb.append("<dark_gray>« Clic para preparar apuesta »</dark_gray>");
        return com.dark.roostergenics.util.Mm.text(sb.toString());
    }

    private void tickBettingWindow() {
        countdownSeconds--;

        if (countdownSeconds == 10 || countdownSeconds == 5 || countdownSeconds <= 3 && countdownSeconds > 0) {
            for (Player p : arenaCenter.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(arenaCenter) <= 900) {
                    dispatchPlayerAction(p.getUniqueId(), player -> {
                        player.sendActionBar(Component.text("⏰ El combate comienza en " + countdownSeconds + "s...", NamedTextColor.GOLD));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
                    });
                }
            }
        }

        if (countdownSeconds <= 0) {
            spawnFightersAndBegin();
        }
    }

    private void spawnFightersAndBegin() {
        this.state = State.FIGHTING;

        if (task != null) task.cancel();

        entityA = spawnRoosterEntity(spawnA, roosterA, statsA, "§c[R] ");
        entityB = spawnRoosterEntity(spawnB, roosterB, statsB, "§9[A] ");
        entityAUuid = entityA.getUniqueId();
        entityBUuid = entityB.getUniqueId();

        bossBarRed = BossBar.bossBar(
                Component.text("🔴 " + roosterA.getName() + " (" + challengerName + ")", NamedTextColor.RED, TextDecoration.BOLD),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10
        );
        bossBarBlue = BossBar.bossBar(
                Component.text("🔵 " + roosterB.getName() + " (" + challengedName + ")", NamedTextColor.BLUE, TextDecoration.BOLD),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.NOTCHED_10
        );

        if (roosterA.getGenetics().hasTrait(RoosterTrait.CANTO_DE_GUERRA)) {
            spawnFloatingText(spawnA.clone().add(0, 0.7, 0), "§6🎺 ¡CANTO DE GUERRA!");
            spawnA.getWorld().spawnParticle(Particle.NOTE, spawnA.clone().add(0, 0.6, 0), 8, 0.2, 0.2, 0.2, 0.1);
        }
        if (roosterB.getGenetics().hasTrait(RoosterTrait.CANTO_DE_GUERRA)) {
            spawnFloatingText(spawnB.clone().add(0, 0.7, 0), "§6🎺 ¡CANTO DE GUERRA!");
            spawnB.getWorld().spawnParticle(Particle.NOTE, spawnB.clone().add(0, 0.6, 0), 8, 0.2, 0.2, 0.2, 0.1);
        }

        for (Player p : arenaCenter.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arenaCenter) <= 900) {
                dispatchPlayerAction(p.getUniqueId(), player -> {
                    player.showTitle(Title.title(
                            Component.text("¡PELEA!", NamedTextColor.RED, TextDecoration.BOLD),
                            Component.text("¡Que gane el mejor espolón!", NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(400))
                    ));
                    player.playSound(arenaCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);
                    player.playSound(arenaCenter, Sound.ENTITY_CHICKEN_AMBIENT, 1.2f, 0.8f);
                });
            }
        }

        // Tarea repetitiva de simulación lógica a alta frecuencia (cada 3 ticks)
        this.task = plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, arenaCenter, t -> {
            processPendingMessages();
            if (state == State.FIGHTING) {
                tickFighting();
            }
        }, 1L, 3L);
    }

    private Chicken spawnRoosterEntity(Location loc, RoosterData rooster, RoosterCombatStats stats, String prefix) {
        return loc.getWorld().spawn(loc, Chicken.class, c -> {
            c.setAdult();
            c.setBreed(false);
            c.setCustomNameVisible(true);
            try {
                c.setVariant(Chicken.Variant.TEMPERATE);
            } catch (Throwable ignored) {}
            String plumage = rooster.getGenetics() != null ? rooster.getGenetics().getExpressedPlumage() : "PLAINS";
            String tag = com.dark.roostergenics.genetics.RoosterGenetics.getInvisibleVariantTag(plumage);
            String shortPlumage = com.dark.roostergenics.genetics.RoosterGenetics.getShortPlumageDisplay(plumage);
            c.setCustomName(prefix + "§eNv." + rooster.getLevel() + " §f" + rooster.getName() + " §7(" + shortPlumage + "§7)" + tag);
            c.setRemoveWhenFarAway(false);
            c.setPersistent(false);
            c.setMetadata("roostergenics_arena", new FixedMetadataValue(plugin, true));
            c.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 120, 1, false, false));

            try {
                java.lang.reflect.Field field = org.bukkit.attribute.Attribute.class.getField("GENERIC_SCALE");
                org.bukkit.attribute.Attribute scaleAttr = (org.bukkit.attribute.Attribute) field.get(null);
                org.bukkit.attribute.AttributeInstance instance = c.getAttribute(scaleAttr);
                if (instance != null) {
                    instance.setBaseValue(stats.getScale());
                }
            } catch (Throwable ignored) {}
        });
    }

    private void tickFighting() {
        fightElapsedTicks += 3;

        // Comprobación de integridad
        if (entityA == null || !entityA.isValid() || entityB == null || !entityB.isValid()) {
            endFightInDraw("Uno de los ejemplares abandonó el cuadrilátero.");
            return;
        }

        // Límite de tiempo (90 segundos = 1800 ticks)
        if (fightElapsedTicks > 1800) {
            endFightInDraw("Tiempo agotado: El combate termina en empate técnico.");
            return;
        }

        Location locA = entityA.getLocation();
        Location locB = entityB.getLocation();

        // 1. Orientación fija permanente: Asegurar que los gallos SIEMPRE se encaren
        Vector toB = locB.toVector().subtract(locA.toVector());
        if (toB.lengthSquared() > 0.001) {
            dispatchEntityCommand(entityA, PhysicalCommandType.FACE_DIRECTION, toB, null, 0.0, 0, null);
        }
        Vector toA = locA.toVector().subtract(locB.toVector());
        if (toA.lengthSquared() > 0.001) {
            dispatchEntityCommand(entityB, PhysicalCommandType.FACE_DIRECTION, toA, null, 0.0, 0, null);
        }

        // 2. Efecto pasivo de Restos de Trigo y Semilla Milagrosa cada 80 ticks
        leftoversTicks += 3;
        if (leftoversTicks >= 80) {
            leftoversTicks = 0;
            if (roosterA.getHeldItem() == RoosterHeldItem.RESTOS && currentHpA < statsA.getMaxHp() && currentHpA > 0) {
                double mult = (roosterA.getHeldItem() == RoosterHeldItem.SEMILLA_MILAGROSA) ? 1.5 : 1.0;
                double heal = statsA.getMaxHp() * 0.04 * mult;
                currentHpA = Math.min(statsA.getMaxHp(), currentHpA + heal);
                entityA.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, locA.clone().add(0, 0.5, 0), 8, 0.2, 0.2, 0.2, 0.02);
                entityA.getWorld().playSound(locA, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
                spawnFloatingText(locA.clone().add(0, 0.6, 0), "§a+" + String.format("%.1f", heal) + " HP §e🌾");
            }
            if (roosterB.getHeldItem() == RoosterHeldItem.RESTOS && currentHpB < statsB.getMaxHp() && currentHpB > 0) {
                double mult = (roosterB.getHeldItem() == RoosterHeldItem.SEMILLA_MILAGROSA) ? 1.5 : 1.0;
                double heal = statsB.getMaxHp() * 0.04 * mult;
                currentHpB = Math.min(statsB.getMaxHp(), currentHpB + heal);
                entityB.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, locB.clone().add(0, 0.5, 0), 8, 0.2, 0.2, 0.2, 0.02);
                entityB.getWorld().playSound(locB, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
                spawnFloatingText(locB.clone().add(0, 0.6, 0), "§a+" + String.format("%.1f", heal) + " HP §e🌾");
            }
        }

        // 3. Tensión visual y latidos si algún gallo está en estado crítico (HP < 25%)
        if (fightElapsedTicks % 12 == 0) {
            if (currentHpA > 0 && (currentHpA / statsA.getMaxHp()) < 0.25) {
                entityA.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, locA.clone().add(0, 0.4, 0), 2, 0.1, 0.1, 0.1, 0.02);
                entityA.getWorld().playSound(locA, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.6f, 0.6f);
            }
            if (currentHpB > 0 && (currentHpB / statsB.getMaxHp()) < 0.25) {
                entityB.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, locB.clone().add(0, 0.4, 0), 2, 0.1, 0.1, 0.1, 0.02);
                entityB.getWorld().playSound(locB, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.6f, 0.6f);
            }
        }

        // Techo de vuelo estricto
        double arenaFloorY = arenaCenter.getY();
        double maxFlightHeight = 4.5;

        if (entityA.isValid()) {
            double deltaYA = locA.getY() - arenaFloorY;
            if (deltaYA > maxFlightHeight) {
                Vector clampA = entityA.getVelocity().clone();
                clampA.setY(-0.45);
                dispatchEntityCommand(entityA, PhysicalCommandType.SET_VELOCITY, clampA, null, 0.0, 0, null);
            }
        }

        if (entityB.isValid()) {
            double deltaYB = locB.getY() - arenaFloorY;
            if (deltaYB > maxFlightHeight) {
                Vector clampB = entityB.getVelocity().clone();
                clampB.setY(-0.45);
                dispatchEntityCommand(entityB, PhysicalCommandType.SET_VELOCITY, clampB, null, 0.0, 0, null);
            }
        }

        double distSq = locA.distanceSquared(locB);

        // Desplazamiento e inteligencia de acercamiento
        if (distSq > 1.8) {
            Vector dirA = locB.toVector().subtract(locA.toVector()).normalize().multiply(0.28);
            dirA.setY(0.10);
            dispatchEntityCommand(entityA, PhysicalCommandType.SET_VELOCITY, dirA, null, 0.0, 0, null);

            Vector dirB = locA.toVector().subtract(locB.toVector()).normalize().multiply(0.28);
            dirB.setY(0.10);
            dispatchEntityCommand(entityB, PhysicalCommandType.SET_VELOCITY, dirB, null, 0.0, 0, null);
        }

        if (cooldownTicksA > 0) cooldownTicksA -= 3;
        if (cooldownTicksB > 0) cooldownTicksB -= 3;

        // Ataques a corta distancia (<= 2.5 bloques)
        if (distSq <= 6.25) {
            if (cooldownTicksA <= 0 && currentHpA > 0) {
                performAttack(entityA, entityB, roosterA, statsA, roosterB, statsB, true);
                int cd = statsA.getAttackSpeedTicks();
                if (roosterA.getGenetics().hasTrait(RoosterTrait.CANTO_DE_GUERRA) && fightElapsedTicks <= 160) {
                    cd = Math.max(4, (int) Math.round(cd * 0.80));
                }
                if (roosterA.hasInjury(RoosterInjury.FATIGA_MUSCULAR)) {
                    cd += 6;
                }
                cooldownTicksA = cd;
            }
            if (cooldownTicksB <= 0 && currentHpB > 0) {
                performAttack(entityB, entityA, roosterB, statsB, roosterA, statsA, false);
                int cd = statsB.getAttackSpeedTicks();
                if (roosterB.getGenetics().hasTrait(RoosterTrait.CANTO_DE_GUERRA) && fightElapsedTicks <= 160) {
                    cd = Math.max(4, (int) Math.round(cd * 0.80));
                }
                if (roosterB.hasInjury(RoosterInjury.FATIGA_MUSCULAR)) {
                    cd += 6;
                }
                cooldownTicksB = cd;
            }
        }

        // Actualizar nombres con barra de vida segmentada
        updateNametag(entityA, "§c[R] ", roosterA.getName(), currentHpA, statsA.getMaxHp(), roosterA.getHeldItem());
        updateNametag(entityB, "§9[A] ", roosterB.getName(), currentHpB, statsB.getMaxHp(), roosterB.getHeldItem());

        // Actualizar BossBars dinámicas
        updateCombatBossBars();
    }

    private void performAttack(Chicken attacker, Chicken defender,
                               RoosterData rAttacker, RoosterCombatStats sAttacker,
                               RoosterData rDefender, RoosterCombatStats sDefender,
                               boolean isAttackerA) {
        Location attLoc = attacker.getLocation();
        Location defLoc = defender.getLocation();

        if (rAttacker.hasInjury(RoosterInjury.SHOCK_DE_COMBATE) && rng.nextDouble() < 0.15) {
            attacker.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, attLoc.clone().add(0, 0.4, 0), 6, 0.1, 0.1, 0.1, 0.02);
            attacker.getWorld().playSound(attLoc, Sound.ENTITY_CHICKEN_AMBIENT, 0.8f, 0.6f);
            spawnFloatingText(attLoc.clone().add(0, 0.6, 0), "§8😨 ¡SHOCK DE COMBATE!");
            return;
        }

        if (rAttacker.hasInjury(RoosterInjury.OJO_HINCHADO) && rng.nextDouble() < 0.20) {
            attacker.getWorld().playSound(attLoc, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.8f, 1.2f);
            attacker.getWorld().spawnParticle(Particle.SMOKE, attLoc.clone().add(0, 0.4, 0), 6, 0.1, 0.1, 0.1, 0.02);
            spawnFloatingText(attLoc.clone().add(0, 0.6, 0), "§c👁 ¡FALLO POR OJO HINCHADO!");
            return;
        }

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.ESPURIO_NERVIOSO) && rng.nextDouble() < 0.10) {
            attacker.getWorld().spawnParticle(Particle.SMOKE, attLoc.clone().add(0, 0.2, 0), 6, 0.1, 0.1, 0.1, 0.02);
            attacker.getWorld().playSound(attLoc, Sound.ENTITY_CHICKEN_STEP, 1.0f, 0.5f);
            spawnFloatingText(attLoc.clone().add(0, 0.6, 0), "§c💫 ¡TROPIEZO NERVIOSO!");
            return;
        }

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.ASUSTADIZO) && rng.nextDouble() < 0.15) {
            Vector panic = attLoc.toVector().subtract(defLoc.toVector()).normalize().multiply(0.35);
            panic.setY(0.15);
            dispatchEntityCommand(attacker, PhysicalCommandType.SET_VELOCITY, panic, null, 0.0, 0, null);
            attacker.getWorld().spawnParticle(Particle.CLOUD, attLoc.clone().add(0, 0.4, 0), 6, 0.15, 0.15, 0.15, 0.05);
            attacker.getWorld().playSound(attLoc, Sound.ENTITY_CHICKEN_AMBIENT, 1.2f, 1.8f);
            spawnFloatingText(attLoc.clone().add(0, 0.6, 0), "§e🐔💨 ¡PÁNICO!");
            return;
        }

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.MIOPIA_CONGENITA) && rng.nextDouble() < 0.15) {
            attacker.getWorld().playSound(attLoc, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.8f, 1.4f);
            attacker.getWorld().spawnParticle(Particle.SMOKE, attLoc.clone().add(0, 0.4, 0), 6, 0.1, 0.1, 0.1, 0.02);
            spawnFloatingText(attLoc.clone().add(0, 0.6, 0), "§7👓 ¡PICOTAZO AL AIRE!");
            return;
        }

        // Embestida física
        Vector leap = defLoc.toVector().subtract(attLoc.toVector()).normalize().multiply(0.30);
        leap.setY(0.12);
        dispatchEntityCommand(attacker, PhysicalCommandType.SET_VELOCITY, leap, null, 0.0, 0, null);
        attacker.getWorld().playSound(attLoc, Sound.ENTITY_CHICKEN_STEP, 1.2f, 1.9f);

        // Evasión
        double defEvasion = sDefender.getEvasion();
        if (rDefender.hasInjury(RoosterInjury.ALA_LASTIMADA)) {
            defEvasion *= 0.50;
        }
        if (rng.nextDouble() < defEvasion) {
            defender.getWorld().spawnParticle(Particle.SMOKE, defLoc.clone().add(0, 0.4, 0), 8, 0.15, 0.15, 0.15, 0.02);
            defender.getWorld().playSound(defLoc, Sound.ENTITY_PHANTOM_SWOOP, 0.8f, 1.6f);
            spawnFloatingText(defLoc.clone().add(0, 0.6, 0), "§7§o¡ESQUIVA! 💨");

            if (rDefender.getGenetics().hasTrait(RoosterTrait.REFLEJOS_DE_RAYO) && rng.nextDouble() < 0.50) {
                double counterDmg = Math.max(1.5, sDefender.getAttack() * 0.45);
                if (isAttackerA) {
                    currentHpA = Math.max(0.0, currentHpA - counterDmg);
                } else {
                    currentHpB = Math.max(0.0, currentHpB - counterDmg);
                }
                attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, attLoc.clone().add(0, 0.4, 0), 12, 0.2, 0.2, 0.2, 0.05);
                attacker.getWorld().playSound(attLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);
                spawnFloatingText(attLoc.clone().add(0, 0.7, 0), "§b⚡ ¡CONTRAATAQUE! -" + String.format("%.1f", counterDmg));
                if (currentHpA <= 0 || currentHpB <= 0) {
                    declareWinner();
                }
            }
            return;
        }

        // Daño base
        double rawAttack = sAttacker.getAttack();
        if (rDefender.getGenetics().hasTrait(RoosterTrait.INTIMIDACION)) {
            rawAttack *= 0.85;
        }

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.FURIA_PRIMAL)) {
            double hpPercent = isAttackerA ? (currentHpA / sAttacker.getMaxHp()) : (currentHpB / sAttacker.getMaxHp());
            if (hpPercent < 0.35) {
                rawAttack *= 1.30;
                attacker.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, attLoc.clone().add(0, 0.5, 0), 3, 0.1, 0.1, 0.1, 0.02);
            }
        }

        if (rAttacker.getHeldItem() == RoosterHeldItem.VIDASFERA) {
            rawAttack *= 1.30;
            attacker.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, attLoc.clone().add(0, 0.3, 0), 5, 0.1, 0.1, 0.1, 0.03);
        }

        if (rAttacker.hasInjury(RoosterInjury.ESPOLON_AGRIETADO)) {
            rawAttack *= 0.80;
        }

        double def = sDefender.getDefense();
        if (rAttacker.getGenetics().hasTrait(RoosterTrait.GARRAS_AFILADAS)) {
            def *= 0.75;
        }

        double damage = Math.max(1.5, rawAttack * (50.0 / (50.0 + def)));
        double critChance = rAttacker.hasInjury(RoosterInjury.ESPOLON_AGRIETADO) ? 0.05 : 0.20;
        double critMultiplier = 1.50;

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.SUERTE_DIVINA)) critChance += 0.15;
        if (rAttacker.getHeldItem() == RoosterHeldItem.PERISCOPIO) {
            critChance += 0.25;
            critMultiplier = 1.80;
        }

        boolean isCrit = rng.nextDouble() < critChance;
        if (isCrit) {
            damage *= critMultiplier;
            if (rDefender.getGenetics().hasTrait(RoosterTrait.HUESOS_FRAGILES)) damage *= 1.20;
            if (rDefender.getGenetics().hasTrait(RoosterTrait.HEMOFILIA)) {
                damage *= 1.25;
                defender.getWorld().spawnParticle(Particle.DUST, defLoc.clone().add(0, 0.4, 0), 12, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                spawnFloatingText(defLoc.clone().add(0, 0.8, 0), "§4🩸 ¡SANGRADO GRAVE!");
            }
            if (rAttacker.getGenetics().hasTrait(RoosterTrait.GOLPE_DEMOLEDOR) && rng.nextDouble() < 0.30) {
                if (isAttackerA) cooldownTicksB += 24; else cooldownTicksA += 24;
                defender.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, defLoc.clone().add(0, 0.6, 0), 8, 0.1, 0.2, 0.1, 0.02);
                defender.getWorld().playSound(defLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 1.8f);
                spawnFloatingText(defLoc.clone().add(0, 0.9, 0), "§e💫 ¡ATURDIDO! 1.2s");
            }
        }

        if (rDefender.getGenetics().hasTrait(RoosterTrait.PIEL_DE_HIERRO)) damage *= 0.85;
        if (rDefender.getGenetics().hasTrait(RoosterTrait.PIEL_DE_PAPEL)) damage *= 1.15;
        if (rDefender.hasInjury(RoosterInjury.HERIDA_ABIERTA)) {
            damage *= 1.15;
            defender.getWorld().spawnParticle(Particle.DUST, defLoc.clone().add(0, 0.4, 0), 6, 0.15, 0.15, 0.15, new Particle.DustOptions(Color.fromRGB(160, 0, 0), 1.2f));
        }

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.ESPOLON_VENENOSO) && rng.nextDouble() < 0.25) {
            PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 60, 0);
            dispatchEntityCommand(defender, PhysicalCommandType.APPLY_POTION, null, null, 0.0, 0, poison);
            defender.getWorld().spawnParticle(Particle.ENTITY_EFFECT, defLoc.clone().add(0, 0.4, 0), 8, 0.1, 0.1, 0.1, Color.fromRGB(40, 180, 40));
        }

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.PICOTAZO_IGNIS) && rng.nextDouble() < 0.20) {
            dispatchEntityCommand(defender, PhysicalCommandType.SET_FIRE, null, null, 0.0, 60, null);
            defender.getWorld().spawnParticle(Particle.FLAME, defLoc.clone().add(0, 0.4, 0), 10, 0.15, 0.15, 0.15, 0.05);
            defender.getWorld().playSound(defLoc, Sound.ITEM_FLINTANDSTEEL_USE, 0.8f, 1.2f);
            spawnFloatingText(defLoc.clone().add(0, 0.7, 0), "§6🔥 ¡LLAMAS!");
        }

        if (rDefender.getGenetics().hasTrait(RoosterTrait.ARMADURA_ESPINA)) {
            double spineDmg = damage * 0.12;
            if (isAttackerA) currentHpA = Math.max(0.0, currentHpA - spineDmg);
            else currentHpB = Math.max(0.0, currentHpB - spineDmg);
            attacker.getWorld().spawnParticle(Particle.CRIT, attLoc.clone().add(0, 0.4, 0), 6, 0.1, 0.1, 0.1, 0.05);
            spawnFloatingText(attLoc.clone().add(0, 0.7, 0), "§7-" + String.format("%.1f", spineDmg) + " 🌵");
        }

        if (rAttacker.getGenetics().hasTrait(RoosterTrait.VAMPIRO_ALADO)) {
            double lifesteal = damage * 0.15;
            double maxAtkHp = sAttacker.getMaxHp();
            if (isAttackerA) currentHpA = Math.min(maxAtkHp, currentHpA + lifesteal);
            else currentHpB = Math.min(maxAtkHp, currentHpB + lifesteal);
            attacker.getWorld().spawnParticle(Particle.HEART, attLoc.clone().add(0, 0.5, 0), 3, 0.15, 0.15, 0.15, 0.02);
            spawnFloatingText(attLoc.clone().add(0, 0.8, 0), "§c+" + String.format("%.1f", lifesteal) + " HP 🦇");
        }

        if (rAttacker.getHeldItem() == RoosterHeldItem.VIDASFERA) {
            double recoil = (isAttackerA ? currentHpA : currentHpB) * 0.05;
            if (isAttackerA) currentHpA = Math.max(1.0, currentHpA - recoil);
            else currentHpB = Math.max(1.0, currentHpB - recoil);
        }

        if (rDefender.getHeldItem() == RoosterHeldItem.CASCO_DENTADO) {
            double thorns = damage * 0.20;
            if (isAttackerA) currentHpA = Math.max(0.0, currentHpA - thorns);
            else currentHpB = Math.max(0.0, currentHpB - thorns);
            attacker.getWorld().playSound(attLoc, Sound.ITEM_SHIELD_BREAK, 0.8f, 1.4f);
            attacker.getWorld().spawnParticle(Particle.CRIT, attLoc.clone().add(0, 0.4, 0), 8, 0.1, 0.1, 0.1, 0.05);
            spawnFloatingText(attLoc.clone().add(0, 0.6, 0), "§6-" + String.format("%.1f", thorns) + " 🛡");
        }

        double defHp = isAttackerA ? currentHpB : currentHpA;
        double defMaxHp = sDefender.getMaxHp();
        boolean focusUsed = isAttackerA ? focusUsedB : focusUsedA;
        if (!focusUsed && rDefender.getHeldItem() == RoosterHeldItem.BANDA_FOCUS && defHp >= (defMaxHp - 0.5) && damage >= defHp) {
            damage = defHp - 1.0;
            if (isAttackerA) focusUsedB = true; else focusUsedA = true;
            defender.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, defLoc.clone().add(0, 0.5, 0), 30, 0.3, 0.4, 0.3, 0.1);
            defender.getWorld().playSound(defLoc, Sound.ITEM_TOTEM_USE, 0.7f, 1.6f);
            spawnFloatingText(defLoc.clone().add(0, 0.9, 0), "§e§l¡BANDA FOCUS! 🛡");
        }

        // Aplicar daño lógico
        if (isAttackerA) {
            currentHpB = Math.max(0.0, currentHpB - damage);
        } else {
            currentHpA = Math.max(0.0, currentHpA - damage);
        }

        double newDefHp = isAttackerA ? currentHpB : currentHpA;
        boolean sitrusUsed = isAttackerA ? sitrusUsedB : sitrusUsedA;
        if (!sitrusUsed && rDefender.getHeldItem() == RoosterHeldItem.BAYA_ZIDRA && newDefHp > 0 && newDefHp <= (defMaxHp * 0.50)) {
            if (isAttackerA) sitrusUsedB = true; else sitrusUsedA = true;
            double heal = defMaxHp * 0.25;
            if (isAttackerA) currentHpB = Math.min(defMaxHp, currentHpB + heal);
            else currentHpA = Math.min(defMaxHp, currentHpA + heal);
            defender.getWorld().spawnParticle(Particle.HEART, defLoc.clone().add(0, 0.6, 0), 8, 0.2, 0.2, 0.2, 0.05);
            defender.getWorld().playSound(defLoc, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.2f);
            spawnFloatingText(defLoc.clone().add(0, 0.9, 0), "§e+" + String.format("%.1f", heal) + " HP §c🍒");
        }

        Location hitLoc = defLoc.clone().add(0, 0.35, 0);
        defender.getWorld().spawnParticle(Particle.CRIT, hitLoc, isCrit ? 20 : 8, 0.25, 0.25, 0.25, 0.12);
        defender.getWorld().spawnParticle(Particle.SWEEP_ATTACK, hitLoc, 1);
        defender.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 5, 0.15, 0.15, 0.15, 0.05);

        defender.getWorld().playSound(hitLoc, Sound.ENTITY_CHICKEN_HURT, 1.0f, isCrit ? 0.7f : 1.1f);
        if (isCrit) {
            defender.getWorld().playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.1f);
            defender.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
            defender.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 1);
            spawnFloatingText(hitLoc.clone().add(0, 0.5, 0), "§c§l💥 -" + String.format("%.1f", damage) + " CRÍT!");
        } else {
            defender.getWorld().playSound(hitLoc, Sound.ITEM_FLINTANDSTEEL_USE, 0.8f, 1.6f);
            spawnFloatingText(hitLoc.clone().add(0, 0.5, 0), "§c-" + String.format("%.1f", damage));
        }

        // Retroceso físico despachado al scheduler de la entidad
        Vector knockback = defLoc.toVector().subtract(attLoc.toVector()).normalize().multiply(isCrit ? 0.28 : 0.18);
        knockback.setY(0.08);
        dispatchEntityCommand(defender, PhysicalCommandType.SET_VELOCITY, knockback, null, 0.0, 0, null);

        // Verificación de Revivir por Fénix
        if (currentHpA <= 0 && !fenixUsedA && roosterA.getGenetics().hasTrait(RoosterTrait.SANGRE_DE_FENIX)) {
            fenixUsedA = true;
            currentHpA = statsA.getMaxHp() * 0.30;
            entityA.getWorld().spawnParticle(Particle.FIREWORK, entityA.getLocation().add(0, 0.5, 0), 25, 0.3, 0.3, 0.3, 0.1);
            entityA.getWorld().playSound(entityA.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.5f);
            spawnFloatingText(entityA.getLocation().add(0, 0.8, 0), "§6§l¡RESURRECCIÓN FÉNIX! 🦅");
        }
        if (currentHpB <= 0 && !fenixUsedB && roosterB.getGenetics().hasTrait(RoosterTrait.SANGRE_DE_FENIX)) {
            fenixUsedB = true;
            currentHpB = statsB.getMaxHp() * 0.30;
            entityB.getWorld().spawnParticle(Particle.FIREWORK, entityB.getLocation().add(0, 0.5, 0), 25, 0.3, 0.3, 0.3, 0.1);
            entityB.getWorld().playSound(entityB.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.5f);
            spawnFloatingText(entityB.getLocation().add(0, 0.8, 0), "§6§l¡RESURRECCIÓN FÉNIX! 🦅");
        }

        if (currentHpA <= 0 || currentHpB <= 0) {
            declareWinner();
        }
    }

    private void spawnFloatingText(Location loc, String text) {
        try {
            loc.getWorld().spawn(loc, TextDisplay.class, td -> {
                td.setBillboard(Billboard.CENTER);
                td.setPersistent(false);
                td.text(Component.text(text));
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                td.getScheduler().runDelayed(plugin, t -> {
                    if (td.isValid()) td.remove();
                }, null, 14L);
            });
        } catch (Throwable ignored) {}
    }

    private void updateNametag(Chicken c, String cornerPrefix, String name, double hp, double maxHp, RoosterHeldItem item) {
        if (c == null || !c.isValid()) return;
        double ratio = Math.max(0.0, Math.min(1.0, hp / maxHp));
        int totalBars = 10;
        int filledBars = (int) Math.round(ratio * totalBars);

        String barColor = ratio > 0.55 ? "§a" : (ratio > 0.25 ? "§e" : "§c");
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append(barColor).append("█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");

        String itemTag = (item != null) ? " §6[" + item.getDisplayName() + "]" : "";
        int percent = (int) (ratio * 100);
        String finalName = cornerPrefix + "§f" + name + " " + bar + " " + barColor + percent + "% §8(" + (int) Math.ceil(hp) + " HP)" + itemTag;

        dispatchEntityCommand(c, PhysicalCommandType.UPDATE_NAMETAG, null, finalName, 0.0, 0, null);
    }

    private void updateCombatBossBars() {
        if (bossBarRed == null || bossBarBlue == null) return;

        float ratioA = (float) Math.max(0.0, Math.min(1.0, currentHpA / statsA.getMaxHp()));
        float ratioB = (float) Math.max(0.0, Math.min(1.0, currentHpB / statsB.getMaxHp()));

        bossBarRed.progress(ratioA);
        bossBarRed.name(Component.text("🔴 " + roosterA.getName() + " (" + challengerName + ") - " + (int) Math.ceil(currentHpA) + "/" + (int) statsA.getMaxHp() + " HP", NamedTextColor.RED, TextDecoration.BOLD));

        bossBarBlue.progress(ratioB);
        bossBarBlue.name(Component.text("🔵 " + roosterB.getName() + " (" + challengedName + ") - " + (int) Math.ceil(currentHpB) + "/" + (int) statsB.getMaxHp() + " HP", NamedTextColor.BLUE, TextDecoration.BOLD));

        Set<UUID> currentNearby = new HashSet<>();
        for (Player p : arenaCenter.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arenaCenter) <= 900) {
                UUID pid = p.getUniqueId();
                currentNearby.add(pid);
                if (bossBarViewers.add(pid)) {
                    dispatchPlayerAction(pid, player -> {
                        player.showBossBar(bossBarRed);
                        player.showBossBar(bossBarBlue);
                    });
                }
            }
        }

        for (Iterator<UUID> it = bossBarViewers.iterator(); it.hasNext();) {
            UUID viewerId = it.next();
            if (!currentNearby.contains(viewerId)) {
                dispatchPlayerAction(viewerId, player -> {
                    player.hideBossBar(bossBarRed);
                    player.hideBossBar(bossBarBlue);
                });
                it.remove();
            }
        }
    }

    // =========================================================================
    // LIQUIDACIÓN DE COMBATE (Exactly-Once Settlement & State Machine Transitions)
    // =========================================================================

    private void declareWinner() {
        this.state = State.ENDING;
        executionLease.nextEpoch(); // Incremento atómico inmediato para descartar comandos stale
        if (task != null) task.cancel();

        if (!settlementGuarded.compareAndSet(false, true)) {
            return; // Garantía estricta de una única liquidación
        }

        boolean aWon = currentHpA > 0;
        UUID winnerPlayerId = aWon ? challengerId : challengedId;
        UUID loserPlayerId = aWon ? challengedId : challengerId;
        String winnerName = aWon ? challengerName : challengedName;
        String loserName = aWon ? challengedName : challengerName;
        RoosterData winnerRooster = aWon ? roosterA : roosterB;
        RoosterData loserRooster = aWon ? roosterB : roosterA;

        winnerRooster.addWin();
        loserRooster.addLoss();

        int winnerXp = 80 + (loserRooster.getLevel() * 40);
        int loserXp = 30 + (winnerRooster.getLevel() * 10);
        int winnerLevelsGained = winnerRooster.addExperience(winnerXp);
        int loserLevelsGained = loserRooster.addExperience(loserXp);

        if (nuzlockeEnabled) {
            loserRooster.setHealthStatus(RoosterHealthStatus.DEAD);
        } else {
            loserRooster.knockOut(knockoutDurationMillis);

            if (rng.nextDouble() < 0.65) {
                List<RoosterInjury> availableInjuries = new ArrayList<>();
                for (RoosterInjury inj : RoosterInjury.values()) {
                    if (!loserRooster.hasInjury(inj)) {
                        availableInjuries.add(inj);
                    }
                }
                if (!availableInjuries.isEmpty()) {
                    RoosterInjury injuryAcquired = availableInjuries.get(rng.nextInt(availableInjuries.size()));
                    loserRooster.addInjury(injuryAcquired);
                    dispatchPlayerAction(loserPlayerId, loserPlayer -> {
                        loserPlayer.sendMessage(Component.empty()
                                .append(Component.text("\n⚠ [VETERINARIA] ", NamedTextColor.RED, TextDecoration.BOLD))
                                .append(Component.text(loserRooster.getName(), NamedTextColor.GOLD))
                                .append(Component.text(" ha sufrido una lesión en combate: ", NamedTextColor.RED))
                                .append(Component.text(injuryAcquired.getDisplayName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                                .append(Component.text("\n➥ Tratamiento sugerido: ", NamedTextColor.GRAY))
                                .append(Component.text(injuryAcquired.getCureHint(), NamedTextColor.WHITE))
                                .append(Component.text("\n")));
                        loserPlayer.playSound(loserPlayer.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f);
                    });
                }
            }

            if (loserRooster.getConsecutiveLosses() >= 3 && rng.nextDouble() < 0.35) {
                RoosterTrait[] negativeTraits = {
                        RoosterTrait.PICO_ROTO, RoosterTrait.COJERA, RoosterTrait.HUESOS_FRAGILES,
                        RoosterTrait.HEMOFILIA, RoosterTrait.PIEL_DE_PAPEL, RoosterTrait.ASUSTADIZO,
                        RoosterTrait.ESPURIO_NERVIOSO, RoosterTrait.MIOPIA_CONGENITA, RoosterTrait.ASMA_DE_CORRAL,
                        RoosterTrait.ESTERILIDAD
                };
                List<RoosterTrait> possibleDefects = new ArrayList<>();
                for (RoosterTrait t : negativeTraits) {
                    if (!loserRooster.getGenetics().hasTrait(t)) possibleDefects.add(t);
                }
                if (!possibleDefects.isEmpty()) {
                    RoosterTrait defectAcquired = possibleDefects.get(rng.nextInt(possibleDefects.size()));
                    loserRooster.getGenetics().addTrait(defectAcquired);
                    dispatchPlayerAction(loserPlayerId, loserPlayer -> {
                        loserPlayer.sendMessage(Component.empty()
                                .append(Component.text("\n☠ [SECUELA GRAVE] ", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                                .append(Component.text(loserRooster.getName(), NamedTextColor.GOLD))
                                .append(Component.text(" ha acumulado " + loserRooster.getConsecutiveLosses() + " derrotas seguidas y ha quedado marcado con un defecto permanente: ", NamedTextColor.RED))
                                .append(Component.text(defectAcquired.getDisplayName(), NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                                .append(Component.text("\n➥ Solo el legendario ", NamedTextColor.GRAY))
                                .append(Component.text("Bálsamo Supremo de la Gran Liga", NamedTextColor.GOLD, TextDecoration.BOLD))
                                .append(Component.text(" puede operar y sanar esta secuela.\n", NamedTextColor.GRAY)));
                        loserPlayer.playSound(loserPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.8f);
                    });
                }
            }
        }

        repository.saveRooster(winnerRooster);
        repository.saveRooster(loserRooster);

        Player winnerPlayer = winnerPlayerId != null ? Bukkit.getPlayer(winnerPlayerId) : null;
        Player loserPlayer = loserPlayerId != null ? Bukkit.getPlayer(loserPlayerId) : null;

        RoosterDuelVictoryEvent event = new RoosterDuelVictoryEvent(this, winnerPlayer, winnerRooster, loserPlayer, loserRooster);
        plugin.getServer().getPluginManager().callEvent(event);

        dispatchPlayerAction(winnerPlayerId, player -> {
            if (winnerLevelsGained > 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.2, 0), 40, 0.5, 0.8, 0.5, 0.2);
                player.showTitle(Title.title(
                        Component.text("⭐ ¡SUBIDA DE NIVEL! ⭐", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text(winnerRooster.getName() + " ahora es Nivel " + winnerRooster.getLevel() + "!", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600))
                ));
                player.sendMessage(Component.empty()
                        .append(Component.text("\n⭐ ¡" + winnerRooster.getName() + " ha alcanzado el Nivel " + winnerRooster.getLevel() + "! ⭐\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text("✦ Salud Máxima: ", NamedTextColor.GRAY)).append(Component.text("+" + String.format("%.1f", winnerLevelsGained * 3.5) + " HP\n", NamedTextColor.GREEN))
                        .append(Component.text("✦ Fuerza de Picotazo: ", NamedTextColor.GRAY)).append(Component.text("+" + String.format("%.2f", winnerLevelsGained * 0.65) + " ATK\n", NamedTextColor.RED))
                        .append(Component.text("✦ Coraza Defensiva: ", NamedTextColor.GRAY)).append(Component.text("+" + String.format("%.2f", winnerLevelsGained * 0.40) + " DEF\n", NamedTextColor.BLUE)));
            } else {
                player.sendMessage(Component.text("✦ Experiencia de combate: ", NamedTextColor.GRAY)
                        .append(Component.text("+" + winnerXp + " XP ", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text("[" + winnerRooster.getExperience() + "/" + winnerRooster.getXpForNextLevel() + " XP]", NamedTextColor.DARK_GRAY)));
            }
        });

        dispatchPlayerAction(loserPlayerId, player -> {
            if (loserLevelsGained > 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
                player.sendMessage(Component.empty()
                        .append(Component.text("\n⭐ ¡A pesar de la derrota, " + loserRooster.getName() + " aprendió del combate y subió al Nivel " + loserRooster.getLevel() + "! ⭐\n", NamedTextColor.GOLD, TextDecoration.BOLD)));
            } else {
                player.sendMessage(Component.text("✦ Experiencia por combate: ", NamedTextColor.GRAY)
                        .append(Component.text("+" + loserXp + " XP ", NamedTextColor.YELLOW))
                        .append(Component.text("[" + loserRooster.getExperience() + "/" + loserRooster.getXpForNextLevel() + " XP]", NamedTextColor.DARK_GRAY)));
            }
        });

        if (bettingHook != null) {
            try {
                bettingHook.onDuelVictory(this, winnerPlayer, winnerRooster, loserPlayer, loserRooster);
            } catch (Exception e) {
                plugin.getLogger().warning("Error al procesar apuestas con el hook externo: " + e.getMessage());
            }
        }

        Component winMsg = Component.empty()
                .append(Component.text("\n🏆 [PALENQUE] ¡Combate Finalizado! 🏆\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" ¡El gallo ", NamedTextColor.WHITE))
                .append(Component.text(winnerRooster.getName(), NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" de ", NamedTextColor.WHITE))
                .append(Component.text(winnerName, NamedTextColor.GOLD))
                .append(Component.text(" ha ganado por K.O.!\n", NamedTextColor.YELLOW));

        for (Player p : arenaCenter.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arenaCenter) <= 900) {
                dispatchPlayerAction(p.getUniqueId(), player -> {
                    player.sendMessage(winMsg);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                });
            }
        }

        if (onDuelEndCallback != null) {
            boolean challengerWon = (winnerRooster == roosterA);
            try {
                onDuelEndCallback.accept(challengerWon);
            } catch (Exception ignored) {}
        }

        this.state = State.FINISHED;
        plugin.getServer().getRegionScheduler().runDelayed(plugin, arenaCenter, t -> cleanupEntities(), 100L);
    }

    private void endFightInDraw(String reason) {
        this.state = State.ENDING;
        executionLease.nextEpoch();
        if (task != null) task.cancel();

        if (!settlementGuarded.compareAndSet(false, true)) {
            return;
        }

        if (onDuelEndCallback != null) {
            try {
                onDuelEndCallback.accept(false);
            } catch (Exception ignored) {}
        }

        roosterA.addDraw();
        roosterB.addDraw();

        int drawXpA = 50 + (roosterB.getLevel() * 15);
        int drawXpB = 50 + (roosterA.getLevel() * 15);
        int lvlA = roosterA.addExperience(drawXpA);
        int lvlB = roosterB.addExperience(drawXpB);

        repository.saveRooster(roosterA);
        repository.saveRooster(roosterB);

        dispatchPlayerAction(challengerId, player -> {
            if (lvlA > 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
                player.sendMessage(Component.text("⭐ ¡" + roosterA.getName() + " ha subido al Nivel " + roosterA.getLevel() + "! ⭐", NamedTextColor.GOLD, TextDecoration.BOLD));
            } else {
                player.sendMessage(Component.text("✦ Experiencia por combate: +" + drawXpA + " XP [" + roosterA.getExperience() + "/" + roosterA.getXpForNextLevel() + " XP]", NamedTextColor.GRAY));
            }
        });

        dispatchPlayerAction(challengedId, player -> {
            if (lvlB > 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
                player.sendMessage(Component.text("⭐ ¡" + roosterB.getName() + " ha subido al Nivel " + roosterB.getLevel() + "! ⭐", NamedTextColor.GOLD, TextDecoration.BOLD));
            } else {
                player.sendMessage(Component.text("✦ Experiencia por combate: +" + drawXpB + " XP [" + roosterB.getExperience() + "/" + roosterB.getXpForNextLevel() + " XP]", NamedTextColor.GRAY));
            }
        });

        RoosterDuelDrawEvent event = new RoosterDuelDrawEvent(this);
        plugin.getServer().getPluginManager().callEvent(event);

        if (bettingHook != null) {
            try {
                bettingHook.onDuelDraw(this);
            } catch (Exception ignored) {}
        }

        broadcastMessageToNearby(Component.text("⚖ [PALENQUE] " + reason, NamedTextColor.YELLOW));

        this.state = State.FINISHED;
        cleanupEntities();
    }

    public void abort(String reason) {
        if (!plugin.isEnabled()) {
            executeAbort(reason);
            return;
        }
        enqueueMessage(new CombatMessage.AdminAbort(reason));
    }

    private void executeAbort(String reason) {
        this.state = State.CANCELLED;
        executionLease.nextEpoch();
        if (task != null) task.cancel();

        if (!settlementGuarded.compareAndSet(false, true)) {
            return;
        }

        RoosterDuelAbortEvent event = new RoosterDuelAbortEvent(this, reason);
        plugin.getServer().getPluginManager().callEvent(event);

        if (bettingHook != null) {
            try {
                bettingHook.onDuelAbort(this, reason);
            } catch (Exception ignored) {}
        }

        cleanupEntities();
    }

    private void cleanupEntities() {
        if (entityA != null) {
            dispatchEntityCommand(entityA, PhysicalCommandType.REMOVE_ENTITY, null, null, 0.0, 0, null);
        }
        if (entityB != null) {
            dispatchEntityCommand(entityB, PhysicalCommandType.REMOVE_ENTITY, null, null, 0.0, 0, null);
        }
        if (bossBarRed != null && bossBarBlue != null) {
            for (UUID viewerId : bossBarViewers) {
                dispatchPlayerAction(viewerId, player -> {
                    player.hideBossBar(bossBarRed);
                    player.hideBossBar(bossBarBlue);
                });
            }
            bossBarViewers.clear();
        }
    }

    public void handlePlayerForfeit(UUID forfeitedPlayerId, String reason) {
        enqueueMessage(new CombatMessage.PlayerQuit(forfeitedPlayerId, reason));
    }

    public boolean addSpectatorBet(SpectatorBet bet) {
        if (!state.canAcceptBets()) return false;
        enqueueMessage(new CombatMessage.SpectatorBetMsg(bet));
        return true;
    }

    public State getState() { return state; }
    public UUID getChallengerId() { return challengerId; }
    public UUID getChallengedId() { return challengedId; }
    public String getChallengerName() { return challengerName; }
    public String getChallengedName() { return challengedName; }
    public RoosterData getRoosterA() { return roosterA; }
    public RoosterData getRoosterB() { return roosterB; }
    public double getWagerAmount() { return wagerAmount; }
    public Location getArenaCenter() { return arenaCenter; }
    public List<SpectatorBet> getSpectatorBets() { return spectatorBets; }
}
