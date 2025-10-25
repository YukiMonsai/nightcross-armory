package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.stardust.NA_StargazerHull;
import data.scripts.stardust.NA_StargazerStars;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class StargazerNebula extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }


    private static final float SCALE_PER_DMOD = 0.1f;
    private static final float CAPACITY_GAIM = .25f;
    private static final float SENSOR_MULT = 0.2f;
    private static final float MAX_DMOD = 5f;


    public static final String ID = "na_sic_nebula";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s to sensor profile", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(100f * SENSOR_MULT) + "%");
        tooltipMakerAPI.addPara("Ships with the %s hullmod gain %s regeneration per d-mod (max 5)", 0f, Misc.getHighlightColor(), NA_StargazerHull.STARGAZER_RED,
                "Stargazer Nebula", "+" + (int)(SCALE_PER_DMOD * 100) + "% Stardust");
        tooltipMakerAPI.addPara("Ships with the %s hullmod gain %s capacity", 0f, Misc.getHighlightColor(), NA_StargazerHull.STARGAZER_RED,
                "Stargazer Nebula", "+" + (int)(CAPACITY_GAIM * 100) + "% Stardust");
    }



    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
        float dmods = 0;
        if (stats.getFleetMember() != null) {
            dmods = DModManager.getNumDMods(stats.getFleetMember().getVariant());
        }
        if (stats.getVariant() != null) {
            dmods = DModManager.getNumDMods(stats.getVariant());
        }
        if (dmods > MAX_DMOD) dmods = MAX_DMOD;

        if (dmods > 0) {
            stats.getDynamic().getMod(NA_StargazerStars.STARDUST_RESPAWN_RATE_MULT).modifyPercent(ID, 1f + dmods * SCALE_PER_DMOD);
        } else {
            stats.getDynamic().getMod(NA_StargazerStars.STARDUST_RESPAWN_RATE_MULT).unmodify(ID);
        }

        stats.getDynamic().getMod(NA_StargazerStars.STARDUST_RESPAWN_MAX_MULT).modifyPercent(ID, 100f * CAPACITY_GAIM);
        stats.getSensorProfile().modifyMult(ID, 1f - SENSOR_MULT);
    }

}
