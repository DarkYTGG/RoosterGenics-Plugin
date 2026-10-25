package com.dark.roostergenics.structure;

import java.util.Objects;
import java.util.UUID;

/**
 * Clave lógica única para un slot de entidad en una estructura persistente.
 */
public record EntityClaimKey(UUID instanceId, EntityRole role) {
    public EntityClaimKey {
        Objects.requireNonNull(instanceId, "instanceId no puede ser null");
        Objects.requireNonNull(role, "role no puede ser null");
    }
}
