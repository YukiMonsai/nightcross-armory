package data.scripts;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.hullmods.NightcrossTargeting;

import java.awt.*;
import java.util.List;

public class NA_RelativityDrive extends BaseShipSystemScript {

    

    public static float TIMEFLOW_MULT = 9f;
    public static float ROF_MULT = 1.1f; // boost weapons at end
    public static float ROF_MULT_DURING = 2.5f; // boost weapons at end

    private String ID = "NA_RelativityDrive";

    private static Color COLOR_AFTERIMAGE = new Color(125, 75, 255, 255);
    private static Color color = new Color(125,75,255,255);

    private static float TIME_AFTERIMAGE = 0.33f;

    public static class NA_RelativityDriveData {
        IntervalUtil interval = new IntervalUtil(TIME_AFTERIMAGE, TIME_AFTERIMAGE);
        public void reset(float time) {
            interval = new IntervalUtil(time, time);
        }
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        String shipID = id + "_" + ship.getId();


        if (state == State.COOLDOWN) {
            stats.getTimeMult().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getBallisticRoFMult().unmodify(id);
            stats.getEnergyRoFMult().unmodify(id);
            stats.getMissileRoFMult().unmodify(id);

            if (stats.getEntity() == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().getTimeMult().unmodify(shipID);
            }
            String key = ID + "_data_" + ship.getId();
            if ((NA_RelativityDriveData) ship.getCustomData().get(key) != null) {
                ship.getCustomData().remove(key);
            }
        } else {
            stats.getTimeMult().modifyMult(id, 1f + effectLevel*TIMEFLOW_MULT);
            stats.getBallisticRoFMult().modifyMult(id, effectLevel == 1 ? ROF_MULT_DURING : ROF_MULT);
            stats.getEnergyRoFMult().modifyMult(id, effectLevel == 1 ? ROF_MULT_DURING : ROF_MULT);
            stats.getMissileRoFMult().modifyMult(id, effectLevel == 1 ? ROF_MULT_DURING : ROF_MULT);

            if (stats.getEntity() == Global.getCombatEngine().getPlayerShip()
                && ((ShipAPI) stats.getEntity()).isAlive()) {
                Global.getCombatEngine().getTimeMult().modifyMult(shipID, 1f/(1f + effectLevel*TIMEFLOW_MULT));
            } else {
                Global.getCombatEngine().getTimeMult().unmodify(shipID);
            }


        }

        if (stats.getEntity() instanceof ShipAPI) {


            ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

            if (ship.getChildModulesCopy() != null && ship.getChildModulesCopy().size() > 0) {
                for (ShipAPI child: ship.getChildModulesCopy()) {
                    child.getEngineController().fadeToOtherColor(child, color, new Color(125, 75, 255, 255), effectLevel, 1.0f);
                    child.getEngineController().extendFlame(child, 0.1f * effectLevel, 0.1f * effectLevel, 0.5f * effectLevel);
                    child.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());
                }
            }

            List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
            if (maneuveringThrusters != null) {
                for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                    ship.getEngineController().fadeToOtherColor(e.getEngineSlot(), color, new Color(125, 75, 255, 255), effectLevel, 1.0f);
                    ship.getEngineController().extendFlame(e.getEngineSlot(), 0.1f * effectLevel, 0.1f * effectLevel, 0.5f * effectLevel);
                }
            }

            if (ship != null) {
                String key = ID + "_data_" + ship.getId();
                NA_RelativityDriveData data = (NA_RelativityDriveData) ship.getCustomData().get(key);
                if (data == null) {
                    data = new NA_RelativityDriveData();
                    ship.setCustomData(key, data);
                }
                data.interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
                if (data.interval.intervalElapsed()) {
                    ship.addAfterimage(COLOR_AFTERIMAGE, 0f, 0f,
                            -ship.getVelocity().x, -ship.getVelocity().y,
                            0.1f * effectLevel,
                            0f, 0.5f, 1.2f * effectLevel, true, false, false);
                    data.reset(TIME_AFTERIMAGE);
                }

            }
        }
    }
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getTimeMult().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getMissileRoFMult().unmodify(id);


        String shipID = id + "_" + ((ShipAPI)(stats.getEntity())).getId();

        Global.getCombatEngine().getTimeMult().unmodify(shipID);

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        String key = ID + "_data_" + ship.getId();
        if ((NA_RelativityDriveData) ship.getCustomData().get(key) != null) {
            ship.getCustomData().remove(key);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("WARNING extreme gravitational anomaly", false);
        } else if (index == 1) {
            return new StatusData("+" + (int)(100*TIMEFLOW_MULT) + "% timeflow", false);
        }
        return null;
    }
}
