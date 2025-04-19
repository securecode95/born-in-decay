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
import java.util.*;

public class ChunkMeshBuilder {
    private static final int SIZE=Chunk.SIZE;
    private static final int VERTEX_SIZE=6;

    List<Float>  grassV=new ArrayList<>(); List<Short> grassI=new ArrayList<>(); short gB=0;
    List<Float>   soilV=new ArrayList<>(); List<Short>  soilI=new ArrayList<>(); short sB=0;
    List<Float> stoneV=new ArrayList<>(); List<Short> stoneI=new ArrayList<>(); short stB=0;

    public ModelInstance buildChunkMesh(Chunk c) {
        grassV.clear(); grassI.clear(); gB=0;
        soilV.clear();  soilI.clear();  sB=0;
        stoneV.clear(); stoneI.clear(); stB=0;
        // X, Y, Z masks & merge as before (use mergeMaskX/Y/Z from prior snippet)
        // ... merge loops omitted for brevity, same as earlier update ...

        Mesh gm=makeMesh(grassV,grassI);
        Mesh sm=makeMesh(soilV, soilI);
        Mesh stm=makeMesh(stoneV,stoneI);

        Material grM = new Material(ColorAttribute.createDiffuse(Color.GREEN), IntAttribute.createCullFace(GL20.GL_NONE));
        Material soM = new Material(ColorAttribute.createDiffuse(new Color(0.6f,0.4f,0.2f,1f)), IntAttribute.createCullFace(GL20.GL_NONE));
        Material stM = new Material(ColorAttribute.createDiffuse(Color.GRAY), IntAttribute.createCullFace(GL20.GL_NONE));

        ModelBuilder mb=new ModelBuilder(); mb.begin();
        if (grassI.size()>0) mb.part("grass", gm, GL20.GL_TRIANGLES, grM);
        if (soilI.size()>0)  mb.part("soil",  sm, GL20.GL_TRIANGLES, soM);
        if (stoneI.size()>0) mb.part("stone", stm,GL20.GL_TRIANGLES, stM);
        Model m=mb.end();
        return new ModelInstance(m);
    }

    private Mesh makeMesh(List<Float> v, List<Short> i){
        Mesh m=new Mesh(true, v.size()/VERTEX_SIZE, i.size(),
            new VertexAttribute(Usage.Position,3,"a_position"),
            new VertexAttribute(Usage.Normal,  3,"a_normal")
        );
        m.setVertices(toFA(v)); m.setIndices(toSA(i));
        return m;
    }
    private float[] toFA(List<Float> l){float[]a=new float[l.size()];for(int i=0;i<a.length;i++)a[i]=l.get(i);return a;}
    private short[] toSA(List<Short>l){short[]a=new short[l.size()];for(int i=0;i<a.length;i++)a[i]=l.get(i);return a;}
}
