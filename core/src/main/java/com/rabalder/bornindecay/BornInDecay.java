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
import com.badlogic.gdx.math.Vector3;
import java.util.List;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class BornInDecay extends ApplicationAdapter {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private ShapeRenderer shapeRenderer;

    private PlayerController player;
    private ModelInstance highlightInstance;
    private boolean highlightVisible = false;

    private WorldManager worldManager;

    //fps counter
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    @Override
    public void create() {

        //fps counter
        spriteBatch = new SpriteBatch();
        font = new BitmapFont(); // default font
        font.setColor(Color.WHITE); // set font color to white

        modelBatch = new ModelBatch();
        shapeRenderer = new ShapeRenderer();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 128;

        player = new PlayerController();
        player.position.set(8f, 10f, 8f); // Start above ground
        player.resetLook();

        highlightInstance = new ModelInstance(Materials.HIGHLIGHT_CUBE);
        Gdx.input.setCursorCatched(true);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(Color.WHITE, -1f, -0.8f, -0.2f));

        worldManager = new WorldManager(Materials.GRASSY_BLOCK_MODEL, Materials.DECAYED_SOIL_MODEL);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Update the world (chunks, meshes, etc.)
        worldManager.update(player.position);

        // Get all visible chunk meshes after greedy meshing is applied
        List<ModelInstance> visibleChunks  = worldManager.getChunkMeshes();
        List<Vector3>    collisionVoxels = worldManager.getCollisionVoxels();
        player.update(camera, deltaTime, collisionVoxels);

        // 🎯 Highlight block (This should remain the same for block interaction)
        ModelInstance targetBlock = RaycastUtil.getTargetedBlock(camera, visibleChunks, 6f);
        if (targetBlock != null) {
            Vector3 targetPos = targetBlock.transform.getTranslation(new Vector3());
            highlightInstance.transform.setToTranslation(targetPos);
            highlightVisible = true;
        } else {
            highlightVisible = false;
        }

        // 🔨 Block removal (Remove block when left mouse button is pressed)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && targetBlock != null) {
            visibleChunks.remove(targetBlock);
        }

        // ➕ Block placement (Place block when right mouse button is pressed)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && targetBlock != null) {
            Vector3 placePos = RaycastUtil.getPlacementPosition(targetBlock, camera);
            placePos.set(Math.round(placePos.x), Math.round(placePos.y), Math.round(placePos.z));

            ModelInstance newBlock = new ModelInstance(Materials.GRASSY_BLOCK_MODEL); // Use appropriate model
            newBlock.transform.setToTranslation(placePos);
            visibleChunks.add(newBlock);
        }

        // 🖱 Cursor locking (Ensure the mouse is locked in the game window)
        if (!Gdx.input.isCursorCatched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Gdx.input.setCursorCatched(true);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
        }

        // 🧼 Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // 🖼 Render chunk meshes
        modelBatch.begin(camera);

        if (highlightVisible) {
            modelBatch.render(highlightInstance, environment); // Highlight the target block
        }

        // Render the chunk meshes
        for (ModelInstance chunkMesh : visibleChunks) {
            Vector3 chunkPos = chunkMesh.transform.getTranslation(new Vector3());
            if (camera.frustum.boundsInFrustum(chunkPos, new Vector3(0.5f, 0.5f, 0.5f))) {
                modelBatch.render(chunkMesh, environment);
            }
        }

        modelBatch.end();

        // 🖌 Draw the cursor (if needed)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 3f);
        shapeRenderer.end();

        // 🧾 FPS counter (To monitor the frame rate)
        spriteBatch.begin();
        font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10);
        spriteBatch.end();

        // 🖱 Re-capture mouse if lost focus
        if (!Gdx.input.isCursorCatched() && Gdx.input.isTouched()) {
            Gdx.input.setCursorCatched(true);
        }
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
