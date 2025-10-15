package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.scripts.campaign.plugins.NAUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NA_WavefrontHit implements OnHitEffectPlugin {

    public static float ARC_DAMAGE = 300f;
    public static float ARC_COUNT = 8f;
    public static float ARC_RANGE = 500f;
    public static float ARC_RANGE_FRIENDLY = 90f;
    public static float ARC_FF_MULT = 0.25f;
    public static float ARC_RANGE_INC = 125f;
    public static float ARC_PROBABILITY_HARDFLUX = 1.5f; // probability at 100% hardflux

    public static float ADD_FLUX = 100f;



    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        List<CombatEntityAPI> nearbyObjects = NAUtils.getEntitiesWithinRange(
                point, ARC_RANGE
        );

        HashMap<CombatEntityAPI, Integer> alreadyHit = new HashMap();
        int currentArcs = 0;

        int maxPerTarget = 3;

        for (int ii = 0; ii < ARC_COUNT && currentArcs < ARC_COUNT; ii++) {
            // Successively larger circles
            for (float dist = 0; dist < ARC_RANGE && currentArcs < ARC_COUNT; dist += ARC_RANGE_INC) {
                List<CombatEntityAPI> misc = new ArrayList<CombatEntityAPI>();
                List<ShipAPI> unshielded = new ArrayList<ShipAPI>();
                List<ShipAPI> shielded = new ArrayList<ShipAPI>();
                for (CombatEntityAPI e : nearbyObjects) {
                    if (e.getCollisionClass() == CollisionClass.NONE) continue;
                    float de = MathUtils.getDistance(e, point);
                    if (de >= dist && de <= ARC_RANGE && de <= dist + ARC_RANGE_INC) {
                        if (de <= ARC_RANGE_FRIENDLY ||
                                !(projectile.getSource() != null
                                        && e.getOwner() != projectile.getSource().getOwner())) {
                            if (!(e instanceof ShipAPI)
                                    || ((ShipAPI) e).getHullSize() == ShipAPI.HullSize.FIGHTER
                                    || (((ShipAPI) e).getShield() == null
                                    || ((ShipAPI) e).getShield().isWithinArc(point))) {
                                if ((e instanceof ShipAPI) && ((ShipAPI) e).getHullSize() != ShipAPI.HullSize.FIGHTER)
                                    unshielded.add((ShipAPI) e);
                                else if ((e instanceof CombatAsteroidAPI)
                                        || ((e instanceof ShipAPI) && ((ShipAPI) e).getHullSize() == ShipAPI.HullSize.FIGHTER)) {
                                    misc.add(e);
                                }
                            } else {
                                shielded.add((ShipAPI) e);
                            }
                        }
                    }
                }

                // prioritize unshielded
                for (int i = 0 ; i < unshielded.size() && currentArcs < ARC_COUNT; i++) {
                    int index = MathUtils.getRandomNumberInRange(0, unshielded.size() - 1);
                    if (!alreadyHit.containsKey(unshielded.get(index))
                        || alreadyHit.get(unshielded.get(index)) < maxPerTarget) {
                        doArc(projectile, point, unshielded.get(index), false);
                        currentArcs++;
                        alreadyHit.put(unshielded.get(index),
                                !alreadyHit.containsKey(unshielded.get(index)) ? 1 :
                                        alreadyHit.get(unshielded.get(index)) + 1);
                    }

                    unshielded.remove(index);
                }
                // then do shield calculation
                for (int i = 0 ; i < shielded.size() && currentArcs < ARC_COUNT; i++) {
                    int index = MathUtils.getRandomNumberInRange(0, shielded.size() - 1);
                    ShipAPI ship = shielded.get(index);
                    if (!alreadyHit.containsKey(ship)
                            || alreadyHit.get(ship) < maxPerTarget) {
                        doArc(projectile, point, ship,
                                MathUtils.getRandomNumberInRange(0f, 1f) <
                                        ARC_PROBABILITY_HARDFLUX
                                                *ship.getFluxTracker().getHardFlux()/ship.getFluxTracker().getMaxFlux()
                                );
                        currentArcs++;
                        alreadyHit.put(ship,
                                !alreadyHit.containsKey(ship) ? 1 :
                                        alreadyHit.get(ship) + 1);
                    }
                    shielded.remove(index);
                }

                // then do non-ships
                for (int i = 0 ; i < misc.size() && currentArcs < ARC_COUNT; i++) {
                    int index = MathUtils.getRandomNumberInRange(0, misc.size() - 1);
                    if (!alreadyHit.containsKey(misc.get(index))
                            || alreadyHit.get(misc.get(index)) < maxPerTarget) {
                        doArc(projectile, point, misc.get(index), false);
                        currentArcs++;
                        alreadyHit.put(misc.get(index),
                                !alreadyHit.containsKey(misc.get(index)) ? 1 :
                                        alreadyHit.get(misc.get(index)) + 1);
                    }
                    misc.remove(index);
                }
            }
        }

        float remainingArcs = ARC_COUNT - currentArcs;
        float angleoffset = (float) Math.random();

        for (int i = 0; i < remainingArcs; i++) {
            // random arcs
            float x = (float) (ARC_RANGE * Math.cos(angleoffset + (i * 2*Math.PI) / remainingArcs));
            float y = (float) (ARC_RANGE * Math.sin(angleoffset + (i * 2*Math.PI) / remainingArcs));
            Vector2f point2 = new Vector2f(point.x + x, point.y + y);
            Global.getCombatEngine().spawnEmpArc(projectile.getSource(),
                    point,
                    new SimpleEntity(point),
                    new SimpleEntity(point2),
                    DamageType.ENERGY,
                    0,
                    0, // emp
                    ARC_RANGE * 2, // max range
                    "tachyon_lance_emp_impact",
                    20f, // thickness
                    new Color(75, 100, 255, 255),
                    new Color(255, 255, 255, 255)
            );
        }
    }

    private void doArc(DamagingProjectileAPI projectile, Vector2f point, CombatEntityAPI target, boolean piercing) {
        float mult = (target instanceof ShipAPI && projectile != null && target.getOwner() == projectile.getOwner())
                ? ARC_FF_MULT : 1.0f;

        ShipAPI ship = projectile.getSource();
        if (ship != null && ship.getMutableStats() != null) {
            mult *= ship.getMutableStats().getMissileWeaponDamageMult().getModifiedValue();
        }
        if (target instanceof ShipAPI) {
            ((ShipAPI) target).getFluxTracker().setHardFlux(Math.min(
                    ((ShipAPI) target).getFluxTracker().getMaxFlux(),
                    ((ShipAPI) target).getFluxTracker().getHardFlux() + ADD_FLUX*mult
            ));
        }
        if (piercing) Global.getCombatEngine().spawnEmpArcPierceShields(projectile.getSource(),
                point,
                new SimpleEntity(point),
                target,
                DamageType.ENERGY,
                mult * ARC_DAMAGE,
                mult * 2.5f*ARC_DAMAGE, // emp
                100000f, // max range
                "tachyon_lance_emp_impact",
                20f, // thickness
                new Color(75, 100, 255, 255),
                new Color(255, 255, 255, 255)
        );
        else Global.getCombatEngine().spawnEmpArc(projectile.getSource(),
                point,
                new SimpleEntity(point),
                target,
                DamageType.ENERGY,
                mult * ARC_DAMAGE,
                mult * 2.5f*ARC_DAMAGE, // emp
                100000f, // max range
                "tachyon_lance_emp_impact",
                20f, // thickness
                new Color(75, 100, 255, 255),
                new Color(255, 255, 255, 255)
        );
    }
}
