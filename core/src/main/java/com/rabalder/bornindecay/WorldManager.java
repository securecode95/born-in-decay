package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import java.util.List;

public class WorldManager {
    private final WorldGenerator generator;

    public WorldManager(Model blockModel) {
        this.generator = new WorldGenerator(blockModel, System.currentTimeMillis());
    }

    public void update(Vector3 playerPos) {
        generator.update(playerPos);
    }

    public List<ModelInstance> getVisibleBlocks() {
        return generator.getVisibleBlocks();
    }
}
