package com.dark.roostergenics.structure;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

/**
 * Representación inmutable de una estructura persistente.
 * No contiene objetos mutables como Location de Bukkit.
 */
public record StructureRecord(
        UUID instanceId,
        StructureType type,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String extraData
) {
    public StructureRecord {
        Objects.requireNonNull(instanceId, "instanceId no puede ser null");
        Objects.requireNonNull(type, "type no puede ser null");
        Objects.requireNonNull(worldName, "worldName no puede ser null");
        if (extraData == null) {
            extraData = "";
        }
    }

    public static StructureRecord fromLocation(UUID instanceId, StructureType type, Location loc, String extraData) {
        Objects.requireNonNull(loc, "loc no puede ser null");
        Objects.requireNonNull(loc.getWorld(), "loc.getWorld() no puede ser null");
        return new StructureRecord(
                instanceId,
                type,
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch(),
                extraData
        );
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public int getChunkX() {
        return ((int) Math.floor(x)) >> 4;
    }

    public int getChunkZ() {
        return ((int) Math.floor(z)) >> 4;
    }

    public double distanceSquared(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return Double.MAX_VALUE;
        }
        double dx = x - loc.getX();
        double dy = y - loc.getY();
        double dz = z - loc.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
