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
    private static final int SIZE = Chunk.SIZE;
    private static final int VSIZE = 6; // x,y,z + nx,ny,nz

    // buffers for each type
    private final List<Float>  grassV = new ArrayList<>();
    private final List<Short>  grassI = new ArrayList<>();
    private short grassB = 0;

    private final List<Float>  dirtV  = new ArrayList<>();
    private final List<Short>  dirtI  = new ArrayList<>();
    private short dirtB = 0;

    private final List<Float>  stoneV = new ArrayList<>();
    private final List<Short>  stoneI = new ArrayList<>();
    private short stoneB = 0;

    public ModelInstance buildChunkMesh(Chunk c) {
        // clear
        grassV.clear(); grassI.clear(); grassB=0;
        dirtV.clear();  dirtI.clear();  dirtB=0;
        stoneV.clear(); stoneI.clear(); stoneB=0;

        // X slices
        for (int x=0; x<=SIZE; x++) {
            byte[][] mask = c.buildMaskX(x);
            greedySlice(mask, x, 0,1,2, new Vector3(+1,0,0), false);
            greedySlice(mask, x, 0,1,2, new Vector3(-1,0,0), false);
        }
        // Y slices (grass tops!)
        for (int y=0; y<=SIZE; y++) {
            byte[][] mask = c.buildMaskY(y);
            greedySlice(mask, y, 1,2,0, new Vector3(0,+1,0), true);
            greedySlice(mask, y, 1,2,0, new Vector3(0,-1,0), false);
        }
        // Z slices
        for (int z=0; z<=SIZE; z++) {
            byte[][] mask = c.buildMaskZ(z);
            greedySlice(mask, z, 2,0,1, new Vector3(0,0,+1), false);
            greedySlice(mask, z, 2,0,1, new Vector3(0,0,-1), false);
        }

        // build meshes
        Mesh gM = buildMesh(grassV, grassI);
        Mesh dM = buildMesh(dirtV,  dirtI);
        Mesh sM = buildMesh(stoneV, stoneI);

        // mats
        Material mG = new Material(ColorAttribute.createDiffuse(Color.GREEN));
        Material mD = new Material(ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f)));
        Material mS = new Material(ColorAttribute.createDiffuse(Color.GRAY));
        IntAttribute noCull = IntAttribute.createCullFace(GL20.GL_NONE);

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        if (!grassI.isEmpty()) mb.part("grass", gM, GL20.GL_TRIANGLES, mG);
        if (!dirtI .isEmpty()) mb.part("dirt",  dM, GL20.GL_TRIANGLES, mD);
        if (!stoneI.isEmpty()) mb.part("stone", sM, GL20.GL_TRIANGLES, mS);
        Model model = mb.end();

        return new ModelInstance(model);
    }

    /**
     * Greedy‐mesh over a 2D mask[][]:
     * uIndex is the axis of slice (0=x,1=y,2=z),
     * v1,v2 are the two in‐plane axes.
     */
    private void greedySlice(byte[][] mask,
                             int slice,
                             int uIndex, int v1Index, int v2Index,
                             Vector3 normal,
                             boolean isTopFace)
    {
        int H = mask.length, W = mask[0].length;
        for (int i=0; i<H; i++) {
            for (int j=0; j<W; ) {
                byte id = mask[i][j];
                if (id==BlockType.AIR) { j++; continue; }
                // width
                int w=1; while(j+w<W && mask[i][j+w]==id) w++;
                // height
                int h=1; outer: while(i+h<H) {
                    for (int k=0;k<w;k++)
                        if (mask[i+h][j+k]!=id) break outer;
                    h++;
                }
                // corner coords low/high
                float[] low  = new float[3], high = new float[3];
                // offset on slice axis:
                float off = ( (uIndex==0?normal.x: uIndex==1?normal.y:normal.z) >0?1f:0f );
                low [uIndex] = slice + off;
                low [v1Index]= i;
                low [v2Index]= j;
                high[uIndex] = low[uIndex];
                high[v1Index]= i+h;
                high[v2Index]= j+w;

                // build 4 corners
                Vector3 p1 = new Vector3(low[0],low[1],low[2]);
                Vector3 p2 = new Vector3(high[0],low[1],low[2]);
                Vector3 p3 = new Vector3(high[0],high[1],high[2]);
                Vector3 p4 = new Vector3(low[0],high[1],high[2]);

                emitQuad(id, p1,p2,p3,p4, normal, isTopFace);

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
                          Vector3 n, boolean isTop)
    {
        List<Float>  vB; List<Short> iB; short base;
        if (id==BlockType.GRASS && isTop)      { vB=grassV; base=grassB; iB=grassI; }
        else if (id==BlockType.GRASS || id==BlockType.DIRT) { vB=dirtV;  base=dirtB;  iB=dirtI; }
        else                                   { vB=stoneV; base=stoneB;iB=stoneI; }

        boolean flip = (n.x+n.y+n.z)<0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};

        for (Vector3 v : quad) {
            vB.add(v.x); vB.add(v.y); vB.add(v.z);
            vB.add(n.x); vB.add(n.y); vB.add(n.z);
        }
        for (short k=0;k<6;k++) iB.add((short)(base+k));

        if      (id==BlockType.GRASS && isTop) grassB +=6;
        else if (id==BlockType.GRASS||id==BlockType.DIRT) dirtB+=6;
        else                                     stoneB+=6;
    }

    private Mesh buildMesh(List<Float> v, List<Short> i) {
        if (i.isEmpty()) return new Mesh(true,0,0);
        Mesh m = new Mesh(true,
            v.size()/VSIZE, i.size(),
            new VertexAttribute(Usage.Position, 3,"a_position"),
            new VertexAttribute(Usage.Normal,   3,"a_normal")
        );
        float[] fv = new float[v.size()];
        for (int j=0;j<fv.length;j++) fv[j]=v.get(j);
        short[] si = new short[i.size()];
        for (int j=0;j<si.length;j++) si[j]=i.get(j);
        m.setVertices(fv);
        m.setIndices(si);
        return m;
    }
}
