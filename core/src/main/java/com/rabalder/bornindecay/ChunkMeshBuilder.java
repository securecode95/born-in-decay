// ChunkMeshBuilder.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy‐mesher that splits:
 *  - grass‐tops (green)
 *  - soil‐sides (brown)
 *  - stone faces (gray)
 */
public class ChunkMeshBuilder {
    private static final int SIZE = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    private final List<Float> grassVerts = new ArrayList<>();
    private final List<Short> grassIdx   = new ArrayList<>();
    private final List<Float> soilVerts  = new ArrayList<>();
    private final List<Short> soilIdx    = new ArrayList<>();
    private final List<Float> stoneVerts = new ArrayList<>();
    private final List<Short> stoneIdx   = new ArrayList<>();
    private short gBase=0, sBase=0, stBase=0;

    public ModelInstance buildChunkMesh(Chunk chunk) {
        // clear
        grassVerts.clear(); grassIdx.clear();
        soilVerts.clear();  soilIdx.clear();
        stoneVerts.clear(); stoneIdx.clear();
        gBase=sBase=stBase=0;

        // 1) X‐faces
        for(int x=0;x<=SIZE;x++){
            byte[][] mask = new byte[SIZE][SIZE];
            for(int y=0;y<SIZE;y++)
                for(int z=0;z<SIZE;z++)
                    mask[y][z] = chunk.getBlock(x,y,z);
            mergeMask(mask, x, 0, 1, new Vector3(+1,0,0));
            mergeMask(mask, x, 0, 1, new Vector3(-1,0,0));
        }
        // 2) Y‐faces
        for(int y=0;y<=SIZE;y++){
            byte[][] mask = new byte[SIZE][SIZE];
            for(int x=0;x<SIZE;x++)
                for(int z=0;z<SIZE;z++)
                    mask[x][z] = chunk.getBlock(x,y,z);
            mergeMask(mask,  y, 1, 2, new Vector3(0,+1,0));
            mergeMask(mask,  y, 1, 2, new Vector3(0,-1,0));
        }
        // 3) Z‐faces
        for(int z=0;z<=SIZE;z++){
            byte[][] mask = new byte[SIZE][SIZE];
            for(int x=0;x<SIZE;x++)
                for(int y=0;y<SIZE;y++)
                    mask[x][y] = chunk.getBlock(x,y,z);
            mergeMask(mask, z, 2, 0, new Vector3(0,0,+1));
            mergeMask(mask, z, 2, 0, new Vector3(0,0,-1));
        }

        // build three meshes
        Mesh gm = buildMesh(grassVerts, grassIdx);
        Mesh sm = buildMesh(soilVerts,  soilIdx);
        Mesh stm= buildMesh(stoneVerts, stoneIdx);

        // materials
        ColorAttribute grassCol = ColorAttribute.createDiffuse(Color.GREEN);
        ColorAttribute soilCol  = ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f));
        ColorAttribute stoneCol = ColorAttribute.createDiffuse(new Color(0.5f,0.5f,0.5f,1f));
        IntAttribute    ncull    = IntAttribute.createCullFace(GL20.GL_NONE);

        // model
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        if (grassIdx.size()>0)
            mb.part("grass", gm, GL20.GL_TRIANGLES, grassCol, ncull);
        if (soilIdx.size()>0)
            mb.part("soil",  sm, GL20.GL_TRIANGLES, soilCol,  ncull);
        if (stoneIdx.size()>0)
            mb.part("stone",stm,GL20.GL_TRIANGLES, stoneCol, ncull);
        Model m = mb.end();

        return new ModelInstance(m);
    }

    // helper to build a Mesh
    private Mesh buildMesh(List<Float> verts, List<Short> idx){
        Mesh mesh = new Mesh(true,
            verts.size()/VERTEX_SIZE,
            idx.size(),
            new VertexAttribute(Usage.Position,3,"a_position"),
            new VertexAttribute(Usage.Normal,  3,"a_normal")
        );
        mesh.setVertices(toFloatArray(verts));
        mesh.setIndices (toShortArray(idx));
        return mesh;
    }

    /** greedy‐rectangle over a single 2D mask of block‐IDs */
    private void mergeMask(byte[][] mask,
                           int u, int a, int b,
                           Vector3 normal)
    {
        int dimU = mask.length, dimV = mask[0].length;
        for(int i=0;i<dimU;i++){
            for(int j=0;j<dimV;){
                byte id = mask[i][j];
                if(id==BlockType.AIR){ j++; continue; }
                // width
                int w=1;
                while(j+w<dimV && mask[i][j+w]==id) w++;
                // height
                int h=1;
                outer: while(i+h<dimU){
                    for(int k=0;k<w;k++)
                        if(mask[i+h][j+k]!=id) break outer;
                    h++;
                }
                // corner coords
                float[] low  = new float[]{0,0,0};
                float[] high = new float[]{0,0,0};
                low[a]  = u + (normal.get(a)>0?1f:0f);
                low[(a+1)%3] = i;
                low[(a+2)%3] = j;
                high[a] = low[a];
                high[(a+1)%3] = i+h;
                high[(a+2)%3] = j+w;

                Vector3 p1 = new Vector3(low[0], low[1], low[2]);
                Vector3 p2 = new Vector3(high[0],low[1],low[2]);
                Vector3 p3 = new Vector3(high[0],high[1],high[2]);
                Vector3 p4 = new Vector3(low[0], high[1],high[2]);

                emitQuad(id, p1,p2,p3,p4, normal);

                // zero‐out
                for(int di=0; di<h; di++)
                    for(int dj=0; dj<w; dj++)
                        mask[i+di][j+dj]=BlockType.AIR;

                j += w;
            }
        }
    }

    /** push two triangles for this face into the correct buffer */
    private void emitQuad(byte id,
                          Vector3 p1,Vector3 p2,Vector3 p3,Vector3 p4,
                          Vector3 n)
    {
        boolean grassTop = (id==BlockType.GRASS && n.y>0);
        List<Float>  vBuf;
        List<Short>  iBuf;
        short        base;
        if (grassTop)      { vBuf=grassVerts; iBuf=grassIdx;  base=gBase;  }
        else if (id==BlockType.SOIL)  { vBuf=soilVerts;  iBuf=soilIdx;   base=sBase;  }
        else if (id==BlockType.STONE) { vBuf=stoneVerts; iBuf=stoneIdx;  base=stBase; }
        else return; // skip any AIR faces, or non‐handled
        boolean flip = (n.x+n.y+n.z)<0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};

        for (Vector3 v : quad) {
            vBuf.add(v.x);
            vBuf.add(v.y);
            vBuf.add(v.z);
            vBuf.add(n.x);
            vBuf.add(n.y);
            vBuf.add(n.z);
        }
        for(short k=0;k<6;k++) iBuf.add((short)(base+k));

        if      (grassTop) gBase += 6;
        else if (id==BlockType.SOIL)  sBase += 6;
        else                         stBase += 6;
    }

    private float[] toFloatArray(List<Float> list){
        float[] a = new float[list.size()];
        for(int i=0;i<a.length;i++) a[i]=list.get(i);
        return a;
    }
    private short[] toShortArray(List<Short> list){
        short[] a = new short[list.size()];
        for(int i=0;i<a.length;i++) a[i]=list.get(i);
        return a;
    }
}
