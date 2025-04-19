package com.rabalder.bornindecay;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

public class BornInDecay extends ApplicationAdapter {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private ShapeRenderer shapeRenderer;
    private PlayerController player;
    private ModelInstance highlightInstance;
    private boolean highlightVisible = false;
    private WorldManager worldManager;

    private SpriteBatch spriteBatch;
    private BitmapFont font;

    @Override
    public void create() {
        // ——— rendering setup ———
        spriteBatch   = new SpriteBatch();
        font          = new BitmapFont();
        font.setColor(Color.WHITE);
        modelBatch    = new ModelBatch();
        shapeRenderer = new ShapeRenderer();
        camera        = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near   = 0.1f;
        camera.far    = 128f;
        Gdx.input.setCursorCatched(true);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f,0.8f,0.8f,1f));
        environment.add(new DirectionalLight().set(Color.WHITE, -1f,-0.8f,-0.2f));

        // ——— world & player setup ———
        worldManager = new WorldManager(Materials.GRASSY_BLOCK_MODEL, Materials.DECAYED_SOIL_MODEL);
        player       = new PlayerController();
        player.resetLook();

        // initial spawn coords (chunk 8,8)
        player.position.set(8f, 0f, 8f);
        worldManager.update(player.position);

        // pick the highest block at (8,8)
        List<Vector3> voxels = worldManager.getCollisionVoxels();
        float surfaceY = 0f;
        for (Vector3 v : voxels) {
            if (Math.abs(v.x - (8f + 0.5f)) < 0.01f &&
                Math.abs(v.z - (8f + 0.5f)) < 0.01f) {
                surfaceY = Math.max(surfaceY, v.y);
            }
        }

        // place player just above that block top
        float playerRadius = 0.4f;
        float blockTopY    = surfaceY + 0.5f;
        player.position.y  = blockTopY + playerRadius;

        // emergency “unjam”: if still colliding, step up until free
        int safety = 0;
        while (safety++ < 50 && isPositionColliding(player.position, voxels)) {
            player.position.y += 0.1f;
        }

        // highlight cube
        highlightInstance = new ModelInstance(Materials.HIGHLIGHT_CUBE);
    }

    // helper, same package or private static inside this class
    private boolean isPositionColliding(Vector3 pos, List<Vector3> voxels) {
        float rP = 0.4f, rB = 0.5f, maxD = rP + rB + 0.2f;
        for (Vector3 c : voxels) {
            if (Math.abs(c.x - (pos.x + 0.5f)) > maxD ||
                Math.abs(c.y - pos.y)         > maxD ||
                Math.abs(c.z - (pos.z + 0.5f))> maxD) continue;
            if (pos.x + rP > c.x - rB && pos.x - rP < c.x + rB &&
                pos.y + rP > c.y - rB && pos.y - rP < c.y + rB &&
                pos.z + rP > c.z - rB && pos.z - rP < c.z + rB) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // 1) Update world geometry around the player
        worldManager.update(player.position);

        // 2) Gather what to render and what to collide against
        List<ModelInstance> visibleChunks   = worldManager.getChunkMeshes();
        List<Vector3>       collisionVoxels = worldManager.getCollisionVoxels();

        // 3) Tell the player to move/collide against the voxels
        player.update(camera, deltaTime, collisionVoxels);

        // 4) Raycast & highlight the targeted block
        ModelInstance targetBlock = RaycastUtil.getTargetedBlock(camera, visibleChunks, 6f);
        highlightVisible = (targetBlock != null);
        if (highlightVisible) {
            Vector3 targetPos = targetBlock.transform.getTranslation(new Vector3());
            highlightInstance.transform.setToTranslation(targetPos);
        }

        // 5) Draw all instances
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        if (highlightVisible) {
            modelBatch.render(highlightInstance, environment);
        }
        for (ModelInstance chunkMesh : visibleChunks) {
            modelBatch.render(chunkMesh, environment);
        }
        modelBatch.end();

        // 6) Draw crosshair and FPS
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
            Gdx.graphics.getHeight() - 10);
        spriteBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        shapeRenderer.dispose();
        Materials.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
