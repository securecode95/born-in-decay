package com.rabalder.bornindecay;

public class Chunk {
    public static final int SIZE = 16;
    private final int chunkX, chunkZ;
    private final byte[][][] blocks = new byte[SIZE][SIZE][SIZE];

    /** Remember which chunk this is, so we can re‑key it. */
    public Chunk(long worldSeed, int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        // -- simple terrain generation as before --
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                float worldX = (chunkX*SIZE + x) * 0.1f;
                float worldZ = (chunkZ*SIZE + z) * 0.1f;
                int height = 8 + (int)(Math.sin(worldX)*Math.cos(worldZ)*4);
                for (int y = 0; y < SIZE; y++) {
                    if      (y + chunkX*0 >= height)                          blocks[x][y][z] = BlockType.AIR;
                    else if (y + chunkX*0 == height - 1)                      blocks[x][y][z] = BlockType.GRASS;
                    else if (y + chunkX*0 > height - 5)                       blocks[x][y][z] = BlockType.DIRT;
                    else                                                     blocks[x][y][z] = BlockType.STONE;
                }
            }
        }
    }

    public byte getBlock(int x,int y,int z) {
        if (x<0||y<0||z<0||x>=SIZE||y>=SIZE||z>=SIZE) return BlockType.AIR;
        return blocks[x][y][z];
    }

    public byte[][] buildMaskX(int x) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int y=0; y<SIZE; y++)
            for (int z=0; z<SIZE; z++)
                m[y][z] = getBlock(x,y,z);
        return m;
    }
    public byte[][] buildMaskY(int y) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x=0; x<SIZE; x++)
            for (int z=0; z<SIZE; z++)
                m[x][z] = getBlock(x,y,z);
        return m;
    }
    public byte[][] buildMaskZ(int z) {
        byte[][] m = new byte[SIZE][SIZE];
        for (int x=0; x<SIZE; x++)
            for (int y=0; y<SIZE; y++)
                m[x][y] = getBlock(x,y,z);
        return m;
    }

    /** Unique 64‑bit key for this chunk’s coordinates. */
    public long getKey() {
        return (((long)chunkX) << 32) | (chunkZ & 0xffffffffL);
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
}
