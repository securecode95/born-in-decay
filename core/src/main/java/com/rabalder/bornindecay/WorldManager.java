package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

public class WorldManager {

    private final WorldGenerator generator;

    public WorldManager(Model grassModel, Model soilModel) {
        long seed = System.currentTimeMillis();  // Set the seed value here, you can use a fixed value or current time
        this.generator = new WorldGenerator(seed);  // Pass the seed to WorldGenerator constructor
    }

    public void update(Vector3 playerPosition) {
        generator.update(playerPosition);  // Update the world (chunks and meshes)
    }

    // ✅ Returns all visible chunk meshes (used for rendering)
    public List<ModelInstance> getChunkMeshes() {
        return generator.getVisibleChunks();
    }

}
