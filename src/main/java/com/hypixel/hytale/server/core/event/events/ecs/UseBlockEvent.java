package com.hypixel.hytale.server.core.event.events.ecs;

import com.hypixel.hytale.server.core.block.BlockType;
import com.hypixel.hytale.server.core.block.InteractionType;
import com.hypixel.hytale.server.core.block.Vector3i;

/**
 * Stub class for Hytale UseBlockEvent.
 */
public class UseBlockEvent {

    /**
     * Post event fired after a successful block interaction.
     */
    public static class Post {

        public Vector3i getTargetBlock() {
            throw new UnsupportedOperationException("Stub");
        }

        public BlockType getBlockType() {
            throw new UnsupportedOperationException("Stub");
        }

        public InteractionType getInteractionType() {
            throw new UnsupportedOperationException("Stub");
        }
    }
}
