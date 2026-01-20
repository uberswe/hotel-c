package com.uberswe.hytale.otel.ecs;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

/**
 * ECS event system that handles block break events.
 */
public class BlockBreakEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final AttributeKey<String> BLOCK_TYPE = AttributeKey.stringKey("block.type");
    private static final AttributeKey<Long> BLOCK_X = AttributeKey.longKey("block.x");
    private static final AttributeKey<Long> BLOCK_Y = AttributeKey.longKey("block.y");
    private static final AttributeKey<Long> BLOCK_Z = AttributeKey.longKey("block.z");

    private final TelemetryManager telemetryManager;

    public BlockBreakEventSystem(TelemetryManager telemetryManager) {
        super(BreakBlockEvent.class);
        this.telemetryManager = telemetryManager;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int entityId, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer, BreakBlockEvent event) {
        try {
            var targetBlock = event.getTargetBlock();
            var blockType = event.getBlockType();

            String blockTypeName = "unknown";
            if (blockType != null) {
                blockTypeName = blockType.getId();
            }

            var attributes = Attributes.builder()
                    .put(BLOCK_TYPE, blockTypeName)
                    .put(BLOCK_X, (long) targetBlock.x)
                    .put(BLOCK_Y, (long) targetBlock.y)
                    .put(BLOCK_Z, (long) targetBlock.z)
                    .build();

            telemetryManager.recordBlockBroken(attributes);
        } catch (Exception e) {
            // Silently ignore errors to not disrupt game
        }
    }
}
