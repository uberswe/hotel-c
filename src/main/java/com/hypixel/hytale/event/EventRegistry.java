package com.hypixel.hytale.event;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Stub class for Hytale EventRegistry API.
 * Based on actual Hytale server API from ProximityCore and SimpleClaims analysis.
 */
public class EventRegistry {

    /**
     * Register a handler for non-keyed events.
     */
    public <E> EventRegistration register(Class<E> eventClass, Consumer<E> handler) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Register a global handler for keyed events (listens across all keys).
     */
    public <E> EventRegistration registerGlobal(Class<E> eventClass, Consumer<E> handler) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Register an async global handler for keyed events.
     */
    public <E> EventRegistration registerAsyncGlobal(Class<E> eventClass, Function<E, CompletableFuture<Void>> handler) {
        throw new UnsupportedOperationException("Stub");
    }
}