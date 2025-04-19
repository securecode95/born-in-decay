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
 * Builds a greedy‐meshed chunk with separate meshes for grass, dirt and stone.
 */
public class ChunkMeshBuilder {
    private static final int SIZE = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + nx,ny,nz

    // per‐type vertex + index buffers
    private final List<Float> grassV = new ArrayList<>();
    private final List<Short> grassI = new ArrayList<>();
    private final List<Float> dirtV  = new ArrayList<>();
    private final List<Short> dirtI  = new ArrayList<>();
    private final List<Float> stoneV = new ArrayList<>();
    private final List<Short> stoneI = new ArrayList<>();
    private short grassIndex, dirtIndex, stoneIndex;

    public ModelInstance buildChunkMesh(Chunk chunk) {
        grassV.clear(); grassI.clear();
        dirtV.clear();  dirtI.clear();
        stoneV.clear(); stoneI.clear();
        grassIndex = dirtIndex = stoneIndex = 0;

        // X faces
        for (int x = 0; x <= SIZE; x++) {
            byte[][] mask = buildMaskX(chunk, x);
            mergeMaskX(mask, x, new Vector3(+1,0,0));
            mergeMaskX(mask, x, new Vector3(-1,0,0));
        }
        // Y faces (grass top only for normal.y>0)
        for (int y = 0; y <= SIZE; y++) {
            byte[][] mask = buildMaskY(chunk, y);
            mergeMaskY(mask, y, new Vector3(0,+1,0));
            mergeMaskY(mask, y, new Vector3(0,-1,0));
        }
        // Z faces
        for (int z = 0; z <= SIZE; z++) {
            byte[][] mask = buildMaskZ(chunk, z);
            mergeMaskZ(mask, z, new Vector3(0,0,+1));
            mergeMaskZ(mask, z, new Vector3(0,0,-1));
        }

        // build meshes
        Mesh grassMesh = makeMesh(grassV, grassI);
        Mesh dirtMesh  = makeMesh(dirtV,  dirtI);
        Mesh stoneMesh = makeMesh(stoneV, stoneI);

        // materials (disable back-face culling)
        Material matG = new Material(
            ColorAttribute.createDiffuse(Color.GREEN),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material matD = new Material(
            ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f)),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        Material matS = new Material(
            ColorAttribute.createDiffuse(Color.GRAY),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("grass", grassMesh, GL20.GL_TRIANGLES, matG);
        mb.part("dirt",  dirtMesh,  GL20.GL_TRIANGLES, matD);
        mb.part("stone", stoneMesh, GL20.GL_TRIANGLES, matS);
        Model model = mb.end();

        return new ModelInstance(model);
    }

    private Mesh makeMesh(List<Float> v, List<Short> i) {
        Mesh m = new Mesh(true,
            v.size()/VERTEX_SIZE,
            i.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        // to float/short arrays
        float[] fv = new float[v.size()];
        for (int j = 0; j < fv.length; j++) fv[j] = v.get(j);
        short[] si = new short[i.size()];
        for (int j = 0; j < si.length; j++) si[j] = i.get(j);
        m.setVertices(fv);
        m.setIndices(si);
        return m;
    }

    private byte[][] buildMaskX(Chunk c, int x) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int y=0; y<SIZE; y++) for (int z=0; z<SIZE; z++)
            mask[y][z] = c.getBlock(x, y, z);
        return mask;
    }
    private byte[][] buildMaskY(Chunk c, int y) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int x=0; x<SIZE; x++) for (int z=0; z<SIZE; z++)
            mask[x][z] = c.getBlock(x, y, z);
        return mask;
    }
    private byte[][] buildMaskZ(Chunk c, int z) {
        byte[][] mask = new byte[SIZE][SIZE];
        for (int x=0; x<SIZE; x++) for (int y=0; y<SIZE; y++)
            mask[x][y] = c.getBlock(x, y, z);
        return mask;
    }

    private void mergeMaskX(byte[][] mask, int x, Vector3 normal) {
        int dy = mask.length, dz = mask[0].length;
        for (int y=0; y<dy; y++) {
            for (int z=0; z<dz; ) {
                byte id = mask[y][z];
                if (id==BlockType.AIR) { z++; continue; }
                int w=1; while (z+w<dz && mask[y][z+w]==id) w++;
                int h=1; outer: while (y+h<dy) {
                    for (int k=0; k<w; k++) if (mask[y+h][z+k]!=id) break outer;
                    h++;
                }
                float x0 = x + (normal.x>0?1:0), y0=y, z0=z;
                float y1 = y+h, z1=z+w;
                Vector3 p1 = new Vector3(x0,y0,z0);
                Vector3 p2 = new Vector3(x0,y1,z0);
                Vector3 p3 = new Vector3(x0,y1,z1);
                Vector3 p4 = new Vector3(x0,y0,z1);
                emitQuad(id, p1,p2,p3,p4, normal);
                for (int yy=0; yy<h; yy++) for (int zz=0; zz<w; zz++) mask[y+yy][z+zz]=BlockType.AIR;
                z+=w;
            }
        }
    }
    private void mergeMaskY(byte[][] mask, int y, Vector3 normal) {
        int dx = mask.length, dz = mask[0].length;
        for (int x=0; x<dx; x++) {
            for (int z=0; z<dz; ) {
                byte id = mask[x][z]; if (id==BlockType.AIR) { z++; continue; }
                int w=1; while (z+w<dz && mask[x][z+w]==id) w++;
                int h=1; outer: while (x+h<dx) {
                    for (int k=0; k<w; k++) if (mask[x+h][z+k]!=id) break outer;
                    h++;
                }
                float x0=x, z0=z,
                    x1=x+h, z1=z+w,
                    y0=y + (normal.y>0?1:0);
                Vector3 p1=new Vector3(x0,y0,z0);
                Vector3 p2=new Vector3(x1,y0,z0);
                Vector3 p3=new Vector3(x1,y0,z1);
                Vector3 p4=new Vector3(x0,y0,z1);
                emitQuad(id,p1,p2,p3,p4,normal);
                for (int xx=0; xx<h; xx++) for (int zz=0; zz<w; zz++) mask[x+xx][z+zz]=BlockType.AIR;
                z+=w;
            }
        }
    }
    private void mergeMaskZ(byte[][] mask, int z, Vector3 normal) {
        int dx = mask.length, dy = mask[0].length;
        for (int x=0; x<dx; x++) {
            for (int y=0; y<dy; ) {
                byte id = mask[x][y]; if (id==BlockType.AIR) { y++; continue; }
                int w=1; while (y+w<dy && mask[x][y+w]==id) w++;
                int h=1; outer: while (x+h<dx) {
                    for (int k=0; k<w; k++) if (mask[x+h][y+k]!=id) break outer;
                    h++;
                }
                float x0=x, y0=y,
                    x1=x+h, y1=y+w,
                    z0=z + (normal.z>0?1:0);
                Vector3 p1=new Vector3(x0,y0,z0);
                Vector3 p2=new Vector3(x1,y0,z0);
                Vector3 p3=new Vector3(x1,y1,z0);
                Vector3 p4=new Vector3(x0,y1,z0);
                emitQuad(id,p1,p2,p3,p4,normal);
                for (int xx=0; xx<h; xx++) for (int yy=0; yy<w; yy++) mask[x+xx][y+yy]=BlockType.AIR;
                y+=w;
            }
        }
    }

    private void emitQuad(byte id,
                          Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4,
                          Vector3 normal) {
        List<Float> vBuf; List<Short> iBuf; short base;
        boolean top = (id==BlockType.GRASS && normal.y>0);
        if (top)      { vBuf=grassV; iBuf=grassI; base=grassIndex; }
        else if (id==BlockType.DIRT)  { vBuf=dirtV; iBuf=dirtI; base=dirtIndex; }
        else if (id==BlockType.STONE) { vBuf=stoneV; iBuf=stoneI; base=stoneIndex; }
        else return;
        boolean flip = (normal.x+normal.y+normal.z) < 0;
        Vector3[] quad = flip
            ? new Vector3[]{p1,p4,p3,p3,p2,p1}
            : new Vector3[]{p1,p2,p3,p3,p4,p1};
        for (Vector3 v : quad) {
            vBuf.add(v.x); vBuf.add(v.y); vBuf.add(v.z);
            vBuf.add(normal.x); vBuf.add(normal.y); vBuf.add(normal.z);
        }
        for (int i=0; i<6; i++) iBuf.add((short)(base + i));
        if (top)      grassIndex += 6;
        else if (id==BlockType.DIRT)  dirtIndex += 6;
        else if (id==BlockType.STONE) stoneIndex += 6;
    }
}
