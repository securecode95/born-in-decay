// WorldGenerator.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import java.util.*;

public class WorldGenerator {
    private final Map<Vector2,Chunk> chunks = new HashMap<>();
    private final Map<Vector2,ModelInstance> meshes = new HashMap<>();
    private final long seed;
    private final int viewDist = 2;
    private final int maxH = 8;

    public WorldGenerator(long seed) {
        this.seed = seed;
    }

    public void update(Vector3 playerPos) {
        int cx=(int)Math.floor(playerPos.x/Chunk.SIZE);
        int cz=(int)Math.floor(playerPos.z/Chunk.SIZE);
        Set<Vector2> keep = new HashSet<>();
        for (int dx=-viewDist; dx<=viewDist; dx++)
            for (int dz=-viewDist; dz<=viewDist; dz++) {
                Vector2 c=new Vector2(cx+dx,cz+dz);
                keep.add(c);
                if (!chunks.containsKey(c)) {
                    Chunk ch = genChunk(cx+dx,cz+dz);
                    chunks.put(c,ch);
                    meshes.put(c,new ChunkMeshBuilder().buildChunkMesh(ch));
                }
            }
        // unload
        chunks.keySet().removeIf(k->!keep.contains(k));
        meshes.keySet().removeIf(k->!keep.contains(k));
    }

    private Chunk genChunk(int cx,int cz) {
        Chunk c=new Chunk();
        for(int x=0;x<Chunk.SIZE;x++)
            for(int z=0;z<Chunk.SIZE;z++){
                int wx=cx*Chunk.SIZE+x;
                int wz=cz*Chunk.SIZE+z;
                double n = OpenSimplexNoise.noise2(seed,wx*0.1,wz*0.1);
                int h=(int)(n*maxH+maxH/2f);
                h=Math.max(0,Math.min(h,Chunk.SIZE-1));
                for(int y=0;y<=h;y++){
                    byte t = (y==h?BlockType.GRASS:(y<2?BlockType.STONE:BlockType.SOIL));
                    c.setBlock(x,y,z,t);
                }
            }
        return c;
    }

    public List<ModelInstance> getVisibleChunks() {
        return new ArrayList<>(meshes.values());
    }

    public List<Vector3> getCollisionVoxels() {
        List<Vector3> vox=new ArrayList<>();
        for (var e:chunks.entrySet()) {
            Vector2 coord=e.getKey();
            Chunk ch=e.getValue();
            for(int x=0;x<Chunk.SIZE;x++)
                for(int y=0;y<Chunk.SIZE;y++)
                    for(int z=0;z<Chunk.SIZE;z++){
                        if (ch.getBlock(x,y,z)!=BlockType.AIR) {
                            vox.add(new Vector3(
                                coord.x*Chunk.SIZE + x,
                                y,
                                coord.y*Chunk.SIZE + z));
                        }
                    }
        }
        return vox;
    }
}
