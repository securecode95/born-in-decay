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
    PerspectiveCamera   camera;
    ModelBatch          modelBatch;
    Environment         environment;
    ShapeRenderer       shapeRenderer;
    SpriteBatch         spriteBatch;
    BitmapFont          font;

    WorldManager        worldManager;
    PlayerController    player;
    ModelInstance       highlightInstance;
    boolean             highlightVisible;

    @Override
    public void create() {
        // -- Standard setup --
        camera        = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near   = 0.1f;
        camera.far    = 1000f;
        modelBatch    = new ModelBatch();
        environment   = new Environment();
        shapeRenderer = new ShapeRenderer();
        spriteBatch   = new SpriteBatch();
        font          = new BitmapFont();

        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);
        // -- World + player setup --
        worldManager  = new WorldManager();
        player        = new PlayerController();

        // 1) choose a spawn X/Z
        float spawnX = 0f;
        float spawnZ = 0f;
        // 2) lift high above terrain so generation can carve out the floor
        player.position.set(spawnX, 200f, spawnZ);

        // 3) generate chunks around that high point
        worldManager.update(player.position);

        // 4) find the highest voxel at spawnX/Z
        List<Vector3> voxels = worldManager.getCollisionVoxels();
        float bestY = Float.NEGATIVE_INFINITY;
        for (Vector3 v : voxels) {
            if (Math.abs(v.x - spawnX) < 0.5f &&
                Math.abs(v.z - spawnZ) < 0.5f) {
                bestY = Math.max(bestY, v.y);
            }
        }
        // 5) snap the player just above that block
        if (bestY > Float.NEGATIVE_INFINITY) {
            player.position.y = bestY + 2f;  // 2 units above block
        }

        // 6) regenerate around the actual spawn height
        worldManager.update(player.position);

        // 7) finalize camera to look at the horizon
        camera.position.set(player.position);
        camera.lookAt(player.position.x, player.position.y, player.position.z - 1f);
        camera.update();

        // optional: prepare your highlight cube if you use one
        highlightInstance = new ModelInstance(Materials.HIGHLIGHT_CUBE);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // ensure nothing vanishes
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        // update world around the moving player
        worldManager.update(player.position);

        // collect what to draw and collide against
        List<ModelInstance> visibleChunks   = worldManager.getChunkMeshes();
        List<Vector3>       collisionVoxels = worldManager.getCollisionVoxels();

        // move & collide
        player.update(camera, deltaTime, collisionVoxels);
        camera.update();

        // highlight aimed block
        ModelInstance target = RaycastUtil.getTargetedBlock(camera, visibleChunks, 6f);
        highlightVisible = target != null;
        if (highlightVisible) {
            Vector3 p = target.transform.getTranslation(new Vector3());
            highlightInstance.transform.setToTranslation(p);
        }

        // clear and draw
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        if (highlightVisible) modelBatch.render(highlightInstance, environment);
        for (ModelInstance chunk : visibleChunks) {
            modelBatch.render(chunk, environment);
        }
        modelBatch.end();

        // crosshair + FPS
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(
            Gdx.graphics.getWidth() * 0.5f,
            Gdx.graphics.getHeight() * 0.5f,
            3f
        );
        shapeRenderer.end();

        spriteBatch.begin();
        font.draw(spriteBatch,
            "FPS: " + Gdx.graphics.getFramesPerSecond(),
            10, Gdx.graphics.getHeight() - 10);
        spriteBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
