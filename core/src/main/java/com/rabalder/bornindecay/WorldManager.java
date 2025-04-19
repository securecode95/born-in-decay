package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldManager {
    private static final int RADIUS = 3;    // how many chunks out in each direction
    private final WorldGenerator generator;
    private final ChunkMeshBuilder meshBuilder;
    private final Map<Long, ModelInstance> chunkInstances = new HashMap<>();

    public WorldManager(long seed) {
        // seed your world generator
        this.generator    = new WorldGenerator(seed);
        this.meshBuilder  = new ChunkMeshBuilder();
    }

    /**
     * Regenerate chunks around the player, rebuild meshes.
     */
    public void update(Vector3 playerPosition) {

        // figure out which chunk the player is in
        int cx = (int) Math.floor(playerPosition.x / Chunk.SIZE);
        int cz = (int) Math.floor(playerPosition.z / Chunk.SIZE);

        // tell the generator to load/unload around us
        generator.update(cx, cz, RADIUS);

        // rebuild our ModelInstances map
        chunkInstances.clear();
        for (Chunk chunk : generator.getLoadedChunks()) {
            ModelInstance mi = meshBuilder.buildChunkMesh(chunk);
            // pack the chunk coords into one long key
            long key = (((long)chunk.getX()) << 32) | (chunk.getZ() & 0xffffffffL);
            chunkInstances.put(key, mi);
        }
    }

    /**
     * What to render.
     */
    public List<ModelInstance> getChunkMeshes() {
        return new ArrayList<>(chunkInstances.values());
    }

    /**
     * Build a list of world‐space block positions for collision.
     */
    public List<Vector3> getCollisionVoxels() {
        List<Vector3> voxels = new ArrayList<>();
        for (Chunk chunk : generator.getLoadedChunks()) {
            int baseX = chunk.getX() * Chunk.SIZE;
            int baseZ = chunk.getZ() * Chunk.SIZE;
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        if (chunk.getBlock(x, y, z) != BlockType.AIR) {
                            voxels.add(new Vector3(baseX + x, y, baseZ + z));
                        }
                    }
                }
            }
        }
        return voxels;
    }
}
