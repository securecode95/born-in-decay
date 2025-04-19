package com.rabalder.bornindecay;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

public class BornInDecay extends ApplicationAdapter {
    PerspectiveCamera camera;
    ModelBatch      batch;
    Environment     env;
    ShapeRenderer   shapes;
    SpriteBatch     uiBatch;
    BitmapFont      font;

    WorldManager    world;
    PlayerController player;
    ModelInstance   highlight;
    boolean         showHighlight;

    @Override
    public void create() {
        // sky‑blue clear
        Gdx.gl.glClearColor(0.5f,0.8f,1f,1f);
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = .1f; camera.far=500f;
        batch   = new ModelBatch();
        env     = new Environment();
        shapes  = new ShapeRenderer();
        uiBatch = new SpriteBatch();
        font    = new BitmapFont();

        world   = new WorldManager();
        player  = new PlayerController();
        // spawn high, then drop onto terrain:
        player.position.set(8,50,8);
        world.update(player.position);
        float bestY = -Float.MAX_VALUE;
        for (Vector3 v : world.getCollisionVoxels()) {
            if (Math.abs(v.x-8)<.5f && Math.abs(v.z-8)<.5f) bestY = Math.max(bestY, v.y);
        }
        if (bestY>-Float.MAX_VALUE) player.position.y = bestY + 1.8f;
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        // update world + player
        world.update(player.position);
        player.update(camera, dt, world.getCollisionVoxels());
        // sync camera
        camera.position.set(player.position);
        camera.lookAt(player.position.x,player.position.y,player.position.z-1f);
        camera.update();
        // draw chunks
        batch.begin(camera);
        for (ModelInstance mi: world.getChunkMeshes()) {
            batch.render(mi, env);
        }
        batch.end();
        // HUD
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.WHITE);
        shapes.circle(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2,3f);
        shapes.end();
        uiBatch.begin();
        font.draw(uiBatch, "FPS: "+Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight()-10);
        uiBatch.end();
    }

    @Override public void dispose() {
        batch.dispose(); shapes.dispose(); uiBatch.dispose(); font.dispose();
    }
}
