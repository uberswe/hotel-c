package com.hypixel.hytale.event;

import java.util.function.Consumer;

/**
 * Stub class for Hytale EventRegistry API.
 */
public class EventRegistry {

    public <E> void register(EventPriority priority, Class<E> eventClass, Consumer<E> handler) {
        throw new UnsupportedOperationException("Stub");
    }

    public <E> void registerGlobal(EventPriority priority, Class<E> eventClass, Consumer<E> handler) {
        throw new UnsupportedOperationException("Stub");
    }
}
