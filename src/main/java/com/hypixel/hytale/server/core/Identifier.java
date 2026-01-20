package com.hypixel.hytale.server.core;

/**
 * Stub for Hytale server API - Identifier.
 * This file is provided for compilation purposes only.
 * The actual implementation is provided by the Hytale server at runtime.
 */
public interface Identifier {
    String getNamespace();
    String getPath();

    @Override
    String toString();
}
