package com.hypixel.hytale.event;

import java.util.function.Consumer;

/**
 * Stub interface for Hytale EventRegistry API.
 */
public interface EventRegistry {

    <E> void register(EventPriority priority, Class<E> eventClass, Consumer<E> handler);

    <E> void registerGlobal(EventPriority priority, Class<E> eventClass, Consumer<E> handler);
}
