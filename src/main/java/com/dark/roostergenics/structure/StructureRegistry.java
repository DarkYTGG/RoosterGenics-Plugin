package com.dark.roostergenics.structure;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Autoridad de persistencia crash-safe para estructuras Roostergenics (structures.yml).
 * Implementa versionado atómico SnapshotVersion, Single-Flight Worker y barrera RUNNING -> QUIESCING -> STOPPED.
 */
public final class StructureRegistry {

    public enum Lifecycle { RUNNING, QUIESCING, STOPPED }

    public record ChunkKey(String worldName, int chunkX, int chunkZ) {}
    public record SnapshotVersion(long version, Map<UUID, StructureRecord> data) {}

    private final JavaPlugin plugin;
    private final File dataFile;

    private final Map<UUID, StructureRecord> structures = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Set<UUID>> chunkIndex = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final ReentrantLock diskLock = new ReentrantLock();

    private volatile Lifecycle lifecycle = Lifecycle.RUNNING;
    private final AtomicLong versionCounter = new AtomicLong(0);
    private final AtomicLong lastSavedVersion = new AtomicLong(0);
    private final AtomicReference<SnapshotVersion> pendingSnapshot = new AtomicReference<>();
    private final AtomicBoolean isWorkerRunning = new AtomicBoolean(false);

    public StructureRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "structures.yml");
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void load() {
        lifecycleLock.writeLock().lock();
        try {
            structures.clear();
            chunkIndex.clear();
            if (!dataFile.exists()) return;

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection sec = yaml.getConfigurationSection("structures");
            if (sec == null) return;

            for (String key : sec.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    String typeStr = sec.getString(key + ".type");
                    StructureType type = StructureType.valueOf(typeStr);
                    String wName = sec.getString(key + ".world");
                    if (wName == null || wName.isEmpty()) continue;

                    double x = sec.getDouble(key + ".x");
                    double y = sec.getDouble(key + ".y");
                    double z = sec.getDouble(key + ".z");
                    float yaw = (float) sec.getDouble(key + ".yaw", 0.0);
                    float pitch = (float) sec.getDouble(key + ".pitch", 0.0);
                    String extra = sec.getString(key + ".extra", "");

                    StructureRecord record = new StructureRecord(id, type, wName, x, y, z, yaw, pitch, extra);
                    structures.put(id, record);
                    addToChunkIndex(record);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error cargando estructura " + key + " de structures.yml: " + e.getMessage());
                }
            }
            long initialVersion = versionCounter.incrementAndGet();
            lastSavedVersion.set(initialVersion);
            pendingSnapshot.set(new SnapshotVersion(initialVersion, Map.copyOf(structures)));
            plugin.getLogger().info("✔ Se cargaron " + structures.size() + " estructuras persistentes desde structures.yml.");
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    public StructureRecord getStructure(UUID instanceId) {
        if (instanceId == null) return null;
        return structures.get(instanceId);
    }

    public Collection<StructureRecord> getAllStructures() {
        return Collections.unmodifiableCollection(structures.values());
    }

    /**
     * Búsqueda espacial O(1) de estructuras en un chunk específico.
     */
    public Set<UUID> getStructureIdsInChunk(String worldName, int chunkX, int chunkZ) {
        if (worldName == null) return Collections.emptySet();
        Set<UUID> set = chunkIndex.get(new ChunkKey(worldName, chunkX, chunkZ));
        if (set == null || set.isEmpty()) return Collections.emptySet();
        return Collections.unmodifiableSet(set);
    }

    public boolean registerStructure(StructureRecord record) {
        Objects.requireNonNull(record, "record");
        lifecycleLock.readLock().lock();
        try {
            if (lifecycle != Lifecycle.RUNNING) {
                return false; // Bloqueado tras QUIESCING
            }

            structures.put(record.instanceId(), record);
            addToChunkIndex(record);

            long v = versionCounter.incrementAndGet();
            pendingSnapshot.set(new SnapshotVersion(v, Map.copyOf(structures)));
            triggerSaveWorker();
            return true;
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    public boolean removeStructure(UUID instanceId) {
        if (instanceId == null) return false;
        lifecycleLock.readLock().lock();
        try {
            if (lifecycle != Lifecycle.RUNNING) {
                return false; // Bloqueado tras QUIESCING
            }

            StructureRecord removed = structures.remove(instanceId);
            if (removed != null) {
                removeFromChunkIndex(removed);
                long v = versionCounter.incrementAndGet();
                pendingSnapshot.set(new SnapshotVersion(v, Map.copyOf(structures)));
                triggerSaveWorker();
                return true;
            }
            return false;
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    private void addToChunkIndex(StructureRecord record) {
        ChunkKey key = new ChunkKey(record.worldName(), record.getChunkX(), record.getChunkZ());
        chunkIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(record.instanceId());
    }

    private void removeFromChunkIndex(StructureRecord record) {
        ChunkKey key = new ChunkKey(record.worldName(), record.getChunkX(), record.getChunkZ());
        Set<UUID> set = chunkIndex.get(key);
        if (set != null) {
            set.remove(record.instanceId());
            if (set.isEmpty()) {
                chunkIndex.remove(key);
            }
        }
    }

    private void triggerSaveWorker() {
        if (isWorkerRunning.compareAndSet(false, true)) {
            if (plugin.isEnabled()) {
                plugin.getServer().getAsyncScheduler().runNow(plugin, task -> drainDiskWorker());
            } else {
                drainDiskWorker();
            }
        }
    }

    private void drainDiskWorker() {
        try {
            while (true) {
                diskLock.lock();
                SnapshotVersion current;
                try {
                    current = pendingSnapshot.get();
                    if (current == null || current.version() <= lastSavedVersion.get()) {
                        break;
                    }
                    writeSnapshotToDiskSync(current);
                    lastSavedVersion.set(current.version());
                } finally {
                    diskLock.unlock();
                }
            }
        } finally {
            isWorkerRunning.set(false);
            SnapshotVersion latest = pendingSnapshot.get();
            if (latest != null && latest.version() > lastSavedVersion.get()) {
                if (isWorkerRunning.compareAndSet(false, true)) {
                    if (plugin.isEnabled()) {
                        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> drainDiskWorker());
                    } else {
                        drainDiskWorker();
                    }
                }
            }
        }
    }

    private void writeSnapshotToDiskSync(SnapshotVersion snapshot) {
        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("structures");

        for (StructureRecord r : snapshot.data().values()) {
            String path = r.instanceId().toString();
            root.set(path + ".type", r.type().name());
            root.set(path + ".world", r.worldName());
            root.set(path + ".x", r.x());
            root.set(path + ".y", r.y());
            root.set(path + ".z", r.z());
            root.set(path + ".yaw", r.yaw());
            root.set(path + ".pitch", r.pitch());
            if (r.extraData() != null && !r.extraData().isEmpty()) {
                root.set(path + ".extra", r.extraData());
            }
        }

        File tmp = new File(parent, "structures.yml.tmp");
        try {
            yaml.save(tmp);
            try {
                Files.move(tmp.toPath(), dataFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicEx) {
                // Fallback si ATOMIC_MOVE no es soportado por el filesystem
                Files.move(tmp.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error escribiendo structures.yml: " + e.getMessage());
        } finally {
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    public void shutdown() {
        // PASO 1: Barrera de parada para productores
        lifecycleLock.writeLock().lock();
        try {
            this.lifecycle = Lifecycle.QUIESCING;
        } finally {
            lifecycleLock.writeLock().unlock();
        }

        // PASO 2: Drenar worker asíncrono y guardar snapshot final exacto
        diskLock.lock();
        try {
            SnapshotVersion finalSnapshot = pendingSnapshot.get();
            if (finalSnapshot != null && finalSnapshot.version() > lastSavedVersion.get()) {
                writeSnapshotToDiskSync(finalSnapshot);
                lastSavedVersion.set(finalSnapshot.version());
            }
            this.lifecycle = Lifecycle.STOPPED;
        } finally {
            diskLock.unlock();
        }
    }
}
