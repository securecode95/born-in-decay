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

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a greedy‐meshed chunk, splitting grass‐tops, soil‐sides, and stone into three
 * separate submeshes for performance (and so you can assign different materials).
 */
public class ChunkMeshBuilder {
    private static final int SIZE        = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    // --- six growable buffers, one pair per block‐type ---
    private final List<Float> grassV = new ArrayList<>();
    private final List<Short> grassI = new ArrayList<>();
    private final List<Float> soilV  = new ArrayList<>();
    private final List<Short> soilI  = new ArrayList<>();
    private final List<Float> stoneV = new ArrayList<>();
    private final List<Short> stoneI = new ArrayList<>();

    // --- running “base” index for each buffer, so indices offset properly ---
    private short gBase, sBase, stBase;

    public ModelInstance buildChunkMesh(Chunk chunk) {
        // 0) clear out buffers & reset bases
        grassV.clear(); grassI.clear();
        soilV.clear();  soilI.clear();
        stoneV.clear(); stoneI.clear();
        gBase = sBase = stBase = 0;

        // 1) X‐axis faces (mask dims = [y][z], a=0)
        for (int x = 0; x <= SIZE; x++) {
            byte[][] mask = buildMaskX(chunk, x);
            mergeMask(mask, x, 0, 1, new Vector3(+1, 0, 0));
            mergeMask(mask, x, 0, 1, new Vector3(-1, 0, 0));
        }

        // 2) Y‐axis faces (mask dims = [x][z], a=1)
        for (int y = 0; y <= SIZE; y++) {
            byte[][] mask = buildMaskY(chunk, y);
            mergeMask(mask, y, 1, 2, new Vector3(0, +1, 0));
            mergeMask(mask, y, 1, 2, new Vector3(0, -1, 0));
        }

        // 3) Z‐axis faces (mask dims = [x][y], a=2)
        for (int z = 0; z <= SIZE; z++) {
            byte[][] mask = buildMaskZ(chunk, z);
            mergeMask(mask, z, 2, 0, new Vector3(0, 0, +1));
            mergeMask(mask, z, 2, 0, new Vector3(0, 0, -1));
        }

        // 4) Bake each buffer into a Mesh
        Mesh grassMesh = buildMesh(grassV, grassI);
        Mesh soilMesh  = buildMesh(soilV,  soilI);
        Mesh stoneMesh = buildMesh(stoneV, stoneI);

        // 5) Three simple flat materials, no back‐face culling
        Material matGrass = new Material(
            ColorAttribute.createDiffuse(Color.GREEN),
            ColorAttribute.createSpecular(0.2f, 0.2f, 0.2f, 1f),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material matSoil = new Material(
            ColorAttribute.createDiffuse(new Color(0.6f, 0.4f, 0.2f, 1f)),
            ColorAttribute.createSpecular(0.1f, 0.1f, 0.1f, 1f),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material matStone = new Material(
            ColorAttribute.createDiffuse(Color.LIGHT_GRAY),
            ColorAttribute.createSpecular(0.1f, 0.1f, 0.1f, 1f),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        // 6) Combine into one Model with three parts
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("grass", grassMesh, GL20.GL_TRIANGLES, matGrass);
        mb.part("soil",  soilMesh,  GL20.GL_TRIANGLES, matSoil);
        mb.part("stone", stoneMesh, GL20.GL_TRIANGLES, matStone);
        Model model = mb.end();

        return new ModelInstance(model);
    }

    // --- builds a Mesh from a pair of float/short lists ---
    private Mesh buildMesh(List<Float> verts, List<Short> idx) {
        Mesh m = new Mesh(
            true,
            verts.size() / VERTEX_SIZE,
            idx.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        m.setVertices(toFloatArray(verts));
        m.setIndices(toShortArray(idx));
        return m;
    }

    // ————————————————————————————————
    // generic greedy‐mesh over a 2D byte[][] mask
    // u = slice index (x/y/z), a = axis index (0=x,1=y,2=z), b unused
    private void mergeMask(byte[][] mask,
                           int u, int a, int b,
                           Vector3 normal)
    {
        int dimU = mask.length, dimV = mask[0].length;
        for (int i = 0; i < dimU; i++) {
            for (int j = 0; j < dimV; ) {
                byte id = mask[i][j];
                if (id == BlockType.AIR) { j++; continue; }
                // find width
                int w = 1;
                while (j + w < dimV && mask[i][j + w] == id) w++;
                // find height
                int h = 1;
                outer:
                while (i + h < dimU) {
                    for (int k = 0; k < w; k++)
                        if (mask[i + h][j + k] != id) break outer;
                    h++;
                }

                // compute two opposing corners in world space
                float[] lo = new float[3];
                float[] hi = new float[3];
                float off = ( (a==0?normal.x : a==1?normal.y : normal.z) > 0 ) ? 1f : 0f;
                lo[a]       = u + off;
                lo[(a+1)%3] = i;
                lo[(a+2)%3] = j;
                hi[a]       = lo[a];
                hi[(a+1)%3] = i + h;
                hi[(a+2)%3] = j + w;

                Vector3 p1 = new Vector3(lo[0], lo[1], lo[2]);
                Vector3 p2 = new Vector3(hi[0], lo[1], lo[2]);
                Vector3 p3 = new Vector3(hi[0], hi[1], hi[2]);
                Vector3 p4 = new Vector3(lo[0], hi[1], hi[2]);

                emitQuad(id, p1, p2, p3, p4, normal);

                // zero‐out this rectangle so we don't double‐draw
                for (int di = 0; di < h; di++)
                    for (int dj = 0; dj < w; dj++)
                        mask[i + di][j + dj] = BlockType.AIR;
                j += w;
            }
        }
    }

    // ————————————————————————————————
    // emit one quad into the proper buffer
    private void emitQuad(byte id,
                          Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4,
                          Vector3 n)
    {
        boolean topGrass = (id == BlockType.GRASS && n.y > 0);
        List<Float>  vBuf;
        List<Short>  iBuf;
        short        base;

        if      (topGrass)           { vBuf = grassV;  iBuf = grassI;  base = gBase; }
        else if (id == BlockType.GRASS ||
            id == BlockType.DIRT) { vBuf = soilV;   iBuf = soilI;   base = sBase; }
        else if (id == BlockType.STONE) { vBuf = stoneV;  iBuf = stoneI;  base = stBase; }
        else return;

        // choose winding so normals point out
        boolean flip = (n.x + n.y + n.z) < 0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};

        // append six vertices
        for (Vector3 v : quad) {
            vBuf.add(v.x); vBuf.add(v.y); vBuf.add(v.z);
            vBuf.add(n.x); vBuf.add(n.y); vBuf.add(n.z);
        }
        // append six indices
        for (short k = 0; k < 6; k++) {
            iBuf.add((short)(base + k));
        }

        // bump the appropriate base
        if (topGrass)            gBase   += 6;
        else if (id == BlockType.GRASS ||
            id == BlockType.DIRT) sBase   += 6;
        else                       stBase += 6;
    }

    // ————————————————————————————————
    private Mesh buildMaskX(Chunk c, int x) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++)
            for (int z = 0; z < SIZE; z++)
                m[y][z] = c.getBlock(x, y, z);
        return m;
    }

    private byte[][] buildMaskY(Chunk c, int y) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int z = 0; z < SIZE; z++)
                m[x][z] = c.getBlock(x, y, z);
        return m;
    }

    private byte[][] buildMaskZ(Chunk c, int z) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                m[x][y] = c.getBlock(x, y, z);
        return m;
    }

    // ————————————————————————————————
    private float[] toFloatArray(List<Float> l) {
        float[] a = new float[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }
    private short[] toShortArray(List<Short> l) {
        short[] a = new short[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }
}
