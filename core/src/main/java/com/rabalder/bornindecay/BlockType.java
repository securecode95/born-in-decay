package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.Model;

public enum BlockType {
    GRASS(Materials.GRASSY_BLOCK_MODEL),
    DECAYED_SOIL(Materials.DECAYED_SOIL_MODEL);

    public final Model model;

    BlockType(Model model) {
        this.model = model;
    }
}
