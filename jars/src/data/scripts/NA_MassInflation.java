package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.util.C;
import data.scripts.campaign.plugins.NAUtils;
import data.scripts.hullmods.NA_GravityDrag;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.*;
import java.util.List;

public class NA_MassInflation extends BaseShipSystemScript {

    public static float MAGNITUDE = 0.9f;
    public static float HFLUX_REDUCTION = 0.5f;
    public static float SCRAMBLE_DURATION = 5f;
    public static float TIMEFLOW = 0.9f;
    public static String TIDAL_PAUSE = "na_tidalpause";

    protected Object STATUSKEY1 = new Object();


    public float lastLevel = 0;

    public static class TargetData {
        public ShipAPI target;
        public EveryFrameCombatPlugin targetEffectPlugin;
        public float timeflowmult = TIMEFLOW;
        public float speedmult = 1.0f;
        public Object KEY_TARGET = new Object();
        public IntervalUtil elapsed = new IntervalUtil(SCRAMBLE_DURATION, SCRAMBLE_DURATION);
        public IntervalUtil afterimage = new IntervalUtil(0.1f, 0.1f);
        public TargetData(ShipAPI ship, ShipAPI target) {
            this.target = target;
        }
    }

    //public static final float INCOMING_DAMAGE_MULT = 0.25f;
    //public static final float INCOMING_DAMAGE_CAPITAL = 0.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (lastLevel < 0.1 && effectLevel >= 0.1) {
            Global.getCombatEngine().addNegativeParticle(ship.getLocation(), ship.getVelocity(), NA_GravityDrag.RANGE * 1.4f, 0.25f, 1f, new Color(0, 255, 255, 50));
        }
        lastLevel = effectLevel;

        if (effectLevel == 1) {
            MagicLensFlare.createSharpFlare(Global.getCombatEngine(), ship, ship.getLocation(), 4, NA_GravityDrag.RANGE * 1.4f, 0,
                    new Color(205, 171, 255),
                    new Color(65, 0, 66));
            Global.getCombatEngine().addSmoothParticle(ship.getLocation(), ship.getVelocity(), NA_GravityDrag.RANGE * 1.4f, 0.1f, 0.25f, new Color(255, 153, 207, 255));

            // do the thing

            List<DamagingProjectileAPI> projectiles = NAUtils.getProjectilesWithinRange(ship.getLocation(), NA_GravityDrag.RANGE, true);
            for (DamagingProjectileAPI proj : projectiles) {
                float dist = MathUtils.getDistance(ship, proj);
                float mult = Math.min(1, Math.max(0, 1.2f - dist/NA_GravityDrag.RANGE));
                if (mult < 0) continue;
                proj.getVelocity().set(proj.getVelocity().x * (float) (1f - .9f * mult), proj.getVelocity().y * (float) (1f - .9f * mult));
            }
            List<ShipAPI> enemies = NAUtils.getEnemyShipsWithinRange(ship, ship.getLocation(), NA_GravityDrag.RANGE, true);

            for (ShipAPI target : enemies) {
                float dist = MathUtils.getDistance(ship, target);
                float mult = Math.min(1, Math.max(0, 1.2f - dist/NA_GravityDrag.RANGE));

                if (mult < 0) continue;


                final String targetDataKey = target.getId() + "_massinflation_target_data";

                Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
                if (targetDataObj == null) {
                    Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(ship, target));
                    if (target != null) {
                        targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
                        ((TargetData) targetDataObj).target = target;
                        ((TargetData) targetDataObj).timeflowmult = TIMEFLOW;
                        ((TargetData) targetDataObj).elapsed.setElapsed(SCRAMBLE_DURATION - SCRAMBLE_DURATION * mult);

					/*if (target.getFluxTracker().showFloaty() ||
							ship == Global.getCombatEngine().getPlayerShip() ||
							target == Global.getCombatEngine().getPlayerShip()) {
						target.getFluxTracker().showOverloadFloatyIfNeeded("Gravity disrupted!", TEXT_COLOR, 4f, true);
					}*/
                    }
                } else {
                    ((TargetData) targetDataObj).target = target;
                    ((TargetData) targetDataObj).elapsed.setElapsed(Math.min(SCRAMBLE_DURATION - SCRAMBLE_DURATION * mult, ((TargetData) targetDataObj).elapsed.getElapsed()));
                    ((TargetData) targetDataObj).timeflowmult = TIMEFLOW;
                }

                if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;

                final TargetData targetData = (TargetData) targetDataObj;

                if (targetData.targetEffectPlugin == null) {
                    targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
                        @Override
                        public void advance(float amount, List<InputEventAPI> events) {
                            if (Global.getCombatEngine().isPaused()) return;
                            if (targetData.target == Global.getCombatEngine().getPlayerShip()) {
                                String str = "WARNING: TIME SINGULARITY";
                                String str2 = "TIME STOPPED";
                                Global.getCombatEngine().maintainStatusForPlayerShip(targetData.KEY_TARGET,
                                        "graphics/icons/na_gravity.png",
                                        str.substring(0, (int) Math.floor(str.length() * targetData.elapsed.getElapsed()/SCRAMBLE_DURATION)),
                                        str2.substring(0, (int) Math.floor(str2.length() * targetData.elapsed.getElapsed()/SCRAMBLE_DURATION)), true);
                            }

                            if (targetData.elapsed.intervalElapsed() || !targetData.target.isAlive()) {
                                targetData.target.getMutableStats().getTimeMult().unmodify(targetDataKey);
                                Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
                                Global.getCombatEngine().getCustomData().remove(targetDataKey);
                            } else {
                                targetData.elapsed.advance(amount);
                                targetData.target.getMutableStats().getTimeMult().modifyMult(targetDataKey, 1-targetData.timeflowmult * Math.min(1f, 1.5f - targetData.elapsed.getElapsed()/SCRAMBLE_DURATION));

                                if (targetData.afterimage.intervalElapsed()) {
                                    targetData.afterimage.randomize();
                                    targetData.afterimage.setInterval(0.3f, 0.3f);
                                    targetData.target.addAfterimage(new Color(113, 1, 35, 155 - (int) Math.floor(100 * targetData.elapsed.getElapsed()/SCRAMBLE_DURATION)),
                                            0,
                                            0,
                                            0,
                                            0,
                                            2f, 0.15f, 0.3f, 0.3f,
                                            true,
                                            false,
                                            true
                                    );
                                } else targetData.afterimage.advance(amount);

                            }
                        }
                    };
                    Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
                }

            }
        }

        float mult = MAGNITUDE;
        stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - mult) * (float) Math.sqrt(Math.min(1, 0.1 + effectLevel)));
        stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - mult) * (float) Math.sqrt(Math.min(1, 0.1 + effectLevel)));
        stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - mult) * (float) Math.sqrt(Math.min(1, 0.1 + effectLevel)));
        stats.getDynamic().getStat(TIDAL_PAUSE).modifyFlat(id, 1f - (1f - mult) * (float) Math.sqrt(Math.min(1, 0.1 + effectLevel)));


        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        }
        if (player) {
            ShipSystemAPI system = ship.getSystem();
            if (system != null) {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                        system.getSpecAPI().getIconSpriteName(), system.getDisplayName(),
                        "Tidal Force: 24.4*10^" + (int)(Math.max(-6, -30 + 100 * effectLevel)), false);
            }
        }


    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getDynamic().getStat(TIDAL_PAUSE).unmodify(id);
    }



    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return !NAUtils.getEnemyShipsWithinRange(ship, ship.getLocation(), NA_GravityDrag.RANGE, true).isEmpty();
    }

}
