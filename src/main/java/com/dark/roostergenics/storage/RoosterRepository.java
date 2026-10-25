package com.dark.roostergenics.storage;

import com.dark.roostergenics.model.RoosterData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Repositorio de persistencia desacoplada para gallos virtuales.
 * Previene la sobrecarga del hilo principal mediante caché en memoria y persistencia YAML organizada.
 */
public class RoosterRepository {

    private final JavaPlugin plugin;
    private final File roostersFolder;
    private final File activeMappingFile;

    private final Map<UUID, RoosterData> roosterCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> playerRoosters = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerActiveRooster = new ConcurrentHashMap<>();

    public RoosterRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.roostersFolder = new File(plugin.getDataFolder(), "roosters");
        this.activeMappingFile = new File(plugin.getDataFolder(), "active_roosters.yml");

        if (!roostersFolder.exists()) {
            roostersFolder.mkdirs();
        }
    }

    public void loadAll() {
        roosterCache.clear();
        playerRoosters.clear();
        playerActiveRooster.clear();

        File[] files = roostersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    RoosterData data = RoosterData.deserializeFrom(config);
                    roosterCache.put(data.getRoosterId(), data);

                    if (data.getOwnerId() != null) {
                        playerRoosters.computeIfAbsent(data.getOwnerId(), k -> new ArrayList<>()).add(data.getRoosterId());
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error al cargar el gallo desde " + file.getName(), e);
                }
            }
        }

        if (activeMappingFile.exists()) {
            YamlConfiguration activeConfig = YamlConfiguration.loadConfiguration(activeMappingFile);
            for (String key : activeConfig.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(key);
                    UUID roosterId = UUID.fromString(activeConfig.getString(key));
                    if (roosterCache.containsKey(roosterId)) {
                        playerActiveRooster.put(playerId, roosterId);
                    }
                } catch (Exception ignored) {}
            }
        }

        plugin.getLogger().info("Se han cargado " + roosterCache.size() + " gallos en memoria.");
    }

    public void saveAll() {
        for (RoosterData data : roosterCache.values()) {
            saveRoosterSync(data);
        }
        saveActiveMappings();
    }

    public void saveRooster(RoosterData data) {
        roosterCache.put(data.getRoosterId(), data);
        if (data.getOwnerId() != null) {
            List<UUID> list = playerRoosters.computeIfAbsent(data.getOwnerId(), k -> new ArrayList<>());
            if (!list.contains(data.getRoosterId())) {
                list.add(data.getRoosterId());
            }
        }

        // Asynchronous disk save to avoid tick stalls
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> saveRoosterSync(data));
    }

    /**
     * Guardado atómico crash-safe: escribe en .tmp y reemplaza con ATOMIC_MOVE.
     * Previene archivos de 0 bytes ante cortes de luz o kill -9.
     */
    private synchronized void saveRoosterSync(RoosterData data) {
        File target = new File(roostersFolder, data.getRoosterId() + ".yml");
        File tmp = new File(roostersFolder, data.getRoosterId() + ".yml.tmp");
        YamlConfiguration config = new YamlConfiguration();
        data.serializeTo(config);
        try {
            config.save(tmp);
            Files.move(tmp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Fallback: si ATOMIC_MOVE no es soportado por el filesystem
            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "No se pudo guardar el archivo del gallo: " + data.getRoosterId(), ex);
            }
        }
    }

    public void deleteRooster(UUID roosterId) {
        RoosterData data = roosterCache.remove(roosterId);
        if (data != null && data.getOwnerId() != null) {
            List<UUID> list = playerRoosters.get(data.getOwnerId());
            if (list != null) list.remove(roosterId);

            if (roosterId.equals(playerActiveRooster.get(data.getOwnerId()))) {
                playerActiveRooster.remove(data.getOwnerId());
                saveActiveMappings();
            }
        }

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            File file = new File(roostersFolder, roosterId + ".yml");
            if (file.exists()) {
                file.delete();
            }
        });
    }

    public RoosterData getRooster(UUID roosterId) {
        if (roosterId == null) return null;
        return roosterCache.get(roosterId);
    }

    public List<RoosterData> getPlayerRoosters(UUID ownerId) {
        List<UUID> ids = playerRoosters.get(ownerId);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        List<RoosterData> result = new ArrayList<>();
        for (UUID id : ids) {
            RoosterData r = roosterCache.get(id);
            if (r != null) result.add(r);
        }
        return result;
    }

    public RoosterData getActiveRooster(UUID ownerId) {
        UUID activeId = playerActiveRooster.get(ownerId);
        if (activeId != null) {
            RoosterData active = roosterCache.get(activeId);
            if (active != null) return active;
        }

        // Si no tiene ninguno seleccionado, asignar el primero si tiene alguno
        List<RoosterData> roosters = getPlayerRoosters(ownerId);
        if (!roosters.isEmpty()) {
            RoosterData fallback = roosters.get(0);
            setActiveRooster(ownerId, fallback.getRoosterId());
            return fallback;
        }

        return null;
    }

    public void setActiveRooster(UUID ownerId, UUID roosterId) {
        if (roosterId == null) {
            playerActiveRooster.remove(ownerId);
        } else {
            playerActiveRooster.put(ownerId, roosterId);
        }
        saveActiveMappings();
    }

    /**
     * Guardado atómico de mapeo de gallos activos por jugador.
     */
    private void saveActiveMappings() {
        Map<String, String> snapshot = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : playerActiveRooster.entrySet()) {
            snapshot.put(entry.getKey().toString(), entry.getValue().toString());
        }

        Runnable writeTask = () -> {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            File tmp = new File(activeMappingFile.getParentFile(), "active_roosters.yml.tmp");
            try {
                config.save(tmp);
                Files.move(tmp.toPath(), activeMappingFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(tmp.toPath(), activeMappingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Error al guardar asignaciones de gallos activos.", ex);
                }
            }
        };

        if (plugin.isEnabled()) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, t -> writeTask.run());
        } else {
            writeTask.run();
        }
    }
}
