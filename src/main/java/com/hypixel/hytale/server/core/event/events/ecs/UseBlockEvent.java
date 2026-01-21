package com.hypixel.hytale.server.core.event.events.ecs;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

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
