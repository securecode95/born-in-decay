package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.List;

public class ChunkMeshBuilder {
    private static final int SIZE  = Chunk.SIZE;
    private static final int VSIZE = 6; // x,y,z + nx,ny,nz

    // per‐type vertex & index buffers
    private final List<Float> grassV = new ArrayList<>();
    private final List<Short> grassI = new ArrayList<>();
    private final List<Float> dirtV  = new ArrayList<>();
    private final List<Short> dirtI  = new ArrayList<>();
    private final List<Float> stoneV = new ArrayList<>();
    private final List<Short> stoneI = new ArrayList<>();
    private short gBase, dBase, sBase;

    public ModelInstance buildChunkMesh(Chunk chunk) {
        // clear
        grassV.clear(); grassI.clear();
        dirtV .clear(); dirtI .clear();
        stoneV.clear(); stoneI.clear();
        gBase = dBase = sBase = 0;

        // emit one quad per exposed face
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    byte id = chunk.getBlock(x, y, z);
                    if (id == BlockType.AIR) continue;
                    tryFace(chunk, x, y, z, +1,  0,  0);
                    tryFace(chunk, x, y, z, -1,  0,  0);
                    tryFace(chunk, x, y, z,  0, +1,  0);
                    tryFace(chunk, x, y, z,  0, -1,  0);
                    tryFace(chunk, x, y, z,  0,  0, +1);
                    tryFace(chunk, x, y, z,  0,  0, -1);
                }
            }
        }

        // build Meshes
        Mesh grassM = buildMesh(grassV, grassI);
        Mesh dirtM  = buildMesh(dirtV,  dirtI);
        Mesh stoneM = buildMesh(stoneV, stoneI);

        // compose Materials (diffuse + disable culling)
        Material mGrass = new Material(
            ColorAttribute.createDiffuse(new Color(0.13f,0.55f,0.13f,1f)),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material mDirt = new Material(
            ColorAttribute.createDiffuse(new Color(0.60f,0.40f,0.20f,1f)),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material mStone = new Material(
            ColorAttribute.createDiffuse(new Color(0.50f,0.50f,0.50f,1f)),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        // bake into a single ModelInstance
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        if (!grassI.isEmpty()) mb.part("grass", grassM, GL20.GL_TRIANGLES, mGrass);
        if (!dirtI .isEmpty()) mb.part("dirt",  dirtM,  GL20.GL_TRIANGLES, mDirt);
        if (!stoneI.isEmpty()) mb.part("stone", stoneM, GL20.GL_TRIANGLES, mStone);
        Model model = mb.end();

        return new ModelInstance(model);
    }

    private void tryFace(Chunk c, int x,int y,int z, int dx,int dy,int dz) {
        if (c.getBlock(x+dx, y+dy, z+dz) != BlockType.AIR) return;
        byte id = c.getBlock(x, y, z);

        Vector3 n = new Vector3(dx,dy,dz);
        Vector3 p0 = new Vector3(x,   y,   z);
        Vector3 u  = new Vector3((dy!=0?1:0),(dz!=0?1:0),(dx!=0?1:0));
        Vector3 v  = new Vector3((dz!=0?1:0),(dx!=0?1:0),(dy!=0?1:0));

        Vector3 p1 = new Vector3(p0).add(u);
        Vector3 p2 = new Vector3(p0).add(u).add(v);
        Vector3 p3 = new Vector3(p0).add(v);

        emit(id, p0, p1, p2, p3, n);
    }

    private void emit(byte id,
                      Vector3 p1,Vector3 p2,Vector3 p3,Vector3 p4,
                      Vector3 n)
    {
        List<Float>  vBuf; List<Short> iBuf; short base;
        if (id==BlockType.GRASS)      { vBuf=grassV; iBuf=grassI; base=gBase; }
        else if (id==BlockType.DIRT)  { vBuf=dirtV;  iBuf=dirtI;  base=dBase; }
        else                           { vBuf=stoneV; iBuf=stoneI; base=sBase; }

        boolean flip = (n.x+n.y+n.z) < 0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};

        for (Vector3 p : quad) {
            vBuf.add(p.x); vBuf.add(p.y); vBuf.add(p.z);
            vBuf.add(n.x); vBuf.add(n.y); vBuf.add(n.z);
        }
        for (short i=0; i<6; i++) iBuf.add((short)(base+i));

        if      (id==BlockType.GRASS) gBase += 6;
        else if (id==BlockType.DIRT)  dBase += 6;
        else                           sBase += 6;
    }

    private Mesh buildMesh(List<Float> v, List<Short> i) {
        if (i.isEmpty()) return new Mesh(true,0,0);
        Mesh m = new Mesh(true, v.size()/VSIZE, i.size(),
            new VertexAttribute(Usage.Position,3,"a_position"),
            new VertexAttribute(Usage.Normal,  3,"a_normal")
        );
        float[] fv = new float[v.size()];
        for (int j=0; j<fv.length; j++) fv[j] = v.get(j);
        short[] si = new short[i.size()];
        for (int j=0; j<si.length; j++) si[j] = i.get(j);
        m.setVertices(fv);
        m.setIndices(si);
        return m;
    }
}
