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
 * Builds a greedy‐meshed chunk as a ModelInstance, splitting grass‐tops from soil sides.
 */
public class ChunkMeshBuilder {
    private static final int SIZE = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    // temporary buffers for grass‐tops and soil
    private final List<Float> grassVerts = new ArrayList<>();
    private final List<Short> grassIdx   = new ArrayList<>();
    private final List<Float> soilVerts  = new ArrayList<>();
    private final List<Short> soilIdx    = new ArrayList<>();
    private short grassIndex = 0, soilIndex = 0;

    private byte[][] buildMaskX(Chunk c, int x) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++)
            for (int z = 0; z < SIZE; z++)
                mask[y][z] = c.getBlock(x, y, z);
        return mask;
    }

    private byte[][] buildMaskY(Chunk c, int y) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int z = 0; z < SIZE; z++)
                mask[x][z] = c.getBlock(x, y, z);
        return mask;
    }

    private byte[][] buildMaskZ(Chunk c, int z) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                mask[x][y] = c.getBlock(x, y, z);
        return mask;
    }

    public ModelInstance buildChunkMesh(Chunk chunk) {
        grassVerts.clear();
        grassIdx.clear();
        soilVerts.clear();
        soilIdx.clear();
        grassIndex = soilIndex = 0;

        // 1) X-axis masks
// 1) X‑axis faces: two masks per slice
        for (int x = 0; x <= SIZE; x++) {
            // +X faces at x: blocks at [x][y][z] that have an empty neighbor at x-1
            byte[][] maskPX = new byte[SIZE][SIZE];
            // –X faces at x: blocks at [x-1][y][z] that have an empty neighbor at x
            byte[][] maskNX = new byte[SIZE][SIZE];

            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean in = chunk.inBounds(x, y, z) && chunk.getBlock(x, y, z) != 0;
                    boolean inPrev = chunk.inBounds(x-1, y, z) && chunk.getBlock(x-1, y, z) != 0;

                    // if there’s a block at x but not at x-1, we expose +X
                    if (in && !inPrev) maskPX[y][z] = chunk.getBlock(x, y, z);

                    // if there’s a block at x-1 but not at x, we expose –X
                    if (inPrev && !in) maskNX[y][z] = chunk.getBlock(x-1, y, z);
                }
            }

            // now merge each mask with the correct normal
            mergeMaskX(maskPX, x, new Vector3(+1,0,0), false);
            mergeMaskX(maskNX, x, new Vector3(-1,0,0), false);
        }
        for (int y = 0; y <= SIZE; y++) {
            byte[][] maskPY = new byte[SIZE][SIZE], maskNY = new byte[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean here     = chunk.inBounds(x, y, z)     && chunk.getBlock(x,y,z)     != 0;
                    boolean below    = chunk.inBounds(x, y-1, z) && chunk.getBlock(x,y-1,z) != 0;
                    // +Y face if block here but none below
                    if (here && !below) maskPY[x][z] = chunk.getBlock(x,y,z);
                    // –Y if block below but none here
                    if (below && !here) maskNY[x][z] = chunk.getBlock(x,y-1,z);
                }
            }
            mergeMaskY(maskPY, y, new Vector3(0,+1,0), true);   // grass‐tops only on +Y
            mergeMaskY(maskNY, y, new Vector3(0,-1,0), false);
        }
        for (int z = 0; z <= SIZE; z++) {
            byte[][] maskPZ = new byte[SIZE][SIZE], maskNZ = new byte[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    boolean here  = chunk.inBounds(x, y, z)     && chunk.getBlock(x,y,z)     != 0;
                    boolean front = chunk.inBounds(x, y, z-1) && chunk.getBlock(x,y,z-1) != 0;
                    if (here && !front) maskPZ[x][y] = chunk.getBlock(x,y,z);
                    if (front && !here) maskNZ[x][y] = chunk.getBlock(x,y,z-1);
                }
            }
            mergeMaskZ(maskPZ, z, new Vector3(0,0,+1), false);
            mergeMaskZ(maskNZ, z, new Vector3(0,0,-1), false);
        }


        // 4) build grass mesh
        Mesh grassMesh = new Mesh(true,
            grassVerts.size() / VERTEX_SIZE,
            grassIdx.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        grassMesh.setVertices(toFloatArray(grassVerts));
        grassMesh.setIndices(toShortArray(grassIdx));

        // 5) build soil mesh
        Mesh soilMesh = new Mesh(true,
            soilVerts.size() / VERTEX_SIZE,
            soilIdx.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        soilMesh.setVertices(toFloatArray(soilVerts));
        soilMesh.setIndices(toShortArray(soilIdx));

// 6) create Material instances (bundle your attributes)
        Material grassMat = new Material(
            ColorAttribute.createDiffuse(Color.GREEN),
            ColorAttribute.createSpecular(0.2f, 0.2f, 0.2f, 1f),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        Material soilMat = new Material(
            ColorAttribute.createDiffuse(new Color(0.6f, 0.4f, 0.2f, 1f)),
            ColorAttribute.createSpecular(0.1f, 0.1f, 0.1f, 1f),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

// 7) bake into one Model with two parts
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("grass", grassMesh, GL20.GL_TRIANGLES, grassMat);
        mb.part("soil",  soilMesh,  GL20.GL_TRIANGLES, soilMat);
        Model model = mb.end();

        return new ModelInstance(model);
    }


    // —————————————————————————————
    // Mask‐merging for +X/–X
    private void mergeMaskX(byte[][] mask, int x, Vector3 normal, boolean isTopFace) {
        int dimY = mask.length, dimZ = mask[0].length;
        for (int y = 0; y < dimY; y++) {
            for (int z = 0; z < dimZ; ) {
                byte b = mask[y][z];
                if (b == 0) { z++; continue; }
                int w = 1;
                while (z+w<dimZ && mask[y][z+w]==b) w++;
                int h = 1;
                outer: while (y+h<dimY) {
                    for (int k=0; k<w; k++)
                        if (mask[y+h][z+k]!=b) break outer;
                    h++;
                }
                float x0 = x + (normal.x>0?1f:0f),
                    y0 = y, z0 = z,
                    y1 = y+h, z1 = z+w;
                // world‐space corners
                Vector3 p1 = new Vector3(x0,y0,z0),
                    p2 = new Vector3(x0,y1,z0),
                    p3 = new Vector3(x0,y1,z1),
                    p4 = new Vector3(x0,y0,z1);
                addQuad(p1,p2,p3,p4, normal, isTopFace);
                // zero‐out
                for (int dy=0; dy<h; dy++)
                    for (int dz2=0; dz2<w; dz2++)
                        mask[y+dy][z+dz2] = 0;
                z += w;
            }
        }
    }

    // —————————————————————————————
    // Mask‐merging for +Y/–Y
    private void mergeMaskY(byte[][] mask, int y, Vector3 normal, boolean isTopFace) {
        int dimX = mask.length, dimZ = mask[0].length;
        for (int x = 0; x < dimX; x++) {
            for (int z = 0; z < dimZ; ) {
                byte b = mask[x][z];
                if (b == 0) { z++; continue; }
                int w = 1;
                while (z+w<dimZ && mask[x][z+w]==b) w++;
                int h = 1;
                outer: while (x+h<dimX) {
                    for (int k=0; k<w; k++)
                        if (mask[x+h][z+k]!=b) break outer;
                    h++;
                }
                float x0 = x, z0 = z,
                    x1 = x+h, z1 = z+w,
                    y0 = y + (normal.y>0?1f:0f);
                Vector3 p1 = new Vector3(x0,y0,z0),
                    p2 = new Vector3(x1,y0,z0),
                    p3 = new Vector3(x1,y0,z1),
                    p4 = new Vector3(x0,y0,z1);
                addQuad(p1,p2,p3,p4, normal, isTopFace);
                for (int dx=0; dx<h; dx++)
                    for (int dz2=0; dz2<w; dz2++)
                        mask[x+dx][z+dz2] = 0;
                z += w;
            }
        }
    }

    // —————————————————————————————
    // Mask‐merging for +Z/–Z
    private void mergeMaskZ(byte[][] mask, int z, Vector3 normal, boolean isTopFace) {
        int dimX = mask.length, dimY = mask[0].length;
        for (int x = 0; x < dimX; x++) {
            for (int y = 0; y < dimY; ) {
                byte b = mask[x][y];
                if (b == 0) { y++; continue; }
                int w = 1;
                while (y+w<dimY && mask[x][y+w]==b) w++;
                int h = 1;
                outer: while (x+h<dimX) {
                    for (int k=0; k<w; k++)
                        if (mask[x+h][y+k]!=b) break outer;
                    h++;
                }
                float x0 = x, y0 = y,
                    x1 = x+h, y1 = y+w,
                    z0 = z + (normal.z>0?1f:0f);
                Vector3 p1 = new Vector3(x0,y0,z0),
                    p2 = new Vector3(x1,y0,z0),
                    p3 = new Vector3(x1,y1,z0),
                    p4 = new Vector3(x0,y1,z0);
                addQuad(p1,p2,p3,p4, normal, isTopFace);
                for (int dx=0; dx<h; dx++)
                    for (int dy=0; dy<w; dy++)
                        mask[x+dx][y+dy] = 0;
                y += w;
            }
        }
    }

    // —————————————————————————————
    /** Emits 6 vertices (2 triangles) CCW, and splits grass‐tops vs soil. */
    private void addQuad(Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4,
                         Vector3 normal, boolean isTopFace)
    {
        List<Float> vBuff = isTopFace ? grassVerts : soilVerts;
        List<Short> iBuff = isTopFace ? grassIdx   : soilIdx;
        short base = isTopFace ? grassIndex : soilIndex;

        // determine if we need to flip (negative normals)
        boolean flip = (normal.x + normal.y + normal.z) < 0;

        // local helper to push one vertex
        for (Vector3 vert : flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1})
        {
            vBuff.add(vert.x);
            vBuff.add(vert.y);
            vBuff.add(vert.z);
            vBuff.add(normal.x);
            vBuff.add(normal.y);
            vBuff.add(normal.z);
        }
        // indices
        for (short i = 0; i < 6; i++) {
            iBuff.add((short)(base + i));
        }
        if (isTopFace) grassIndex += 6; else soilIndex += 6;
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
