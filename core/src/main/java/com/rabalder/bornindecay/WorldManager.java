package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

public class WorldManager {

    private final WorldGenerator generator;

    public WorldManager(Model grassModel, Model soilModel) {
        this.generator = new WorldGenerator(grassModel, soilModel, System.currentTimeMillis(), 2);
    }

    public void update(Vector3 playerPosition) {
        generator.update(playerPosition);
    }

    // ✅ Returns all visible chunk meshes (used for rendering)
    public List<ModelInstance> getChunkMeshes() {
        List<ModelInstance> meshes = new ArrayList<>();
        for (Chunk chunk : generator.getActiveChunks()) {
            if (chunk.meshInstance != null) {
                meshes.add(chunk.meshInstance);
            }
        }
        return meshes;
    }
}
