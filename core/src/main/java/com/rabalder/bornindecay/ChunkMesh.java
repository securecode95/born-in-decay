// File: ChunkMesh.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Disposable;

public class ChunkMesh implements Disposable {
    public final Mesh mesh;
    public final Model model;
    public final ModelInstance instance;

    public ChunkMesh(Mesh mesh, Model model) {
        this.mesh = mesh;
        this.model = model;
        this.instance = new ModelInstance(model);
    }

    public ModelInstance getModelInstance() {
        return instance;
    }

    @Override
    public void dispose() {
        mesh.dispose();
        model.dispose();
    }
}
