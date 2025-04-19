// WorldGenerator.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

/** Procedurally fills each chunk with stone at the bottom, soil in the middle, and a grass roof */
public class WorldGenerator {
    private final Map<Vector2,Chunk>       activeChunks = new HashMap<>();
    private final Map<Vector2,ModelInstance> chunkMeshes = new HashMap<>();
    private final OpenSimplexNoise noise = new OpenSimplexNoise();
    private final long seed;
    private final int viewDistance   = 2;
    private final int maxTerrainHeight = 8;

    public WorldGenerator(long seed) {
        this.seed = seed;
    }

    /** call every frame with the player’s world‐space position */
    public void update(Vector3 playerPos) {
        int cx = (int)Math.floor(playerPos.x / Chunk.SIZE);
        int cz = (int)Math.floor(playerPos.z / Chunk.SIZE);
        Set<Vector2> needed = new HashSet<>();

        for (int dx=-viewDistance; dx<=viewDistance; dx++) {
            for (int dz=-viewDistance; dz<=viewDistance; dz++) {
                Vector2 coord = new Vector2(cx+dx, cz+dz);
                needed.add(coord);
                if (!activeChunks.containsKey(coord)) {
                    Chunk c = generateChunk(cx+dx, cz+dz);
                    activeChunks.put(coord, c);
                    ModelInstance mi = new ChunkMeshBuilder().buildChunkMesh(c);
                    chunkMeshes.put(coord, mi);
                }
            }
        }

        // unload far chunks
        activeChunks.keySet().removeIf(k -> !needed.contains(k));
        chunkMeshes.keySet().removeIf(k -> !needed.contains(k));
    }

    private Chunk generateChunk(int chunkX, int chunkZ) {
        Chunk c = new Chunk();
        for (int x=0; x<Chunk.SIZE; x++) {
            for (int z=0; z<Chunk.SIZE; z++) {
                int wx = chunkX*Chunk.SIZE + x;
                int wz = chunkZ*Chunk.SIZE + z;
                double n = OpenSimplexNoise.noise2(seed, wx*0.1, wz*0.1);
                int h = (int)(n*maxTerrainHeight + maxTerrainHeight/2f);
                h = Math.min(Math.max(h,0), Chunk.SIZE-1);

                for (int y=0; y<=h; y++) {
                    byte type;
                    if (y == h)                type = BlockType.GRASS;
                    else if (y < 2)           type = BlockType.STONE;
                    else                       type = BlockType.SOIL;
                    c.setBlock(x, y, z, type);
                }
            }
        }
        return c;
    }

    public List<ModelInstance> getVisibleChunks() {
        return new ArrayList<>(chunkMeshes.values());
    }
}
