package com.dark.roostergenics.structure;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de runtime para slots y reclamos atómicos de entidades en estructuras persistentes.
 * Garantiza:
 * 1. Cero efectos secundarios Bukkit dentro de compute().
 * 2. Reconciliación en dos fases (Descubrimiento inmediato + Reconciliación diferida de 1 tick).
 * 3. Preservación estricta de claims durante descarga de chunks vía EntitiesUnloadEvent.
 * 4. Búsqueda espacial O(1) vía chunkIndex.
 */
public final class StructureEntityTracker implements Listener {

    public static final NamespacedKey KEY_INSTANCE_ID = new NamespacedKey("roostergenics", "instance_id");
    public static final NamespacedKey KEY_ENTITY_ROLE = new NamespacedKey("roostergenics", "entity_role");
    public static final NamespacedKey KEY_STRUCTURE_TYPE = new NamespacedKey("roostergenics", "structure_type");

    public enum ClaimState { CLAIMED, UNLOADED, RESERVED }
    public record ClaimRecord(UUID entityUuid, ClaimState state) {}

    public enum ClaimOutcome {
        ACCEPTED,
        RESTORED,
        REJECT_ORPHAN,
        REJECT_DUPLICATE,
        REJECT_INVALID_POSITION,
        REJECT_FOREIGN_WORLD,
        IGNORED
    }

    private final JavaPlugin plugin;
    private final StructureRegistry registry;
    private final StructureManager structureManager;
    private final ConcurrentHashMap<EntityClaimKey, ClaimRecord> activeClaims = new ConcurrentHashMap<>();

    public StructureEntityTracker(JavaPlugin plugin, StructureRegistry registry, StructureManager structureManager) {
        this.plugin = plugin;
        this.registry = registry;
        this.structureManager = structureManager;
    }

    public Map<EntityClaimKey, ClaimRecord> getActiveClaims() {
        return Collections.unmodifiableMap(activeClaims);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        evaluateEntityClaim(event.getEntity(), event.getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        org.bukkit.World world = event.getWorld();
        for (Entity entity : event.getEntities()) {
            evaluateEntityClaim(entity, world);
        }
    }

    public ClaimOutcome evaluateEntityClaim(Entity entity) {
        return evaluateEntityClaim(entity, entity != null ? entity.getWorld() : null);
    }

    public ClaimOutcome evaluateEntityClaim(Entity entity, org.bukkit.World currentWorld) {
        if (entity == null || !entity.isValid() || currentWorld == null) return ClaimOutcome.IGNORED;

        EntityClaimKey key = extractClaimProposal(entity);
        if (key == null) {
            // Intentar migración determinista si es entidad legacy sin instance_id
            return handleLegacyMigration(entity);
        }

        ClaimOutcome[] outcomeHolder = new ClaimOutcome[]{ClaimOutcome.IGNORED};

        // OPERACIÓN ATÓMICA PURA: Únicamente transición de estado sin efectos secundarios Bukkit
        activeClaims.compute(key, (k, current) -> {
            StructureRecord record = registry.getStructure(k.instanceId());
            if (record == null) {
                outcomeHolder[0] = ClaimOutcome.REJECT_ORPHAN;
                return current;
            }

            // Validación estricta del mundo de llegada (currentWorld / event.getWorld())
            if (!currentWorld.getName().equals(record.worldName())) {
                outcomeHolder[0] = ClaimOutcome.REJECT_FOREIGN_WORLD;
                return current;
            }

            // Validación espacial de proximidad (máximo 4 bloques de tolerancia)
            if (record.distanceSquared(entity.getLocation()) > 16.0) {
                outcomeHolder[0] = ClaimOutcome.REJECT_INVALID_POSITION;
                return current;
            }

            // Resolución de slot
            if (current == null || current.state() == ClaimState.RESERVED) {
                outcomeHolder[0] = ClaimOutcome.ACCEPTED;
                return new ClaimRecord(entity.getUniqueId(), ClaimState.CLAIMED);
            }

            if (current.entityUuid().equals(entity.getUniqueId())) {
                outcomeHolder[0] = ClaimOutcome.RESTORED;
                return new ClaimRecord(entity.getUniqueId(), ClaimState.CLAIMED);
            }

            // Slot ocupado por otra entidad viva o descargada -> DUPLICADO
            outcomeHolder[0] = ClaimOutcome.REJECT_DUPLICATE;
            return current;
        });

        ClaimOutcome outcome = outcomeHolder[0];

        // EFECTOS FÍSICOS FUERA DE COMPUTE
        if (outcome == ClaimOutcome.REJECT_ORPHAN 
                || outcome == ClaimOutcome.REJECT_DUPLICATE 
                || outcome == ClaimOutcome.REJECT_INVALID_POSITION 
                || outcome == ClaimOutcome.REJECT_FOREIGN_WORLD) {
            despawnEntity(entity);
        }

        return outcome;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            EntityClaimKey key = extractClaimProposal(entity);
            if (key == null) continue;

            activeClaims.computeIfPresent(key, (k, current) -> {
                if (current.entityUuid().equals(entity.getUniqueId())) {
                    // CHUNK UNLOAD: Preservar claim en estado UNLOADED para evitar usurpación
                    return new ClaimRecord(current.entityUuid(), ClaimState.UNLOADED);
                }
                return current;
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        EntityClaimKey key = extractClaimProposal(entity);
        if (key == null) return;

        StructureRecord record = registry.getStructure(key.instanceId());
        if (record == null) return;

        // OBLIGATORIO: Usar event.getWorld() como mundo de origen fáctico de la remoción
        org.bukkit.World sourceWorld = event.getWorld();
        if (sourceWorld == null || !sourceWorld.getName().equals(record.worldName())) {
            // La entidad fue removida de un mundo foráneo; no liberar el claim del mundo oficial de la estructura
            return;
        }

        boolean[] wasFreed = new boolean[]{false};
        activeClaims.computeIfPresent(key, (k, current) -> {
            if (!current.entityUuid().equals(entity.getUniqueId())) {
                return current;
            }

            // Si ya estaba marcado como UNLOADED por EntitiesUnloadEvent, preservar la reserva
            if (current.state() == ClaimState.UNLOADED) {
                return current;
            }

            // Si se remueve mientras estaba CLAIMED en un chunk activo: muerte física, /kill, CHANGED_DIMENSION o descarte
            wasFreed[0] = true;
            return null; // Liberar slot a RECLAIMABLE
        });

        if (wasFreed[0]) {
            // Disparar reconciliación explícita con chunk todavía cargado en el mundo legítimo
            scheduleStructureReconciliation(record);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        EntityClaimKey key = extractClaimProposal(entity);
        if (key != null) {
            ClaimRecord removed = activeClaims.remove(key); // Liberar slot a RECLAIMABLE
            if (removed != null) {
                StructureRecord record = registry.getStructure(key.instanceId());
                if (record != null) {
                    scheduleStructureReconciliation(record);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String worldName = event.getWorld().getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();

        // 1. Consulta espacial O(1) vía chunkIndex
        Set<UUID> structureIds = registry.getStructureIdsInChunk(worldName, cx, cz);
        if (structureIds.isEmpty()) {
            return;
        }

        // FASE 1: Descubrimiento inmediato de entidades presentes en el chunk NBT
        for (Entity entity : chunk.getEntities()) {
            evaluateEntityClaim(entity);
        }

        // FASE 2: Reconciliación diferida (1 tick después en el scheduler regional del chunk)
        for (UUID id : structureIds) {
            StructureRecord record = registry.getStructure(id);
            if (record == null) continue;
            scheduleStructureReconciliation(record);
        }
    }

    public void scheduleStructureReconciliation(StructureRecord record) {
        if (record == null) return;
        Location loc = record.toLocation();
        if (loc == null || loc.getWorld() == null) return;

        Bukkit.getRegionScheduler().runDelayed(plugin, loc, task -> {
            reconcileMissingSlots(record);
        }, 1L);
    }

    public void reconcileMissingSlots(StructureRecord record) {
        if (record == null || registry.getStructure(record.instanceId()) == null) {
            return; // Estructura eliminada de disco
        }

        for (EntityRole role : EntityRole.values()) {
            EntityClaimKey key = new EntityClaimKey(record.instanceId(), role);
            UUID reservationToken = UUID.randomUUID();
            ClaimRecord prior = activeClaims.putIfAbsent(key, new ClaimRecord(reservationToken, ClaimState.RESERVED));
            if (prior != null) {
                // Ya existe claim activo (CLAIMED), descargado (UNLOADED) o reservado por otra reconciliación (RESERVED)
                continue;
            }

            // Reserva atómica garantizada para este hilo/región: exactamente 1 spawn físico
            boolean spawned = false;
            try {
                structureManager.spawnPhysicalSlot(record, role);
                spawned = true;
            } finally {
                if (!spawned) {
                    // Si el spawn no se completó (excepción/error), liberar la reserva para reintento futuro
                    activeClaims.remove(key, new ClaimRecord(reservationToken, ClaimState.RESERVED));
                }
            }
        }
    }

    public boolean reserveSlotForSpawn(UUID instanceId, EntityRole role, UUID reservationToken) {
        if (instanceId == null || role == null || reservationToken == null) return false;
        EntityClaimKey key = new EntityClaimKey(instanceId, role);
        ClaimRecord prior = activeClaims.putIfAbsent(key, new ClaimRecord(reservationToken, ClaimState.RESERVED));
        return prior == null;
    }

    public void releaseStructureClaims(UUID instanceId) {
        if (instanceId == null) return;
        for (EntityRole role : EntityRole.values()) {
            activeClaims.remove(new EntityClaimKey(instanceId, role));
        }
    }

    public EntityClaimKey extractClaimProposal(Entity entity) {
        if (entity == null) return null;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(KEY_INSTANCE_ID, PersistentDataType.STRING) 
                || !pdc.has(KEY_ENTITY_ROLE, PersistentDataType.STRING)) {
            return null;
        }
        try {
            UUID instanceId = UUID.fromString(pdc.get(KEY_INSTANCE_ID, PersistentDataType.STRING));
            EntityRole role = EntityRole.valueOf(pdc.get(KEY_ENTITY_ROLE, PersistentDataType.STRING));
            return new EntityClaimKey(instanceId, role);
        } catch (Exception e) {
            return null;
        }
    }

    private ClaimOutcome handleLegacyMigration(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        StructureType inferredType = null;
        EntityRole role = (entity instanceof TextDisplay) ? EntityRole.HOLOGRAM : 
                          (entity instanceof Interaction) ? EntityRole.INTERACTION : null;

        if (role == null) return ClaimOutcome.IGNORED;

        if (pdc.has(com.dark.roostergenics.npc.NurseNPC.NURSE_NPC_KEY, PersistentDataType.STRING)) {
            inferredType = StructureType.ROOSTER_CENTER;
        } else if (pdc.has(com.dark.roostergenics.npc.MerchantNPC.MERCHANT_NPC_KEY, PersistentDataType.STRING)) {
            inferredType = StructureType.MERCHANT_SHOP;
        } else if (pdc.has(com.dark.roostergenics.gym.GymLeaderNPC.GYM_LEADER_KEY, PersistentDataType.STRING)) {
            inferredType = StructureType.GYM;
        }

        if (inferredType == null) return ClaimOutcome.IGNORED;

        // Búsqueda determinista de estructuras existentes cercanas (< 3.5 bloques)
        List<StructureRecord> candidates = new ArrayList<>();
        for (StructureRecord r : registry.getAllStructures()) {
            if (r.type() == inferredType && r.distanceSquared(entity.getLocation()) <= 12.25) {
                candidates.add(r);
            }
        }

        if (candidates.size() == 1) {
            // EXACTAMENTE 1 CANDIDATO: Migración exitosa adoptando la estructura
            StructureRecord matched = candidates.get(0);
            tagEntityPdc(entity, matched.instanceId(), role, matched.type());
            return evaluateEntityClaim(entity);
        } else if (candidates.isEmpty()) {
            // 0 CANDIDATOS: NO crear estructura automáticamente sin evidencia suficiente
            plugin.getLogger().info("Entidad legacy sin estructura autorizada en " + entity.getLocation() + ". No se adopta automáticamente.");
            return ClaimOutcome.IGNORED;
        } else {
            // 2+ CANDIDATOS: MIGRATION_AMBIGUOUS. NO eliminar silenciosamente.
            plugin.getLogger().warning("MIGRATION_AMBIGUOUS: Entidad en " + entity.getLocation() + " coincide con " + candidates.size() + " estructuras cercanas. Se preserva sin adopción.");
            return ClaimOutcome.IGNORED;
        }
    }

    public static void tagEntityPdc(Entity entity, UUID instanceId, EntityRole role, StructureType type) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(KEY_INSTANCE_ID, PersistentDataType.STRING, instanceId.toString());
        pdc.set(KEY_ENTITY_ROLE, PersistentDataType.STRING, role.name());
        pdc.set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, type.name());
    }

    public void despawnEntity(Entity entity) {
        if (entity == null) return;
        entity.getScheduler().run(plugin, task -> {
            if (entity.isValid()) {
                entity.remove();
            }
        }, () -> {
            // Retired callback de Paper: la entidad ya está siendo retirada/eliminada. No-op seguro.
        });
    }
}
