package com.rabalder.bornindecay;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class BornInDecay extends ApplicationAdapter {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private Model cubeModel;
    private ArrayList<ModelInstance> cubes;
    private PlayerController player;
    private ModelInstance highlightInstance;
    private boolean highlightVisible = false;

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        Model decayedModel = Materials.DECAYED_SOIL_MODEL;
        Model grassyModel = Materials.GRASSY_BLOCK_MODEL;

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(8f, 8f, 20f);
        camera.near = 0.1f;
        camera.far = 1000f;
        camera.update();

        player = new PlayerController();
        player.resetLook(camera);

        highlightInstance = new ModelInstance(Materials.HIGHLIGHT_CUBE);
        Gdx.input.setCursorCatched(true);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(Color.WHITE, -1f, -0.8f, -0.2f));

        // Create block model
        ModelBuilder modelBuilder = new ModelBuilder();
        cubeModel = modelBuilder.createBox(1f, 1f, 1f,
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Load chunks
        cubes = new ArrayList<>();
        int chunkCountX = 2;
        int chunkCountZ = 2;

        for (int cx = 0; cx < chunkCountX; cx++) {
            for (int cz = 0; cz < chunkCountZ; cz++) {
                Chunk chunk = new Chunk(cubeModel, cx * Chunk.SIZE, 0, cz * Chunk.SIZE);
                for (int x = 0; x < Chunk.SIZE; x++) {
                    for (int y = 0; y < Chunk.SIZE; y++) {
                        for (int z = 0; z < Chunk.SIZE; z++) {
                            ModelInstance block = chunk.blocks[x][y][z];
                            if (block != null) {
                                cubes.add(block);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render() {
        player.update(camera, Gdx.graphics.getDeltaTime(), cubes);

        // 🔫 Raycast target
        ModelInstance targetBlock = RaycastUtil.getTargetedBlock(camera, cubes, 6f);
        if (targetBlock != null) {
            Vector3 targetPos = targetBlock.transform.getTranslation(new Vector3());
            highlightInstance.transform.setToTranslation(targetPos);
            highlightVisible = true;
        } else {
            highlightVisible = false;
        }

        // 🔨 Remove block
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && targetBlock != null) {
            cubes.remove(targetBlock);
        }

        // 🧱 Place block
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && targetBlock != null) {
            Vector3 placePos = RaycastUtil.getPlacementPosition(targetBlock, camera);

            // Snap to grid
            placePos.set(
                (float) Math.floor(placePos.x) + 0.5f,
                (float) Math.floor(placePos.y) + 0.5f,
                (float) Math.floor(placePos.z) + 0.5f
            );

            ModelInstance newBlock = new ModelInstance(Materials.GRASSY_BLOCK_MODEL);
            newBlock.transform.setToTranslation(placePos);
            cubes.add(newBlock);
        }

        // Mouse locking
        if (!Gdx.input.isCursorCatched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Gdx.input.setCursorCatched(true);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        camera.update();
        modelBatch.begin(camera);

        if (highlightVisible) {
            modelBatch.render(highlightInstance, environment);
        }

        for (ModelInstance cube : cubes) {
            modelBatch.render(cube, environment);
        }

        modelBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        cubeModel.dispose();
        Materials.dispose();
    }
}
