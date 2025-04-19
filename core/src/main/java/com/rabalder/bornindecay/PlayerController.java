package com.rabalder.bornindecay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

public class PlayerController {
    public Vector3 position = new Vector3(8f, 2.5f, 8f);
    public Vector3 velocity = new Vector3();

    private float yaw  = -90f;
    private float pitch = 0f;
    private float moveSpeed   = 8f;
    private float gravity     = -18f;
    private float jumpSpeed   = 8.5f;
    private boolean onGround  = false;
    private final float mouseSensitivity = 0.2f;

    /**
     * @param voxels List of world‐space centers of each solid voxel.
     */
    public void update(PerspectiveCamera camera, float delta, List<Vector3> voxels) {
        // --- handle mouse look ---
        float dX = Gdx.input.getDeltaX() * mouseSensitivity;
        float dY = -Gdx.input.getDeltaY() * mouseSensitivity;
        yaw   += dX;
        pitch += dY;
        pitch = Math.max(-89f, Math.min(89f, pitch));

        Vector3 look = getLookDirection();

        // update camera
        camera.position.set(position.x, position.y + 0.6f, position.z);
        camera.direction.set(look);
        camera.update();

        // --- handle WASD ---
        Vector3 forward = new Vector3(look).set(look.x, 0, look.z).nor();
        Vector3 right   = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move    = new Vector3();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        move.nor().scl(moveSpeed);

        // store horizontal into velocity
        velocity.x = move.x;
        velocity.z = move.z;

        // --- gravity ---
        velocity.y += gravity * delta;

        // predict next position
        Vector3 nextPos = new Vector3(position).mulAdd(velocity, delta);

        // --- collision ---
        if (!isColliding(nextPos, voxels)) {
            position.set(nextPos);
        } else {
            // try XZ only
            Vector3 testXZ = new Vector3(position.x + velocity.x * delta,
                position.y,
                position.z + velocity.z * delta);
            if (!isColliding(testXZ, voxels)) {
                position.x = testXZ.x;
                position.z = testXZ.z;
            }
            // try Y only
            Vector3 testY = new Vector3(position.x,
                position.y + velocity.y * delta,
                position.z);
            if (!isColliding(testY, voxels)) {
                position.y = testY.y;
            } else {
                velocity.y = 0;
            }
        }

        // --- ground check ---
        onGround = false;
        Vector3 below = new Vector3(position.x, position.y - 0.1f, position.z);
        for (Vector3 center : voxels) {
            if (below.x > center.x - 0.5f && below.x < center.x + 0.5f &&
                below.z > center.z - 0.5f && below.z < center.z + 0.5f &&
                Math.abs(below.y - (center.y + 0.5f)) <= 0.1f) {
                onGround = true;
                break;
            }
        }

        // --- jump ---
        if (onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            velocity.y = jumpSpeed;
            onGround   = false;
        }
    }

    /** AABB vs. block tests */
    private boolean isColliding(Vector3 pos, List<Vector3> voxels) {
        float r = 0.4f;
        for (Vector3 center : voxels) {
            if (pos.x + r > center.x - 0.5f && pos.x - r < center.x + 0.5f &&
                pos.y + r > center.y - 0.5f && pos.y - r < center.y + 0.5f &&
                pos.z + r > center.z - 0.5f && pos.z - r < center.z + 0.5f) {
                return true;
            }
        }
        return false;
    }

    /** Standard yaw/pitch → direction vector */
    public Vector3 getLookDirection() {
        float cy = (float)Math.cos(Math.toRadians(yaw));
        float sy = (float)Math.sin(Math.toRadians(yaw));
        float cp = (float)Math.cos(Math.toRadians(pitch));
        float sp = (float)Math.sin(Math.toRadians(pitch));
        return new Vector3(cy * cp, sp, sy * cp).nor();
    }

    public void resetLook() {
        pitch = 0f;
        yaw   = -90f;
    }
}
