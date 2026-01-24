package com.hypixel.hytale.component.query;

/**
 * Stub interface for Hytale Query.
 * Based on actual Hytale server API from SimpleClaims analysis.
 */
public interface Query<T> {

    static <T> Query<T> any() {
        return new Query<T>() {};
    }
}
