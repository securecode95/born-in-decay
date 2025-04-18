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


public class BornInDecay extends ApplicationAdapter {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private ShapeRenderer shapeRenderer;

    private PlayerController player;
    private ModelInstance highlightInstance;
    private boolean highlightVisible = false;

    private WorldManager worldManager;

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        shapeRenderer = new ShapeRenderer();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 1000f;

        player = new PlayerController();
        player.position.set(8f, 10f, 8f); // start above terrain
        player.resetLook();

        highlightInstance = new ModelInstance(Materials.HIGHLIGHT_CUBE);
        Gdx.input.setCursorCatched(true);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(Color.WHITE, -1f, -0.8f, -0.2f));

        worldManager = new WorldManager(Materials.GRASSY_BLOCK_MODEL);

    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // 🌍 Get current visible blocks
        List<ModelInstance> visibleBlocks = worldManager.getVisibleBlocks(player.position);


        // 🧍 Update player movement
        player.update(camera, deltaTime, visibleBlocks);

        // 🔍 Highlight block
        ModelInstance targetBlock = RaycastUtil.getTargetedBlock(camera, visibleBlocks, 6f);
        if (targetBlock != null) {
            Vector3 targetPos = targetBlock.transform.getTranslation(new Vector3());
            highlightInstance.transform.setToTranslation(targetPos);
            highlightVisible = true;
        } else {
            highlightVisible = false;
        }

        // ⛏ Remove block
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && targetBlock != null) {
            visibleBlocks.remove(targetBlock);
        }

        // 🧱 Place block
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && targetBlock != null) {
            Vector3 placePos = RaycastUtil.getPlacementPosition(targetBlock, camera);
            placePos.set(
                Math.round(placePos.x),
                Math.round(placePos.y),
                Math.round(placePos.z)
            );

            ModelInstance newBlock = new ModelInstance(Materials.GRASSY_BLOCK_MODEL);
            newBlock.transform.setToTranslation(placePos);
            visibleBlocks.add(newBlock);
        }

        // 🔒 Cursor lock toggle
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

        // 🖼 Render all blocks + highlight
        modelBatch.begin(camera);
        if (highlightVisible) modelBatch.render(highlightInstance, environment);
        for (ModelInstance block : visibleBlocks) {
            modelBatch.render(block, environment);
        }
        modelBatch.end();

        // 🎯 Draw center circle cursor
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 3f);
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        shapeRenderer.dispose();
        Materials.dispose();
    }
}
