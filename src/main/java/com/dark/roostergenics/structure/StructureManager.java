package com.dark.roostergenics.structure;

import com.dark.roostergenics.center.RoosterCenterManager;
import com.dark.roostergenics.gym.GymLeader;
import com.dark.roostergenics.gym.GymLeaderNPC;
import com.dark.roostergenics.gym.GymManager;
import com.dark.roostergenics.npc.MerchantNPC;
import com.dark.roostergenics.util.Mm;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import com.dark.roostergenics.npc.NurseNPC;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestor de Generación Procedural de Estructuras del Ecosistema RoosterGenics:
 * - Se adapta a la topografía del terreno construyendo cimientos sólidos hacia abajo.
 * - Soporta: Gimnasios 1-6 con su líder, Centro de Gallos, Tienda de Mercader y Arenas de Coliseo.
 * - Garantiza simetría arquitectónica matemática y colocación diagonal correcta de esquinas de combate.
 */
public class StructureManager implements Listener {

    private final JavaPlugin plugin;
    private final RoosterCenterManager centerManager;
    private final GymManager gymManager;
    private final List<MerchantNPC> merchantNpcs = new CopyOnWriteArrayList<>();
    private StructureRegistry registry;
    private StructureEntityTracker tracker;

    public StructureManager(JavaPlugin plugin, RoosterCenterManager centerManager, GymManager gymManager) {
        this.plugin = plugin;
        this.centerManager = centerManager;
        this.gymManager = gymManager;
    }

    public void setRegistryAndTracker(StructureRegistry registry, StructureEntityTracker tracker) {
        this.registry = registry;
        this.tracker = tracker;
    }

    public StructureRegistry getRegistry() {
        return registry;
    }

    public StructureEntityTracker getTracker() {
        return tracker;
    }

    private record GymTheme(
            Material base,
            Material trim,
            Material ringFloor,
            Material ringAccent,
            Material pillar,
            Material wallFence,
            Material stairs,
            Material light
    ) {}

    private GymTheme getTheme(GymLeader leader) {
        return switch (leader) {
            case VIENTO -> new GymTheme(
                    Material.OAK_PLANKS, Material.OAK_LOG, Material.STRIPPED_OAK_WOOD,
                    Material.MOSS_BLOCK, Material.OAK_FENCE, Material.OAK_FENCE,
                    Material.OAK_STAIRS, Material.OCHRE_FROGLIGHT
            );
            case CANTERA -> new GymTheme(
                    Material.COBBLESTONE, Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS,
                    Material.MOSSY_STONE_BRICKS, Material.STONE_BRICK_WALL, Material.COBBLESTONE_WALL,
                    Material.STONE_BRICK_STAIRS, Material.GLOWSTONE
            );
            case PANTANO -> new GymTheme(
                    Material.MUD_BRICKS, Material.PACKED_MUD, Material.MUD_BRICK_WALL,
                    Material.MANGROVE_WOOD, Material.MUD_BRICK_WALL, Material.MANGROVE_FENCE,
                    Material.MUD_BRICK_STAIRS, Material.VERDANT_FROGLIGHT
            );
            case VOLCAN -> new GymTheme(
                    Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS,
                    Material.MAGMA_BLOCK, Material.NETHER_BRICK_WALL, Material.NETHER_BRICK_FENCE,
                    Material.NETHER_BRICK_STAIRS, Material.SHROOMLIGHT
            );
            case SOMBRAS -> new GymTheme(
                    Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_BRICKS, Material.CHISELED_DEEPSLATE,
                    Material.CRYING_OBSIDIAN, Material.DEEPSLATE_BRICK_WALL, Material.IRON_BARS,
                    Material.DEEPSLATE_BRICK_STAIRS, Material.SOUL_LANTERN
            );
            case TITAN -> new GymTheme(
                    Material.POLISHED_BLACKSTONE_BRICKS, Material.CRYING_OBSIDIAN, Material.POLISHED_BLACKSTONE,
                    Material.GOLD_BLOCK, Material.POLISHED_BLACKSTONE_WALL, Material.IRON_CHAIN,
                    Material.POLISHED_BLACKSTONE_BRICK_STAIRS, Material.GLOWSTONE
            );
        };
    }

    private static void setBlock(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }

    /**
     * Construye un Centro de Gallos oficial completo (Clínica médica temática).
     */
    public void buildRoosterCenter(Player player, Location origin) {
        World world = origin.getWorld();
        int baseY = origin.getBlockY();
        int startX = origin.getBlockX() - 4;
        int startZ = origin.getBlockZ() - 4;

        buildFoundation(world, startX, startZ, 9, 9, baseY, Material.SMOOTH_QUARTZ);

        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                world.getBlockAt(startX + x, baseY, startZ + z).setType(Material.SMOOTH_QUARTZ);
            }
        }

        // Cruz roja médica central en el suelo
        for (int i = 2; i <= 6; i++) {
            world.getBlockAt(startX + 4, baseY, startZ + i).setType(Material.RED_CONCRETE);
            world.getBlockAt(startX + i, baseY, startZ + 4).setType(Material.RED_CONCRETE);
        }

        // Pilares en las esquinas
        int[][] corners = {{0, 0}, {8, 0}, {0, 8}, {8, 8}};
        for (int[] c : corners) {
            for (int y = 1; y <= 4; y++) {
                world.getBlockAt(startX + c[0], baseY + y, startZ + c[1]).setType(Material.QUARTZ_PILLAR);
            }
        }

        // Techo de cristal rosa y blanco
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                boolean isEdge = (x == 1 || x == 7 || z == 1 || z == 7);
                world.getBlockAt(startX + x, baseY + 5, startZ + z).setType(isEdge ? Material.PINK_STAINED_GLASS : Material.WHITE_STAINED_GLASS);
            }
        }

        // Altar médico central con Bloque de Curación
        Location altarLoc = new Location(world, startX + 4, baseY + 1, startZ + 4);
        centerManager.placeHealingBlock(altarLoc);

        // Spawn de la Enfermera del Palenque (Player NPC con Skin 3D) y registro persistente
        Location nurseLoc = new Location(world, startX + 4.5, baseY + 1, startZ + 6.5, 180f, 0f);
        UUID instanceId = UUID.randomUUID();
        if (registry != null) {
            registry.registerStructure(StructureRecord.fromLocation(instanceId, StructureType.ROOSTER_CENTER, nurseLoc, ""));
        }
        centerManager.spawnNurseNpc(nurseLoc, instanceId);

        player.sendMessage(Mm.noItalic("<green>✔ ¡Centro de Gallos oficial construido con éxito!"));
        player.sendMessage(Mm.noItalic("<gray>Haz clic en el bloque o en la Enfermera para restaurar la salud de tus gallos."));
        player.playSound(origin, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * Construye un Gimnasio monumental completo con arena de combate 100% simétrica,
     * esquinas diagonales de combate y podio elevado para el Líder de Gimnasio.
     */
    public void buildGym(Player player, Location origin, GymLeader leader) {
        World world = origin.getWorld();
        int baseY = origin.getBlockY();
        int startX = origin.getBlockX() - 8;
        int startZ = origin.getBlockZ() - 8;

        GymTheme theme = getTheme(leader);

        // 1. Limpiar volumen de aire superior (17x17 x 8 de altura)
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                for (int y = 1; y <= 8; y++) {
                    setBlock(world, startX + x, baseY + y, startZ + z, Material.AIR);
                }
            }
        }

        // 2. Cimientos profundos adaptativos hacia abajo
        buildFoundation(world, startX, startZ, 17, 17, baseY, theme.base);

        // 3. Suelo de la explanada del coliseo (baseY)
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                boolean isBorder = (x <= 1 || x >= 15 || z <= 1 || z >= 15);
                Material floor = isBorder ? theme.trim : theme.base;
                setBlock(world, startX + x, baseY, startZ + z, floor);
            }
        }

        // 4. Las 4 Torres de las Esquinas (3x3 en cada esquina, altura 5)
        buildCornerTower(world, startX, startZ, 0, 0, baseY, theme);
        buildCornerTower(world, startX, startZ, 14, 0, baseY, theme);
        buildCornerTower(world, startX, startZ, 0, 14, baseY, theme);
        buildCornerTower(world, startX, startZ, 14, 14, baseY, theme);

        // 5. Columnas perimetrales y balaustradas entre torres (3..13)
        for (int i = 3; i <= 13; i++) {
            // Lado Norte (Z = 16)
            buildPerimeterSegment(world, startX + i, baseY, startZ + 16, i, theme);
            // Lado Este (X = 16)
            buildPerimeterSegment(world, startX + 16, baseY, startZ + i, i, theme);
            // Lado Oeste (X = 0)
            buildPerimeterSegment(world, startX + 0, baseY, startZ + i, i, theme);
            // Lado Sur (Z = 0) - Puerta de entrada simétrica en 7..9
            if (i < 7 || i > 9) {
                buildPerimeterSegment(world, startX + i, baseY, startZ + 0, i, theme);
            } else {
                // Arco de entrada triunfal al Sur (i = 7..9)
                setBlock(world, startX + i, baseY + 4, startZ + 0, theme.pillar);
                setBlock(world, startX + i, baseY + 5, startZ + 0, theme.trim);
            }
        }

        // 6. Ring Central de Combate 9x9 (Elevado a baseY + 1)
        // X: 4..12 (ancho 9, centro 8)
        // Z: 3..11 (largo 9, centro 7)
        for (int x = 4; x <= 12; x++) {
            for (int z = 3; z <= 11; z++) {
                setBlock(world, startX + x, baseY + 1, startZ + z, theme.ringFloor);

                // Emblema central 3x3 (X = 7..9, Z = 6..8)
                if (x >= 7 && x <= 9 && z >= 6 && z <= 8) {
                    setBlock(world, startX + x, baseY + 1, startZ + z, theme.ringAccent);
                }
            }
        }
        // Centro resplandeciente del ring (X=8, Z=7)
        setBlock(world, startX + 8, baseY + 1, startZ + 7, theme.light);

        // Esquinas diagonales opuestas de combate:
        // Esquina Azul (Desafiante): X = 5, Z = 4
        setBlock(world, startX + 5, baseY + 1, startZ + 4, Material.BLUE_CONCRETE);
        setBlock(world, startX + 5, baseY + 2, startZ + 4, Material.BLUE_CARPET);

        // Esquina Roja (Líder): X = 11, Z = 10
        setBlock(world, startX + 11, baseY + 1, startZ + 10, Material.RED_CONCRETE);
        setBlock(world, startX + 11, baseY + 2, startZ + 10, Material.RED_CARPET);

        // Escalones de acceso al ring en los 4 costados
        for (int i = 7; i <= 9; i++) {
            setBlock(world, startX + i, baseY + 1, startZ + 2, theme.stairs);
            setBlock(world, startX + i, baseY + 1, startZ + 12, theme.stairs);
        }
        for (int i = 6; i <= 8; i++) {
            setBlock(world, startX + 3, baseY + 1, startZ + i, theme.stairs);
            setBlock(world, startX + 13, baseY + 1, startZ + i, theme.stairs);
        }

        // 4 Postes del Ring en las esquinas
        int[][] ringCorners = {{4, 3}, {12, 3}, {4, 11}, {12, 11}};
        for (int[] corner : ringCorners) {
            int cx = startX + corner[0];
            int cz = startZ + corner[1];
            setBlock(world, cx, baseY + 1, cz, theme.pillar);
            setBlock(world, cx, baseY + 2, cz, theme.pillar);
            setBlock(world, cx, baseY + 3, cz, theme.pillar);
            setBlock(world, cx, baseY + 4, cz, theme.light);
        }

        // Cuerdas perimetrales del ring con aberturas centrales
        for (int x = 5; x <= 11; x++) {
            if (x != 8) {
                setBlock(world, startX + x, baseY + 2, startZ + 3, theme.wallFence);
                setBlock(world, startX + x, baseY + 2, startZ + 11, theme.wallFence);
            }
        }
        for (int z = 4; z <= 10; z++) {
            if (z != 7) {
                setBlock(world, startX + 4, baseY + 2, startZ + z, theme.wallFence);
                setBlock(world, startX + 12, baseY + 2, startZ + z, theme.wallFence);
            }
        }

        // 7. Podio y Trono Ceremonial del Líder (Norte: Z = 13..15, X = 6..10)
        // Separado del ring por un pasillo libre en Z = 12
        for (int x = 6; x <= 10; x++) {
            for (int z = 14; z <= 15; z++) {
                setBlock(world, startX + x, baseY + 1, startZ + z, theme.trim);
                setBlock(world, startX + x, baseY + 2, startZ + z, theme.base);
            }
        }
        // Escaleras frontales al podio en Z = 13
        for (int x = 7; x <= 9; x++) {
            setBlock(world, startX + x, baseY + 1, startZ + 13, theme.stairs);
            setBlock(world, startX + x, baseY + 2, startZ + 13, theme.stairs);
        }
        // Respaldo monumental del trono (Z = 15)
        for (int x = 6; x <= 10; x++) {
            setBlock(world, startX + x, baseY + 3, startZ + 15, theme.pillar);
            setBlock(world, startX + x, baseY + 4, startZ + 15, theme.base);
        }
        // Antorchas / Faros flanqueando el trono
        setBlock(world, startX + 6, baseY + 3, startZ + 14, theme.pillar);
        setBlock(world, startX + 6, baseY + 4, startZ + 14, theme.light);
        setBlock(world, startX + 10, baseY + 3, startZ + 14, theme.pillar);
        setBlock(world, startX + 10, baseY + 4, startZ + 14, theme.light);

        // 8. Spawn del Líder de Gimnasio como Fake Player NPC con Skin Themed y registro persistente
        Location leaderLoc = new Location(world, startX + 8.5, baseY + 3, startZ + 14.0, 180f, 0f);
        UUID instanceId = UUID.randomUUID();
        if (registry != null) {
            registry.registerStructure(StructureRecord.fromLocation(instanceId, StructureType.GYM, leaderLoc, leader.name()));
        }
        spawnGymLeaderNpc(leaderLoc, leader, instanceId);

        // Registrar la nueva arena en DuelManager
        Location arenaCenter = new Location(world, startX + 8.5, baseY + 2, startZ + 7.5);
        if (plugin instanceof com.dark.roostergenics.RoostergenicsPlugin rgPlugin) {
            rgPlugin.getDuelManager().addKnownArena(arenaCenter);
        }

        player.sendMessage(Mm.noItalic("<gold>✔ ¡Coliseo del Gimnasio <bold>" + leader.getGymName() + "</bold> construido con éxito!"));
        player.sendMessage(Mm.noItalic("<gray>El Líder <color:" + leader.getThemeHex() + "><bold>" + leader.getLeaderName() + "</bold></color> te aguarda en su trono. ¡Hazle clic para iniciar el desafío!"));
        player.playSound(origin, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private void buildCornerTower(World world, int startX, int startZ, int offsetX, int offsetZ, int baseY, GymTheme theme) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                int bx = startX + offsetX + x;
                int bz = startZ + offsetZ + z;
                boolean isCorner = (x == 0 || x == 2) && (z == 0 || z == 2);
                Material mat = isCorner ? theme.pillar : theme.base;

                for (int y = 1; y <= 4; y++) {
                    setBlock(world, bx, baseY + y, bz, mat);
                }
                setBlock(world, bx, baseY + 5, bz, theme.trim);

                if (isCorner) {
                    setBlock(world, bx, baseY + 6, bz, theme.wallFence);
                } else if (x == 1 && z == 1) {
                    setBlock(world, bx, baseY + 6, bz, theme.light);
                }
            }
        }
    }

    private void buildPerimeterSegment(World world, int x, int baseY, int z, int index, GymTheme theme) {
        if (index == 5 || index == 11) {
            // Columnas gemelas exteriores
            setBlock(world, x, baseY + 1, z, theme.pillar);
            setBlock(world, x, baseY + 2, z, theme.pillar);
            setBlock(world, x, baseY + 3, z, theme.pillar);
            setBlock(world, x, baseY + 4, z, theme.trim);
            setBlock(world, x, baseY + 5, z, theme.light);
        } else if (index == 8) {
            // Eje central monumental
            setBlock(world, x, baseY + 1, z, theme.pillar);
            setBlock(world, x, baseY + 2, z, theme.pillar);
            setBlock(world, x, baseY + 3, z, theme.trim);
            setBlock(world, x, baseY + 4, z, theme.pillar);
        } else if (index == 4 || index == 12 || index == 7 || index == 9) {
            // Verja perimetral
            setBlock(world, x, baseY + 1, z, theme.base);
            setBlock(world, x, baseY + 2, z, theme.wallFence);
            setBlock(world, x, baseY + 3, z, theme.wallFence);
        } else {
            // Muros simétricos de soporte sólido (3, 6, 10, 13)
            setBlock(world, x, baseY + 1, z, theme.base);
            setBlock(world, x, baseY + 2, z, theme.trim);
            setBlock(world, x, baseY + 3, z, theme.base);
        }
    }

    /**
     * Construye un Puesto / Tienda de Mercader de Held Items.
     */
    public void buildMerchantShop(Player player, Location origin) {
        World world = origin.getWorld();
        int baseY = origin.getBlockY();
        int startX = origin.getBlockX() - 3;
        int startZ = origin.getBlockZ() - 3;

        buildFoundation(world, startX, startZ, 7, 7, baseY, Material.SPRUCE_PLANKS);

        // Piso de madera noble
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                setBlock(world, startX + x, baseY, startZ + z, Material.SPRUCE_PLANKS);
            }
        }

        // Mostrador frontal
        for (int x = 1; x <= 5; x++) {
            setBlock(world, startX + x, baseY + 1, startZ + 2, Material.SPRUCE_TRAPDOOR);
        }

        // Columnas y toldo
        setBlock(world, startX + 1, baseY + 1, startZ + 1, Material.SPRUCE_FENCE);
        setBlock(world, startX + 1, baseY + 2, startZ + 1, Material.SPRUCE_FENCE);
        setBlock(world, startX + 5, baseY + 1, startZ + 1, Material.SPRUCE_FENCE);
        setBlock(world, startX + 5, baseY + 2, startZ + 1, Material.SPRUCE_FENCE);

        // Toldo a rayas amarillas y rojas
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (z <= 3) {
                    setBlock(world, startX + x, baseY + 3, startZ + z, (x % 2 == 0) ? Material.YELLOW_WOOL : Material.RED_WOOL);
                }
            }
        }

        // Spawn del Mercader de Held Items (Player NPC con Skin 3D) y registro persistente
        Location shopLoc = new Location(world, startX + 3.5, baseY + 1, startZ + 4.5, 180f, 0f);
        UUID instanceId = UUID.randomUUID();
        if (registry != null) {
            registry.registerStructure(StructureRecord.fromLocation(instanceId, StructureType.MERCHANT_SHOP, shopLoc, ""));
        }
        spawnMerchantNpc(shopLoc, instanceId);

        player.sendMessage(Mm.noItalic("<green>✔ ¡Tienda del Mercader de Held Items generada con éxito!"));
        player.sendMessage(Mm.noItalic("<gray>Haz clic en el Mercader para abrir el catálogo y comprar objetos oficiales de combate."));
    }

    /**
     * Construye una Arena de Combate 11x11 estándar.
     */
    public void buildArena(Player player, Location origin) {
        World world = origin.getWorld();
        int baseY = origin.getBlockY();
        int startX = origin.getBlockX() - 5;
        int startZ = origin.getBlockZ() - 5;

        buildFoundation(world, startX, startZ, 11, 11, baseY, Material.SMOOTH_STONE);

        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                setBlock(world, startX + x, baseY, startZ + z, Material.SMOOTH_SANDSTONE);
                if (x == 0 || x == 10 || z == 0 || z == 10) {
                    setBlock(world, startX + x, baseY + 1, startZ + z, Material.OAK_FENCE);
                } else {
                    setBlock(world, startX + x, baseY + 1, startZ + z, Material.AIR);
                }
            }
        }

        // Esquinas diagonales de combate
        setBlock(world, startX + 1, baseY, startZ + 1, Material.RED_WOOL);
        setBlock(world, startX + 9, baseY, startZ + 9, Material.BLUE_WOOL);

        Location arenaCenter = new Location(world, startX + 5.5, baseY + 1, startZ + 5.5);
        if (plugin instanceof com.dark.roostergenics.RoostergenicsPlugin rgPlugin) {
            rgPlugin.getDuelManager().addKnownArena(arenaCenter);
        }

        player.sendMessage(Mm.noItalic("<green>✔ ¡Arena oficial de combate 11x11 construida con éxito!"));
    }

    public MerchantNPC spawnMerchantNpc(Location loc) {
        return spawnMerchantNpc(loc, UUID.randomUUID());
    }

    public MerchantNPC spawnMerchantNpc(Location loc, UUID instanceId) {
        MerchantNPC npc = new MerchantNPC(plugin, loc, instanceId);
        npc.spawnEntities();
        merchantNpcs.add(npc);
        return npc;
    }

    public Collection<MerchantNPC> getAllMerchantNpcs() {
        return merchantNpcs;
    }

    public void removeMerchantNPCNear(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return;
        double rSq = radius * radius;
        merchantNpcs.removeIf(npc -> {
            if (npc != null && npc.getLocation().getWorld().equals(loc.getWorld()) && npc.getLocation().distanceSquared(loc) <= rSq) {
                npc.remove();
                return true;
            }
            return false;
        });
    }

    public void cleanupAll() {
        for (MerchantNPC npc : merchantNpcs) {
            if (npc != null) {
                npc.remove();
            }
        }
        merchantNpcs.clear();
    }

    /**
     * Remueve proceduralmente cualquier estructura en el área circundante,
     * restaurando los bloques de terreno y limpiando NPCs, hologramas y arenas.
     */
    public int removeStructure(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return 0;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int removedBlocks = 0;

        // 1. Limpiar NPCs registrados
        removeMerchantNPCNear(center, radius + 3);
        if (centerManager != null) {
            centerManager.removeNurseNPCNear(center, radius + 3);
        }
        if (gymManager != null) {
            gymManager.removeGymNPCNear(center, radius + 3);
        }

        // Limpiar registro persistente de structures.yml y claims
        if (registry != null) {
            double rSq = (radius + 4) * (radius + 4);
            for (StructureRecord r : registry.getAllStructures()) {
                if (r.distanceSquared(center) <= rSq) {
                    registry.removeStructure(r.instanceId());
                    if (tracker != null) {
                        tracker.releaseStructureClaims(r.instanceId());
                    }
                }
            }
        }

        // 2. Limpiar entidades físicas huérfanas en el área
        for (Entity e : world.getNearbyEntities(center, radius + 3, radius + 3, radius + 3)) {
            if (e.getPersistentDataContainer().has(GymLeaderNPC.GYM_LEADER_KEY, PersistentDataType.STRING)
                    || e.getPersistentDataContainer().has(MerchantNPC.MERCHANT_NPC_KEY, PersistentDataType.STRING)
                    || e.getPersistentDataContainer().has(com.dark.roostergenics.npc.NurseNPC.NURSE_NPC_KEY, PersistentDataType.STRING)
                    || e.hasMetadata("roostergenics_shop_npc")
                    || e.hasMetadata("roostergenics_gym_leader")
                    || e.hasMetadata(RoosterCenterManager.METADATA_NURSE_NPC)) {
                e.remove();
            }
        }

        // 3. Limpiar arenas en DuelManager
        if (plugin instanceof com.dark.roostergenics.RoostergenicsPlugin rgPlugin) {
            rgPlugin.getDuelManager().removeArenasNear(center, radius + 4);
        }

        // 4. Restauración de bloques del área
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Parte aérea y superior (paredes, cuerdas, postes, techos, luces, trono)
                for (int y = 1; y <= 10; y++) {
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (b.getType() != Material.AIR) {
                        b.setType(Material.AIR, false);
                        removedBlocks++;
                    }
                }

                // Suelo base (reemplazar piso artificial por césped / tierra)
                Block floor = world.getBlockAt(cx + x, cy, cz + z);
                if (isStructureBlock(floor.getType())) {
                    floor.setType(Material.GRASS_BLOCK, false);
                    removedBlocks++;
                }

                // Cimientos hacia abajo
                for (int y = -1; y >= -5; y--) {
                    Block fBlock = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (isStructureBlock(fBlock.getType())) {
                        fBlock.setType(Material.DIRT, false);
                        removedBlocks++;
                    }
                }
            }
        }

        world.spawnParticle(org.bukkit.Particle.EXPLOSION, center.clone().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticle(org.bukkit.Particle.CLOUD, center.clone().add(0, 1, 0), 25, 1.5, 1.0, 1.5, 0.1);
        world.playSound(center, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);

        return removedBlocks;
    }

    private boolean isStructureBlock(Material mat) {
        if (mat == null || mat.isAir()) return false;
        String name = mat.name();
        return name.contains("BRICK") || name.contains("STONE") || name.contains("QUARTZ")
                || name.contains("CONCRETE") || name.contains("TERRACOTTA") || name.contains("WOOL")
                || name.contains("CARPET") || name.contains("PRISMARINE") || name.contains("PURPUR")
                || name.contains("DEEPSLATE") || name.contains("BLACKSTONE") || name.contains("PLANK")
                || name.contains("FENCE") || name.contains("WALL") || name.contains("STAIRS")
                || name.contains("TRAPDOOR") || name.contains("GLASS") || name.contains("LANTERN")
                || name.contains("FROGLIGHT") || name.contains("SHROOMLIGHT") || name.contains("MUD")
                || mat == Material.LODESTONE || mat == Material.OBSIDIAN || mat == Material.CRYING_OBSIDIAN
                || mat == Material.MAGMA_BLOCK || mat == Material.MOSS_BLOCK || mat == Material.MOSS_CARPET
                || mat == Material.GLOWSTONE || mat == Material.SEA_LANTERN || mat == Material.END_ROD
                || mat == Material.GOLD_BLOCK || mat == Material.IRON_BARS || mat == Material.IRON_CHAIN;
    }

    public GymLeaderNPC spawnGymLeaderNpc(Location loc, GymLeader leader) {
        return spawnGymLeaderNpc(loc, leader, UUID.randomUUID());
    }

    public GymLeaderNPC spawnGymLeaderNpc(Location loc, GymLeader leader, UUID instanceId) {
        if (gymManager != null) {
            return gymManager.spawnGymLeaderNPC(loc, leader, instanceId);
        }
        return null;
    }

    public void spawnPhysicalSlot(StructureRecord record, EntityRole role) {
        if (record == null || role == null) return;
        Location loc = record.toLocation();
        if (loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();

        switch (record.type()) {
            case ROOSTER_CENTER -> {
                if (role == EntityRole.HOLOGRAM) {
                    Location holoLoc = loc.clone().add(0, 2.25, 0);
                    world.spawn(holoLoc, TextDisplay.class, td -> {
                        td.setBillboard(Display.Billboard.CENTER);
                        td.setDefaultBackground(true);
                        td.text(NurseNPC.buildHologramText());
                        td.setInvulnerable(true);
                        td.setPersistent(true);
                        td.getPersistentDataContainer().set(NurseNPC.NURSE_NPC_KEY, PersistentDataType.STRING, "nurse");
                        StructureEntityTracker.tagEntityPdc(td, record.instanceId(), EntityRole.HOLOGRAM, StructureType.ROOSTER_CENTER);
                    });
                } else if (role == EntityRole.INTERACTION) {
                    world.spawn(loc, Interaction.class, inter -> {
                        inter.setInteractionHeight(1.95f);
                        inter.setInteractionWidth(0.75f);
                        inter.setResponsive(true);
                        inter.setInvulnerable(true);
                        inter.setPersistent(true);
                        inter.getPersistentDataContainer().set(NurseNPC.NURSE_NPC_KEY, PersistentDataType.STRING, "nurse");
                        inter.setMetadata(RoosterCenterManager.METADATA_NURSE_NPC, new FixedMetadataValue(plugin, true));
                        StructureEntityTracker.tagEntityPdc(inter, record.instanceId(), EntityRole.INTERACTION, StructureType.ROOSTER_CENTER);
                    });
                }
            }
            case MERCHANT_SHOP -> {
                if (role == EntityRole.HOLOGRAM) {
                    Location holoLoc = loc.clone().add(0, 2.25, 0);
                    world.spawn(holoLoc, TextDisplay.class, td -> {
                        td.setBillboard(Display.Billboard.CENTER);
                        td.setDefaultBackground(true);
                        td.text(MerchantNPC.buildHologramText());
                        td.setInvulnerable(true);
                        td.setPersistent(true);
                        td.getPersistentDataContainer().set(MerchantNPC.MERCHANT_NPC_KEY, PersistentDataType.STRING, "merchant");
                        StructureEntityTracker.tagEntityPdc(td, record.instanceId(), EntityRole.HOLOGRAM, StructureType.MERCHANT_SHOP);
                    });
                } else if (role == EntityRole.INTERACTION) {
                    world.spawn(loc, Interaction.class, inter -> {
                        inter.setInteractionHeight(1.95f);
                        inter.setInteractionWidth(0.75f);
                        inter.setResponsive(true);
                        inter.setInvulnerable(true);
                        inter.setPersistent(true);
                        inter.getPersistentDataContainer().set(MerchantNPC.MERCHANT_NPC_KEY, PersistentDataType.STRING, "merchant");
                        inter.setMetadata("roostergenics_shop_npc", new FixedMetadataValue(plugin, true));
                        StructureEntityTracker.tagEntityPdc(inter, record.instanceId(), EntityRole.INTERACTION, StructureType.MERCHANT_SHOP);
                    });
                }
            }
            case GYM -> {
                GymLeader leader = GymLeader.VIENTO;
                try {
                    if (record.extraData() != null) leader = GymLeader.valueOf(record.extraData());
                } catch (Exception ignored) {}
                final GymLeader fLeader = leader;

                if (role == EntityRole.HOLOGRAM) {
                    Location holoLoc = loc.clone().add(0, 2.25, 0);
                    world.spawn(holoLoc, TextDisplay.class, td -> {
                        td.setBillboard(Display.Billboard.CENTER);
                        td.setDefaultBackground(true);
                        td.text(GymLeaderNPC.buildHologramText(fLeader));
                        td.setInvulnerable(true);
                        td.setPersistent(true);
                        td.getPersistentDataContainer().set(GymLeaderNPC.GYM_LEADER_KEY, PersistentDataType.STRING, fLeader.name());
                        StructureEntityTracker.tagEntityPdc(td, record.instanceId(), EntityRole.HOLOGRAM, StructureType.GYM);
                    });
                } else if (role == EntityRole.INTERACTION) {
                    world.spawn(loc, Interaction.class, inter -> {
                        inter.setInteractionHeight(1.95f);
                        inter.setInteractionWidth(0.75f);
                        inter.setResponsive(true);
                        inter.setInvulnerable(true);
                        inter.setPersistent(true);
                        inter.getPersistentDataContainer().set(GymLeaderNPC.GYM_LEADER_KEY, PersistentDataType.STRING, fLeader.name());
                        inter.setMetadata("roostergenics_gym_leader", new FixedMetadataValue(plugin, fLeader.name()));
                        StructureEntityTracker.tagEntityPdc(inter, record.instanceId(), EntityRole.INTERACTION, StructureType.GYM);
                    });
                }
            }
        }
    }

    @EventHandler
    public void onMerchantInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity target = event.getRightClicked();
        if (target.hasMetadata("roostergenics_shop_npc") 
                || target.getPersistentDataContainer().has(MerchantNPC.MERCHANT_NPC_KEY, PersistentDataType.STRING)
                || (target instanceof Villager v && v.getCustomName() != null && v.getCustomName().contains("Mercader"))) {
            event.setCancelled(true);
            com.dark.roostergenics.gui.HeldItemShopGui.open(event.getPlayer());
        }
    }

    @EventHandler
    public void onMerchantDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if (target.hasMetadata("roostergenics_shop_npc") 
                || target.getPersistentDataContainer().has(MerchantNPC.MERCHANT_NPC_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                com.dark.roostergenics.gui.HeldItemShopGui.open(player);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            if (event.getPlayer() instanceof Player player) {
                for (MerchantNPC npc : merchantNpcs) {
                    if (npc.getLocation().getWorld().equals(player.getWorld()) 
                            && npc.getLocation().distanceSquared(player.getLocation()) <= 25.0) {
                        event.setCancelled(true);
                        com.dark.roostergenics.gui.HeldItemShopGui.open(player);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Rellena hacia abajo con bloques sólidos hasta encontrar tierra/roca para que la estructura no flote.
     */
    private void buildFoundation(World world, int startX, int startZ, int width, int length, int baseY, Material mat) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                int currY = baseY - 1;
                while (currY > world.getMinHeight()) {
                    Block b = world.getBlockAt(startX + x, currY, startZ + z);
                    if (b.getType().isSolid()) {
                        break; // Ya hay suelo sólido
                    }
                    b.setType(mat, false);
                    currY--;
                }
            }
        }
    }
}
