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
    ModelBatch      modelBatch;
    Environment     environment;
    ShapeRenderer   shapeRenderer;
    SpriteBatch     spriteBatch;
    BitmapFont      font;

    WorldManager     worldManager;
    PlayerController player;
    ModelInstance    highlightInstance;
    boolean          highlightVisible;

    @Override
    public void create() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f; camera.far = 1000f;
        modelBatch    = new ModelBatch();
        environment   = new Environment();
        shapeRenderer = new ShapeRenderer();
        spriteBatch   = new SpriteBatch();
        font          = new BitmapFont();

        // pass a seed so WorldManager(long) exists
        worldManager = new WorldManager(System.currentTimeMillis());
        player       = new PlayerController();

        // pick spawn X/Z
        float spawnX = 0f, spawnZ = 0f;
        player.position.set(spawnX, 200f, spawnZ);

        // prebuild around spawn
        worldManager.update(player.position);
        // find highest voxel under (spawnX,spawnZ)
        List<Vector3> voxels = worldManager.getCollisionVoxels();
        float bestY = Float.NEGATIVE_INFINITY;
        for (Vector3 v : voxels) {
            if (Math.abs(v.x - spawnX) < 0.5f &&
                Math.abs(v.z - spawnZ) < 0.5f) {
                bestY = Math.max(bestY, v.y);
            }
        }
        if (bestY > Float.NEGATIVE_INFINITY) {
            player.position.y = bestY + 2f;
        }

        // rebuild once more at final spawn
        worldManager.update(player.position);

        camera.position.set(player.position);
        camera.lookAt(player.position.x, player.position.y, player.position.z - 1f);
        camera.update();
    }

    @Override
    public void render() {
        // 1) get delta
        float deltaTime = Gdx.graphics.getDeltaTime();

        // 2) regenerate chunks around player
        worldManager.update(player.position);

        // 3) gather data
        List<ModelInstance> chunks         = worldManager.getChunkMeshes();
        List<Vector3>       collisionVoxels = worldManager.getCollisionVoxels();

        // 4) move + collide
        player.update(camera, deltaTime, collisionVoxels);

        // 5) optional highlight
        ModelInstance target = RaycastUtil.getTargetedBlock(camera, chunks, 6f);
        highlightVisible = (target != null);
        if (highlightVisible) {
            Vector3 p = target.transform.getTranslation(new Vector3());
            highlightInstance.transform.setToTranslation(p);
        }

        // 6) clear
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // 7) draw all
        modelBatch.begin(camera);
        if (highlightVisible) modelBatch.render(highlightInstance, environment);
        for (ModelInstance mi : chunks) {
            modelBatch.render(mi, environment);
        }
        modelBatch.end();

        // 8) HUD
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f, 3f);
        shapeRenderer.end();

        spriteBatch.begin();
        font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(),
            10, Gdx.graphics.getHeight()-10);
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
