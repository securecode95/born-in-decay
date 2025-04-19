// Chunk.java
package com.rabalder.bornindecay;

public class Chunk {
    public static final int SIZE = 16;
    private final byte[][][] blocks = new byte[SIZE][SIZE][SIZE];

    public Chunk() {
        // initialize to AIR
        for (int x=0; x<SIZE; x++)
            for (int y=0; y<SIZE; y++)
                for (int z=0; z<SIZE; z++)
                    blocks[x][y][z] = BlockType.AIR;
    }

    public void setBlock(int x,int y,int z, byte type) {
        if (x>=0&&x<SIZE && y>=0&&y<SIZE && z>=0&&z<SIZE)
            blocks[x][y][z] = type;
    }

    public byte getBlock(int x,int y,int z) {
        if (x<0||x>=SIZE||y<0||y>=SIZE||z<0||z>=SIZE)
            return BlockType.AIR;
        return blocks[x][y][z];
    }
}
