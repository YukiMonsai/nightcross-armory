package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class StargazerLogistics extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }


    private static final float ABYSS_FUEL = 0.5f;
    private static final float ABYSS_DEPTH_SCALE = 2.0f;
    private static final float ABYSS_BURN = 2.0f;
    private static final float KINETIC_HULL_DAMAGE = 0.05f;


    public static final String ID = "na_sic_voyager";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s fuel use and supplies maintenance in the abyss", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(ABYSS_FUEL * 100) + "%");
        tooltipMakerAPI.addPara("%s to burn level in the abyss", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(ABYSS_BURN) + "");
        tooltipMakerAPI.addPara("%s kinetic damage taken", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(KINETIC_HULL_DAMAGE * 100f) + "%");
    }



    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
        stats.getKineticDamageTakenMult().modifyMult(ID, 1f - KINETIC_HULL_DAMAGE);

    }

    @Override
    public void advance(SCData data, Float amunt) {
        if (data.getFleet() != null && (
                (data.getFleet().isInHyperspace() && Misc.isInAbyss(data.getFleet().getLocation()))
                || (data.getFleet().getStarSystem() != null && Misc.isInAbyss(data.getFleet().getStarSystem().getLocation()))
                )) {
            float depth = Misc.getAbyssalDepth(data.getFleet().getLocation());
            float scale = Math.min(1f, Math.max(0, depth / ABYSS_DEPTH_SCALE));
            if (scale > 0) {
                data.getFleet().getStats().getFuelUseHyperMult().modifyMult(ID, 1f - scale* ABYSS_FUEL, "Voyager");
                data.getFleet().getStats().getFuelUseHyperMult().modifyMult(ID, 1f - scale* ABYSS_FUEL, "Voyager");
                data.getFleet().getStats().getFleetwideMaxBurnMod().modifyFlat(ID, scale*ABYSS_BURN/HyperspaceTerrainPlugin.ABYSS_BURN_MULT, "Voyager");

                for (FleetMemberAPI member : data.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.getStats() != null) {

                        member.getStats().getSuppliesPerMonth().modifyMult(ID, 1f - scale* ABYSS_FUEL, "Voyager");
                    }
                }
            } else {
                data.getFleet().getStats().getFuelUseHyperMult().unmodify(ID);
                data.getFleet().getStats().getFleetwideMaxBurnMod().unmodify(ID);
                for (FleetMemberAPI member : data.getFleet().getFleetData().getMembersListCopy()) {
                    if (member.getStats() != null) {

                        member.getStats().getSuppliesPerMonth().unmodify(ID);
                    }
                }
            }
        } else {
            data.getFleet().getStats().getFuelUseHyperMult().unmodify(ID);
            data.getFleet().getStats().getFleetwideMaxBurnMod().unmodify(ID);

            for (FleetMemberAPI member : data.getFleet().getFleetData().getMembersListCopy()) {
                if (member.getStats() != null) {

                    member.getStats().getSuppliesPerMonth().unmodify(ID);
                }
            }
        }
    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
