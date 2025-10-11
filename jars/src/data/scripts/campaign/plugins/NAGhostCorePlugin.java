package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.ids.NightcrossPeople;
import data.scripts.campaign.skills.NAFulldiveOfficer;
import data.scripts.hullmods.NA_ProjectGhost;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Random;

// Credit Inventor Raccoon (SoTF)
public class NAGhostCorePlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {

    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        PersonAPI person = null;

        switch (aiCoreId) {
            case NightcrossID.TETO_CORE:
                person = NightcrossPeople.getPerson(NightcrossPeople.TETO);
                break;
            case NightcrossID.GHOST_CORE_ID:
                person = NightcrossPeople.createAIPerson(NightcrossPeople.GHOST_CORE, Factions.PLAYER, random);
                break;
            case NightcrossID.GHOST_MATRIX_ID:
                person = NightcrossPeople.createAIPerson(NightcrossPeople.GHOST_MATRIX, Factions.PLAYER, random);
                break;
            default:
                return null;
        }
        // clear admiral skills because they count against the number of skills they can pick
        for (MutableCharacterStatsAPI.SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
            if (skillLevel.getSkill().isAdmiralSkill()) {
                person.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
            }
        }
        // undo the +1 level from integration if the ship was scuttled since we're reusing the same PersonAPI
        if (Misc.isUnremovable(person)) {
            Misc.setUnremovable(person, false);
            person.getStats().setLevel(person.getStats().getLevel() - 1);
            for (MutableCharacterStatsAPI.SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
                if (skillLevel.getSkill().isCombatOfficerSkill() && !skillLevel.getSkill().hasTag(Skills.TAG_NPC_ONLY)) {
                    person.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
                    break;
                }
            }
        }
        return person;
    }



    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        float opad = 10f;

        float autoMult = 1f;
        try {
            if (person.getMemoryWithoutUpdate().getBoolean(NAFulldiveOfficer.TAG_SET_TO_NO_PENALTY)) autoMult = 0f;
            else autoMult = person.getMemoryWithoutUpdate().getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT);
        } catch (Exception e) {
            // beep
        }
        String autoMultString = new DecimalFormat("#.##").format(autoMult);

        tooltip.addPara("Automated ship points multiplier: "
                + autoMultString, opad, Misc.getHighlightColor(),autoMultString + "x");
    }
}
