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
 * Greedy‐meshes a Chunk into two Model parts:
 *  - grass‐tops (green) for +Y faces of grass blocks,
 *  - soil (brown) for every other exposed face.
 */
public class ChunkMeshBuilder {
    private static final int SIZE = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    // temporary buffers
    private final List<Float> grassVerts = new ArrayList<>();
    private final List<Short> grassIdx   = new ArrayList<>();
    private final List<Float> soilVerts  = new ArrayList<>();
    private final List<Short> soilIdx    = new ArrayList<>();
    private short grassIndex = 0, soilIndex = 0;

    public ModelInstance buildChunkMesh(Chunk chunk) {
        grassVerts.clear(); grassIdx.clear();
        soilVerts.clear();  soilIdx.clear();
        grassIndex = soilIndex = 0;

        // X‐axis masks (+X and –X)
        for (int x = 0; x <= SIZE; x++) {
            byte[][] mask = new byte[SIZE][SIZE];
            for (int y = 0; y < SIZE; y++) for (int z = 0; z < SIZE; z++) {
                boolean here = x < SIZE  && chunk.getBlock(x,y,z)   != 0;
                boolean prev = x > 0      && chunk.getBlock(x-1,y,z) != 0;
                if (here && !prev) mask[y][z] = chunk.getBlock(x,y,z);
                else if (prev && !here) mask[y][z] = chunk.getBlock(x-1,y,z);
                else mask[y][z] = 0;
            }
            mergeMaskX(mask, x, new Vector3(+1,0,0), false);
            mergeMaskX(mask, x, new Vector3(-1,0,0), false);
        }

        // Y‐axis masks (+Y grass‐tops, –Y soil)
        for (int y = 0; y <= SIZE; y++) {
            byte[][] mask = new byte[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) {
                boolean here  = y < SIZE  && chunk.getBlock(x,y,z)   != 0;
                boolean below = y > 0     && chunk.getBlock(x,y-1,z) != 0;
                if (here && !below) mask[x][z] = chunk.getBlock(x,y,z);
                else if (below && !here) mask[x][z] = chunk.getBlock(x,y-1,z);
                else mask[x][z] = 0;
            }
            mergeMaskY(mask, y, new Vector3(0,+1,0), true);
            mergeMaskY(mask, y, new Vector3(0,-1,0), false);
        }

        // Z‐axis masks (+Z and –Z)
        for (int z = 0; z <= SIZE; z++) {
            byte[][] mask = new byte[SIZE][SIZE];
            for (int x = 0; x < SIZE; x++) for (int y = 0; y < SIZE; y++) {
                boolean here  = z < SIZE  && chunk.getBlock(x,y,z)   != 0;
                boolean prev  = z > 0     && chunk.getBlock(x,y,z-1) != 0;
                if (here && !prev) mask[x][y] = chunk.getBlock(x,y,z);
                else if (prev && !here) mask[x][y] = chunk.getBlock(x,y,z-1);
                else mask[x][y] = 0;
            }
            mergeMaskZ(mask, z, new Vector3(0,0,+1), false);
            mergeMaskZ(mask, z, new Vector3(0,0,-1), false);
        }

        // build grass mesh
        Mesh grassMesh = new Mesh(true,
            grassVerts.size()/VERTEX_SIZE, grassIdx.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        grassMesh.setVertices(toFloatArray(grassVerts));
        grassMesh.setIndices (toShortArray(grassIdx));

        // build soil mesh
        Mesh soilMesh = new Mesh(true,
            soilVerts.size()/VERTEX_SIZE, soilIdx.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        soilMesh.setVertices(toFloatArray(soilVerts));
        soilMesh.setIndices (toShortArray(soilIdx));

        // materials (no culling)
        Material grassMat = new Material(
            ColorAttribute.createDiffuse(Color.GREEN),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material soilMat = new Material(
            ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f)),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        // bake into Model
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("grass", grassMesh, GL20.GL_TRIANGLES, grassMat);
        mb.part("soil",  soilMesh,  GL20.GL_TRIANGLES, soilMat);
        Model m = mb.end();
        return new ModelInstance(m);
    }

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
                Vector3 p1 = new Vector3(x0,y0,z0),
                    p2 = new Vector3(x0,y1,z0),
                    p3 = new Vector3(x0,y1,z1),
                    p4 = new Vector3(x0,y0,z1);
                addQuad(p1,p2,p3,p4, normal, isTopFace);
                for (int dy=0; dy<h; dy++)
                    for (int dz2=0; dz2<w; dz2++)
                        mask[y+dy][z+dz2] = 0;
                z += w;
            }
        }
    }

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

    /**
     * Pushes two triangles (6 vertices) CCW, splitting into grass vs soil.
     */
    private void addQuad(Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4,
                         Vector3 normal, boolean isTopFace)
    {
        List<Float> vBuff = isTopFace ? grassVerts : soilVerts;
        List<Short> iBuff = isTopFace ? grassIdx   : soilIdx;
        short base = isTopFace ? grassIndex : soilIndex;
        boolean flip = (normal.x + normal.y + normal.z) < 0;
        Vector3[] seq = flip
            ? new Vector3[]{p1,p4,p3, p3,p2,p1}
            : new Vector3[]{p1,p2,p3, p3,p4,p1};
        for (Vector3 v : seq) {
            vBuff.add(v.x); vBuff.add(v.y); vBuff.add(v.z);
            vBuff.add(normal.x); vBuff.add(normal.y); vBuff.add(normal.z);
        }
        for (short i=0; i<6; i++) iBuff.add((short)(base + i));
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
