package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.GL20;
import java.util.ArrayList;

/**
 * Builds a greedy‑meshed chunk: merges adjacent faces on X, Y and Z slices.
 */
public class ChunkMeshBuilder {
    private static final int VERTEX_SIZE = 3 + 3; // pos + normal

    private final ArrayList<Float> vertices = new ArrayList<>();
    private final ArrayList<Short> indices = new ArrayList<>();
    private short index = 0;

    public ChunkMeshBuilder() {}

    // Low‑level: add a quad (p1→p2→p3 + p3→p4→p1)
    public void addFace(Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4, Vector3 normal) {
        addVertex(p1, normal);
        addVertex(p2, normal);
        addVertex(p3, normal);
        addVertex(p3, normal);
        addVertex(p4, normal);
        addVertex(p1, normal);
        
    }

    private void addVertex(Vector3 pos, Vector3 normal) {
        vertices.add(pos.x);
        vertices.add(pos.y);
        vertices.add(pos.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        indices.add(index++);
    }

    /** Build the low‑level Mesh object from accumulated verts+inds. **/
    public ChunkMesh build() {
        // 1) convert your ArrayLists into primitive arrays
        float[] vertArray = new float[vertices.size()];
        for (int i = 0; i < vertArray.length; i++) {
            vertArray[i] = vertices.get(i);
        }

        short[] indArray = new short[indices.size()];
        for (int i = 0; i < indArray.length; i++) {
            indArray[i] = indices.get(i);
        }

        // 2) create the Mesh
        Mesh mesh = new Mesh(
            true,
            vertArray.length / VERTEX_SIZE, // num vertices
            indArray.length,                // num indices
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal,   3, "a_normal")
        );
        mesh.setVertices(vertArray);
        mesh.setIndices(indArray);

        // 3) bake the Mesh into a Model
        Material mat = new Material(ColorAttribute.createDiffuse(Color.WHITE),IntAttribute.createCullFace(GL20.GL_NONE));
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("chunk", mesh, GL20.GL_TRIANGLES, mat);
        Model model = mb.end();

        // 4) return both mesh+model wrapped in ChunkMesh
        return new ChunkMesh(mesh, model);
    }



    /**
     * Greedy‑mesh the six directions (+X,‑X; +Y,‑Y; +Z,‑Z).
     */
    public ModelInstance buildChunkMesh(Chunk chunk) {
        // 1) Reset our buffers
        vertices.clear();
        indices.clear();
        index = 0;

        // 2) A reusable 2D mask for each slice
        byte[][] mask = new byte[Chunk.SIZE][Chunk.SIZE];

        // ---- X‑axis faces (+X = EAST, -X = WEST) ----
        for (int x = 0; x < Chunk.SIZE; x++) {
            // +X (EAST)
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    byte cur  = chunk.blocks[x][y][z];
                    byte next = (x+1 < Chunk.SIZE ? chunk.blocks[x+1][y][z] : Chunk.AIR);
                    mask[y][z] = (cur != Chunk.AIR && next == Chunk.AIR) ? cur : 0;
                }
            }
            mergeMaskX(mask, x, Direction.EAST);

            // -X (WEST)
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    byte cur  = chunk.blocks[x][y][z];
                    byte prev = (x-1 >= 0      ? chunk.blocks[x-1][y][z] : Chunk.AIR);
                    mask[y][z] = (cur != Chunk.AIR && prev == Chunk.AIR) ? cur : 0;
                }
            }
            mergeMaskX(mask, x, Direction.WEST);
        }

        // ---- Y‑axis faces (+Y = UP, -Y = DOWN) ----
        for (int y = 0; y < Chunk.SIZE; y++) {
            // +Y (UP)
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    byte cur  = chunk.blocks[x][y][z];
                    byte next = (y+1 < Chunk.SIZE ? chunk.blocks[x][y+1][z] : Chunk.AIR);
                    mask[x][z] = (cur != Chunk.AIR && next == Chunk.AIR) ? cur : 0;
                }
            }
            mergeMaskY(mask, y, Direction.UP);

            // -Y (DOWN)
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    byte cur  = chunk.blocks[x][y][z];
                    byte prev = (y-1 >= 0      ? chunk.blocks[x][y-1][z] : Chunk.AIR);
                    mask[x][z] = (cur != Chunk.AIR && prev == Chunk.AIR) ? cur : 0;
                }
            }
            mergeMaskY(mask, y, Direction.DOWN);
        }

        // ---- Z‑axis faces (+Z = SOUTH, -Z = NORTH) ----
        for (int z = 0; z < Chunk.SIZE; z++) {
            // +Z (SOUTH)
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    byte cur  = chunk.blocks[x][y][z];
                    byte next = (z+1 < Chunk.SIZE ? chunk.blocks[x][y][z+1] : Chunk.AIR);
                    mask[x][y] = (cur != Chunk.AIR && next == Chunk.AIR) ? cur : 0;
                }
            }
            mergeMaskZ(mask, z, Direction.SOUTH);

            // -Z (NORTH)
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    byte cur  = chunk.blocks[x][y][z];
                    byte prev = (z-1 >= 0      ? chunk.blocks[x][y][z-1] : Chunk.AIR);
                    mask[x][y] = (cur != Chunk.AIR && prev == Chunk.AIR) ? cur : 0;
                }
            }
            mergeMaskZ(mask, z, Direction.NORTH);
        }

        // 3) Build the low‑level Mesh and bake into a ModelInstance
        ChunkMesh chunkMesh = build();          // build() now returns your greedy‑meshed ChunkMesh
        Mesh mesh           = chunkMesh.mesh;   // extract the Mesh
        Material mat        = new Material(ColorAttribute.createDiffuse(Color.WHITE),IntAttribute.createCullFace(GL20.GL_NONE));

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("chunk", mesh, GL20.GL_TRIANGLES, mat);
        Model model = mb.end();

        return new ModelInstance(model);
    }


    // ---- Helper: merge a mask on an X‑slice at fixed x ----
    private void mergeMaskX(byte[][] mask, int x, Direction dir) {
        int H = Chunk.SIZE, W = Chunk.SIZE;
        for (int y = 0; y < H; y++) {
            for (int z = 0; z < W; z++) {
                byte type = mask[y][z];
                if (type == 0) continue;

                // find width
                int w;
                for (w = 1; z + w < W && mask[y][z + w] == type; w++) {}

                // find height
                int h = 1;
                outer: for (; y + h < H; h++) {
                    for (int k = 0; k < w; k++) {
                        if (mask[y + h][z + k] != type) break outer;
                    }
                }

                // zero out
                for (int dy = 0; dy < h; dy++)
                    for (int dx = 0; dx < w; dx++)
                        mask[y + dy][z + dx] = 0;

                emitQuadX(x, y, z, w, h, dir);
            }
        }
    }

    private void emitQuadX(int x, int y, int z, int w, int h, Direction dir) {
        float x0 = (dir == Direction.EAST ? x + 1 : x);
        float y0 = y,   z0 = z;
        float y1 = y + h, z1 = z + w;
        Vector3 n = dir.normal;

        Vector3 p1 = new Vector3(x0, y0, z0);
        Vector3 p2 = new Vector3(x0, y1, z0);
        Vector3 p3 = new Vector3(x0, y1, z1);
        Vector3 p4 = new Vector3(x0, y0, z1);

        if (dir == Direction.WEST) {
            addFace(p1, p4, p3, p2, n);
        } else {
            addFace(p1, p2, p3, p4, n);
        }
    }

    // ---- Helper: merge a mask on a Y‑slice at fixed y ----
    private void mergeMaskY(byte[][] mask, int y, Direction dir) {
        int H = Chunk.SIZE, W = Chunk.SIZE;
        for (int x = 0; x < H; x++) {
            for (int z = 0; z < W; z++) {
                byte type = mask[x][z];
                if (type == 0) continue;

                int w;
                for (w = 1; z + w < W && mask[x][z + w] == type; w++) {}

                int h = 1;
                outer: for (; x + h < H; h++) {
                    for (int k = 0; k < w; k++) {
                        if (mask[x + h][z + k] != type) break outer;
                    }
                }

                for (int dx = 0; dx < h; dx++)
                    for (int dz = 0; dz < w; dz++)
                        mask[x + dx][z + dz] = 0;

                emitQuadY(y, x, z, w, h, dir);
            }
        }
    }

    private void emitQuadY(int y, int x, int z, int w, int h, Direction dir) {
        float y0 = (dir == Direction.UP ? y + 1 : y);
        float x0 = x,     z0 = z;
        float x1 = x + w, z1 = z + h;
        Vector3 n = dir.normal;

        Vector3 p1 = new Vector3(x0, y0, z0);
        Vector3 p2 = new Vector3(x0, y0, z1);
        Vector3 p3 = new Vector3(x1, y0, z1);
        Vector3 p4 = new Vector3(x1, y0, z0);

        if (dir == Direction.DOWN) {
            addFace(p1, p4, p3, p2, n);
        } else {
            addFace(p1, p2, p3, p4, n);
        }
    }

    // ---- Helper: merge a mask on a Z‑slice at fixed z ----
    private void mergeMaskZ(byte[][] mask, int z, Direction dir) {
        int H = Chunk.SIZE, W = Chunk.SIZE;
        for (int x = 0; x < H; x++) {
            for (int y = 0; y < W; y++) {
                byte type = mask[x][y];
                if (type == 0) continue;

                int w;
                for (w = 1; y + w < W && mask[x][y + w] == type; w++) {}

                int h = 1;
                outer: for (; x + h < H; h++) {
                    for (int k = 0; k < w; k++) {
                        if (mask[x + h][y + k] != type) break outer;
                    }
                }

                for (int dx = 0; dx < h; dx++)
                    for (int dy = 0; dy < w; dy++)
                        mask[x + dx][y + dy] = 0;

                emitQuadZ(z, x, y, w, h, dir);
            }
        }
    }

    private void emitQuadZ(int z, int x, int y, int w, int h, Direction dir) {
        float z0 = (dir == Direction.SOUTH ? z + 1 : z);
        float x0 = x,     y0 = y;
        float x1 = x + w, y1 = y + h;
        Vector3 n = dir.normal;

        Vector3 p1 = new Vector3(x0, y0, z0);
        Vector3 p2 = new Vector3(x0, y1, z0);
        Vector3 p3 = new Vector3(x1, y1, z0);
        Vector3 p4 = new Vector3(x1, y0, z0);

        if (dir == Direction.NORTH) {
            addFace(p1, p4, p3, p2, n);
        } else {
            addFace(p1, p2, p3, p4, n);
        }
    }
}
