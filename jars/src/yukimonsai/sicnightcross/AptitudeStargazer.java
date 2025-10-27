package yukimonsai.sicnightcross;


import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.specs.SCAptitudeSection;
import second_in_command.specs.SCBaseAptitudePlugin;

import java.util.List;
import java.util.Objects;

public class AptitudeStargazer extends SCBaseAptitudePlugin {
    @Override
    public String getOriginSkillId() {
        return "stargazer_machina";
    }

    @Override
    public Boolean guaranteePick(CampaignFleetAPI fleet) {
        return Objects.equals(fleet.getFaction().getId(), "stargazer");
    }
    @Override
    public void addCodexDescription(TooltipMakerAPI tooltip) {
        tooltip.addPara("This aptitude specializes in Automated Ships that favor energy weapons and mobility." +
                "Stargazer vessels are Automated Ships that often have many D-mods, and gain benefits from having an increased number of D-mods.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "Automated ships", "");

    }

    @Override
    public void createSections() {
        SCAptitudeSection section1 = new SCAptitudeSection(true, 0, "technology1");
        section1.addSkill("stargazer_logistics");
        section1.addSkill("stargazer_nebula");
        section1.addSkill("stargazer_atomize");
        section1.addSkill("stargazer_ecm");
        section1.addSkill("stargazer_shell");
        addSection(section1);

        SCAptitudeSection section2 = new SCAptitudeSection(true, 2, "technology3");
        section2.addSkill("stargazer_overwhelm");
        section2.addSkill("stargazer_furor");
        section2.addSkill("stargazer_necromancy");
        addSection(section2);

        SCAptitudeSection section3 = new SCAptitudeSection(false, 3, "technology4");
        section3.addSkill("stargazer_nightmare");
        section3.addSkill("stargazer_override");
        addSection(section3);

    }

    @Override
    public Float getNPCFleetSpawnWeight(SCData scData, CampaignFleetAPI campaignFleetAPI) {
        if(Objects.equals(campaignFleetAPI.getFaction().getId(), "stargazer")) return Float.MAX_VALUE;
        return 0f;
    }
}
