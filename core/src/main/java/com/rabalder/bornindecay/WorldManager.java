// WorldManager.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import java.util.List;

public class WorldManager {
    private final WorldGenerator gen;
    public WorldManager() { gen=new WorldGenerator(System.currentTimeMillis()); }
    public void update(Vector3 p) { gen.update(p); }
    public List<ModelInstance> getChunkMeshes() { return gen.getVisibleChunks(); }
    public List<Vector3> getCollisionVoxels() { return gen.getCollisionVoxels(); }
}
