// core/src/main/java/com/rabalder/bornindecay/ChunkMeshBuilder.java
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
    private static final int SIZE        = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    // per‐type vertex & index buffers
    private final List<Float>  grassVerts = new ArrayList<>();
    private final List<Short>  grassIdx   = new ArrayList<>();
    private short              grassBase  = 0;

    private final List<Float>  dirtVerts  = new ArrayList<>();
    private final List<Short>  dirtIdx    = new ArrayList<>();
    private short              dirtBase   = 0;

    private final List<Float>  stoneVerts = new ArrayList<>();
    private final List<Short>  stoneIdx   = new ArrayList<>();
    private short              stoneBase  = 0;

    /** Builds a single ModelInstance containing greedy‑meshed grass, dirt, stone. */
    public ModelInstance buildChunkMesh(Chunk chunk) {
        // clear all buffers
        grassVerts.clear(); grassIdx.clear(); grassBase = 0;
        dirtVerts.clear();  dirtIdx.clear();  dirtBase  = 0;
        stoneVerts.clear(); stoneIdx.clear(); stoneBase = 0;

        // 1) X slices – east + west faces
        for (int x = 0; x <= SIZE; x++) {
            byte[][] mask = buildMaskX(chunk, x);
            mergeMaskX(mask, x, new Vector3(+1,0,0), false);
            mergeMaskX(mask, x, new Vector3(-1,0,0), false);
        }

        // 2) Y slices – up (+grass top) + down faces
        for (int y = 0; y <= SIZE; y++) {
            byte[][] mask = buildMaskY(chunk, y);
            mergeMaskY(mask, y, new Vector3(0,+1,0), true);  // grass‑tops
            mergeMaskY(mask, y, new Vector3(0,-1,0), false); // dirt/stone bottoms
        }

        // 3) Z slices – north + south faces
        for (int z = 0; z <= SIZE; z++) {
            byte[][] mask = buildMaskZ(chunk, z);
            mergeMaskZ(mask, z, new Vector3(0,0,+1), false);
            mergeMaskZ(mask, z, new Vector3(0,0,-1), false);
        }

        // build three meshes
        Mesh grassM = buildMesh(grassVerts, grassIdx);
        Mesh dirtM  = buildMesh(dirtVerts,  dirtIdx);
        Mesh stoneM = buildMesh(stoneVerts, stoneIdx);

        // materials
        Material matG = new Material(ColorAttribute.createDiffuse(Color.GREEN));
        Material matD = new Material(ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f)));
        Material matS = new Material(ColorAttribute.createDiffuse(Color.GRAY));
        IntAttribute noCull = IntAttribute.createCullFace(GL20.GL_NONE);

        // bake into single Model
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        if (!grassIdx.isEmpty()) mb.part("grass", grassM, GL20.GL_TRIANGLES, matG);
        if (!dirtIdx.isEmpty())  mb.part("dirt",  dirtM,  GL20.GL_TRIANGLES, matD);
        if (!stoneIdx.isEmpty()) mb.part("stone", stoneM, GL20.GL_TRIANGLES, matS);
        Model model = mb.end();

        return new ModelInstance(model);
    }

    // —————————————————————————————
    // Extract a 2D [SIZE×SIZE] slice at x
    private byte[][] buildMaskX(Chunk c, int x) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int y=0; y<SIZE; y++)
            for (int z=0; z<SIZE; z++)
                mask[y][z] = c.getBlock(x, y, z);
        return mask;
    }
    // at y
    private byte[][] buildMaskY(Chunk c, int y) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int x=0; x<SIZE; x++)
            for (int z=0; z<SIZE; z++)
                mask[x][z] = c.getBlock(x, y, z);
        return mask;
    }
    // at z
    private byte[][] buildMaskZ(Chunk c, int z) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int x=0; x<SIZE; x++)
            for (int y=0; y<SIZE; y++)
                mask[x][y] = c.getBlock(x, y, z);
        return mask;
    }

    // —————————————————————————————
    // Greedy‐merge on X
    private void mergeMaskX(byte[][] mask, int x, Vector3 normal, boolean isTopFace) {
        int hDim = mask.length, wDim = mask[0].length;
        for (int y=0; y<hDim; y++) {
            for (int z=0; z<wDim; ) {
                byte id = mask[y][z];
                if (id == BlockType.AIR) { z++; continue; }
                // width
                int w=1; while (z+w<wDim && mask[y][z+w]==id) w++;
                // height
                int h=1; outer: while (y+h<hDim) {
                    for (int k=0; k<w; k++) if (mask[y+h][z+k]!=id) break outer;
                    h++;
                }
                // quad corners
                float x0 = x + (normal.x>0?1f:0f),
                    y0 = y,        z0 = z,
                    y1 = y + h,    z1 = z + w;
                Vector3 p1 = new Vector3(x0,y0,z0),
                    p2 = new Vector3(x0,y1,z0),
                    p3 = new Vector3(x0,y1,z1),
                    p4 = new Vector3(x0,y0,z1);
                addQuad(id, p1,p2,p3,p4, normal, isTopFace);

                // zero out
                for (int dy=0; dy<h; dy++)
                    for (int dz=0; dz<w; dz++)
                        mask[y+dy][z+dz] = BlockType.AIR;
                z += w;
            }
        }
    }

    // Greedy‐merge on Y
    private void mergeMaskY(byte[][] mask, int y, Vector3 normal, boolean isTopFace) {
        int hDim = mask.length, wDim = mask[0].length;
        for (int x=0; x<hDim; x++) {
            for (int z=0; z<wDim; ) {
                byte id = mask[x][z];
                if (id == BlockType.AIR) { z++; continue; }
                int w=1; while (z+w<wDim && mask[x][z+w]==id) w++;
                int h=1; outer: while (x+h<hDim) {
                    for (int k=0; k<w; k++) if (mask[x+h][z+k]!=id) break outer;
                    h++;
                }
                float x0 = x,       z0 = z,
                    x1 = x+h,     z1 = z+w,
                    y0 = y + (normal.y>0?1f:0f);
                Vector3 p1 = new Vector3(x0,y0,z0),
                    p2 = new Vector3(x1,y0,z0),
                    p3 = new Vector3(x1,y0,z1),
                    p4 = new Vector3(x0,y0,z1);
                addQuad(id, p1,p2,p3,p4, normal, isTopFace);
                for (int dx=0; dx<h; dx++)
                    for (int dz=0; dz<w; dz++)
                        mask[x+dx][z+dz] = BlockType.AIR;
                z += w;
            }
        }
    }

    // Greedy‐merge on Z
    private void mergeMaskZ(byte[][] mask, int z, Vector3 normal, boolean isTopFace) {
        int hDim = mask.length, wDim = mask[0].length;
        for (int x=0; x<hDim; x++) {
            for (int y=0; y<wDim; ) {
                byte id = mask[x][y];
                if (id == BlockType.AIR) { y++; continue; }
                int w=1; while (y+w<wDim && mask[x][y+w]==id) w++;
                int h=1; outer: while (x+h<hDim) {
                    for (int k=0; k<w; k++) if (mask[x+h][y+k]!=id) break outer;
                    h++;
                }
                float x0 = x,       y0 = y,
                    x1 = x+h,     y1 = y+w,
                    z0 = z + (normal.z>0?1f:0f);
                Vector3 p1 = new Vector3(x0,y0,z0),
                    p2 = new Vector3(x1,y0,z0),
                    p3 = new Vector3(x1,y1,z0),
                    p4 = new Vector3(x0,y1,z0);
                addQuad(id, p1,p2,p3,p4, normal, isTopFace);
                for (int dx=0; dx<h; dx++)
                    for (int dy=0; dy<w; dy++)
                        mask[x+dx][y+dy] = BlockType.AIR;
                y += w;
            }
        }
    }

    // Emit one 2‑triangle quad into the correct per‑type buffer
    private void addQuad(byte id,
                         Vector3 p1,Vector3 p2,Vector3 p3,Vector3 p4,
                         Vector3 n, boolean isTopFace)
    {
        List<Float>  vBuf; List<Short> iBuf; short base;
        // choose buffer by block type & top‑face flag
        if (isTopFace && id==BlockType.GRASS)      { vBuf=grassVerts; iBuf=grassIdx; base=grassBase; }
        else if (id==BlockType.GRASS)              { vBuf=dirtVerts;  iBuf=dirtIdx;  base=dirtBase;  }
        else if (id==BlockType.DIRT)               { vBuf=dirtVerts;  iBuf=dirtIdx;  base=dirtBase;  }
        else /*ST O NE*/                           { vBuf=stoneVerts; iBuf=stoneIdx; base=stoneBase; }

        boolean flip = (n.x + n.y + n.z) < 0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};

        // add vertices (pos + normal)
        for (Vector3 v : quad) {
            vBuf.add(v.x); vBuf.add(v.y); vBuf.add(v.z);
            vBuf.add(n.x); vBuf.add(n.y); vBuf.add(n.z);
        }
        // add indices
        for (short i=0; i<6; i++) iBuf.add((short)(base + i));

        // bump base counters
        if      (isTopFace && id==BlockType.GRASS) grassBase += 6;
        else if (id==BlockType.GRASS || id==BlockType.DIRT) dirtBase  += 6;
        else                                         stoneBase += 6;
    }

    // Build a Mesh from raw floats & shorts
    private Mesh buildMesh(List<Float> verts, List<Short> idx) {
        if (idx.isEmpty()) return new Mesh(true, 0, 0);
        Mesh m = new Mesh(true,
            verts.size()/VERTEX_SIZE,
            idx .size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        float[]  fv = new float [verts.size()];
        short[] si = new short[idx .size()];
        for (int i=0; i<fv.length; i++) fv[i] = verts.get(i);
        for (int i=0; i<si.length; i++) si[i] = idx .get(i);
        m.setVertices(fv);
        m.setIndices(si);
        return m;
    }
}
