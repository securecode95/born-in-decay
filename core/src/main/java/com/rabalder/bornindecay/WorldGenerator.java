package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

public class WorldGenerator {
    private final Map<Vector2, Chunk> activeChunks = new HashMap<>();
    private final Map<Vector2, ModelInstance> chunkMeshes = new HashMap<>();
    private final OpenSimplexNoise noise;
    private final long seed;
    private final int viewDistance = 2;
    private final int maxTerrainHeight = 8;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.noise = new OpenSimplexNoise();
    }

    public void update(Vector3 playerPosition) {
        int currentChunkX = (int) Math.floor(playerPosition.x / Chunk.SIZE);
        int currentChunkZ = (int) Math.floor(playerPosition.z / Chunk.SIZE);

        Set<Vector2> requiredChunks = new HashSet<>();

        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                int chunkX = currentChunkX + dx;
                int chunkZ = currentChunkZ + dz;
                Vector2 chunkCoord = new Vector2(chunkX, chunkZ);
                requiredChunks.add(chunkCoord);

                if (!activeChunks.containsKey(chunkCoord)) {
                    // 1) Generate and store the raw chunk
                    Chunk newChunk = generateChunk(chunkX, chunkZ);
                    activeChunks.put(chunkCoord, newChunk);

                    // 2) Build its greedy‐meshed ModelInstance
                    ChunkMeshBuilder builder = new ChunkMeshBuilder();
                    ModelInstance mesh = builder.buildChunkMesh(newChunk);

                    // 3) Keep a map of meshes if you still need getVisibleChunks()
                    chunkMeshes.put(chunkCoord, mesh);

                    // 4) **HOOK** the mesh back onto the chunk itself:
                    newChunk.meshInstance = mesh;
                }
            }
        }

        // Unload chunks that moved out of view
        Iterator<Vector2> it = activeChunks.keySet().iterator();
        while (it.hasNext()) {
            Vector2 coord = it.next();
            if (!requiredChunks.contains(coord)) {
                activeChunks.remove(coord);
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

                double heightNoise = OpenSimplexNoise.noise2(seed, worldX * 0.1, worldZ * 0.1);
                int height = (int) (heightNoise * maxTerrainHeight + maxTerrainHeight / 2f);
                height = Math.min(height, Chunk.SIZE - 1); // Prevent going out of bounds

                for (int y = 0; y <= height; y++) {
                    byte type = (y == height) ? Chunk.GRASS : Chunk.SOIL;
                    chunk.setBlock(x, y, z, type);
                }
            }
        }

        return chunk;
    }

    // This method returns the visible chunks with greedy meshing applied
    public List<ModelInstance> getVisibleChunks() {
        return new ArrayList<>(chunkMeshes.values());
    }

    // This method is to access active chunks if needed (e.g., for further management)
    public Collection<Chunk> getActiveChunks() {
        return activeChunks.values();
    }
}
