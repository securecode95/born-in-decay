package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import java.util.List;

public class WorldManager {
    private final WorldGenerator gen;

    /** No-arg constructor now seeds the generator properly. */
    public WorldManager() {
        this.gen = new WorldGenerator(System.currentTimeMillis());
    }

    /** Generate/unload terrain around the player. */
    public void update(Vector3 playerPosition) {
        gen.update(playerPosition);
    }

    /** What to render this frame. */
    public List<ModelInstance> getChunkMeshes() {
        // switched from getLoadedChunks() to getVisibleChunks()
        return gen.getVisibleChunks();
    }

    /** What to collide against this frame. */
    public List<Vector3> getCollisionVoxels() {
        return gen.getCollisionVoxels();
    }
}
