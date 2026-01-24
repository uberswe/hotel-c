package com.uberswe.hytale.otel.ecs;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

/**
 * ECS event system that handles block use/interaction events.
 * Uses the Post event to record successful interactions.
 */
public class BlockUseEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {
    private static final AttributeKey<String> BLOCK_TYPE = AttributeKey.stringKey("block.type");
    private static final AttributeKey<String> INTERACTION_TYPE = AttributeKey.stringKey("interaction.type");
    private static final AttributeKey<Long> BLOCK_X = AttributeKey.longKey("block.x");
    private static final AttributeKey<Long> BLOCK_Y = AttributeKey.longKey("block.y");
    private static final AttributeKey<Long> BLOCK_Z = AttributeKey.longKey("block.z");

    private final TelemetryManager telemetryManager;

    public BlockUseEventSystem(TelemetryManager telemetryManager) {
        super(UseBlockEvent.Post.class);
        this.telemetryManager = telemetryManager;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Return null - getQuery is @Nullable per SimpleClaims analysis
        return null;
    }

    @Override
    public void handle(int entityId, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer, UseBlockEvent.Post event) {
        try {
            var targetBlock = event.getTargetBlock();
            var blockType = event.getBlockType();
            var interactionType = event.getInteractionType();

            String blockTypeName = "unknown";
            if (blockType != null) {
                blockTypeName = blockType.getId();
            }

            String interactionTypeName = "unknown";
            if (interactionType != null) {
                interactionTypeName = interactionType.name();
            }

            var attributes = Attributes.builder()
                    .put(BLOCK_TYPE, blockTypeName)
                    .put(INTERACTION_TYPE, interactionTypeName)
                    .put(BLOCK_X, (long) targetBlock.x)
                    .put(BLOCK_Y, (long) targetBlock.y)
                    .put(BLOCK_Z, (long) targetBlock.z)
                    .build();

            telemetryManager.recordBlockInteraction(attributes);
        } catch (Exception e) {
            // Silently ignore errors to not disrupt game
        }
    }
}
