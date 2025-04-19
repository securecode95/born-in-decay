package com.rabalder.bornindecay;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import java.util.ArrayList;
import java.util.List;

public class WorldManager {
    private final WorldGenerator gen = new WorldGenerator(12345L);
    private final ChunkMeshBuilder meshBuilder = new ChunkMeshBuilder();
    private final List<ModelInstance> instances = new ArrayList<>();

    public WorldManager() {
        // generate a single chunk at (0,0)
        gen.generateAt(0,0);
        rebuildMeshes();
    }

    /** Call whenever the player moves far enough */
    public void update(Vector3 playerPos) {
        // (we only ever have one chunk here)
        rebuildMeshes();
    }

    private void rebuildMeshes() {
        instances.clear();
        for (Chunk c : gen.getLoadedChunks()) {
            instances.add(meshBuilder.buildChunkMesh(c));
        }
    }

    public List<ModelInstance> getChunkMeshes() {
        return instances;
    }

    public List<Vector3> getCollisionVoxels() {
        return gen.getCollisionVoxels();
    }
}
