package data.scripts.campaign.rulecmd.nca;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NAGhostCorePlugin;
import data.scripts.campaign.plugins.NAModPlugin;
import data.scripts.stardust.NA_StargazerHull;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class NA_NightcrossWIPStargazerXO extends BaseCommandPlugin {


    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (NAModPlugin.hasSiC) {
            Global.getSector().getMemoryWithoutUpdate().set("$naai_gotstargazerxo", true);
            Global.getSector().getMemoryWithoutUpdate().set("$naai_gotstargazerplaceholder", true);
            AICoreOfficerPlugin plugin = new NAGhostCorePlugin();
            //PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), 20, true, SkillPickPreference.NON_CARRIER, random);
            PersonAPI person = plugin.createPerson(NightcrossID.GHOST_CORE_ID, NightcrossID.FACTION_STARGAZER, new Random());
            SCOfficer officer = new SCOfficer(person, "sc_stargazer");
            officer.increaseLevel(2);

            SCData data = SCUtils.getPlayerData();
            data.addOfficerToFleet(officer);
            data.setOfficerInEmptySlotIfAvailable(officer);
            Global.getSector().getMemoryWithoutUpdate().set("$naai_stargazerxo", officer);

            var text = dialog.getTextPanel();
            text.setFontSmallInsignia();
            text.addParagraph( "Gained a \"Stargazer\" executive officer", Misc.getPositiveHighlightColor());
            text.highlightInLastPara(NA_StargazerHull.STARGAZER_RED, "\"Stargazer\"");
            text.setFontInsignia();

            SCUtils.showSkillOverview(dialog, officer);
        } else {
            Global.getSector().getMemoryWithoutUpdate().set("$naai_gotstargazerplaceholder", true, 10);
        }
        return true;
    }


}
