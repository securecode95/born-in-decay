package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

public class WorldGenerator {
    private final Map<Vector2, Chunk> activeChunks = new HashMap<>();
    private final Model blockModel;
    private final OpenSimplexNoise noise;

    public final List<ModelInstance> visibleBlocks = new ArrayList<>();
    private final int viewDistance = 2;
    private final int maxTerrainHeight = 6;

    public WorldGenerator(Model blockModel, long seed) {
        this.blockModel = blockModel;
        this.noise = new OpenSimplexNoise(seed);
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
                    Chunk newChunk = generateChunk(chunkX, chunkZ);
                    activeChunks.put(chunkCoord, newChunk);
                    addChunkBlocksToVisible(newChunk);
                }
            }
        }

        // Unload chunks outside view distance
        Iterator<Map.Entry<Vector2, Chunk>> iterator = activeChunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Vector2, Chunk> entry = iterator.next();
            if (!requiredChunks.contains(entry.getKey())) {
                removeChunkBlocksFromVisible(entry.getValue());
                iterator.remove();
            }
        }
    }

    private Chunk generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(blockModel, chunkX * Chunk.SIZE, 0, chunkZ * Chunk.SIZE);
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int worldX = chunkX * Chunk.SIZE + x;
                int worldZ = chunkZ * Chunk.SIZE + z;

                double heightNoise = noise.eval(worldX * 0.1, worldZ * 0.1);
                int height = (int) (heightNoise * maxTerrainHeight + maxTerrainHeight);

                for (int y = 0; y <= height; y++) {
                    ModelInstance block = new ModelInstance(blockModel);
                    block.transform.setToTranslation(worldX, y, worldZ);
                    chunk.blocks[x][y][z] = block;
                }
            }
        }
        return chunk;
    }

    private void addChunkBlocksToVisible(Chunk chunk) {
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    ModelInstance block = chunk.blocks[x][y][z];
                    if (block != null) {
                        visibleBlocks.add(block);
                    }
                }
            }
        }
    }

    private void removeChunkBlocksFromVisible(Chunk chunk) {
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    ModelInstance block = chunk.blocks[x][y][z];
                    if (block != null) {
                        visibleBlocks.remove(block);
                    }
                }
            }
        }
    }
}
