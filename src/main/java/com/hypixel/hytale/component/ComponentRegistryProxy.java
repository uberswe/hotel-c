package com.hypixel.hytale.component;

import com.hypixel.hytale.component.system.EntityEventSystem;

/**
 * Stub interface for Hytale ComponentRegistryProxy (EntityStoreRegistry).
 */
public interface ComponentRegistryProxy {

    void registerSystem(EntityEventSystem<?, ?> system);
}
