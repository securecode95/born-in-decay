// Chunk.java
package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

/** A 16³ grid of blocks, each using our BlockType IDs */
public class Chunk {
    public static final int SIZE = 16;
    public final byte[][][] blocks = new byte[SIZE][SIZE][SIZE];
    public ModelInstance meshInstance;

    public Chunk() {
        // fill everything with AIR
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                for (int z = 0; z < SIZE; z++)
                    blocks[x][y][z] = BlockType.AIR;
    }

    /** place a block only if inside [0,SIZE) */
    public void setBlock(int x, int y, int z, byte type) {
        if (x >= 0 && x < SIZE &&
            y >= 0 && y < SIZE &&
            z >= 0 && z < SIZE)
            blocks[x][y][z] = type;
    }

    /** returns AIR if out of bounds */
    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE ||
            y < 0 || y >= SIZE ||
            z < 0 || z >= SIZE)
            return BlockType.AIR;
        return blocks[x][y][z];
    }
}
