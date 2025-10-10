package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.NightcrossID;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;


public class NA_StargazerRage extends BaseHullMod {

    public static float RATE_INCREASE = 30f;
    public static float HULL_RATE_INCREASE = 30f;
    public static float SMOD_HULL_RATE_INCREASE = 60f;
    public static float MIN_HULL = .3f;
    public static String id = "na_stargazerrage";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        /*stats.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).modifyPercent(id, RATE_INCREASE);

        boolean sMod = isSMod(stats);
        if (sMod) {
            stats.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).modifyPercent(id, RATE_INCREASE + SMOD_RATE_INCREASE);
        }*/
    }


    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (stats != null) {

            if (ship.getFluxLevel() > 0)
                stats.getDynamic().getStat(NA_StargazerStars.STARDUST_RESPAWN_RATE_MULT).modifyPercent(id + "flux", RATE_INCREASE * ship.getFluxLevel());
            else stats.getDynamic().getStat(NA_StargazerStars.STARDUST_RESPAWN_RATE_MULT).unmodify(id + "flux");
            if (ship.getHullLevel() < 1f) {
                stats.getDynamic().getStat(NA_StargazerStars.STARDUST_RESPAWN_RATE_MULT).modifyPercent(id + "hull", RATE_INCREASE * Math.min(1f, (1f - ship.getHullLevel()) / (1f - MIN_HULL)));
            } else stats.getDynamic().getStat(NA_StargazerStars.STARDUST_RESPAWN_RATE_MULT).unmodify(id + "hull");

            boolean sMod = isSMod(stats);
            if (sMod) {
                if (ship.getHullLevel() < 1f) {

                    stats.getDynamic().getStat(NA_StargazerStars.STARDUST_RESPAWN_MAX_MULT).modifyPercent(id, SMOD_HULL_RATE_INCREASE * Math.min(1f, (1f - ship.getHullLevel())/(1f - MIN_HULL)));
                } else {
                    stats.getDynamic().getStat(NA_StargazerStars.STARDUST_RESPAWN_MAX_MULT).unmodify(id);
                }
            }
        }

        if (amount > 0 && MathUtils.getRandomNumberInRange(0, 1f) < (0.2f + 0.4f * ship.getFluxLevel() + 0.6f * (1f - ship.getHullLevel())) * amount) {
            NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
            WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker2 = swarm.getPicker(true, true);
            NA_StargazerStardust.SwarmMember fragment = picker2.pick();
            NA_StargazerStardust.SwarmMember fragment2 = picker2.pick();
            if (fragment2 != null && fragment != null && fragment2 != fragment) {
                Global.getCombatEngine().spawnEmpArcVisual(
                        fragment.loc, ship, fragment2.loc, ship, 15f,
                        new Color(132, 0, 255, 50),
                        new Color(255, 222, 234, 150)
                );
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
        if (index == 0) return "" + (int)RATE_INCREASE + "%";
        if (index == 1) return "" + (int)HULL_RATE_INCREASE + "%";
        if (index == 2) return "" + (int)MIN_HULL + "%";
        return null;
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)SMOD_HULL_RATE_INCREASE + "%";
        if (index == 1) return "" + (int)(MIN_HULL * 100f) + "%";
        return null;
    }
}











