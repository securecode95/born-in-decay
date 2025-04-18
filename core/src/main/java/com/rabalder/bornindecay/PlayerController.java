package com.rabalder.bornindecay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class PlayerController {
    public Vector3 position = new Vector3(8, 8, 20);
    public Vector3 velocity = new Vector3(0, 0, 0);

    private float moveSpeed = 8f;
    private float gravity = -20f;
    private float jumpSpeed = 10f;
    private boolean onGround = false;

    private float yaw = -90f; // ← forward along Z+
    private float pitch = 0f;
    private float mouseSensitivity = 0.2f;

    public void update(PerspectiveCamera camera, float delta, ArrayList<ModelInstance> blocks) {
        // Mouse input
        float deltaX = Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        yaw += deltaX;
        pitch += deltaY;
        pitch = Math.max(-89f, Math.min(89f, pitch)); // Clamp up/down
        System.out.printf("Pitch: %.4f\n", pitch);
        // Calculate direction
        Vector3 look = new Vector3(
            (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch))
        ).nor();

        camera.position.set(position.x, position.y + 0.6f, position.z);

        // Movement input
        Vector3 move = new Vector3();
        Vector3 forward = new Vector3(look).nor();
        Vector3 strafe = new Vector3(look).crs(camera.up).nor();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(strafe);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) move.add(strafe);

        move.nor().scl(moveSpeed);

        velocity.x = move.x;
        velocity.z = move.z;

        // Apply gravity
        velocity.y += gravity * delta;

        // Jumping
        if (onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            velocity.y = jumpSpeed;
            onGround = false;
        }

        // Predict next position
        Vector3 nextPos = new Vector3(position).mulAdd(velocity, delta);

        // Ground plane
        if (nextPos.y <= 1f) {
            nextPos.y = 1f;
            velocity.y = 0;
            onGround = true;
        } else {
            onGround = false;
        }

        // Check voxel collision
        if (!isColliding(nextPos, blocks)) {
            position.set(nextPos);
        } else {
            // Try horizontal movement only
            Vector3 testXZ = new Vector3(position.x + velocity.x * delta, position.y, position.z + velocity.z * delta);
            if (!isColliding(testXZ, blocks)) {
                position.x = testXZ.x;
                position.z = testXZ.z;
            }

            // Try vertical movement only
            Vector3 testY = new Vector3(position.x, position.y + velocity.y * delta, position.z);
            if (!isColliding(testY, blocks)) {
                position.y = testY.y;
            } else {
                velocity.y = 0;
            }
        }

        // Update camera position
        camera.position.set(position);
    }

    private boolean isColliding(Vector3 nextPos, ArrayList<ModelInstance> blocks) {
        float playerSize = 0.4f;

        for (ModelInstance block : blocks) {
            Vector3 blockPos = block.transform.getTranslation(new Vector3());

            if (
                nextPos.x + playerSize > blockPos.x - 0.5f &&
                    nextPos.x - playerSize < blockPos.x + 0.5f &&
                    nextPos.y < blockPos.y + 1.0f && nextPos.y > blockPos.y - 0.1f &&
                    nextPos.z + playerSize > blockPos.z - 0.5f &&
                    nextPos.z - playerSize < blockPos.z + 0.5f
            ) {
                return true;
            }
        }

        return false;
    }

    public void resetLook(PerspectiveCamera camera) {
        pitch = 0f;
        yaw = -90f;
        updateDirection(camera);
    }

    private void updateDirection(PerspectiveCamera camera) {
        Vector3 look = new Vector3(
            (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch))
        ).nor();

        camera.direction.set(look);
    }


}
