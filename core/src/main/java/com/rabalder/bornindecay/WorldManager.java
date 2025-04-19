package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldManager {
    private static final int VIEW_DISTANCE = 2;
    private final WorldGenerator generator;
    private final Map<Integer, ModelInstance> chunkInstances = new HashMap<>();

    /** No‑arg constructor defaults to seed=0L */
    public WorldManager() {
        this(0L);
    }

    /** Seeded constructor */
    public WorldManager(long seed) {
        this.generator = new WorldGenerator(seed);
    }

    /** Must be called each frame with the player/camera position */
    public void update(Vector3 position) {
        int cx = Math.floorDiv((int)position.x, Chunk.SIZE);
        int cz = Math.floorDiv((int)position.z, Chunk.SIZE);

        // generate & mesh all chunks in view distance
        for (int dx = -VIEW_DISTANCE; dx <= VIEW_DISTANCE; dx++) {
            for (int dz = -VIEW_DISTANCE; dz <= VIEW_DISTANCE; dz++) {
                Chunk c = generator.getOrCreateChunk(cx + dx, cz + dz);
                int key = System.identityHashCode(c);
                if (!chunkInstances.containsKey(key)) {
                    ModelInstance mi = new ChunkMeshBuilder().buildChunkMesh(c);
                    chunkInstances.put(key, mi);
                }
            }
        }
    }

    /** Returns all chunk ModelInstances to render */
    public List<ModelInstance> getChunkMeshes() {
        return List.copyOf(chunkInstances.values());
    }

    /** Returns world‐space positions of every solid voxel for collision */
    public List<Vector3> getCollisionVoxels() {
        return generator.getCollisionVoxels();
    }
}
