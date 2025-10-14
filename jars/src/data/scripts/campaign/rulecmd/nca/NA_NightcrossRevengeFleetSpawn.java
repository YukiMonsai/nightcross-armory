package data.scripts.campaign.rulecmd.nca;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class NA_NightcrossRevengeFleetSpawn extends BaseCommandPlugin {


    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        SectorEntityToken entity = dialog.getInteractionTarget();
        if (entity != null && entity.getMemoryWithoutUpdate().getBoolean("$entity.na_didRevengeSpawn")) return false;


        String faction = NightcrossID.NIGHTCROSS_ARMORY;
        String hail = "NCASECRETREVENGEHAIL";
        DelayedFleetEncounter e = new DelayedFleetEncounter(new Random(), "na_revengefleet");
        e.setDelay(5f);
        e.setLocationCoreOnly(true, faction);
        e.setEncounterInHyper();
        e.beginCreate();
        HubMissionWithTriggers.FleetSize size = HubMissionWithTriggers.FleetSize.MEDIUM;
        String type = FleetTypes.MERC_BOUNTY_HUNTER;
        HubMissionWithTriggers.FleetQuality q = HubMissionWithTriggers.FleetQuality.SMOD_1;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.contains("$na_secretrevengecount")) {
            // size
            if (mem.getFloat("$na_secretrevengecount") >= 2.9f) {
                size = HubMissionWithTriggers.FleetSize.VERY_LARGE;
            } else if (mem.getFloat("$na_secretrevengecount") >= 1.9f) {
                size = HubMissionWithTriggers.FleetSize.LARGER;
            } else if (mem.getFloat("$na_secretrevengecount") >= .9f) {
                size = HubMissionWithTriggers.FleetSize.LARGE;
            }


            // type
            if (mem.getFloat("$na_secretrevengecount") >= 5.9f) {
                type = FleetTypes.MERC_ARMADA;
            } else if (mem.getFloat("$na_secretrevengecount") >= 3.9f) {
                type = FleetTypes.PATROL_LARGE;
            } else if (mem.getFloat("$na_secretrevengecount") >= 1.9f) {
                type = FleetTypes.MERC_PATROL;
            }

            // quality
            if (mem.getFloat("$na_secretrevengecount") >= 2.9f) {
                q = HubMissionWithTriggers.FleetQuality.SMOD_3;
            } else if (mem.getFloat("$na_secretrevengecount") >= 0.9f) {
                q = HubMissionWithTriggers.FleetQuality.SMOD_2;
            }
        }
        e.triggerCreateFleet(size, q, faction, type, new Vector2f());
        e.triggerSetAdjustStrengthBasedOnQuality(true, 2.0f);
        e.triggerSetStandardAggroPirateFlags();
        e.triggerSetStandardAggroInterceptFlags();
        if (!mem.contains("$na_secretrevengecount")) {
            mem.set("$na_secretrevengecount", 1f);
        } else {
            mem.set("$na_secretrevengecount", mem.getFloat("$na_secretrevengecount") + 1f);
        }
        e.triggerSetFleetGenericHailPermanent(hail);
        e.endCreate();


        return true;
    }
}
