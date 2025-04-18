package com.rabalder.bornindecay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class PlayerController {
    public Vector3 position = new Vector3(8, 2.5f, 8); // center of chunk 0,0
    public Vector3 velocity = new Vector3();

    private float yaw = -90f;
    private float pitch = 0f;
    private float moveSpeed = 8f;
    private float gravity = -18f;
    private float jumpSpeed = 11f;
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

        move.nor().scl(moveSpeed);
        velocity.x = move.x;
        velocity.z = move.z;

        // Gravity
        velocity.y += gravity * delta;

        // Predict movement
        Vector3 nextPos = new Vector3(position).mulAdd(velocity, delta);

        // Collision and position update
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

        // Check grounded status after position update
        onGround = false;
        Vector3 foot = new Vector3(position.x, position.y - 0.1f, position.z);

        System.out.printf("👣 Player foot at: %.2f, %.2f, %.2f\n", foot.x, foot.y, foot.z);

        for (ModelInstance block : blocks) {
            Vector3 bp = block.transform.getTranslation(new Vector3());

            float dx = Math.abs(foot.x - bp.x);
            float dz = Math.abs(foot.z - bp.z);
            float dy = Math.abs(foot.y - (bp.y + 0.5f));

            // Show every check
            System.out.printf("⬛ Block at %.2f, %.2f, %.2f | dx=%.3f dy=%.3f dz=%.3f\n", bp.x, bp.y, bp.z, dx, dy, dz);

            if (dx <= 0.5f && dz <= 0.5f && dy <= 0.15f) {
                System.out.println("✅ Standing on this block!");
                onGround = true;
                break;
            }
        }

        // Jump
        if (onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            System.out.println("✅ Jump initiated - Player was onGround");
            velocity.y = jumpSpeed;
            onGround = false;
        } else if (!onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            System.out.println("⛔ Cannot jump - Player not onGround");
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
