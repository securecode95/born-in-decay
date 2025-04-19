package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

/**
 * Generates and maintains chunks, their greedy‑meshed ModelInstances,
 * and provides a list of non‑AIR voxel positions for collision.
 */
public class WorldGenerator {
    private final Map<Vector2, Chunk> activeChunks = new HashMap<>();
    private final Map<Vector2, ModelInstance> chunkMeshes = new HashMap<>();

    private final OpenSimplexNoise noise;
    private final long seed;
    private final int viewDistance = 2;
    private final int maxTerrainHeight = 8;

    public WorldGenerator(long seed) {
        this.seed  = seed;
        this.noise = new OpenSimplexNoise();
    }

    /**
     * Call each frame with the player’s world‑space position.
     * Generates new chunks in view, greedy‑meshes them, and
     * stores the ModelInstance back onto the Chunk for rendering.
     */
    public void update(Vector3 playerPosition) {
        int currentChunkX = (int)Math.floor(playerPosition.x / Chunk.SIZE);
        int currentChunkZ = (int)Math.floor(playerPosition.z / Chunk.SIZE);

        Set<Vector2> required = new HashSet<>();
        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                int chunkX = currentChunkX + dx;
                int chunkZ = currentChunkZ + dz;
                Vector2 coord = new Vector2(chunkX, chunkZ);
                required.add(coord);

                if (!activeChunks.containsKey(coord)) {
                    // 1) generate raw blocks
                    Chunk newChunk = generateChunk(chunkX, chunkZ);
                    activeChunks.put(coord, newChunk);

                    // 2) greedy‑mesh into a ModelInstance
                    ChunkMeshBuilder builder = new ChunkMeshBuilder();
                    ModelInstance mesh = builder.buildChunkMesh(newChunk);
                    chunkMeshes.put(coord, mesh);

                    // 3) hook it back onto the chunk so WorldManager finds it
                    newChunk.meshInstance = mesh;
                }
            }
        }

        // unload chunks that moved out of view
        Iterator<Vector2> it = activeChunks.keySet().iterator();
        while (it.hasNext()) {
            Vector2 coord = it.next();
            if (!required.contains(coord)) {
                it.remove();
                chunkMeshes.remove(coord);
            }
        }
    }

    private Chunk generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk();
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int worldX = chunkX * Chunk.SIZE + x;
                int worldZ = chunkZ * Chunk.SIZE + z;
                double n = OpenSimplexNoise.noise2(seed, worldX * 0.1, worldZ * 0.1);
                int height = (int)(n * maxTerrainHeight + maxTerrainHeight / 2f);
                height = Math.min(height, Chunk.SIZE - 1);

                for (int y = 0; y <= height; y++) {
                    byte type = (y == height) ? Chunk.GRASS : Chunk.SOIL;
                    chunk.setBlock(x, y, z, type);
                }
            }
        }
        return chunk;
    }

    /** Returns the list of all chunk ModelInstances for rendering. */
    public List<ModelInstance> getVisibleChunks() {
        return new ArrayList<>(chunkMeshes.values());
    }

    /** Returns the active chunks themselves (if you need them). */
    public Collection<Chunk> getActiveChunks() {
        return activeChunks.values();
    }

    /**
     * Returns world‐space centers of every non‐AIR voxel
     * in all active chunks. Used for precise collision.
     */
    public List<Vector3> getCollisionVoxels() {
        List<Vector3> voxels = new ArrayList<>();
        for (Map.Entry<Vector2,Chunk> e : activeChunks.entrySet()) {
            int chunkX = (int)e.getKey().x;
            int chunkZ = (int)e.getKey().y;
            Chunk c = e.getValue();
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        if (c.blocks[x][y][z] != Chunk.AIR) {
                            // center of this voxel in world coordinates
                            voxels.add(new Vector3(
                                chunkX*Chunk.SIZE + x + 0.5f,
                                y + 0.5f,
                                chunkZ*Chunk.SIZE + z + 0.5f
                            ));
                        }
                    }
                }
            }
        }
        return voxels;
    }
}
