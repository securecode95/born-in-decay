package com.rabalder.bornindecay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class PlayerController {
    public Vector3 position = new Vector3(8f, 2.5f, 8f);
    public Vector3 velocity = new Vector3();

    private float yaw = -90f;
    private float pitch = 0f;
    private float moveSpeed = 8f;
    private float gravity = -18f;
    private float jumpSpeed = 8.5f;
    private boolean onGround = false;

    private final float mouseSensitivity = 0.2f;

    public void update(PerspectiveCamera camera, float delta, ArrayList<ModelInstance> blocks) {
        // Mouse movement
        float deltaX = Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        yaw += deltaX;
        pitch += deltaY;
        pitch = Math.max(-89f, Math.min(89f, pitch));

        Vector3 look = getLookDirection();

        // Apply camera position and direction
        camera.position.set(position.x, position.y + 0.6f, position.z);
        camera.direction.set(look);
        camera.update();

        // Movement input
        Vector3 forward = new Vector3(look).set(look.x, 0, look.z).nor(); // horizontal movement only
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move = new Vector3();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            System.out.println("Spacebar pressed");
        }

        move.nor().scl(moveSpeed);
        velocity.x = move.x;
        velocity.z = move.z;

        // Apply gravity
        velocity.y += gravity * delta;

        // Predict next position
        Vector3 nextPos = new Vector3(position).mulAdd(velocity, delta);

        // Attempt to move to next position
        if (!isColliding(nextPos, blocks)) {
            position.set(nextPos);
        } else {
            // Try horizontal only
            Vector3 testXZ = new Vector3(position.x + velocity.x * delta, position.y, position.z + velocity.z * delta);
            if (!isColliding(testXZ, blocks)) {
                position.x = testXZ.x;
                position.z = testXZ.z;
            }

            // Try vertical only
            Vector3 testY = new Vector3(position.x, position.y + velocity.y * delta, position.z);
            if (!isColliding(testY, blocks)) {
                position.y = testY.y;
            } else {
                velocity.y = 0;
            }
        }

// Ground detection after movement is resolved
        onGround = false;
        Vector3 below = new Vector3(position.x, position.y - 0.1f, position.z);

        for (ModelInstance block : blocks) {
            Vector3 bp = block.transform.getTranslation(new Vector3());
            if (
                below.x > bp.x - 0.5f && below.x < bp.x + 0.5f &&
                    below.z > bp.z - 0.5f && below.z < bp.z + 0.5f &&
                    Math.abs(below.y - (bp.y + 1.0f)) <= 0.1f
            ) {
                onGround = true;
                break;
            }
        }



        // Jump logic (after confirming grounded)
        if (onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            velocity.y = jumpSpeed;
            onGround = false;
        }
    }

    private boolean isColliding(Vector3 pos, ArrayList<ModelInstance> blocks) {
        float size = 0.4f;
        for (ModelInstance block : blocks) {
            Vector3 bp = block.transform.getTranslation(new Vector3());
            if (
                pos.x + size > bp.x - 0.5f && pos.x - size < bp.x + 0.5f &&
                    pos.y < bp.y + 1.0f && pos.y > bp.y - 0.1f &&
                    pos.z + size > bp.z - 0.5f && pos.z - size < bp.z + 0.5f
            ) {
                return true;
            }
        }
        return false;
    }

    public Vector3 getLookDirection() {
        return new Vector3(
            (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch))
        ).nor();
    }

    public void resetLook() {
        pitch = 0f;
        yaw = -90f;
    }
}
