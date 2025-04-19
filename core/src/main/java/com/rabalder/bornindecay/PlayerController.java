package com.rabalder.bornindecay;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

public class PlayerController {
    public final Vector3 position = new Vector3();
    private final Vector3 velocity = new Vector3();

    public void update(PerspectiveCamera cam, float dt, List<Vector3> coll) {
        float speed = 10f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) velocity.z = -speed;
        else if (Gdx.input.isKeyPressed(Input.Keys.S)) velocity.z = speed;
        else velocity.z=0;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) velocity.x = -speed;
        else if (Gdx.input.isKeyPressed(Input.Keys.D)) velocity.x = speed;
        else velocity.x=0;

        // simple gravity
        velocity.y -= 20f*dt;

        // apply
        position.mulAdd(velocity, dt);

        // floor at y=1
        if (position.y<1) { position.y=1; velocity.y=0; }

        cam.position.set(position);
        cam.lookAt(position.x,position.y,position.z-1);
        cam.update();
    }
}
