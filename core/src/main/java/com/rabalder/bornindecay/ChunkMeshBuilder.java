package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.*;
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
    private static final int VERTEX_SIZE = 6; // 3 pos + 3 normal

    // buffers for grass top faces
    private final List<Float> grassVerts = new ArrayList<>();
    private final List<Short> grassIdx   = new ArrayList<>();
    private short grassIndex = 0;

    // buffers for soil faces
    private final List<Float> soilVerts  = new ArrayList<>();
    private final List<Short> soilIdx    = new ArrayList<>();
    private short soilIndex  = 0;

    public ModelInstance buildChunkMesh(Chunk chunk) {
        // clear buffers
        grassVerts.clear(); grassIdx.clear(); grassIndex = 0;
        soilVerts.clear();  soilIdx.clear();  soilIndex  = 0;

        byte[][] mask = new byte[Chunk.SIZE][Chunk.SIZE];

        // X-axis
        for(int x=0; x<Chunk.SIZE; x++){
            // +X faces
            for(int y=0;y<Chunk.SIZE;y++) for(int z=0;z<Chunk.SIZE;z++){
                byte cur = chunk.blocks[x][y][z];
                byte next = x+1<Chunk.SIZE? chunk.blocks[x+1][y][z] : Chunk.AIR;
                mask[y][z] = (cur!=Chunk.AIR && next==Chunk.AIR)? cur : 0;
            }
            mergeMaskX(mask, x, new Vector3( 1,0,0));
            // -X faces
            for(int y=0;y<Chunk.SIZE;y++) for(int z=0;z<Chunk.SIZE;z++){
                byte cur = chunk.blocks[x][y][z];
                byte prev = x-1>=0? chunk.blocks[x-1][y][z] : Chunk.AIR;
                mask[y][z] = (cur!=Chunk.AIR && prev==Chunk.AIR)? cur : 0;
            }
            mergeMaskX(mask, x, new Vector3(-1,0,0));
        }

        // Y-axis
        for(int y=0; y<Chunk.SIZE; y++){
            // +Y
            for(int x=0;x<Chunk.SIZE;x++) for(int z=0;z<Chunk.SIZE;z++){
                byte cur = chunk.blocks[x][y][z];
                byte next = y+1<Chunk.SIZE? chunk.blocks[x][y+1][z] : Chunk.AIR;
                mask[x][z] = (cur!=Chunk.AIR && next==Chunk.AIR)? cur : 0;
            }
            mergeMaskY(mask, y, new Vector3(0, 1,0));
            // -Y
            for(int x=0;x<Chunk.SIZE;x++) for(int z=0;z<Chunk.SIZE;z++){
                byte cur = chunk.blocks[x][y][z];
                byte prev = y-1>=0? chunk.blocks[x][y-1][z] : Chunk.AIR;
                mask[x][z] = (cur!=Chunk.AIR && prev==Chunk.AIR)? cur : 0;
            }
            mergeMaskY(mask, y, new Vector3(0,-1,0));
        }

        // Z-axis
        for(int z=0; z<Chunk.SIZE; z++){
            // +Z
            for(int x=0;x<Chunk.SIZE;x++) for(int y=0;y<Chunk.SIZE;y++){
                byte cur = chunk.blocks[x][y][z];
                byte next = z+1<Chunk.SIZE? chunk.blocks[x][y][z+1] : Chunk.AIR;
                mask[x][y] = (cur!=Chunk.AIR && next==Chunk.AIR)? cur : 0;
            }
            mergeMaskZ(mask, z, new Vector3(0,0, 1));
            // -Z
            for(int x=0;x<Chunk.SIZE;x++) for(int y=0;y<Chunk.SIZE;y++){
                byte cur = chunk.blocks[x][y][z];
                byte prev = z-1>=0? chunk.blocks[x][y][z-1] : Chunk.AIR;
                mask[x][y] = (cur!=Chunk.AIR && prev==Chunk.AIR)? cur : 0;
            }
            mergeMaskZ(mask, z, new Vector3(0,0,-1));
        }

        // build grass mesh
        Mesh gMesh = new Mesh(true,
            grassVerts.size()/VERTEX_SIZE,
            grassIdx.size(),
            new VertexAttribute(VertexAttributes.Usage.Position,3,"a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal,3,"a_normal")
        );
        gMesh.setVertices(toFloatArray(grassVerts));
        gMesh.setIndices (toShortArray(grassIdx));

        // build soil mesh
        Mesh sMesh = new Mesh(true,
            soilVerts.size()/VERTEX_SIZE,
            soilIdx.size(),
            new VertexAttribute(VertexAttributes.Usage.Position,3,"a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal,3,"a_normal")
        );
        sMesh.setVertices(toFloatArray(soilVerts));
        sMesh.setIndices (toShortArray(soilIdx));

        // materials
        Material grassMat = new Material(
            ColorAttribute.createDiffuse(Color.GREEN),
            ColorAttribute.createSpecular(0.2f, 0.2f, 0.2f, 1f),
            IntAttribute.createCullFace(GL20.GL_BACK)
        );
        Material soilMat = new Material(
            ColorAttribute.createDiffuse(new Color(0.6f, 0.4f, 0.2f, 1f)),
            ColorAttribute.createSpecular(0.1f, 0.1f, 0.1f, 1f),
            IntAttribute.createCullFace(GL20.GL_BACK)
        );

        // bake into model
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("grass", gMesh, GL20.GL_TRIANGLES, grassMat);
        mb.part("soil",  sMesh, GL20.GL_TRIANGLES, soilMat);
        Model model = mb.end();

        return new ModelInstance(model);
    }

    private void mergeMaskX(byte[][] mask, int x, Vector3 normal) {
        int dimY = mask.length;
        int dimZ = mask[0].length;
        for(int y=0;y<dimY;y++){
            for(int z=0;z<dimZ;){
                byte b = mask[y][z]; if(b==0){ z++; continue; }
                int w=1; while(z+w<dimZ && mask[y][z+w]==b) w++;
                int h=1; outer: while(y+h<dimY){
                    for(int k=0;k<w;k++) if(mask[y+h][z+k]!=b) break outer;
                    h++; }
                float x0 = x + (normal.x>0?1f:0f);
                Vector3 p1=new Vector3(x0, y,   z  );
                Vector3 p2=new Vector3(x0, y+h, z  );
                Vector3 p3=new Vector3(x0, y+h, z+w);
                Vector3 p4=new Vector3(x0, y,   z+w);
                boolean top = (normal.x==0 && normal.y>0 && b==Chunk.GRASS);
                addQuad(p1,p2,p3,p4, normal, top);
                for(int dy=0;dy<h;dy++) for(int dz2=0;dz2<w;dz2++) mask[y+dy][z+dz2]=0;
                z += w;
            }
        }
    }

    private void mergeMaskY(byte[][] mask, int y, Vector3 normal) {
        int dimX = mask.length;
        int dimZ = mask[0].length;
        for(int x=0;x<dimX;x++){
            for(int z=0;z<dimZ;){
                byte b = mask[x][z]; if(b==0){ z++; continue; }
                int w=1; while(z+w<dimZ && mask[x][z+w]==b) w++;
                int h=1; outer: while(x+h<dimX){
                    for(int k=0;k<w;k++) if(mask[x+h][z+k]!=b) break outer;
                    h++; }
                float y0 = y + (normal.y>0?1f:0f);
                Vector3 p1=new Vector3(x,   y0, z  );
                Vector3 p2=new Vector3(x+h, y0, z  );
                Vector3 p3=new Vector3(x+h, y0, z+w);
                Vector3 p4=new Vector3(x,   y0, z+w);
                boolean top = (normal.y>0 && b==Chunk.GRASS);
                addQuad(p1,p2,p3,p4, normal, top);
                for(int dx=0;dx<h;dx++) for(int dz2=0;dz2<w;dz2++) mask[x+dx][z+dz2]=0;
                z += w;
            }
        }
    }

    private void mergeMaskZ(byte[][] mask, int z, Vector3 normal) {
        int dimX = mask.length;
        int dimY = mask[0].length;
        for(int x=0;x<dimX;x++){
            for(int y=0;y<dimY;){
                byte b = mask[x][y]; if(b==0){ y++; continue; }
                int w=1; while(y+w<dimY && mask[x][y+w]==b) w++;
                int h=1; outer: while(x+h<dimX){
                    for(int k=0;k<w;k++) if(mask[x+h][y+k]!=b) break outer;
                    h++; }
                float z0 = z + (normal.z>0?1f:0f);
                Vector3 p1=new Vector3(x,   y,   z0);
                Vector3 p2=new Vector3(x+h, y,   z0);
                Vector3 p3=new Vector3(x+h, y+w, z0);
                Vector3 p4=new Vector3(x,   y+w, z0);
                boolean top = (normal.y>0 && b==Chunk.GRASS);
                addQuad(p1,p2,p3,p4, normal, top);
                for(int dx=0;dx<h;dx++) for(int dy=0;dy<w;dy++) mask[x+dx][y+dy]=0;
                y += w;
            }
        }
    }

    private void addQuad(Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4,
                         Vector3 normal, boolean isTop) {
        List<Float>  vBuf = isTop? grassVerts: soilVerts;
        List<Short>  iBuf = isTop? grassIdx:  soilIdx;
        short        idx  = isTop? grassIndex: soilIndex;
        boolean flip = (normal.x + normal.y + normal.z) < 0;
        // push helper
        java.util.function.BiConsumer<Vector3,Vector3> push =
            (pos,nrm)->{
                vBuf.add(pos.x); vBuf.add(pos.y); vBuf.add(pos.z);
                vBuf.add(nrm.x); vBuf.add(nrm.y); vBuf.add(nrm.z);
            };
        if(!flip){ push.accept(p1,normal); push.accept(p2,normal); push.accept(p3,normal);
            push.accept(p3,normal); push.accept(p4,normal); push.accept(p1,normal);} else{
            push.accept(p1,normal); push.accept(p4,normal); push.accept(p3,normal);
            push.accept(p3,normal); push.accept(p2,normal); push.accept(p1,normal);}
        for(int i=0;i<6;i++) iBuf.add((short)(idx + i));
        if(isTop) grassIndex += 6; else soilIndex += 6;
    }

    private float[] toFloatArray(List<Float> list){
        float[] arr = new float[list.size()];
        for(int i=0;i<arr.length;i++) arr[i] = list.get(i);
        return arr;
    }
    private short[] toShortArray(List<Short> list){
        short[] arr = new short[list.size()];
        for(int i=0;i<arr.length;i++) arr[i] = list.get(i);
        return arr;
    }
}
