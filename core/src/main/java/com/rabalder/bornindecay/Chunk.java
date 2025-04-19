package com.rabalder.bornindecay;

public class Chunk {
    public static final int SIZE = 16;
    private final byte[][][] blocks = new byte[SIZE][SIZE][SIZE];


    /** Returns the block at (x,y,z), or AIR if out of bounds */
    public byte getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE)
            return BlockType.AIR;
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, byte id) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE)
            return;
        blocks[x][y][z] = id;
    }

    /** Greedy‐mesh mask for X cuts (faces perpendicular to X) */
    public byte[][] buildMaskX(int x) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++)
            for (int z = 0; z < SIZE; z++)
                m[y][z] = getBlock(x - 1, y, z);
        return m;
    }

    /** Greedy‐mesh mask for Y cuts (faces perpendicular to Y) */
    public byte[][] buildMaskY(int y) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int z = 0; z < SIZE; z++)
                m[x][z] = getBlock(x, y - 1, z);
        return m;
    }

    /** Greedy‐mesh mask for Z cuts (faces perpendicular to Z) */
    public byte[][] buildMaskZ(int z) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                m[x][y] = getBlock(x, y, z - 1);
        return m;
    }
}
