package com.dark.roostergenics.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/**
 * Constructor del cuadrilátero físico del Palenque en el mundo para combates 1v1.
 */
public class ArenaBuilder {

    public static final int DEFAULT_RADIUS = 4; // Cuadrilátero 9x9 (radio 4 desde el centro)

    /**
     * Construye un cuadrilátero de palenque tradicional:
     * - Suelo nivelado de tierra compacta y tablones pulidos.
     * - Esquina Roja (Gallo A) y Esquina Azul (Gallo B).
     * - Calla perimetral de vallas con 4 postes esquineros y farolillos.
     * - Puerta de acceso para jueces/espectadores.
     */
    public static void buildArena(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Limpiar aire interior del cuadrilátero (y hasta y+3)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= 3; y++) {
                    world.getBlockAt(cx + x, cy + y, cz + z).setType(Material.AIR, false);
                }
            }
        }

        // 2. Colocar suelo del cuadrilátero (en cy - 1)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block floorBlock = world.getBlockAt(cx + x, cy - 1, cz + z);
                boolean isBorder = Math.abs(x) == radius || Math.abs(z) == radius;

                if (isBorder) {
                    floorBlock.setType(Material.SMOOTH_STONE, false);
                } else if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    floorBlock.setType(Material.STRIPPED_OAK_WOOD, false);
                } else {
                    floorBlock.setType(Material.COARSE_DIRT, false);
                }
            }
        }

        // 3. Marcadores de esquina para los gallos
        // Esquina Roja (Gallo A - Oeste)
        world.getBlockAt(cx - (radius - 1), cy - 1, cz).setType(Material.RED_TERRACOTTA, false);
        world.getBlockAt(cx - (radius - 1), cy, cz).setType(Material.RED_CARPET, false);

        // Esquina Azul (Gallo B - Este)
        world.getBlockAt(cx + (radius - 1), cy - 1, cz).setType(Material.BLUE_TERRACOTTA, false);
        world.getBlockAt(cx + (radius - 1), cy, cz).setType(Material.BLUE_CARPET, false);

        // 4. Vallado perimetral y postes esquineros
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                boolean isCorner = Math.abs(x) == radius && Math.abs(z) == radius;
                boolean isBorder = Math.abs(x) == radius || Math.abs(z) == radius;

                if (isCorner) {
                    // Poste esquinero de 2 bloques con farolillo
                    world.getBlockAt(cx + x, cy, cz + z).setType(Material.STRIPPED_DARK_OAK_LOG, false);
                    world.getBlockAt(cx + x, cy + 1, cz + z).setType(Material.STRIPPED_DARK_OAK_LOG, false);
                    world.getBlockAt(cx + x, cy + 2, cz + z).setType(Material.LANTERN, false);
                } else if (isBorder) {
                    // Puerta de acceso en el lado Sur (z = radius, x = 0)
                    if (z == radius && x == 0) {
                        world.getBlockAt(cx + x, cy, cz + z).setType(Material.OAK_FENCE_GATE, false);
                    } else {
                        world.getBlockAt(cx + x, cy, cz + z).setType(Material.DARK_OAK_FENCE, false);
                    }
                }
            }
        }
    }

    /**
     * Remueve la estructura física del cuadrilátero devolviendo aire.
     */
    public static void removeArena(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 3; y++) {
                    world.getBlockAt(cx + x, cy + y, cz + z).setType(Material.AIR, false);
                }
            }
        }
    }
}
