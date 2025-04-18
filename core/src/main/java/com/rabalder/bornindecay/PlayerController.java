package com.rabalder.bornindecay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.PerspectiveCamera;

import java.util.ArrayList;

public class PlayerController {
    public Vector3 position = new Vector3(5, 6.5f, 5); // lift spawn above terrain
    public Vector3 velocity = new Vector3();
    private float yaw = -90f;
    private float pitch = 0f;
    private float moveSpeed = 8f;
    private float gravity = -20f;
    private float jumpSpeed = 10f;
    private boolean onGround = false;

    private final float mouseSensitivity = 0.2f;

    // --- PlayerController.java ---
    public void update(PerspectiveCamera camera, float delta, ArrayList<ModelInstance> blocks) {
        float deltaX = Gdx.input.getDeltaX() * mouseSensitivity;
        float deltaY = -Gdx.input.getDeltaY() * mouseSensitivity;

        yaw += deltaX;
        pitch += deltaY;
        pitch = Math.max(-89f, Math.min(89f, pitch));

        Vector3 look = new Vector3(
            (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(pitch)),
            (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch))
        ).nor();

        // ✅ Update camera to follow player
        camera.position.set(position.x, position.y + 0.6f, position.z);
        camera.direction.set(look); // ← You missed this
        camera.update();

        // Movement input
        Vector3 forward = new Vector3(look).nor();
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move = new Vector3();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);

        move.nor().scl(moveSpeed);
        velocity.x = move.x;
        velocity.z = move.z;
        velocity.y += gravity * delta;

        if (onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            velocity.y = jumpSpeed;
            onGround = false;
        }

        Vector3 nextPos = new Vector3(position).mulAdd(velocity, delta);

        // Basic ground check
        if (nextPos.y <= 1f) {
            nextPos.y = 1f;
            velocity.y = 0;
            onGround = true;
        } else {
            onGround = false;
        }

        // ✅ Finally update the player position
        if (!isColliding(nextPos, blocks)) {
            position.set(nextPos); // ← YOU MISSED THIS LINE
        }
    }

    private boolean isColliding(Vector3 pos, ArrayList<ModelInstance> blocks) {
        float size = 0.4f;
        for (ModelInstance block : blocks) {
            Vector3 blockPos = block.transform.getTranslation(new Vector3());
            if (
                pos.x + size > blockPos.x - 0.5f &&
                    pos.x - size < blockPos.x + 0.5f &&
                    pos.y < blockPos.y + 1f &&
                    pos.y > blockPos.y - 0.1f &&
                    pos.z + size > blockPos.z - 0.5f &&
                    pos.z - size < blockPos.z + 0.5f
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
