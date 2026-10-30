package org.solocode.automotion.Data;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockPositionTest {

    @Test
    void negativeCoordinatesMapToTheCorrectChunk() {
        BlockPosition position = new BlockPosition(UUID.randomUUID(), -1, 64, -17);

        assertEquals(-1, position.chunkX());
        assertEquals(15, position.localX());
        assertEquals(-2, position.chunkZ());
        assertEquals(15, position.localZ());
    }

    @Test
    void serializationRoundTrips() {
        UUID world = UUID.randomUUID();
        BlockPosition position = new BlockPosition(world, 12, -40, 300);

        Optional<BlockPosition> parsed = BlockPosition.parse(world, position.asString());

        assertTrue(parsed.isPresent());
        assertEquals(position, parsed.get());
    }

    @Test
    void malformedValuesAreRejected() {
        UUID world = UUID.randomUUID();

        assertTrue(BlockPosition.parse(world, "1,2").isEmpty());
        assertTrue(BlockPosition.parse(world, "a,b,c").isEmpty());
        assertTrue(BlockPosition.parse(world, null).isEmpty());
    }
}
