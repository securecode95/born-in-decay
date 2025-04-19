package com.rabalder.bornindecay;

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
 * Naive voxel‐mesher: emits one quad per exposed face.
 * Splits grass‐tops (+Y on GRASS) into a green mesh, all other GRASS/SOIL into a brown “dirt” mesh,
 * and STONE into a gray mesh.
 */
public class ChunkMeshBuilder {
    private static final int SIZE = Chunk.SIZE;
    private static final int VERTEX_SIZE = 6; // x,y,z + normal x,y,z

    // per‐material buffers
    private final List<Float> grassVerts = new ArrayList<>();
    private final List<Short> grassIdx   = new ArrayList<>();
    private short grassBase = 0;

    private final List<Float> dirtVerts  = new ArrayList<>();
    private final List<Short> dirtIdx    = new ArrayList<>();
    private short dirtBase  = 0;

    private final List<Float> stoneVerts = new ArrayList<>();
    private final List<Short> stoneIdx   = new ArrayList<>();
    private short stoneBase = 0;

    // 6 cardinal normals
    private static final Vector3[] NORMALS = {
        new Vector3( 1,  0,  0),  // +X
        new Vector3(-1,  0,  0),  // -X
        new Vector3( 0,  1,  0),  // +Y
        new Vector3( 0, -1,  0),  // -Y
        new Vector3( 0,  0,  1),  // +Z
        new Vector3( 0,  0, -1)   // -Z
    };

    // neighbor offsets for each face
    private static final int[][] OFFS = {
        { 1,  0,  0},
        {-1,  0,  0},
        { 0,  1,  0},
        { 0, -1,  0},
        { 0,  0,  1},
        { 0,  0, -1}
    };

    // for each face, the four corner offsets (in [x,y,z] order)
    // arranged so that two triangles (0,1,2) and (2,3,0) are CCW.
    private static final int[][][] CORNERS = {
        // +X
        {{1,0,0},{1,1,0},{1,1,1},{1,0,1}},
        // -X
        {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
        // +Y
        {{0,1,0},{1,1,0},{1,1,1},{0,1,1}},
        // -Y
        {{0,0,1},{1,0,1},{1,0,0},{0,0,0}},
        // +Z
        {{0,0,1},{0,1,1},{1,1,1},{1,0,1}},
        // -Z
        {{1,0,0},{1,1,0},{0,1,0},{0,0,0}}
    };

    public ModelInstance buildChunkMesh(Chunk chunk) {
        // clear all buffers
        grassVerts.clear(); grassIdx.clear(); grassBase = 0;
        dirtVerts.clear();  dirtIdx.clear();  dirtBase  = 0;
        stoneVerts.clear(); stoneIdx.clear(); stoneBase = 0;

        // for every block in the chunk...
        for(int x = 0; x < SIZE; x++){
            for(int y = 0; y < SIZE; y++){
                for(int z = 0; z < SIZE; z++){
                    byte type = chunk.getBlock(x,y,z);
                    if(type == BlockType.AIR) continue;
                    // test its 6 neighbors
                    for(int f = 0; f < 6; f++){
                        int nx = x + OFFS[f][0];
                        int ny = y + OFFS[f][1];
                        int nz = z + OFFS[f][2];

                        boolean exposed;
                        if(nx < 0|| nx >= SIZE
                            || ny < 0|| ny >= SIZE
                            || nz < 0|| nz >= SIZE) {
                            exposed = true;
                        } else {
                            exposed = chunk.getBlock(nx,ny,nz) == BlockType.AIR;
                        }
                        if(exposed) emitFace(type, x,y,z, f);
                    }
                }
            }
        }

        // build Meshes
        Mesh grassMesh = makeMesh(grassVerts, grassIdx);
        Mesh dirtMesh  = makeMesh(dirtVerts,  dirtIdx);
        Mesh stoneMesh = makeMesh(stoneVerts, stoneIdx);

        // create Materials
        ColorAttribute grassMat = ColorAttribute.createDiffuse(Color.GREEN);
        ColorAttribute dirtMat  = ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f));
        ColorAttribute stoneMat = ColorAttribute.createDiffuse(Color.LIGHT_GRAY);
        IntAttribute    cullBack = IntAttribute.createCullFace(GL20.GL_BACK);

        // bake them into one Model with three parts
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("grass", grassMesh, GL20.GL_TRIANGLES, grassMat, cullBack);
        mb.part("dirt",  dirtMesh,  GL20.GL_TRIANGLES, dirtMat,  cullBack);
        mb.part("stone", stoneMesh, GL20.GL_TRIANGLES, stoneMat, cullBack);
        Model m = mb.end();

        return new ModelInstance(m);
    }

    private Mesh makeMesh(List<Float> v, List<Short> i) {
        Mesh m = new Mesh(true, v.size()/VERTEX_SIZE, i.size(),
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal"));
        m.setVertices(toFloatArray(v));
        m.setIndices(toShortArray(i));
        return m;
    }

    private void emitFace(byte type, int x, int y, int z, int face) {
        Vector3 n = NORMALS[face];
        int[][] c = CORNERS[face];

        // decide which buffer+base to use
        List<Float>  bufV;
        List<Short>  bufI;
        short        base;
        // only the +Y face of a GRASS block becomes “grass”
        if(type == BlockType.GRASS && face == 2) {
            bufV = grassVerts; bufI = grassIdx; base = grassBase;
        } else if(type == BlockType.GRASS || type == BlockType.SOIL) {
            bufV = dirtVerts;  bufI = dirtIdx;  base = dirtBase;
        } else { // STONE
            bufV = stoneVerts; bufI = stoneIdx; base = stoneBase;
        }

        // push 6 verts (2 tris)
        for(int k=0;k<6;k++){
            int iv = (k<3 ? k : (5-k));
            int iw = (k<3 ? (k+1)%3 : (4-k));
            int[] c1 = c[iv], c2 = c[iw];
            bufV.add((float) (x + c1[0]));
            bufV.add((float) (y + c1[1]));
            bufV.add((float) (z + c1[2]));
            bufV.add(n.x);
            bufV.add(n.y);
            bufV.add(n.z);
        }

        // push indices
        for(short k=0;k<6;k++){
            bufI.add((short)(base + k));
        }

        // advance the correct base
        if(type == BlockType.GRASS && face == 2) grassBase += 6;
        else if(type == BlockType.GRASS || type == BlockType.SOIL) dirtBase += 6;
        else stoneBase += 6;
    }

    private float[] toFloatArray(List<Float> list) {
        float[] a = new float[list.size()];
        for(int i=0;i<a.length;i++) a[i] = list.get(i);
        return a;
    }

    private short[] toShortArray(List<Short> list) {
        short[] a = new short[list.size()];
        for(int i=0;i<a.length;i++) a[i] = list.get(i);
        return a;
    }
}
