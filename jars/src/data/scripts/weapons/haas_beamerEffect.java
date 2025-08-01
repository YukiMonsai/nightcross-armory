package data.scripts.weapons;

import java.util.Iterator;
import java.util.List;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.hullmods.NightcrossTargeting;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class haas_beamerEffect implements BeamEffectPlugin {

    public static float RANGE_FACTOR = 300f;
    public static float MIN_DIST = 100f;
    public static float ANGLE_MAX = 9f; // In degree



    public haas_beamerEffect() {
    }

    private static class BeamEffect {
        protected IntervalUtil interval = new IntervalUtil(0.5f, 0.7f);
        protected float originalAngle = 0f;
        protected boolean direction = false;
        protected float lastBrightness = 0f;
    }

    //public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        WeaponAPI weapon = beam.getWeapon();
        if (weapon == null) return;
        ShipAPI ship = weapon.getShip();
        float shipAngle = (ship != null) ? ship.getFacing() : 0;

        String key = "haasbeameffect_" + (weapon.getSlot() != null ?
                ((int) weapon.getSlot().getLocation().x) + "," + ((int) weapon.getSlot().getLocation().y)
                : weapon.getId());
        BeamEffect data = (BeamEffect) ship.getCustomData().get(key);
        if (data != null && data.interval.intervalElapsed() && beam.getBrightness() > data.lastBrightness) {
            ship.removeCustomData(key);
            data = null;
        }
        if (data == null) {
            data = new BeamEffect();
            ship.setCustomData(key, data);

            data.originalAngle = weapon.getCurrAngle() - shipAngle;


            if (weapon.getSlot() != null) {
                WeaponSlotAPI slot = weapon.getSlot();

                data.direction = slot.getLocation().y > 0;
            } else {
                data.direction = MathUtils.getRandomNumberInRange(0f, 1f) > 0.5f;
            }
        }

        //if (beam.getBrightness() < 1f) return;

        data.lastBrightness = beam.getBrightness();

        if (!data.interval.intervalElapsed())
            data.interval.advance(amount);
        //if (beam.getLengthPrevFrame() < 10) return;

        /*Vector2f loc;
        CombatEntityAPI target = findTarget(beam, beam.getWeapon(), engine);
        if (target == null) {
            loc = pickNoTargetDest(beam, beam.getWeapon(), engine);
        } else {
            loc = target.getLocation();
        }

        Vector2f from = Misc.closestPointOnSegmentToPoint(beam.getFrom(), beam.getRayEndPrevFrame(), loc);
        Vector2f to = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(from, loc));*/

        float angmax = ANGLE_MAX;
        if (weapon.getSpec() != null && weapon.getSpec().getWeaponId() == "na_sweeplaser") {
            angmax = 25;
        }
        float angle = Math.min(weapon.getArc()/2f, angmax * (RANGE_FACTOR / Math.max(weapon.getSpec().getMaxRange(), MIN_DIST)));
        float sweepLevel = data.interval.intervalElapsed() ? 1f : Math.min(1f, Math.max(0f, data.interval.getElapsed()/data.interval.getIntervalDuration()));

        weapon.setCurrAngle(shipAngle + data.originalAngle + (data.direction ? -1 : 1) * (-angle + 2f*angle*sweepLevel));



//			float thickness = beam.getWidth();
//			EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, null, to, null, thickness, beam.getFringeColor(), Color.white);
//			arc.setCoreWidthOverride(Math.max(20f, thickness * 0.67f));
        //Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f, 1f, arc.getLocation(), arc.getVelocity());
    }


    public Vector2f pickNoTargetDest(BeamAPI beam, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f from = beam.getFrom();
        Vector2f to = beam.getRayEndPrevFrame();
        float length = beam.getLengthPrevFrame();

        float f = 0.25f + (float) Math.random() * 0.75f;
        Vector2f loc = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(from, to));
        loc.scale(length * f);
        Vector2f.add(from, loc, loc);

        return Misc.getPointWithinRadius(loc, MIN_DIST);
    }

    public CombatEntityAPI findTarget(BeamAPI beam, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f to = beam.getRayEndPrevFrame();

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(to,
                MIN_DIST * 2f, MIN_DIST * 2f);
        int owner = weapon.getShip().getOwner();
        WeightedRandomPicker<CombatEntityAPI> picker = new WeightedRandomPicker<CombatEntityAPI>();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof MissileAPI) &&
                    !(o instanceof ShipAPI)) continue;
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) continue;
            if (other instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) other;
                if (!ship.isFighter() && !ship.isDrone()) continue;
            }

            float radius = Misc.getTargetingRadius(to, other, false);
            Vector2f p = Misc.closestPointOnSegmentToPoint(beam.getFrom(), beam.getRayEndPrevFrame(), other.getLocation());
            float dist = Misc.getDistance(p, other.getLocation()) - radius;
            if (dist > MIN_DIST) continue;

            picker.add(other);

        }
        return picker.pick();
    }

}





