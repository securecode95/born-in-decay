package com.rabalder.bornindecay;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
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

    WorldManager    worldManager;
    PlayerController player;
    ModelInstance   highlightInstance;
    boolean         highlightVisible;

    @Override
    public void create() {
        // -------------- camera / rendering setup --------------
        camera = new PerspectiveCamera(67,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far  = 1000f;

        modelBatch    = new ModelBatch();
        environment   = new Environment();
        shapeRenderer = new ShapeRenderer();
        spriteBatch   = new SpriteBatch();
        font          = new BitmapFont();

        // ---- add some lights so our blocks aren't pitch‑dark ----
        // ambient light (soft overall illumination)
        environment.set(
            new ColorAttribute(ColorAttribute.AmbientLight,
                0.8f, 0.8f, 0.8f, 1f)
        );

        // directional light (adds simple shading)
        environment.add(
            new DirectionalLight()
                .set(0.8f, 0.8f, 0.8f,  // color
                    -1f, -0.8f, -0.2f) // direction
        );

        // ---------------- world + player setup ----------------
        worldManager = new WorldManager();
        player       = new PlayerController();

        // pick a spawn X/Z
        float spawnX = 0f, spawnZ = 0f;
        // lift player way up
        player.position.set(spawnX, 200f, spawnZ);
        // force‑generate chunks
        worldManager.update(player.position);

        // drop player onto highest block at spawnX,Z
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
        // one more world update now that we're on ground
        worldManager.update(player.position);

        // initialize camera on player
        camera.position.set(player.position);
        camera.lookAt(
            player.position.x,
            player.position.y,
            player.position.z - 1f
        );
        camera.update();
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // disable back‑face culling, enable depth test
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        // 1) update world around player
        worldManager.update(player.position);

        // 2) collect meshes + collision voxels
        List<ModelInstance> visibleChunks   = worldManager.getChunkMeshes();
        List<Vector3>       collisionVoxels = worldManager.getCollisionVoxels();

        // 3) move + collide player (this also rotates camera via mouse)
        player.update(camera, deltaTime, collisionVoxels);

        // *** NEW: after player.update, re‑apply camera.update() so look changes ***
        camera.update();

        // 4) optional block highlight
        ModelInstance target = RaycastUtil.getTargetedBlock(
            camera, visibleChunks, 6f
        );
        highlightVisible = target != null;
        if (highlightVisible) {
            Vector3 p = target.transform
                .getTranslation(new Vector3());
            highlightInstance.transform
                .setToTranslation(p);
        }

        // 5) clear & render all
        Gdx.gl.glViewport(
            0, 0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT |
                GL20.GL_DEPTH_BUFFER_BIT
        );

        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        if (highlightVisible) {
            modelBatch.render(highlightInstance, environment);
        }
        for (ModelInstance chunk : visibleChunks) {
            modelBatch.render(chunk, environment);
        }
        modelBatch.end();

        // 6) HUD: crosshair + FPS
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(
            Gdx.graphics.getWidth()/2f,
            Gdx.graphics.getHeight()/2f,
            3f
        );
        shapeRenderer.end();

        spriteBatch.begin();
        font.draw(spriteBatch,
            "FPS: " + Gdx.graphics.getFramesPerSecond(),
            10,
            Gdx.graphics.getHeight() - 10
        );
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
