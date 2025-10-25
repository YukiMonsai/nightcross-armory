package yukimonsai.sicnightcross;


import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.specs.SCAptitudeSection;
import second_in_command.specs.SCBaseAptitudePlugin;

import java.util.Objects;

public class AptitudeNightcross extends SCBaseAptitudePlugin {
    @Override
    public String getOriginSkillId() {
        return "nightcross_science";
    }

    @Override
    public void addCodexDescription(TooltipMakerAPI tooltip) {
        tooltip.addPara("This aptitude focuses on fast, long-ranged ships embodying a precursor to the Cruiser School of Domain naval doctrine." +
                        "Notably, the doctrine makes heavy use of shields and dynamic defense to protect its otherwise vulnerable assets," +
                        "while concentrating firepower where it hurts the enemy the most.",
                0f, Misc.getTextColor(), Misc.getHighlightColor());

    }

    @Override
    public void createSections() {
        SCAptitudeSection section1 = new SCAptitudeSection(true, 0, "technology1");
        section1.addSkill("nightcross_ecccm");
        section1.addSkill("nightcross_cr");
        section1.addSkill("nightcross_reactiveplating");
        section1.addSkill("nightcross_shield");
        addSection(section1);

        SCAptitudeSection section2 = new SCAptitudeSection(true, 1, "technology3");
        section2.addSkill("nightcross_weapons");
        section2.addSkill("nightcross_strike");
        section2.addSkill("nightcross_flux");
        addSection(section2);

        SCAptitudeSection section3 = new SCAptitudeSection(true, 3, "technology4");
        section3.addSkill("nightcross_machina");
        section3.addSkill("nightcross_heavysupport");
        section3.addSkill("nightcross_encore");
        addSection(section3);

    }

    @Override
    public Float getNPCFleetSpawnWeight(SCData scData, CampaignFleetAPI campaignFleetAPI) {
        if(Objects.equals(campaignFleetAPI.getFaction().getId(), "nightcross")) return Float.MAX_VALUE;
        if(Objects.equals(campaignFleetAPI.getFaction().getId(), "stargazer")) return 0.5f;
        return 0.25f;
    }
}
