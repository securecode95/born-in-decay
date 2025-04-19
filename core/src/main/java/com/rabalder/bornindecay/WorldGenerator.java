package com.rabalder.bornindecay;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Simple heightmap + stone below. */
public class WorldGenerator {
    private final long seed;
    private final Random rnd;
    private final List<Chunk> chunks = new ArrayList<>();

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.rnd  = new Random(seed);
    }

    public List<Chunk> getLoadedChunks() {
        return chunks;
    }

    public void generateAt(int cx, int cz) {
        // one flat chunk at origin only:
        chunks.clear();
        Chunk c = new Chunk();
        for (int x=0; x<Chunk.SIZE; x++) {
            for (int z=0; z<Chunk.SIZE; z++) {
                // simple hill:
                float h = 8 + 4 * (float)Math.sin((x+cx*16)*.2)*.5f;
                int height = 4 + (int)h;
                for (int y=0; y<height; y++) {
                    c.setBlock(x,y,z,
                        y==height-1 ? BlockType.GRASS
                            : y>2          ? BlockType.DIRT
                            : BlockType.STONE);
                }
            }
        }
        chunks.add(c);
    }

    /** Produce a flat list of world‐space collision voxels. */
    public List<Vector3> getCollisionVoxels() {
        List<Vector3> out = new ArrayList<>();
        for (int i=0; i<chunks.size(); i++) {
            Chunk c = chunks.get(i);
            for (int x=0; x<Chunk.SIZE; x++)
                for (int y=0; y<Chunk.SIZE; y++)
                    for (int z=0; z<Chunk.SIZE; z++) {
                        if (c.getBlock(x,y,z)!=BlockType.AIR) {
                            out.add(new Vector3(x, y, z));
                        }
                    }
        }
        return out;
    }
}
