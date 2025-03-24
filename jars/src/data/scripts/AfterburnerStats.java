package data.scripts;


import java.awt.Color;
import java.util.List;
import java.util.Vector;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.Global;
import org.lwjgl.util.vector.Vector2f;

public class AfterburnerStats extends BaseShipSystemScript {

    public static float SPEED_BONUS = 200f;
    public static float SPEED_BONUS_FORWARD = 300f;
    public static float BONUS_DURATION = 2.5f;
    public static float TURN_BONUS = 150f;
    public static float TURN_RATE_BONUS = 150f;
    //public static float FLUX_GEN = -300f; // Generates flux per second

    private String ID = "NA_Afterburners";

    private Color color = new Color(255,180,110,255);


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        float multiplier = 1.0f;
        ShipAPI ship = (ShipAPI) stats.getEntity();


        if (state == State.COOLDOWN) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
            stats.getFluxDissipation().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);

            /*List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
            if (maneuveringThrusters != null) {
                for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                    if (e.getEngineSlot().getContrailWidth() == 128f) {
                        ship.getEngineController().setFlameLevel(e.getEngineSlot(), 0f);
                    }
                }
            }*/

        } else {
            boolean isThrustingForward = false;
            boolean isThrustingBackward = false;
            if (stats.getEntity() instanceof ShipAPI) {
                ShipEngineControllerAPI controller = ship.getEngineController();
                if (controller.isAcceleratingBackwards()) {
                    isThrustingForward = false;
                    isThrustingBackward = true;
                } else if (controller.isAccelerating()) {
                    isThrustingForward = true;
                }

            }
            stats.getMaxSpeed().modifyFlat(id, (!isThrustingBackward ? SPEED_BONUS_FORWARD : SPEED_BONUS));
            stats.getAcceleration().modifyPercent(id, (isThrustingForward ? SPEED_BONUS_FORWARD * (0.5f + 0.5f*effectLevel) * 2.0f : SPEED_BONUS * (0.5f + 0.5f*effectLevel) * 2.0f));
            stats.getDeceleration().modifyPercent(id, SPEED_BONUS * 2.5f * effectLevel);
            if (isThrustingForward) {
                stats.getMaxTurnRate().unmodify(id);
                stats.getTurnAcceleration().unmodify(id);
            } else {
                stats.getTurnAcceleration().modifyFlat(id, TURN_BONUS * effectLevel);
                stats.getMaxTurnRate().modifyPercent(id, TURN_RATE_BONUS);
            }
            //stats.getFluxDissipation().modifyFlat(id, FLUX_GEN * effectLevel);
        }

        if (stats.getEntity() instanceof ShipAPI) {



            ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

            if (ship.getChildModulesCopy() != null && ship.getChildModulesCopy().size() > 0) {
                for (ShipAPI child: ship.getChildModulesCopy()) {
                    child.getEngineController().fadeToOtherColor(child, color, new Color(0, 0, 0, 0), effectLevel, 1.0f);
                    child.getEngineController().extendFlame(child, 0.5f * effectLevel, 0.5f * effectLevel, 0.0f * effectLevel);
                    child.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());
                }
            }

            List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
            if (maneuveringThrusters != null) {
                for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                    if (Math.abs(Math.sin(Math.toRadians(e.getEngineSlot().getAngle()))) > 0.1
                    || Math.cos(Math.toRadians(e.getEngineSlot().getAngle())) > 0) {
                        // Nothing!!
                    } else {
                        ship.getEngineController().fadeToOtherColor(e.getEngineSlot(), color, new Color(0, 0, 0, 0), effectLevel, 1.0f);
                        ship.getEngineController().extendFlame(e.getEngineSlot(), 0.5f * effectLevel, 0.5f * effectLevel, 0.0f * effectLevel);
                    }
                }
            }


			/*String key = ship.getId() + "_" + id;
			Object test = Global.getCombatEngine().getCustomData().get(key);
			if (state == State.IN) {
				if (test == null && effectLevel > 0.2f) {
					Global.getCombatEngine().getCustomData().put(key, new Object());
					ship.getEngineController().getExtendLengthFraction().advance(1f);
					for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
						if (engine.isSystemActivated()) {
							ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 1f);
						}
					}
				}
			} else {
				Global.getCombatEngine().getCustomData().remove(key);
			}*/
        }
    }
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("improved maneuverability", false);
        } else if (index == 1) {
            return new StatusData("+" + (int)SPEED_BONUS + " top speed", false);
        }
        return null;
    }
}
