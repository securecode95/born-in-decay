package com.rabalder.bornindecay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Simple chunk loader / cache around a seed. */
public class WorldGenerator {
    private final long seed;
    private final Map<Long,Chunk> chunks = new HashMap<>();

    public WorldGenerator(long seed) {
        this.seed = seed;
    }

    /**
     * Ensures that only the (2*radius+1)^2 chunks centered on (centerX,centerZ)
     * remain loaded.  Any new positions get new Chunk(seed,x,z).
     */
    public void update(int centerX, int centerZ, int radius) {
        Map<Long,Chunk> old = new HashMap<>(chunks);
        chunks.clear();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                long key = (((long)x)<<32) | (z & 0xffffffffL);
                chunks.put(
                    key,
                    old.computeIfAbsent(
                        key,
                        k -> new Chunk(seed, x, z)
                    )
                );
            }
        }
    }

    /** Used by WorldManager to rebuild meshes & collisions */
    public List<Chunk> getLoadedChunks() {
        return new ArrayList<>(chunks.values());
    }
}
