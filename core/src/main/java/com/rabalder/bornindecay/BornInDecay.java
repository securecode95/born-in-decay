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
        // ——— setup rendering & input ———
        spriteBatch    = new SpriteBatch();
        font           = new BitmapFont();
        font.setColor(Color.WHITE);
        modelBatch     = new ModelBatch();
        shapeRenderer  = new ShapeRenderer();
        camera         = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near    = 0.1f;
        camera.far     = 128f;
        Gdx.input.setCursorCatched(true);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f,0.8f,0.8f,1f));
        environment.add(new DirectionalLight().set(Color.WHITE, -1f, -0.8f, -0.2f));

        // ——— initialize world manager ———
        worldManager = new WorldManager(Materials.GRASSY_BLOCK_MODEL, Materials.DECAYED_SOIL_MODEL);

        // ——— spawn player at (8, ?, 8) but y to be computed ———
        player = new PlayerController();
        // temporarily set y=0 so worldManager will generate around (8,0,8)
        player.position.set(8f, 0f, 8f);
        player.resetLook();

        // generate the chunks around the spawn point
        worldManager.update(player.position);

        // find the highest solid‐voxel under (8,8)
        List<Vector3> voxels = worldManager.getCollisionVoxels();
        float surfaceY = 0f;
        for (Vector3 v : voxels) {
            // each v.x = blockX + 0.5, so match around 8.5
            if (Math.abs(v.x - (player.position.x + 0.5f)) < 0.01f &&
                Math.abs(v.z - (player.position.z + 0.5f)) < 0.01f) {
                surfaceY = Math.max(surfaceY, v.y);
            }
        }
        // place player half‐unit above that block
        player.position.y = surfaceY + 0.5f;

        // highlight cube for block targeting
        highlightInstance = new ModelInstance(Materials.HIGHLIGHT_CUBE);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // 1) update world (generate/remove chunks)
        worldManager.update(player.position);

        // 2) get visible chunk meshes + collision voxels
        List<ModelInstance> visibleChunks  = worldManager.getChunkMeshes();
        List<Vector3>       collisionVoxels = worldManager.getCollisionVoxels();

        // 3) update player (now colliding against actual voxels)
        player.update(camera, deltaTime, collisionVoxels);

        // … rest of your render code (highlight, draw chunks, UI) unchanged …
        ModelInstance targetBlock = RaycastUtil.getTargetedBlock(camera, visibleChunks, 6f);
        highlightVisible = targetBlock != null;
        if (highlightVisible) {
            Vector3 targetPos = targetBlock.transform.getTranslation(new Vector3());
            highlightInstance.transform.setToTranslation(targetPos);
        }

        // clear screen
        Gdx.gl.glViewport(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.05f,0.05f,0.1f,1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // render
        modelBatch.begin(camera);
        if (highlightVisible) modelBatch.render(highlightInstance, environment);

        for (ModelInstance chunkMesh : visibleChunks) {
            modelBatch.render(chunkMesh, environment);
        }

        modelBatch.end();

        // cursor & FPS
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(
            Gdx.graphics.getWidth()/2f,
            Gdx.graphics.getHeight()/2f,
            3f);
        shapeRenderer.end();

        spriteBatch.begin();
        font.draw(spriteBatch,
            "FPS: "+Gdx.graphics.getFramesPerSecond(),
            10,
            Gdx.graphics.getHeight()-10);
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
