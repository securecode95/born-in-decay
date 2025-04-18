package com.rabalder.bornindecay;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class RaycastUtil {

    public static ModelInstance getTargetedBlock(Camera camera, ArrayList<ModelInstance> blocks, float maxDistance) {
        Ray ray = new Ray(camera.position.cpy(), camera.direction.cpy().nor());

        ModelInstance closest = null;
        float closestDist = maxDistance;

        for (ModelInstance block : blocks) {
            Vector3 pos = block.transform.getTranslation(new Vector3());
            Vector3 min = new Vector3(pos).sub(0.5f, 0.5f, 0.5f);
            Vector3 max = new Vector3(pos).add(0.5f, 0.5f, 0.5f);

            if (Intersector.intersectRayBoundsFast(ray, min, max)) {
                float dist = camera.position.dst(pos);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = block;
                }
            }
        }

        return closest;
    }

    public static Vector3 getPlacementPosition(ModelInstance target, Camera camera) {
        Vector3 targetPos = target.transform.getTranslation(new Vector3());
        Vector3 dir = camera.direction.cpy().nor();

        // Snap to dominant axis to place on correct face
        Vector3 offset = new Vector3(
            Math.abs(dir.x) > Math.abs(dir.y) && Math.abs(dir.x) > Math.abs(dir.z) ? Math.signum(dir.x) : 0,
            Math.abs(dir.y) > Math.abs(dir.x) && Math.abs(dir.y) > Math.abs(dir.z) ? Math.signum(dir.y) : 0,
            Math.abs(dir.z) > Math.abs(dir.x) && Math.abs(dir.z) > Math.abs(dir.y) ? Math.signum(dir.z) : 0
        );

        return targetPos.add(offset);
    }
}
