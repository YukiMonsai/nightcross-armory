package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.NightcrossID;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;


public class NA_StargazerDiss extends BaseHullMod {

    public static float AMOUNT_PER = 5f;
    public static float SMOD_BONUS = 50f;
    public static String id = "na_stargazerdiss";



    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

        boolean sMod = isSMod(stats);
        if (sMod) {
            stats.getDynamic().getStat(NA_StargazerStars.STARDUST_RESPAWN_MAX_MULT).modifyPercent(id, SMOD_BONUS);
        }
    }


    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (stats != null) {

            NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
            if (swarm != null && swarm.getNumActiveMembers() > 0) {
                stats.getFluxDissipation().modifyFlat(id, AMOUNT_PER * swarm.getNumActiveMembers());
            } else
                stats.getFluxDissipation().unmodify(id);

            if (amount > 0 && MathUtils.getRandomNumberInRange(0, 1f) < (0.2f + 0.25f * ship.getFluxLevel() + 0.6f * swarm.getNumActiveMembers()/(1f + swarm.getNumMembersToMaintain())) * amount) {
                if (swarm != null) {
                    WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker2 = swarm.getPicker(true, true);
                    NA_StargazerStardust.SwarmMember fragment = picker2.pick();
                    NA_StargazerStardust.SwarmMember fragment2 = picker2.pick();
                    if (fragment2 != null && fragment != null && fragment2 != fragment) {
                        Global.getCombatEngine().spawnEmpArcVisual(
                                fragment.loc, ship, fragment2.loc, ship, 15f,
                                new Color(255, 132, 252, 50),
                                new Color(255, 185, 246, 150)
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
        return "Requires Stardust Nebula";
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)AMOUNT_PER + "";
        return null;
    }



    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)SMOD_BONUS + "%";
        return null;
    }
}











