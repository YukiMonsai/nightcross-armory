package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.misc.SCSettings;
import second_in_command.skills.automated.SCBaseAutoPointsSkillPlugin;

public class Machina2 extends SCBaseAutoPointsSkillPlugin {
    public int getProvidedPoints() {
        return (int)(90 * SCSettings.getAutoPointsMult());
    }
    @Override
    public String getAffectsString() {
        return "all automated ships with d-mods";
    }


    private static final float DMOD_SCALE = 0.07f;
    private static final float DMOD_MAX = 5f;


    public static final String ID = "na_sic_machina2";


    public void addTooltip(SCData data, TooltipMakerAPI tooltip) {
        tooltip.addPara("Reduces automated points cost of automated vessels by %s per d-mod (max %s).", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(DMOD_SCALE * 100) + "%", (int)(DMOD_MAX) + "");
        super.addTooltip(data, tooltip);
    }
    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

        if (Misc.isAutomated(variant)) {
            float dmods = 0;
            if (stats.getFleetMember() != null) {
                dmods = DModManager.getNumDMods(stats.getFleetMember().getVariant());
            }
            if (stats.getVariant() != null) {
                dmods = DModManager.getNumDMods(stats.getVariant());
            }
            if (dmods > DMOD_MAX) dmods = DMOD_MAX;


            if (dmods > 0) {
                stats.getFleetMember().getStats().getDynamic().getStat("sc_auto_points_mult").modifyMult(ID, 1f - dmods * DMOD_SCALE);
            } else {
                stats.getFleetMember().getStats().getDynamic().getStat("sc_auto_points_mult").unmodify(ID);
            }
        }

    }

    @Override
    public void onDeactivation(SCData data) {
        for (FleetMemberAPI member : data.getFleet().getFleetData().getMembersListCopy()) {
            member.getStats().getDynamic().getStat("sc_auto_points_mult").unmodify(ID);
        }
    }

}
