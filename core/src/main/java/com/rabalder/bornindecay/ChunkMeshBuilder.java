// ChunkMeshBuilder.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.rabalder.bornindecay.BlockType;
import com.rabalder.bornindecay.Chunk;

import java.util.ArrayList;
import java.util.List;

public class ChunkMeshBuilder {
    private static final int SIZE = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    // buffers for each block‐type
    private final List<Float> grassV = new ArrayList<>();
    private final List<Short> grassI = new ArrayList<>();
    private final List<Float> soilV  = new ArrayList<>();
    private final List<Short> soilI  = new ArrayList<>();
    private final List<Float> stoneV = new ArrayList<>();
    private final List<Short> stoneI = new ArrayList<>();
    private short gBase=0, sBase=0, stBase=0;

    public ModelInstance buildChunkMesh(Chunk c) {
        grassV.clear(); grassI.clear(); gBase=0;
        soilV.clear();  soilI.clear();  sBase=0;
        stoneV.clear(); stoneI.clear(); stBase=0;

        // 1) X‐faces
        for (int x=0; x<=SIZE; x++) {
            byte[][] mask = new byte[SIZE][SIZE];
            for (int y=0; y<SIZE; y++)
                for (int z=0; z<SIZE; z++)
                    mask[y][z] = c.getBlock(x,y,z);
            mergeMask(mask, x, 0, 1, new Vector3(+1,0,0));
            mergeMask(mask, x, 0, 1, new Vector3(-1,0,0));
        }

        // 2) Y‐faces
        for (int y=0; y<=SIZE; y++) {
            byte[][] mask = new byte[SIZE][SIZE];
            for (int x=0; x<SIZE; x++)
                for (int z=0; z<SIZE; z++)
                    mask[x][z] = c.getBlock(x,y,z);
            mergeMask(mask, y, 1, 2, new Vector3(0,+1,0));
            mergeMask(mask, y, 1, 2, new Vector3(0,-1,0));
        }

        // 3) Z‐faces
        for (int z=0; z<=SIZE; z++) {
            byte[][] mask = new byte[SIZE][SIZE];
            for (int x=0; x<SIZE; x++)
                for (int y=0; y<SIZE; y++)
                    mask[x][y] = c.getBlock(x,y,z);
            mergeMask(mask, z, 2, 0, new Vector3(0,0,+1));
            mergeMask(mask, z, 2, 0, new Vector3(0,0,-1));
        }

        // bake meshes
        Mesh gm = buildMesh(grassV, grassI);
        Mesh sm = buildMesh(soilV,  soilI);
        Mesh stm= buildMesh(stoneV,stoneI);

        // materials (no cull face)
        Material mg = new Material(
            ColorAttribute.createDiffuse(Color.GREEN),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material ms = new Material(
            ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f)),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material mt = new Material(
            ColorAttribute.createDiffuse(Color.GRAY),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        if (!grassI.isEmpty()) mb.part("grass", gm, GL20.GL_TRIANGLES, mg);
        if (!soilI .isEmpty()) mb.part("soil",  sm, GL20.GL_TRIANGLES, ms);
        if (!stoneI.isEmpty()) mb.part("stone", stm,GL20.GL_TRIANGLES, mt);
        Model mdl = mb.end();

        return new ModelInstance(mdl);
    }

    private Mesh buildMesh(List<Float> v, List<Short> i) {
        Mesh m = new Mesh(true, v.size()/VERTEX_SIZE, i.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        m.setVertices(toFA(v));
        m.setIndices(toSA(i));
        return m;
    }

    private void mergeMask(byte[][] mask,
                           int u, int a, int b,
                           Vector3 normal)
    {
        int dimU = mask.length, dimV = mask[0].length;
        for (int i=0; i<dimU; i++) {
            for (int j=0; j<dimV; ) {
                byte id = mask[i][j];
                if (id==BlockType.AIR) { j++; continue; }
                // width
                int w=1;
                while (j+w<dimV && mask[i][j+w]==id) w++;
                // height
                int h=1;
                outer: while (i+h<dimU) {
                    for (int k=0; k<w; k++)
                        if (mask[i+h][j+k]!=id) break outer;
                    h++;
                }
                // build corners
                float[] low  = new float[3], high=new float[3];
                // correct component access
                float offset = ((a==0?normal.x:(a==1?normal.y:normal.z))>0?1f:0f);
                low[a] = u + offset;
                low[(a+1)%3] = i;
                low[(a+2)%3] = j;
                high[a] = low[a];
                high[(a+1)%3] = i+h;
                high[(a+2)%3] = j+w;

                Vector3 p1 = new Vector3(low[0],low[1],low[2]);
                Vector3 p2 = new Vector3(high[0],low[1],low[2]);
                Vector3 p3 = new Vector3(high[0],high[1],high[2]);
                Vector3 p4 = new Vector3(low[0],high[1],high[2]);

                emitQuad(id, p1,p2,p3,p4, normal);

                // zero‐out
                for (int di=0; di<h; di++)
                    for (int dj=0; dj<w; dj++)
                        mask[i+di][j+dj] = BlockType.AIR;

                j += w;
            }
        }
    }

    private void emitQuad(byte id,
                          Vector3 p1,Vector3 p2,Vector3 p3,Vector3 p4,
                          Vector3 n)
    {
        boolean topGrass = (id==BlockType.GRASS && n.y>0);
        List<Float>  vBuf; List<Short> iBuf; short base;
        if (topGrass)      { vBuf=grassV; iBuf=grassI; base=gBase; }
        else if (id==BlockType.SOIL)  { vBuf=soilV;  iBuf=soilI;  base=sBase; }
        else if (id==BlockType.STONE) { vBuf=stoneV; iBuf=stoneI; base=stBase;}
        else return;

        boolean flip = (n.x+n.y+n.z)<0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};

        for (Vector3 v : quad) {
            vBuf.add(v.x); vBuf.add(v.y); vBuf.add(v.z);
            vBuf.add(n.x); vBuf.add(n.y); vBuf.add(n.z);
        }
        for (short k=0; k<6; k++) iBuf.add((short)(base+k));

        if (topGrass) gBase   +=6;
        else if (id== BlockType.SOIL)  sBase+=6;
        else                          stBase+=6;
    }

    private float[] toFA(List<Float> l){
        float[] a=new float[l.size()];
        for(int i=0;i<a.length;i++) a[i]=l.get(i);
        return a;
    }
    private short[] toSA(List<Short>l){
        short[] a=new short[l.size()];
        for(int i=0;i<a.length;i++) a[i]=l.get(i);
        return a;
    }
}


