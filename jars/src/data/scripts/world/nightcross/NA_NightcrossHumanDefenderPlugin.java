package data.scripts.world.nightcross;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;

import java.util.Random;

public class NA_NightcrossHumanDefenderPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {

    public float getStrength(SalvageGenFromSeed.SDMParams p, float strength, Random random, boolean withOverride) {

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.contains("$na_secretrevengecount")) {
            strength += 60 * Math.min(4f, mem.getFloat("$na_secretrevengecount"));
        }
        return strength;
    }
    public float getMinSize(SalvageGenFromSeed.SDMParams p, float minSize, Random random, boolean withOverride) {
        return minSize;
    }
    public float getMaxSize(SalvageGenFromSeed.SDMParams p, float maxSize, Random random, boolean withOverride) {
        return maxSize;
    }
    public float getProbability(SalvageGenFromSeed.SDMParams p, float probability, Random random, boolean withOverride) {
        return probability;
    }
    public void reportDefeated(SalvageGenFromSeed.SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
        // TODO
    }

    public void modifyFleet(SalvageGenFromSeed.SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
        fleet.setName("Defense Fleet");
        NA_StargazerFleets.modifyStargazerFleet(fleet, random);

        for (FleetMemberAPI m : fleet.getMembersWithFightersCopy()) {
            m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        }


    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SalvageGenFromSeed.SDMParams)) return 0;
        SalvageGenFromSeed.SDMParams p = (SalvageGenFromSeed.SDMParams) params;

        if (p.entity!=null && p.entity.getCustomEntitySpec() != null && p.entity.getCustomEntitySpec().getId().startsWith("na_research")) {
            return 2;
        }
        return 0;
    }
    public float getQuality(SalvageGenFromSeed.SDMParams p, float quality, Random random, boolean withOverride) {
        return quality;
    }
}

