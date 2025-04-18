package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;

public class ChunkMeshBuilder {
    private static final int VERTEX_SIZE = 3 + 3; // position + normal
    private final ArrayList<Float> vertices = new ArrayList<>();
    private final ArrayList<Short> indices = new ArrayList<>();

    private short index = 0;

    public ChunkMeshBuilder() {}

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
            new VertexAttribute(Usage.Normal, 3, "a_normal")
        );

        mesh.setVertices(verts);
        mesh.setIndices(inds);

        // ✅ Create a model from the mesh
        ModelBuilder modelBuilder = new ModelBuilder();
        Model model = modelBuilder.createMeshPart(
            "chunk",
            mesh,
            GL20.GL_TRIANGLES,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            Usage.Position | Usage.Normal
        );

        return new ChunkMesh(mesh, model);
    }


    public ModelInstance buildChunkMesh(Chunk chunk) {
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    if (chunk.blocks[x][y][z] == BlockType.AIR) continue;

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

        // 🔧 Wrap the mesh in a Model manually
        Model model = new Model();
        MeshPart meshPart = new MeshPart("chunk", chunkMesh.mesh, 0, chunkMesh.mesh.getNumIndices(), GL20.GL_TRIANGLES);
        meshPart.update();
        meshPart.primitiveType = GL20.GL_TRIANGLES;

        Node node = new Node();
        node.parts.add(new NodePart(meshPart, new Material(ColorAttribute.createDiffuse(Color.WHITE))));
        model.nodes.add(node);
        model.meshes.add(chunkMesh.mesh);
        model.manageDisposable(chunkMesh.mesh);
        model.calculateTransforms();

        return new ModelInstance(model);
    }



    private boolean isAir(Chunk chunk, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= Chunk.SIZE || y >= Chunk.SIZE || z >= Chunk.SIZE) {
            return true;
        }
        return chunk.blocks[x][y][z] == BlockType.AIR;
    }

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

    private void addFace(Vector3[] corners) {
        Vector3 normal = calculateNormal(corners[0], corners[1], corners[2]);
        addFace(corners[0], corners[1], corners[2], corners[3], normal);
    }

    private Vector3 calculateNormal(Vector3 p1, Vector3 p2, Vector3 p3) {
        Vector3 u = new Vector3(p2).sub(p1);
        Vector3 v = new Vector3(p3).sub(p1);
        return u.crs(v).nor();
    }
}
