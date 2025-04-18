package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.GL20;

import java.util.ArrayList;

public class ChunkMeshBuilder {
    private static final int VERTEX_SIZE = 3 + 3; // position + normal
    private final ArrayList<Float> vertices = new ArrayList<>();
    private final ArrayList<Short> indices = new ArrayList<>();
    private short index = 0;

    public ChunkMeshBuilder() {}

    // Add a face to the chunk mesh
    public void addFace(Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4, Vector3 normal) {
        addVertex(p1, normal);
        addVertex(p2, normal);
        addVertex(p3, normal);

        addVertex(p3, normal);
        addVertex(p4, normal);
        addVertex(p1, normal);
    }

    // Add a single vertex to the mesh data
    private void addVertex(Vector3 pos, Vector3 normal) {
        vertices.add(pos.x);
        vertices.add(pos.y);
        vertices.add(pos.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);

        indices.add(index++);
    }

    // Build the mesh
    public ChunkMesh build() {
        float[] verts = new float[vertices.size()];
        for (int i = 0; i < verts.length; i++) {
            verts[i] = vertices.get(i);
        }

        short[] inds = new short[indices.size()];
        for (int i = 0; i < inds.length; i++) {
            inds[i] = indices.get(i);
        }

        Mesh mesh = new Mesh(true, verts.length / VERTEX_SIZE, inds.length,
            new VertexAttribute(Usage.Position, 3, "a_position"),
            new VertexAttribute(Usage.Normal, 3, "a_normal"));

        mesh.setVertices(verts);
        mesh.setIndices(inds);

        return new ChunkMesh(mesh, null);  // Modify this to include Model if needed
    }

    // Greedy meshing logic to combine adjacent blocks into faces
    public ModelInstance buildChunkMesh(Chunk chunk) {
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    if (chunk.blocks[x][y][z] == Chunk.AIR) continue; // Skip empty blocks

                    // Merge faces in all directions if they are the same
                    if (isAir(chunk, x + 1, y, z)) addFace(createFace(x, y, z, Direction.EAST));
                    if (isAir(chunk, x - 1, y, z)) addFace(createFace(x, y, z, Direction.WEST));
                    if (isAir(chunk, x, y + 1, z)) addFace(createFace(x, y, z, Direction.UP));
                    if (isAir(chunk, x, y - 1, z)) addFace(createFace(x, y, z, Direction.DOWN));
                    if (isAir(chunk, x, y, z + 1)) addFace(createFace(x, y, z, Direction.NORTH));
                    if (isAir(chunk, x, y, z - 1)) addFace(createFace(x, y, z, Direction.SOUTH));
                }
            }
        }

        ChunkMesh chunkMesh = build();

        // Use the model to create the ModelInstance for the chunk
        ModelBuilder modelBuilder = new ModelBuilder();
        Model model = modelBuilder.createBox(1f, 1f, 1f,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Returning ModelInstance for rendering
        return new ModelInstance(model);
    }

    // Checks if a block is "air" or outside the chunk's bounds
    private boolean isAir(Chunk chunk, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= Chunk.SIZE || y >= Chunk.SIZE || z >= Chunk.SIZE) {
            return true; // Outside the chunk is considered air
        }
        return chunk.blocks[x][y][z] == Chunk.AIR;
    }

    // Creates a face for the given block coordinates and direction
    private Vector3[] createFace(int x, int y, int z, Direction dir) {
        float x0 = x, y0 = y, z0 = z;
        float x1 = x + 1, y1 = y + 1, z1 = z + 1;

        switch (dir) {
            case UP:
                return new Vector3[]{
                    new Vector3(x0, y1, z0),
                    new Vector3(x1, y1, z0),
                    new Vector3(x1, y1, z1),
                    new Vector3(x0, y1, z1)
                };
            case DOWN:
                return new Vector3[]{
                    new Vector3(x0, y0, z1),
                    new Vector3(x1, y0, z1),
                    new Vector3(x1, y0, z0),
                    new Vector3(x0, y0, z0)
                };
            case NORTH:
                return new Vector3[]{
                    new Vector3(x0, y0, z0),
                    new Vector3(x1, y0, z0),
                    new Vector3(x1, y1, z0),
                    new Vector3(x0, y1, z0)
                };
            case SOUTH:
                return new Vector3[]{
                    new Vector3(x1, y0, z1),
                    new Vector3(x0, y0, z1),
                    new Vector3(x0, y1, z1),
                    new Vector3(x1, y1, z1)
                };
            case EAST:
                return new Vector3[]{
                    new Vector3(x1, y0, z0),
                    new Vector3(x1, y0, z1),
                    new Vector3(x1, y1, z1),
                    new Vector3(x1, y1, z0)
                };
            case WEST:
                return new Vector3[]{
                    new Vector3(x0, y0, z1),
                    new Vector3(x0, y0, z0),
                    new Vector3(x0, y1, z0),
                    new Vector3(x0, y1, z1)
                };
            default:
                return new Vector3[0];
        }
    }

    // Add face to vertices and indices
    private void addFace(Vector3[] corners) {
        Vector3 normal = calculateNormal(corners[0], corners[1], corners[2]);
        addFace(corners[0], corners[1], corners[2], corners[3], normal);
    }

    // Calculate normal from three points
    private Vector3 calculateNormal(Vector3 p1, Vector3 p2, Vector3 p3) {
        Vector3 u = new Vector3(p2).sub(p1);
        Vector3 v = new Vector3(p3).sub(p1);
        return u.crs(v).nor();
    }
}
