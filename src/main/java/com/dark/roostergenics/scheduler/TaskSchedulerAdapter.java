package com.dark.roostergenics.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/**
 * Adaptador universal de planificación y tareas programadas compatible de forma transparente con:
 * - Paper tradicional (BukkitScheduler)
 * - Folia multihilo por regiones (RegionScheduler, AsyncScheduler, GlobalRegionScheduler)
 */
public class TaskSchedulerAdapter {

    private final Plugin plugin;
    private final boolean isFolia;

    public TaskSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = checkFolia();
    }

    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Ejecuta una tarea en el hilo de la región correspondiente a una ubicación.
     */
    public TaskHandle runAtLocation(Location location, Consumer<TaskHandle> taskConsumer) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> {
                taskConsumer.accept(new FoliaTaskHandle(scheduledTask));
            });
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
                taskConsumer.accept(handle);
            });
            handle.setTask(task);
            return handle;
        }
    }

    /**
     * Ejecuta una tarea con retraso en ticks en la región de una ubicación.
     */
    public TaskHandle runDelayedAtLocation(Location location, Consumer<TaskHandle> taskConsumer, long delayTicks) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> {
                taskConsumer.accept(new FoliaTaskHandle(scheduledTask));
            }, Math.max(1L, delayTicks));
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                taskConsumer.accept(handle);
            }, Math.max(1L, delayTicks));
            handle.setTask(task);
            return handle;
        }
    }

    /**
     * Ejecuta una tarea periódica en ticks en la región de una ubicación.
     */
    public TaskHandle runRepeatingAtLocation(Location location, Consumer<TaskHandle> taskConsumer, long initialDelayTicks, long periodTicks) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> {
                taskConsumer.accept(new FoliaTaskHandle(scheduledTask));
            }, Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                taskConsumer.accept(handle);
            }, Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
            handle.setTask(task);
            return handle;
        }
    }

    /**
     * Ejecuta una tarea con retraso en una entidad (jugador, etc.).
     */
    public TaskHandle runDelayedAtEntity(Entity entity, Consumer<TaskHandle> taskConsumer, long delayTicks) {
        if (isFolia) {
            ScheduledTask task = entity.getScheduler().runDelayed(plugin, scheduledTask -> {
                taskConsumer.accept(new FoliaTaskHandle(scheduledTask));
            }, null, Math.max(1L, delayTicks));
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                taskConsumer.accept(handle);
            }, Math.max(1L, delayTicks));
            handle.setTask(task);
            return handle;
        }
    }

    /**
     * Ejecuta una tarea periódica global en el servidor.
     */
    public TaskHandle runRepeatingGlobal(Consumer<TaskHandle> taskConsumer, long initialDelayTicks, long periodTicks) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
                taskConsumer.accept(new FoliaTaskHandle(scheduledTask));
            }, Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                taskConsumer.accept(handle);
            }, Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
            handle.setTask(task);
            return handle;
        }
    }

    /**
     * Ejecuta una tarea asíncrona fuera del hilo de tick.
     */
    public TaskHandle runAsync(Runnable runnable) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            handle.setTask(task);
            return handle;
        }
    }

    private static boolean isFoliaStatic() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static TaskHandle runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFoliaStatic()) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), Math.max(1L, delayTicks));
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, Math.max(1L, delayTicks));
            handle.setTask(task);
            return handle;
        }
    }

    public static TaskHandle runTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (isFoliaStatic()) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> runnable.run(), Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
            return new FoliaTaskHandle(task);
        } else {
            BukkitTaskHandle handle = new BukkitTaskHandle();
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
            handle.setTask(task);
            return handle;
        }
    }

    public interface TaskHandle {
        void cancel();
        boolean isCancelled();
    }

    public static class FoliaTaskHandle implements TaskHandle {
        private final ScheduledTask scheduledTask;

        public FoliaTaskHandle(ScheduledTask scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        public void cancel() {
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        }

        @Override
        public boolean isCancelled() {
            return scheduledTask == null || scheduledTask.isCancelled();
        }
    }

    public static class BukkitTaskHandle implements TaskHandle {
        private volatile BukkitTask bukkitTask;
        private volatile boolean cancelled = false;

        public BukkitTaskHandle() {
            this.bukkitTask = null;
        }

        public void setTask(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
            if (cancelled && bukkitTask != null) {
                bukkitTask.cancel();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            if (bukkitTask != null) {
                bukkitTask.cancel();
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled || (bukkitTask != null && bukkitTask.isCancelled());
        }
    }
}
