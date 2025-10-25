package yukimonsai.sicstargazer.skills.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.misc.SCSettings;
import second_in_command.skills.automated.SCBaseAutoPointsSkillPlugin;

public class Machina extends SCBaseAutoPointsSkillPlugin {
    public int getProvidedPoints() {
        return (int)(30 * SCSettings.getAutoPointsMult());
    }
    @Override
    public String getAffectsString() {
        return "all automated ships";
    }


    private static final float DUMB_AI_SCALE = 25f;


    public static final String ID = "na_sic_machina";


    public void addTooltip(SCData data, TooltipMakerAPI tooltip) {
        tooltip.addPara("Ships without AI cores cost %s less automated ship points.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(DUMB_AI_SCALE) + "%");
        super.addTooltip(data, tooltip);
    }
    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

        if (Misc.isAutomated(variant)) {
            if (stats.getFleetMember().getCaptain() == null || (stats.getFleetMember().getCaptain().getPortraitSprite().equals("graphics/portraits/portrait_generic_grayscale.png"))) {
                stats.getFleetMember().getStats().getDynamic().getStat("sc_auto_points_mult").modifyMult(ID, 1f - 0.01f * DUMB_AI_SCALE);
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
