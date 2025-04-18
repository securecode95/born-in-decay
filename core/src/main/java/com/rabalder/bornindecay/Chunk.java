package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class Chunk {
    public static final int SIZE = 16;
    public final ModelInstance[][][] blocks;

    public Chunk(Model model, int offsetX, int offsetY, int offsetZ) {
        blocks = new ModelInstance[SIZE][SIZE][SIZE];

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (y == 0) {
                        BlockType type = (x + z) % 2 == 0 ? BlockType.GRASS : BlockType.DECAYED_SOIL;
                        ModelInstance instance = new ModelInstance(type.model);
                        instance.transform.setToTranslation(
                            x + offsetX + 0.5f,
                            y + offsetY + 0.5f,
                            z + offsetZ + 0.5f
                        );
                        blocks[x][y][z] = instance;
                    }
                }
            }
        }
    }
}
