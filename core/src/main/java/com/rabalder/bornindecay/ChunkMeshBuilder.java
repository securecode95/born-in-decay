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

/**
 * Greedy‐meshes a Chunk into three merged meshes: grass, dirt, stone.
 */
public class ChunkMeshBuilder {
    private static final int SIZE        = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    // per‐type buffers
    private final List<Float> grassV = new ArrayList<>(); private final List<Short> grassI = new ArrayList<>(); private short gBase=0;
    private final List<Float> dirtV  = new ArrayList<>(); private final List<Short> dirtI  = new ArrayList<>(); private short dBase=0;
    private final List<Float> stoneV = new ArrayList<>(); private final List<Short> stoneI = new ArrayList<>(); private short sBase=0;

    // the 6 cardinal normals
    private static final Vector3[] NORMALS = {
        new Vector3( 1,0,0), new Vector3(-1,0,0),
        new Vector3( 0,1,0), new Vector3( 0,-1,0),
        new Vector3( 0,0,1), new Vector3( 0,0,-1)
    };

    /**
     * Build a single ModelInstance with three parts: "grass","dirt","stone".
     */
    public ModelInstance buildChunkMesh(Chunk chunk) {
        // clear out old data
        grassV.clear(); grassI.clear(); gBase = 0;
        dirtV.clear();  dirtI.clear();  dBase = 0;
        stoneV.clear(); stoneI.clear(); sBase = 0;

        // 1) X‐axis slices
        for (int x = 0; x <= SIZE; x++) {
            byte[][] mask = buildMaskX(chunk, x);
            mergeMask(mask, x, 0, 1, NORMALS[0]);
            mergeMask(mask, x, 0, 1, NORMALS[1]);
        }
        // 2) Y‐axis slices
        for (int y = 0; y <= SIZE; y++) {
            byte[][] mask = buildMaskY(chunk, y);
            mergeMask(mask, y, 1, 2, NORMALS[2]);
            mergeMask(mask, y, 1, 2, NORMALS[3]);
        }
        // 3) Z‐axis slices
        for (int z = 0; z <= SIZE; z++) {
            byte[][] mask = buildMaskZ(chunk, z);
            mergeMask(mask, z, 2, 0, NORMALS[4]);
            mergeMask(mask, z, 2, 0, NORMALS[5]);
        }

        // turn buffers into Meshes
        Mesh grassMesh = makeMesh(grassV, grassI);
        Mesh dirtMesh  = makeMesh(dirtV,  dirtI);
        Mesh stoneMesh = makeMesh(stoneV, stoneI);

        // materials
        ColorAttribute matG = ColorAttribute.createDiffuse(Color.GREEN);
        ColorAttribute matD = ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f));
        ColorAttribute matS = ColorAttribute.createDiffuse(Color.LIGHT_GRAY);
        IntAttribute    noCull = IntAttribute.createCullFace(GL20.GL_NONE);

// create Materials
        Material grassMaterial = new Material(
            ColorAttribute.createDiffuse(Color.GREEN),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material dirtMaterial = new Material(
            ColorAttribute.createDiffuse(new Color(0.6f, 0.4f, 0.2f, 1f)),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material stoneMaterial = new Material(
            ColorAttribute.createDiffuse(Color.LIGHT_GRAY),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

// bake into a single Model with three named parts
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("grass", grassMesh, GL20.GL_TRIANGLES, grassMaterial);
        mb.part("dirt",  dirtMesh,  GL20.GL_TRIANGLES, dirtMaterial);
        mb.part("stone", stoneMesh, GL20.GL_TRIANGLES, stoneMaterial);
        Model model = mb.end();

        return new ModelInstance(model);
    }

    // build a simple 2D mask for X = constant
    private byte[][] buildMaskX(Chunk c, int x) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++)
            for (int z = 0; z < SIZE; z++)
                m[y][z] = c.getBlock(Math.min(x, SIZE-1), y, z);
        return m;
    }
    // Y = constant
    private byte[][] buildMaskY(Chunk c, int y) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int z = 0; z < SIZE; z++)
                m[x][z] = c.getBlock(x, Math.min(y, SIZE-1), z);
        return m;
    }
    // Z = constant
    private byte[][] buildMaskZ(Chunk c, int z) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                m[x][y] = c.getBlock(x, y, Math.min(z, SIZE-1));
        return m;
    }

    /**
     * Greedily merge runs of identical block‐IDs in this 2D mask,
     * then emit one big quad per rectangle.
     *
     * @param mask 2D block‐ID grid
     * @param coord the fixed coordinate on axis a
     * @param a axis index of that coord (0=X,1=Y,2=Z)
     * @param b axis of mask rows
     * @param normal which face we’re emitting
     */
    private void mergeMask(byte[][] mask,
                           int coord, int a, int b,
                           Vector3 normal)
    {
        int rows = mask.length, cols = mask[0].length;
        for (int u = 0; u < rows; u++) {
            for (int v = 0; v < cols; ) {
                byte id = mask[u][v];
                if (id == BlockType.AIR) { v++; continue; }
                // measure width
                int w = 1;
                while (v + w < cols && mask[u][v + w] == id) w++;
                // measure height
                int h = 1;
                outer:
                while (u + h < rows) {
                    for (int k = 0; k < w; k++)
                        if (mask[u + h][v + k] != id) break outer;
                    h++;
                }

                // build world‐space corners
                float low[]  = new float[3], high[] = new float[3];
                float comp = (a == 0 ? normal.x
                    : (a == 1 ? normal.y
                    : normal.z));
                float off  = comp > 0 ? 1f : 0f;
                low[a]  = coord + off;   high[a]  = low[a];
                int c = 3 - (a + b); // because 0+1+2=3
                low[b]  = u;            high[b]  = u + h;
                low[c]  = v;            high[c]  = v + w;

                Vector3 p1 = new Vector3(low[0],  low[1],  low[2]);
                Vector3 p2 = new Vector3(high[0], low[1],  low[2]);
                Vector3 p3 = new Vector3(high[0], high[1], high[2]);
                Vector3 p4 = new Vector3(low[0],  high[1], high[2]);

                // emit that quad
                emitQuad(id, p1,p2,p3,p4, normal);

                // zero out so we don’t re‐emit
                for (int uu = 0; uu < h; uu++)
                    for (int vv = 0; vv < w; vv++)
                        mask[u + uu][v + vv] = BlockType.AIR;

                v += w;
            }
        }
    }

    /**
     * Choose the correct buffer (grass/dirt/stone), write 6 verts + 6 idx.
     */
    private void emitQuad(byte id,
                          Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4,
                          Vector3 n)
    {
        List<Float>  vBuf;
        List<Short>  iBuf;
        short        base;
        if      (id == BlockType.GRASS) { vBuf = grassV; base = gBase; iBuf = grassI; }
        else if (id == BlockType.SOIL)  { vBuf = dirtV;  base = dBase; iBuf = dirtI; }
        else if (id == BlockType.STONE) { vBuf = stoneV; base = sBase; iBuf = stoneI; }
        else return;

        boolean flip = n.x + n.y + n.z < 0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};

        // write verts
        for (Vector3 v : quad) {
            vBuf.add(v.x); vBuf.add(v.y); vBuf.add(v.z);
            vBuf.add(n.x); vBuf.add(n.y); vBuf.add(n.z);
        }
        // write indices
        for (short k = 0; k < 6; k++) {
            iBuf.add((short)(base + k));
        }
        // bump the base
        if      (id == BlockType.GRASS) gBase += 6;
        else if (id == BlockType.SOIL)  dBase += 6;
        else                             sBase += 6;
    }

    // helper to turn lists into a Mesh
    private Mesh makeMesh(List<Float> v, List<Short> i) {
        Mesh m = new Mesh(true,
            v.size() / VERTEX_SIZE,
            i.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        m.setVertices(toFloatArray(v));
        m.setIndices (toShortArray(i));
        return m;
    }

    private float[] toFloatArray(List<Float> list) {
        float[] a = new float[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }
    private short[] toShortArray(List<Short> list) {
        short[] a = new short[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }
}
