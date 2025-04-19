package com.rabalder.bornindecay;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

public class BornInDecay extends ApplicationAdapter {
    PerspectiveCamera camera;
    ModelBatch      batch;
    Environment     env;
    ShapeRenderer   sr;
    SpriteBatch     sb;
    BitmapFont      font;

    WorldManager    wm;
    PlayerController player;
    ModelInstance   highlight;
    boolean         hl;

    @Override
    public void create() {
        camera = new PerspectiveCamera(67,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        camera.near=0.1f; camera.far=500f;
        batch = new ModelBatch();
        env   = new Environment();
        sr    = new ShapeRenderer();
        sb    = new SpriteBatch();
        font  = new BitmapFont();
        wm = new WorldManager(System.currentTimeMillis());
        player = new PlayerController();

        // spawn high then drop
        player.position.set(8,50,8);
        wm.update(player.position);
        camera.position.set(player.position);
        camera.lookAt(player.position.x,player.position.y,player.position.z-1);
        camera.update();
    }

    @Override public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable (GL20.GL_DEPTH_TEST);

        wm.update(player.position);
        List<ModelInstance> visibleChunks   = wm.getChunkMeshes();
        List<Vector3>       collisionVoxels = wm.getCollisionVoxels();
        player.update(camera, deltaTime, collisionVoxels);

        Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);

        batch.begin(camera);
        for (ModelInstance mi:chunks) batch.render(mi, env);
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(Color.WHITE);
        sr.circle(Gdx.graphics.getWidth()/2f,Gdx.graphics.getHeight()/2f,3f);
        sr.end();

        sb.begin();
        font.draw(sb,"FPS:"+Gdx.graphics.getFramesPerSecond(),10,Gdx.graphics.getHeight()-10);
        sb.end();
    }

    @Override public void dispose(){
        batch.dispose();
        sr.dispose();
        sb.dispose();
        font.dispose();
    }
}
