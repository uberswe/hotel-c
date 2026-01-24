package com.hypixel.hytale.component.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;

/**
 * Stub class for Hytale EntityEventSystem.
 * Based on actual Hytale server API from SimpleClaims analysis.
 */
public abstract class EntityEventSystem<S, E> implements ISystem {

    protected EntityEventSystem(Class<E> eventClass) {
    }

    public abstract Query<S> getQuery();

    public abstract void handle(int entityId, ArchetypeChunk<S> chunk, Store<S> store,
                                CommandBuffer<S> commandBuffer, E event);
}
