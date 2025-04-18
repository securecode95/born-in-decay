package com.rabalder.bornindecay;

import java.util.Random;

public class OpenSimplexNoise {
    private static final int PSIZE = 2048;
    private static final int PMASK = 2047;

    private final short[] perm;

    public OpenSimplexNoise(long seed) {
        perm = new short[PSIZE];
        short[] source = new short[PSIZE];
        for (short i = 0; i < PSIZE; i++)
            source[i] = i;

        Random rand = new Random(seed);
        for (int i = PSIZE - 1; i >= 0; i--) {
            int r = rand.nextInt(i + 1);
            perm[i] = source[r];
            source[r] = source[i];
        }
    }

    public double noise2D(double x, double y) {
        int xi = fastFloor(x);
        int yi = fastFloor(y);

        double xf = x - xi;
        double yf = y - yi;

        double u = fade(xf);
        double v = fade(yf);

        int aa = perm[(perm[xi & PMASK] + yi) & PMASK];
        int ab = perm[(perm[xi & PMASK] + yi + 1) & PMASK];
        int ba = perm[(perm[(xi + 1) & PMASK] + yi) & PMASK];
        int bb = perm[(perm[(xi + 1) & PMASK] + yi + 1) & PMASK];

        double x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
        double x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);

        return lerp(x1, x2, v);
    }

    private static int fastFloor(double x) {
        return x >= 0 ? (int) x : (int) x - 1;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        switch (hash & 0x3) {
            case 0: return  x + y;
            case 1: return -x + y;
            case 2: return  x - y;
            case 3: return -x - y;
            default: return 0; // Never happens
        }
    }
}
