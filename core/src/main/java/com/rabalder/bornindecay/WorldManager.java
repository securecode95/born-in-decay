package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

import java.util.ArrayList;
import java.util.HashMap;

public class WorldManager {
    public static final int CHUNK_SIZE = 16;
    public static final int RENDER_DISTANCE = 2;

    private final HashMap<String, Chunk> chunks = new HashMap<>();
    private final Model grassModel;
    private final Model soilModel;

    public WorldManager(Model grassModel, Model soilModel) {
        this.grassModel = grassModel;
        this.soilModel = soilModel;
    }

    public ArrayList<ModelInstance> getVisibleBlocks(Vector3 playerPos) {
        ArrayList<ModelInstance> visible = new ArrayList<>();

        int currentChunkX = (int) Math.floor(playerPos.x / CHUNK_SIZE);
        int currentChunkZ = (int) Math.floor(playerPos.z / CHUNK_SIZE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = currentChunkX + dx;
                int chunkZ = currentChunkZ + dz;

                String key = chunkKey(chunkX, chunkZ);
                Chunk chunk = chunks.get(key);

                if (chunk == null) {
                    chunk = generateChunk(chunkX, 0, chunkZ);
                    chunks.put(key, chunk);
                }

                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int y = 0; y < CHUNK_SIZE; y++) {
                        for (int z = 0; z < CHUNK_SIZE; z++) {
                            ModelInstance instance = chunk.blocks[x][y][z];
                            if (instance != null) {
                                visible.add(instance);
                            }
                        }
                    }
                }
            }
        }

        return visible;
    }

    private Chunk generateChunk(int chunkX, int chunkY, int chunkZ) {
        Chunk chunk = new Chunk(grassModel, chunkX * CHUNK_SIZE, chunkY * CHUNK_SIZE, chunkZ * CHUNK_SIZE);
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = chunkX * CHUNK_SIZE + x;
                int worldZ = chunkZ * CHUNK_SIZE + z;

                // Simple height function (random for now)
                int height = (int) (Math.random() * 4 + 1); // 1-4 blocks high

                for (int y = 0; y < height; y++) {
                    Model model = y == height - 1 ? grassModel : soilModel;
                    chunk.setBlock(x, y, z, new ModelInstance(model));
                }
            }
        }
        return chunk;
    }

    private String chunkKey(int x, int z) {
        return x + "," + z;
    }
}
