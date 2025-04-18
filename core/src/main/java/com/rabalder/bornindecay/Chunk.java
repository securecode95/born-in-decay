package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class Chunk {
    public static final int SIZE = 16; // Size of the chunk
    public static final byte AIR = 0;  // Representing empty space (air)
    public static final byte GRASS = 1; // Grass block type
    public static final byte SOIL = 2; // Soil block type

    // 3D array of blocks, each block will be represented by a byte (AIR, GRASS, SOIL)
    public final byte[][][] blocks = new byte[SIZE][SIZE][SIZE];

    // Used to store the mesh after greedy meshing is applied
    public ModelInstance meshInstance;

    // Constructor initializes all blocks to AIR
    public Chunk() {
        // Initialize all blocks to AIR (empty)
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = AIR; // Set the default state to AIR
                }
            }
        }
    }

    // Method to set a block at a given position to a specified type
    public void setBlock(int x, int y, int z, byte type) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE) {
            blocks[x][y][z] = type;
        }
    }

    // Method to get the type of block at a given position
    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) {
            return AIR; // Return AIR if out of bounds
        }
        return blocks[x][y][z]; // Return the block type (AIR, GRASS, SOIL)
    }
}
