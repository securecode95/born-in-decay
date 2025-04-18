package com.rabalder.bornindecay;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.Vector3;
import java.util.List;

public class RaycastUtil {

    public static ModelInstance getTargetedBlock(Camera camera, List<ModelInstance> blocks, float maxDistance)
    {
        Ray ray = new Ray(camera.position.cpy(), camera.direction.cpy().nor());
        ModelInstance closest = null;
        float closestDist = maxDistance;
        Vector3 intersection = new Vector3();

        for (ModelInstance block : blocks) {
            Vector3 pos = block.transform.getTranslation(new Vector3());

            BoundingBox bounds = new BoundingBox(
                new Vector3(pos).sub(0.5f, 0.5f, 0.5f),
                new Vector3(pos).add(0.5f, 0.5f, 0.5f)
            );

            if (Intersector.intersectRayBounds(ray, bounds, intersection)) {
                float dist = ray.origin.dst(intersection);
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
        Vector3 normal = camera.direction.cpy().nor();

        Vector3 face = new Vector3(
            Math.abs(normal.x) > Math.abs(normal.y) && Math.abs(normal.x) > Math.abs(normal.z) ? Math.signum(normal.x) : 0,
            Math.abs(normal.y) > Math.abs(normal.x) && Math.abs(normal.y) > Math.abs(normal.z) ? Math.signum(normal.y) : 0,
            Math.abs(normal.z) > Math.abs(normal.x) && Math.abs(normal.z) > Math.abs(normal.y) ? Math.signum(normal.z) : 0
        );

        return targetPos.add(face);
    }
}
