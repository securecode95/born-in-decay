package com.rabalder.bornindecay;

public class Chunk {
    public static final int SIZE = 16;
    private final byte[] data = new byte[SIZE*SIZE*SIZE];

    public byte getBlock(int x, int y, int z) {
        if (x<0||x>=SIZE||y<0||y>=SIZE||z<0||z>=SIZE) return BlockType.AIR;
        return data[x + SIZE*(y + SIZE*z)];
    }

    public void setBlock(int x,int y,int z, byte id) {
        if (x<0||x>=SIZE||y<0||y>=SIZE||z<0||z>=SIZE) return;
        data[x + SIZE*(y + SIZE*z)] = id;
    }
}
