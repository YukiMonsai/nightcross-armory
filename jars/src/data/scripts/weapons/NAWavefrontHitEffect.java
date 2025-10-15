package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import data.scripts.campaign.plugins.NAUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;



public class NAWavefrontHitEffect implements ProximityExplosionEffect {

    private static final float AREA_EFFECT = 600f;
    private static final float AREA_EFFECT_INNER = 250f;



    private static final Color PARTICLE_COLOR = new Color(75, 175, 255, 190);
    private static final float FRIENDLY_FIRE_MULT = 0.25f;
    private static final int EMP_ARC_COUNT = 5;


    private static final Vector2f ZERO = new Vector2f();

    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI proj
                      ) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f point = proj.getLocation();
        if (point == null) {
            return;
        }

        float emp = proj.getEmpAmount();
        Vector2f loc = proj.getLocation();

        List<CombatEntityAPI> targets = NAUtils.getEntitiesWithinRange(point, AREA_EFFECT);
        List<ShipAPI> targetsToHit = new ArrayList<ShipAPI>();
        List<CombatEntityAPI> innerRegion = new ArrayList<CombatEntityAPI>();
        List<CombatEntityAPI> outerRegion = new ArrayList<CombatEntityAPI>();

        // Get the ones in the inner region first
        for (int i = 0; i < targets.size(); i++) {
            CombatEntityAPI tt = targets.get(i);
            if (MathUtils.isWithinRange(tt, point, AREA_EFFECT_INNER)) {
                innerRegion.add(tt);
                if (tt instanceof ShipAPI)
                    targetsToHit.add((ShipAPI) tt);
            } else outerRegion.add(tt);
        }

        // targets in the inner region block targets in the outer region

        for (int i = 0; i < outerRegion.size(); i++) {
            CombatEntityAPI ot = outerRegion.get(i);
            if (ot instanceof ShipAPI) {
                Vector2f otLoc = ot.getLocation();
                boolean obscured = false;
                for (int ii = 0; ii < innerRegion.size(); ii++) {
                    // Trace ray thru circle
                    CombatEntityAPI it = innerRegion.get(ii);
                    Vector2f itLoc = it.getLocation();
                    Vector2f nearestP = MathUtils.getNearestPointOnLine(itLoc, loc, otLoc);
                    if (MathUtils.isPointWithinCircle(nearestP, itLoc, it.getCollisionRadius())) {
                        obscured = true;
                        break;
                    }
                }
                if (obscured) continue;
                targetsToHit.add((ShipAPI) ot);
            }

        }

        for (int i = 0; i < targetsToHit.size(); i++) {
            ShipAPI tt = targetsToHit.get(i);
            for (int ii = 0; ii < EMP_ARC_COUNT; ii++) {
                engine.spawnEmpArc(proj.getSource(),
                        loc,
                        tt,
                        tt,
                        DamageType.ENERGY,
                        0f,
                        emp
                                * ((proj.getSource() == null || tt.getOwner() != proj.getSource().getOwner()) ?
                                1f : FRIENDLY_FIRE_MULT), // emp
                        AREA_EFFECT, // max range
                        "tachyon_lance_emp_impact", //"tachyon_lance_emp_impact",
                        25f, // thickness
                        PARTICLE_COLOR,
                        new Color(65, 125, 255, 255)
                );
            }
        }

    }
}
