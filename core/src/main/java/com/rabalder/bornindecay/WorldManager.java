package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

public class WorldManager {
    private final WorldGenerator generator = new WorldGenerator(12345L);
    private final ChunkMeshBuilder meshBuilder = new ChunkMeshBuilder();
    private final Map<Long,ModelInstance> chunkInstances = new HashMap<>();

    /** Call each frame with the player’s world‐space position. */
    public void update(Vector3 playerPos) {
        // load a 5×5 area around the player
        generator.update(playerPos.x, playerPos.z, 2);

        // rebuild *all* instances (you can optimize later)
        chunkInstances.clear();
        for (Chunk c : generator.getLoadedChunks()) {
            ModelInstance mi = meshBuilder.buildChunkMesh(c);
            // translate it to its world‐position:
            mi.transform.setToTranslation(
                c.getChunkX() * Chunk.SIZE,
                0,
                c.getChunkZ() * Chunk.SIZE
            );
            chunkInstances.put(c.getKey(), mi);
        }
    }

    /** All the chunk‐meshes we should render. */
    public List<ModelInstance> getChunkMeshes() {
        return new ArrayList<>(chunkInstances.values());
    }

    /** Returns one sample‑point per non‑air block for collision. */
    public List<Vector3> getCollisionVoxels() {
        List<Vector3> out = new ArrayList<>();
        for (Chunk c : generator.getLoadedChunks()) {
            int baseX = c.getChunkX()*Chunk.SIZE;
            int baseZ = c.getChunkZ()*Chunk.SIZE;
            for (int x=0; x<Chunk.SIZE; x++)
                for (int y=0; y<Chunk.SIZE; y++)
                    for (int z=0; z<Chunk.SIZE; z++) {
                        if (c.getBlock(x,y,z) != BlockType.AIR) {
                            out.add(new Vector3(
                                baseX + x + 0.5f,
                                y + 0.5f,
                                baseZ + z + 0.5f
                            ));
                        }
                    }
        }
        return out;
    }
}
