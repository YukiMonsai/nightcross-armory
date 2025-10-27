package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.plugins.NAUtils;
import data.scripts.stardust.NA_StargazerHull;
import data.scripts.stardust.NA_StargazerStardust;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class StargazerNecro extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships with the Stardust Nebula hullmod";
    }

    private static final float RELOAD_PER_HULL_SIZE = 25f;


    public static final String ID = "na_sic_necro";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("After disabling an enemy ship, gain %s per ship size class of the disabled ship.", 0f, Misc.getHighlightColor(), NA_StargazerHull.STARGAZER_RED,
                "" + (int)(RELOAD_PER_HULL_SIZE) + " Stardust");

        tooltipMakerAPI.addSpacer(10f);
        tooltipMakerAPI.addPara("Also steals all %s from the disabled ship if it has its own %s.", 0f, Misc.getGrayColor(), NA_StargazerHull.STARGAZER_RED, "Stardust", "Stardust Nebula");


    }


    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {

        // this doesnt work actually because the swarm is created after the game starts
        //if (NA_StargazerStardust.getSwarmFor(ship) == null) return;


        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        var listeners = engine.getListenerManager().getListeners(NA_NecroDMGListener.class);
        boolean found = false;

        for (NA_NecroDMGListener listener : listeners) {
            if (listener.side == ship.getOwner()) {
                found = true;
                return;
            }
        }
        engine.getListenerManager().addListener(new NA_NecroDMGListener(ship.getOwner()));
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {


    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/


    protected class NA_NecroDMGListener implements HullDamageAboutToBeTakenListener {
        public NA_NecroDMGListener(int side) {
            this.side = side;
        }
        int side= 0;
        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {

            if (param instanceof ShipAPI killer) {
                //if (param != pilotedShip) return false
                if (ship.isFighter()) return false;
                if (ship.getOwner() == side) return false;
                if (NA_StargazerStardust.getSwarmFor(killer) == null) return false;
                if (ship.getHitpoints() <= 0 && !ship.hasTag("sc_na_necro_counted")) {
                    ship.addTag("sc_na_necro_counted");

                    float count = RELOAD_PER_HULL_SIZE * NAUtils.shipSize(ship);
                    NA_StargazerStardust killedswarm = NA_StargazerStardust.getSwarmFor(ship);
                    float kcount = 0;
                    if (killedswarm != null) kcount += killedswarm.getNumActiveMembers();
                    if (count > 0) {
                        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(killer);
                        for (int i = 0; i < count; i++) {
                            NA_StargazerStardust.SwarmMember p = swarm.addMember();
                            p.loc.set(MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius()));
                            p.fader.setDurationIn(0.3f);
                        }
                    }
                    if (kcount > 0) {
                        // also steal killed swarm :)
                        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(killer);
                        for (int i = 0; i < kcount; i++) {
                            WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = killedswarm.getPicker(false, true);
                            NA_StargazerStardust.SwarmMember picked = picker.pick();
                            if (picked != null) {
                                killedswarm.removeMember(picked);
                                swarm.addMember(picked);
                            }
                        }
                    }
                    //stacks.add(MomentumStacks(duration))

                    /*var existing = ship.getListeners(NA_EncorseAdvanceListener.class).stream().findFirst();

                    if (!existing.isEmpty()) {
                        existing.get().duration = maxTime;
                    } else {
                        doer.addListener(new NA_EncorseAdvanceListener(doer, maxTime))
                    }*/

                }
            }

            return false;
        }

    }


}

