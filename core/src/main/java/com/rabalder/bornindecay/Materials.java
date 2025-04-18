package com.rabalder.bornindecay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;

public class Materials {

    public static ModelBuilder modelBuilder = new ModelBuilder();

    // Load texture from assets/textures/
    public static final Texture DECAYED_SOIL_TEXTURE = new Texture(Gdx.files.internal("textures/block_decayed_soil.png"));

    public static final Material DECAYED_SOIL = new Material(
        TextureAttribute.createDiffuse(DECAYED_SOIL_TEXTURE)
    );

    public static final Material GRASSY_BLOCK = new Material(
        ColorAttribute.createDiffuse(Color.GREEN)
    );

    public static final Model DECAYED_SOIL_MODEL = modelBuilder.createBox(
        1f, 1f, 1f,
        DECAYED_SOIL,
        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates
    );

    public static final Model GRASSY_BLOCK_MODEL = modelBuilder.createBox(
        1f, 1f, 1f,
        GRASSY_BLOCK,
        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
    );

    // Optional cleanup method (call from your main game class)
    public static void dispose() {
        DECAYED_SOIL_TEXTURE.dispose();
    }


    public static final Material HIGHLIGHT_MATERIAL = new Material(
        ColorAttribute.createDiffuse(new Color(1f, 1f, 1f, 0.2f)) // translucent white
    );

    public static final Model HIGHLIGHT_CUBE = modelBuilder.createBox(
        1.01f, 1.01f, 1.01f, // slightly bigger to avoid z-fighting
        HIGHLIGHT_MATERIAL,
        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
    );
}
