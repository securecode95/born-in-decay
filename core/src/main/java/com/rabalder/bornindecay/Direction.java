package com.rabalder.bornindecay;

import com.badlogic.gdx.math.Vector3;

public enum Direction {
    UP(new Vector3(0, 1, 0)),
    DOWN(new Vector3(0, -1, 0)),
    NORTH(new Vector3(0, 0, -1)),
    SOUTH(new Vector3(0, 0, 1)),
    EAST(new Vector3(1, 0, 0)),
    WEST(new Vector3(-1, 0, 0));

    public final Vector3 normal;

    Direction(Vector3 normal) {
        this.normal = normal;
    }
}
