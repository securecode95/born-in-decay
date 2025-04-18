// File: Chunk.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class Chunk {
    public static final int SIZE = 16;
    public static final byte AIR = 0;
    public static final byte GRASS = 1;
    public static final byte SOIL = 2;

    public final byte[][][] blocks = new byte[SIZE][SIZE][SIZE];

    // Used to hold the mesh instance for rendering
    public ModelInstance meshInstance;

    public Chunk() {
        // All AIR by default
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = AIR;
                }
            }
        }
    }

    public void setBlock(int x, int y, int z, byte type) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE) {
            blocks[x][y][z] = type;
        }
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return AIR;
        return blocks[x][y][z];
    }
}
