package com.rabalder.bornindecay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldGenerator {
    private final long seed;
    private final Map<Long, Chunk> chunks = new HashMap<>();

    public WorldGenerator(long seed) {
        this.seed = seed;
    }

    /**
     * Ensure all chunks within `radius` of the player (px,pz) are generated.
     */
    public void update(float px, float pz, int radius) {
        // which chunk the player is in
        int cx = (int)Math.floor(px / Chunk.SIZE);
        int cz = (int)Math.floor(pz / Chunk.SIZE);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // final per‑iteration coords, so no lambda capture issue:
                int fx = cx + dx;
                int fz = cz + dz;
                long key = (((long)fx) << 32) | (fz & 0xffffffffL);

                if (!chunks.containsKey(key)) {
                    chunks.put(key, new Chunk(seed, fx, fz));
                }
            }
        }
    }

    /** Returns all currently loaded chunks. */
    public List<Chunk> getLoadedChunks() {
        return new ArrayList<>(chunks.values());
    }
}
