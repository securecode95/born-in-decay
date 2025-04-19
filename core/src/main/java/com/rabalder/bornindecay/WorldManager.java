package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps exactly one ModelInstance per loaded Chunk,
 * and produces a flat List<ModelInstance> + List<Vector3> for rendering & collision.
 */
public class WorldManager {
    private static final int RADIUS = 3;

    private final WorldGenerator       generator;
    private final ChunkMeshBuilder     meshBuilder;
    private final Map<Chunk,ModelInstance> chunkInstances = new HashMap<>();

    /** Now takes a seed so you don’t get “no-arg vs long” errors */
    public WorldManager(long seed) {
        this.generator   = new WorldGenerator(seed);
        this.meshBuilder = new ChunkMeshBuilder();
    }

    /**
     * Regenerates the set of loaded Chunks around player, rebuilds meshes.
     */
    public void update(Vector3 playerPos) {
        int cx = (int)Math.floor(playerPos.x / Chunk.SIZE);
        int cz = (int)Math.floor(playerPos.z / Chunk.SIZE);

        generator.update(cx, cz, RADIUS);

        // rebuild our map of ModelInstances
        chunkInstances.clear();
        for (Chunk chunk : generator.getLoadedChunks()) {
            ModelInstance mi = meshBuilder.buildChunkMesh(chunk);
            chunkInstances.put(chunk, mi);
        }
    }

    /** What to draw this frame */
    public List<ModelInstance> getChunkMeshes() {
        return new ArrayList<>(chunkInstances.values());
    }

    /** Build a flat list of all non‑air block centers for collision detection */
    public List<Vector3> getCollisionVoxels() {
        List<Vector3> voxels = new ArrayList<>();
        for (Chunk chunk : generator.getLoadedChunks()) {
            int baseX = chunk.getChunkX() * Chunk.SIZE;
            int baseZ = chunk.getChunkZ() * Chunk.SIZE;
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        if (chunk.getBlock(x,y,z) != BlockType.AIR) {
                            voxels.add(new Vector3(baseX+x, y, baseZ+z));
                        }
                    }
                }
            }
        }
        return voxels;
    }
}
