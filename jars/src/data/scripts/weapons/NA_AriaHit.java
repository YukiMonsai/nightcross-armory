package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.entities.Ship;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NA_AriaHit implements OnHitEffectPlugin {

    private static final float SCRAMBLE_MULT = 0.8f;
    private static final float SCRAMBLE_DURATION = 2f;


    public static Color JITTER_UNDER_COLOR = new Color(75,175,255,155);


    public static Color TEXT_COLOR = new Color(55,175,255,255);


    public static class TargetData {
        public ShipAPI target;
        public EveryFrameCombatPlugin targetEffectPlugin;
        public float fluxDissMult;
        public Object KEY_TARGET = new Object();
        public IntervalUtil elapsed = new IntervalUtil(SCRAMBLE_DURATION, SCRAMBLE_DURATION);
        //public IntervalUtil arcInterval = new IntervalUtil(0.25f, 0.5f);
        public TargetData(ShipAPI ship, ShipAPI target) {
            this.target = target;
        }
    }

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI targett, Vector2f point, boolean shieldHit, ApplyDamageResultAPI resultAPI, CombatEngineAPI engineAPI) {
        if (point == null || shieldHit) {
            return;
        }

        if (targett instanceof ShipAPI) {
            float dmg = 1;
            final ShipAPI target = (ShipAPI) targett;

            if (dmg > 0) {
                if (shieldHit) {
                    // reduced damage to shields unless hardflux
                    float hflux_max = 0.8f;
                    float hflux_level = ((ShipAPI) targett).getHardFluxLevel();
                    float factor = Math.max(0, Math.min(1f, hflux_level / hflux_max));
                    float mult = 0.6f;
                    float mmult = 0.4f;
                    resultAPI.setDamageToShields(resultAPI.getDamageToShields() * (mmult + mult * factor));
                }


                // apply the debuff
                ShipAPI ship = proj.getSource();

                final String targetDataKey = target.getId() + "_ariahit_target_data";

                Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
                if (targetDataObj == null) {
                    Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(ship, target));
                    if (target != null) {
                        targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
                        ((TargetData) targetDataObj).target = target;
                        if (target.getFluxTracker().showFloaty() ||
                                ship == Global.getCombatEngine().getPlayerShip() ||
                                target == Global.getCombatEngine().getPlayerShip()) {
                            target.getFluxTracker().showOverloadFloatyIfNeeded("Gravity disrupted!", TEXT_COLOR, 4f, true);
                        }
                    }
                } else {
                    ((TargetData) targetDataObj).target = target;
                    ((TargetData) targetDataObj).elapsed.setElapsed(0f); // reset
                }

                if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;

                final TargetData targetData = (TargetData) targetDataObj;
                targetData.fluxDissMult = SCRAMBLE_MULT;

                if (targetData.targetEffectPlugin == null) {
                    targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
                        @Override
                        public void advance(float amount, List<InputEventAPI> events) {
                            if (Global.getCombatEngine().isPaused()) return;
                            if (targetData.target == Global.getCombatEngine().getPlayerShip()) {
                                Global.getCombatEngine().maintainStatusForPlayerShip(targetData.KEY_TARGET,
                                        "graphics/icons/hullsys/high_energy_focus.png",
                                        "Gravitic Distortion",
                                        "-" + (int)(100f - (targetData.fluxDissMult - 1f) * 100f) + "% mobility", true);
                            }

                            if (targetData.elapsed.intervalElapsed() || !targetData.target.isAlive()) {
                                targetData.target.getMutableStats().getAcceleration().unmodify(targetDataKey);
                                targetData.target.getMutableStats().getTurnAcceleration().unmodify(targetDataKey);
                                targetData.target.getMutableStats().getMaxTurnRate().unmodify(targetDataKey);
                                targetData.target.getMutableStats().getMaxSpeed().unmodify(targetDataKey);
                                Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
                                Global.getCombatEngine().getCustomData().remove(targetDataKey);
                            } else {
                                targetData.elapsed.advance(amount);
                                targetData.target.getMutableStats().getAcceleration().modifyMult(targetDataKey, targetData.fluxDissMult);
                                targetData.target.getMutableStats().getTurnAcceleration().modifyMult(targetDataKey, targetData.fluxDissMult);
                                targetData.target.getMutableStats().getMaxTurnRate().modifyMult(targetDataKey, targetData.fluxDissMult);
                                targetData.target.getMutableStats().getMaxSpeed().modifyMult(targetDataKey, targetData.fluxDissMult);

                                targetData.target.setJitterUnder(this, JITTER_UNDER_COLOR, 0.7f, 3, 10f, 45f);
                            }
                        }
                    };
                    Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
                }


            }

        }
    }
}
