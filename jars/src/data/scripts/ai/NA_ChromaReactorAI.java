package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NA_FastCaps;
import org.lwjgl.util.vector.Vector2f;

// based on Darkened Gaze AI
public class NA_ChromaReactorAI implements ShipSystemAIScript {

    protected ShipAPI ship;
    protected CombatEngineAPI engine;
    protected ShipwideAIFlags flags;
    protected ShipSystemAPI system;
    protected NA_FastCaps script;
    protected float systemFluxPerSecond = 0f;

    protected IntervalUtil tracker = new IntervalUtil(0.75f, 1.25f);

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;

        script = (NA_FastCaps)system.getScript();

    }

    protected ShipAPI targetOverride = null;

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        //boolean toggle = system.getSpec().isToggle();


        if (system.isActive()) {
            if (ship.getAI() instanceof ShipAIPlugin) {
                ShipAIPlugin b = (ShipAIPlugin) ship.getAI();
                b.setTargetOverride(targetOverride); // needs to be set every frame
            }
        } else {
            targetOverride = null;
        }

        if (tracker.intervalElapsed()) {

            if (ship.getCaptain() != null && ship.getCaptain().getPersonalityAPI() != null) {
                if (!ship.getCaptain().getPersonalityAPI().equals(Personalities.RECKLESS)
                    && !ship.getCaptain().getPersonalityAPI().equals(Personalities.AGGRESSIVE)
                    && ship.getAIFlags() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) {
                    return;
                }
            }
            if (system.getCooldownRemaining() > 0) return;
            if (system.isOutOfAmmo()) return;
            if (ship.getFluxTracker().isOverloadedOrVenting()) return;

            if (target != null) {
                if (target.isHulk() || !target.isAlive()) {
                    target = null;
                }
            }

            if (script.weapons != null && script.weapons.size() > 0) {
                systemFluxPerSecond = 0f;
                for (WeaponAPI w: script.weapons) {
                    if (w.getAmmoTracker() == null
                            || w.getAmmoTracker().getAmmo() > 0) {
                        systemFluxPerSecond += w.getFluxCostToFire()/Math.max(0.05f, Math.max(w.getCooldown(), w.getSpec().getChargeTime()));
                    }

                }
            }

            float activeTimeRemaining = (ship.getMaxFlux() - ship.getCurrFlux()) / Math.max(1f, systemFluxPerSecond);


            boolean inRange = false;
            boolean inArc = false;
            boolean isFarFromArc = false;
            if (target != null) {
                float range = Misc.getDistance(ship.getLocation(), target.getLocation()) -
                        Misc.getTargetingRadius(ship.getLocation(), target, true);
                inRange = range < script.getRange();
                inArc = Misc.isInArc(ship.getFacing(), 5f,
                        Misc.getAngleInDegrees(ship.getLocation(), target.getLocation()));
                if (!inArc) {
                    isFarFromArc = !Misc.isInArc(ship.getFacing(), Math.max(30f, 60f - range * 0.05f),
                            Misc.getAngleInDegrees(ship.getLocation(), target.getLocation()));
                }
            }

            Vector2f to = Misc.getUnitVectorAtDegreeAngle(ship.getFacing());
            to.scale(script.getRange());
            Vector2f.add(ship.getLocation(), to, to);
            boolean ffDanger;
            if (script.isFFAConcern() ) {
                ffDanger = Global.getSettings().getFriendlyFireDanger(ship, null,
                        ship.getLocation(), to, Float.MAX_VALUE, 3f, script.getRange()) > 0.1f;
            } else {
                // pretend FF concern, so it doesn't fire right through friendlies but can clip them without
                // worrying too much about it
                ffDanger = Global.getSettings().getFriendlyFireDanger(ship, null,
                        ship.getLocation(), to, Float.MAX_VALUE, 3f, script.getRange()) > 0.5f;
            }
            if (system.isActive()) {
                flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT, 1.0f);

                if (!flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) {
                    if (ship.getCaptain() != null && ship.getCaptain().getPersonalityAPI() != null
                            && (ship.getCaptain().getPersonalityAPI().equals(Personalities.RECKLESS)
                            || ship.getCaptain().getPersonalityAPI().equals(Personalities.AGGRESSIVE))) {
                        flags.setFlag(ShipwideAIFlags.AIFlags.MANEUVER_RANGE_FROM_TARGET, 1.0f, script.getRange() * 1f / (1f + script.RANGE_BOOST));
                    } else {
                        flags.setFlag(ShipwideAIFlags.AIFlags.MANEUVER_RANGE_FROM_TARGET, 1.0f, script.getRange() * 0.9f);
                    }
                }

                if (target == null || !inRange || isFarFromArc || ffDanger) {
                    giveCommand();
                    return;
                }

                if (activeTimeRemaining < system.getChargeActiveDur()) {
                    giveCommand();
                    return;
                }

                return;
            }

            float minFireTime = 1.5f;
            float fluxLevel = ship.getFluxLevel();

            if (fluxLevel > 0.9f || activeTimeRemaining < minFireTime) {
                return;
            }

            if (inRange && inArc && !ffDanger) {
                giveCommand();
                targetOverride = target;
            } else {
                // Override maneuver range to stay at standoff distance unless aggressive/reckless
                if (!flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) {
                    if (ship.getCaptain() != null && ship.getCaptain().getPersonalityAPI() != null
                            && (ship.getCaptain().getPersonalityAPI().equals(Personalities.RECKLESS)
                            || ship.getCaptain().getPersonalityAPI().equals(Personalities.AGGRESSIVE))) {
                        flags.setFlag(ShipwideAIFlags.AIFlags.MANEUVER_RANGE_FROM_TARGET, 1.0f, script.getRange() * 1f / (1f + script.RANGE_BOOST));
                    } else {
                        flags.setFlag(ShipwideAIFlags.AIFlags.MANEUVER_RANGE_FROM_TARGET, 1.0f, script.getRange() * 0.9f);
                    }
                }
            }
        }
    }


    public void giveCommand() {
        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
    }

}
