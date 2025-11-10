package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NAUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;


public class NA_StargazerSoulsteal extends BaseHullMod {

    public static String id = "na_stargazersoulsteal";
    private static final float RELOAD_PER_HULL_SIZE = 20f;
    private static final float MAX_BONUS_MULT = 30f;


    private static Map<ShipAPI.HullSize, Float> mag = new HashMap();
    static {
        mag.put(ShipAPI.HullSize.FIGHTER, 2f);
        mag.put(ShipAPI.HullSize.FRIGATE, 1f);
        mag.put(ShipAPI.HullSize.DESTROYER, 0.5f);
        mag.put(ShipAPI.HullSize.CRUISER, 0.25f);
        mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.125f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        var listeners = engine.getListenerManager().getListeners(NA_SoulstealerDMGListener.class);
        boolean found = false;

        for (NA_SoulstealerDMGListener listener : listeners) {
            if (listener.side == ship.getOwner()) {
                found = true;
                return;
            }
        }
        engine.getListenerManager().addListener(new NA_SoulstealerDMGListener(ship.getOwner()));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        /*stats.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).modifyPercent(id, RATE_INCREASE);

        boolean sMod = isSMod(stats);
        if (sMod) {
            stats.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).modifyPercent(id, RATE_INCREASE + SMOD_RATE_INCREASE);
        }*/
    }


    public static Object STATUS_KEY1 = new Object();
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        MutableShipStatsAPI stats = ship.getMutableStats();
        if (stats != null) {

            NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
            if (swarm != null) {
                float num = swarm.getNumActiveMembers();
                float mult = num * mag.getOrDefault(ship.getHullSize(), 1f);
                if (mult > MAX_BONUS_MULT) mult = MAX_BONUS_MULT;

                if (mult > 0) {
                    stats.getEnergyWeaponDamageMult().modifyPercent(id, mult);
                    stats.getMissileWeaponDamageMult().modifyPercent(id, mult);
                    if (ship == Global.getCombatEngine().getPlayerShip()) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(STATUS_KEY1,
                                Global.getSettings().getSpriteName("ui", "icon_tactical_fragment_swarm"),
                                spec.getDisplayName(),
                                "+" + ((int)(mult)) + "% bonus energy and missile weapon damage",
                                false);
                    }
                } else {
                    stats.getEnergyWeaponDamageMult().unmodify(id);
                    stats.getMissileWeaponDamageMult().unmodify(id);
                }

            }
        }
    }


    @Override
    public CargoStackAPI getRequiredItem() {
        return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL,
                new SpecialItemData(NightcrossID.STARDUST_CORE, null), null);
    }

    public static boolean hasStardust(ShipAPI ship) {
        if (ship == null || ship.getVariant() == null) return false;
        for (String id : ship.getVariant().getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (spec != null && (spec.getId().equals("na_stargazerhullmod"))) return true;
        }
        return ship.getVariant().hasHullMod("na_stargazerstars");
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        if (!hasStardust(ship)) return false;

        return true;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        return "Requires Stardust Nebula";
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }


    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltipMakerAPI, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltipMakerAPI.addPara("After disabling an enemy ship, gain %s per ship size class of the disabled ship.", 0f, Misc.getHighlightColor(), NA_StargazerHull.STARGAZER_RED,
                "" + (int)(RELOAD_PER_HULL_SIZE) + " Stardust");
        tooltipMakerAPI.addPara("Increases damage of energy and missile weapons by %s per %s, depending on hull size. Maximum %s damage bonus.", 0f, Misc.getHighlightColor(), NA_StargazerHull.STARGAZER_RED,
                "1%", Math.round(1f / (mag.containsKey(ship.getHullSize()) ? mag.get(ship.getHullSize()) : 1f)) + " Stardust",  "" + (MAX_BONUS_MULT) + "%");

        tooltipMakerAPI.addSpacer(10f);
        tooltipMakerAPI.addPara("Also steals all %s from the disabled ship if it has its own %s.", 0f, Misc.getGrayColor(), NA_StargazerHull.STARGAZER_RED, "Stardust", "Stardust Nebula");
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        return null;
    }



    protected class NA_SoulstealerDMGListener implements HullDamageAboutToBeTakenListener {
        public NA_SoulstealerDMGListener(int side) {
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
                if (ship.getHitpoints() <= 0 && !ship.hasTag("sc_na_soulstealer_counted")) {
                    ship.addTag("sc_na_soulstealer_counted");

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











