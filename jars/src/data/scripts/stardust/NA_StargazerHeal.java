package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.NightcrossID;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;


public class NA_StargazerHeal extends BaseHullMod {

    public static float AMOUNT_PER = 10f;
    public static float AMOUNT_PER_S = 1.0f;
    public static float SMOD_MAX_PERC = 0.9f;
    public static String id = "na_stargazerheal";

    private class NA_StargazerHealData {
        ShipAPI ship = null;
        public float prev = 0;
        public NA_StargazerHealData(ShipAPI ship) {
            this.ship = ship;
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

    }


    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (stats != null) {

            NA_StargazerHealData data = null;
            if (ship.getCustomData().containsKey(id)) data = (NA_StargazerHealData) ship.getCustomData().get(id);
            if (data == null) {
                data = new NA_StargazerHealData(ship);
                ship.getCustomData().put(id, data);
            }

            if (data != null) {
                NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
                float count = 0;
                if (swarm != null) {
                    float current = swarm.getNumActiveMembers();
                    if (data.prev > current) {
                        float diff = data.prev - current;
                        count = Math.round(diff);
                        ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + count * AMOUNT_PER * (
                                1f + (isSMod(stats) ? Math.min(1f, ship.getFluxLevel() / SMOD_MAX_PERC) : 0)
                                )));

                    }
                    data.prev = current;
                }

                for (int i = 0; i < count; i++) {
                    WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker2 = swarm.getPicker(true, true);
                    NA_StargazerStardust.SwarmMember fragment = picker2.pick();
                    if (fragment != null) {
                        Global.getCombatEngine().spawnEmpArcPierceShields(ship,
                                fragment.loc, ship, ship, DamageType.FRAGMENTATION, 0, 0, ship.getCollisionRadius(), "",15f,
                                new Color(255, 11, 32, 50),
                                new Color(236, 54, 131, 150)
                        );
                    }
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
       return ( ship.getSystem() != null && ship.getSystem().getId().equals("na_stardustlash")) ? "Incompatible with the ship's system" : "Requires Stardust Nebula";
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)AMOUNT_PER + "";
        return null;
    }



    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return (int)(AMOUNT_PER_S * 100) +  "%";
        if (index == 1) return (int)(SMOD_MAX_PERC * 100) + "%";
        return null;
    }
}











