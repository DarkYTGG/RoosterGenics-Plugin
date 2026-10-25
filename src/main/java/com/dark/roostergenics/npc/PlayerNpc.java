package com.dark.roostergenics.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Representa un Fake Player NPC nativo por paquetes NMS para Paper 1.21.11.
 * Muestra el cuerpo humano 100% completo con la textura de la skin del jugador
 * (brazos, torso, piernas y capas 3D completas) sin usar ArmorStands ni plugins externos.
 */
public class PlayerNpc {

    public static final List<String> RANDOM_SKINS = List.of(
            "ElRichMC", "iDarkt", "cosorro", "Xero315", "Misenba", "PePizzaOP",
            "RebelAndrew", "iSheero", "PachamancaUwu", "comertetass",
            "Technoblade", "FitMC", "DanTDM", "Etho", "Grian", "MumboJumbo",
            "BdoubleO100", "GoodTimesWithScar", "knekro", "Luzu", "Vegetta777",
            "Willyrex", "AuronPlay", "IlloJuan", "Rubius", "Spreen", "Quackity"
    );

    private static final Map<String, Property> SKIN_CACHE = new ConcurrentHashMap<>();

    private final UUID npcId;
    private final String skinName;
    private Location currentLoc;
    private ServerPlayer fakePlayer;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    public PlayerNpc(UUID npcId, String skinName, Location location) {
        this.npcId = npcId != null ? npcId : UUID.randomUUID();
        this.skinName = resolveSkinName(skinName);
        this.currentLoc = location.clone();
    }

    public static String getRandomSkin() {
        return RANDOM_SKINS.get(ThreadLocalRandom.current().nextInt(RANDOM_SKINS.size()));
    }

    public static String resolveSkinName(String input) {
        if (input == null || input.isBlank()
                || input.equalsIgnoreCase("random")
                || input.equalsIgnoreCase("aleatorio")
                || input.equalsIgnoreCase("aleatoria")) {
            return getRandomSkin();
        }
        return input.trim();
    }

    public UUID getNpcId() {
        return npcId;
    }

    public String getSkinName() {
        return skinName;
    }

    public Location getLocation() {
        return currentLoc.clone();
    }

    public int getEntityId() {
        return fakePlayer != null ? fakePlayer.getId() : -1;
    }

    public ServerPlayer getFakePlayer() {
        return fakePlayer;
    }

    public boolean isSpawned() {
        return fakePlayer != null;
    }

    public void spawn() {
        World world = currentLoc.getWorld();
        if (world == null) return;

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((CraftWorld) world).getHandle();

        // Obtener o descargar la skin completa de Mojang
        Property textureProp = getSkinProperty(skinName);
        GameProfile gameProfile = createGameProfile(UUID.randomUUID(), skinName, textureProp);

        // ClientInformation con modelCustomisation = 127 (activa todas las capas 3D de la skin)
        ClientInformation clientInfo = new ClientInformation(
                "es_ES",
                10,
                ChatVisiblity.FULL,
                true,
                127,
                HumanoidArm.RIGHT,
                false,
                false,
                ParticleStatus.ALL
        );

        fakePlayer = new ServerPlayer(server, level, gameProfile, clientInfo);
        fakePlayer.setPos(currentLoc.getX(), currentLoc.getY(), currentLoc.getZ());
        fakePlayer.setRot(currentLoc.getYaw(), currentLoc.getPitch());
        fakePlayer.setYHeadRot(currentLoc.getYaw());

        // Mostrar a todos los jugadores cercanos en el mundo
        updateViewers();
    }

    public void showTo(Player player) {
        if (fakePlayer == null || player == null || !player.isOnline()) return;
        if (!player.getWorld().equals(currentLoc.getWorld())) return;
        if (player.getLocation().distanceSquared(currentLoc) > 64.0 * 64.0) return;
        if (!viewers.add(player.getUniqueId())) return;

        // Ocultar nametag vanilla del jugador y objetivos de scoreboard
        ensureNametagHidden(player, skinName);

        ServerPlayer sp = ((CraftPlayer) player).getHandle();

        // 1. Paquete de información del jugador (construido directamente con listed = false para no saturar el Tab)
        ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                fakePlayer.getUUID(),
                fakePlayer.getGameProfile(),
                false,
                0,
                net.minecraft.world.level.GameType.SURVIVAL,
                null,
                true,
                0,
                null
        );
        ClientboundPlayerInfoUpdatePacket infoPacket = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT
                ),
                entry
        );
        sp.connection.send(infoPacket);

        // 2. Paquete de adición de entidad con modelo 3D humano
        ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(
                fakePlayer.getId(),
                fakePlayer.getUUID(),
                currentLoc.getX(),
                currentLoc.getY(),
                currentLoc.getZ(),
                currentLoc.getPitch(),
                currentLoc.getYaw(),
                EntityType.PLAYER,
                0,
                Vec3.ZERO,
                currentLoc.getYaw()
        );
        sp.connection.send(addPacket);

        // 3. Rotación de cabeza exacta
        ClientboundRotateHeadPacket headPacket = new ClientboundRotateHeadPacket(
                fakePlayer,
                (byte) ((currentLoc.getYaw() * 256.0F) / 360.0F)
        );
        sp.connection.send(headPacket);

        // 4. Metadatos de la entidad (capas de la skin 3D)
        ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(
                fakePlayer.getId(),
                fakePlayer.getEntityData().packAll()
        );
        sp.connection.send(dataPacket);
    }

    public void hideFrom(Player player) {
        if (fakePlayer == null || player == null) return;
        if (!viewers.remove(player.getUniqueId())) return;
        if (!player.isOnline()) return;

        ServerPlayer sp = ((CraftPlayer) player).getHandle();
        ClientboundRemoveEntitiesPacket removePacket = new ClientboundRemoveEntitiesPacket(fakePlayer.getId());
        ClientboundPlayerInfoRemovePacket infoRemovePacket = new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID()));

        sp.connection.send(removePacket);
        sp.connection.send(infoRemovePacket);
    }

    public void teleport(Location newLoc) {
        this.currentLoc = newLoc.clone();
        if (fakePlayer == null) return;

        fakePlayer.setPos(newLoc.getX(), newLoc.getY(), newLoc.getZ());
        fakePlayer.setRot(newLoc.getYaw(), newLoc.getPitch());
        fakePlayer.setYHeadRot(newLoc.getYaw());

        PositionMoveRotation moveRot = new PositionMoveRotation(
                new Vec3(newLoc.getX(), newLoc.getY(), newLoc.getZ()),
                Vec3.ZERO,
                newLoc.getYaw(),
                newLoc.getPitch()
        );
        ClientboundTeleportEntityPacket tpPacket = ClientboundTeleportEntityPacket.teleport(
                fakePlayer.getId(),
                moveRot,
                Set.of(),
                false
        );
        ClientboundRotateHeadPacket headPacket = new ClientboundRotateHeadPacket(
                fakePlayer,
                (byte) ((newLoc.getYaw() * 256.0F) / 360.0F)
        );

        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                ServerPlayer sp = ((CraftPlayer) p).getHandle();
                sp.connection.send(tpPacket);
                sp.connection.send(headPacket);
            }
        }
    }

    public void updateViewers() {
        World world = currentLoc.getWorld();
        if (world == null) return;

        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(currentLoc) <= 64.0 * 64.0) {
                showTo(p);
            } else {
                hideFrom(p);
            }
        }
    }

    public void destroy() {
        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                hideFrom(p);
            }
        }
        viewers.clear();
        fakePlayer = null;
    }

    public static void preloadSkins(org.bukkit.plugin.Plugin plugin, Collection<String> names) {
        if (plugin == null || !plugin.isEnabled() || names == null || names.isEmpty()) return;
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            for (String name : names) {
                fetchAndCacheSkin(name);
            }
        });
    }

    public static Property fetchAndCacheSkin(String name) {
        if (name == null || name.isBlank()) return null;
        Property cached = SKIN_CACHE.get(name);
        if (cached != null) return cached;

        try {
            PlayerProfile profile = Bukkit.createProfile(name);
            profile.complete(true);
            for (ProfileProperty prop : profile.getProperties()) {
                if ("textures".equalsIgnoreCase(prop.getName())) {
                    Property p = new Property("textures", prop.getValue(), prop.getSignature());
                    SKIN_CACHE.put(name, p);
                    return p;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Property getSkinProperty(String name) {
        if (name == null || name.isBlank()) return null;
        Property cached = SKIN_CACHE.get(name);
        if (cached != null) {
            return cached;
        }

        // Si se ejecuta fuera del hilo principal, podemos resolver la skin directamente
        if (!Bukkit.isPrimaryThread()) {
            return fetchAndCacheSkin(name);
        }

        // En el hilo principal, NUNCA bloquear el tick con llamadas de red. Despachar al AsyncScheduler.
        try {
            org.bukkit.plugin.Plugin plugin = org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(PlayerNpc.class);
            if (plugin != null && plugin.isEnabled()) {
                plugin.getServer().getAsyncScheduler().runNow(plugin, t -> fetchAndCacheSkin(name));
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static GameProfile createGameProfile(UUID id, String name, Property textureProp) {
        try {
            // AuthLib 7+ (Paper 1.21.11 Record: GameProfile(UUID, String, PropertyMap(Multimap)))
            Multimap<String, Property> multimap = (textureProp != null)
                    ? ImmutableMultimap.of("textures", textureProp)
                    : ImmutableMultimap.of();
            try {
                Constructor<PropertyMap> ctor = PropertyMap.class.getConstructor(Multimap.class);
                PropertyMap propMap = ctor.newInstance(multimap);
                Constructor<GameProfile> gpCtor = GameProfile.class.getConstructor(UUID.class, String.class, PropertyMap.class);
                return gpCtor.newInstance(id, name, propMap);
            } catch (NoSuchMethodException ignored) {
                // AuthLib 6 fallback
                Constructor<GameProfile> gpCtor = GameProfile.class.getConstructor(UUID.class, String.class);
                GameProfile gp = gpCtor.newInstance(id, name);
                if (textureProp != null) {
                    Method m = GameProfile.class.getMethod("getProperties");
                    Object propMap = m.invoke(gp);
                    Method putMethod = propMap.getClass().getMethod("put", Object.class, Object.class);
                    putMethod.invoke(propMap, "textures", textureProp);
                }
                return gp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new GameProfile(id, name);
        }
    }

    public static final String HIDE_TEAM_NAME = "rg_hide_trainer";

    /**
     * Oculta el nametag vanilla del jugador registrando el nombre de la skin en un Scoreboard Team con NameTagVisibility.NEVER.
     */
    public static void ensureNametagHidden(Player player, String name) {
        if (player == null || name == null || name.isEmpty()) return;
        try {
            Scoreboard sb = player.getScoreboard();
            applyHideTeam(sb, name);
        } catch (Exception ignored) {}
        try {
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            if (main != null) {
                applyHideTeam(main, name);
            }
        } catch (Exception ignored) {}
    }

    public static void applyHideTeam(Scoreboard sb, String name) {
        if (sb == null || name == null || name.isEmpty()) return;
        try {
            Team team = sb.getTeam(HIDE_TEAM_NAME);
            if (team == null) {
                team = sb.registerNewTeam(HIDE_TEAM_NAME);
            }
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            if (!team.hasEntry(name)) {
                team.addEntry(name);
            }
        } catch (Exception ignored) {}
    }
}
