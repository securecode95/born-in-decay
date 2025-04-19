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
    private float moveSpeed = 8f;
    private float gravity   = -18f;
    private float jumpSpeed = 8.5f;
    private boolean onGround;
    private final float mouseSensitivity = 0.2f;

    /**
     * @param voxels world‐space centers of every solid voxel
     */
    public void update(PerspectiveCamera camera, float delta, List<Vector3> voxels) {
        // 0) If we somehow got stuck, push upward until free
        int tries = 0;
        while (tries++ < 20 && isColliding(position, voxels)) {
            position.y += 0.05f;
        }

        // 1) Mouse look
        float dX = Gdx.input.getDeltaX() * mouseSensitivity;
        float dY = -Gdx.input.getDeltaY() * mouseSensitivity;
        yaw   += dX;
        pitch += dY;
        pitch = Math.max(-89f, Math.min(89f, pitch));
        Vector3 look = getLookDirection();

        // 2) Update camera
        camera.position.set(position.x, position.y + 0.6f, position.z);
        camera.direction.set(look);
        camera.update();

        // 3) Compute desired velocity
        Vector3 forward = new Vector3(look).set(look.x, 0, look.z).nor();
        Vector3 right   = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move    = new Vector3();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        move.nor().scl(moveSpeed);
        velocity.x = move.x;
        velocity.z = move.z;

        // 4) Gravity
        velocity.y += gravity * delta;

        // 5) Attempt X movement
        Vector3 testX = new Vector3(position.x + velocity.x * delta, position.y, position.z);
        if (!isColliding(testX, voxels)) {
            position.x = testX.x;
        } else {
            velocity.x = 0;
        }

        // 6) Attempt Z movement
        Vector3 testZ = new Vector3(position.x, position.y, position.z + velocity.z * delta);
        if (!isColliding(testZ, voxels)) {
            position.z = testZ.z;
        } else {
            velocity.z = 0;
        }

        // 7) Attempt Y movement (jump/fall)
        Vector3 testY = new Vector3(position.x,
            position.y + velocity.y * delta,
            position.z);
        if (!isColliding(testY, voxels)) {
            position.y = testY.y;
            onGround   = false;
        } else {
            if (velocity.y < 0) onGround = true;  // landed
            velocity.y = 0;
        }

        // 8) Jump
        if (onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            velocity.y = jumpSpeed;
            onGround   = false;
        }
    }

    /** AABB vs. block centers, skipping far-away voxels. */
    private boolean isColliding(Vector3 pos, List<Vector3> voxels) {
        float rp = 0.4f;  // player half‑size
        float rb = 0.5f;  // block half‑size
        float maxDist = rp + rb + 0.2f;

        for (Vector3 c : voxels) {
            if (Math.abs(c.x - pos.x) > maxDist ||
                Math.abs(c.y - pos.y) > maxDist ||
                Math.abs(c.z - pos.z) > maxDist) {
                continue;
            }
            if (pos.x + rp > c.x - rb && pos.x - rp < c.x + rb &&
                pos.y + rp > c.y - rb && pos.y - rp < c.y + rb &&
                pos.z + rp > c.z - rb && pos.z - rp < c.z + rb) {
                return true;
            }
        }
        return false;
    }

    /** Yaw/pitch → look dir */
    public Vector3 getLookDirection() {
        float cy = (float)Math.cos(Math.toRadians(yaw));
        float sy = (float)Math.sin(Math.toRadians(yaw));
        float cp = (float)Math.cos(Math.toRadians(pitch));
        float sp = (float)Math.sin(Math.toRadians(pitch));
        return new Vector3(cy*cp, sp, sy*cp).nor();
    }

    public void resetLook() {
        yaw   = -90f;
        pitch = 0f;
    }
}
