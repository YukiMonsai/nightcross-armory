package data.scripts.world.nightcross;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NACampaignPlugin;
import data.scripts.world.nightcross.pascal.Pascal;

public class NAGen implements SectorGeneratorPlugin {

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
        FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
        FactionAPI pirates = sector.getFaction(Factions.PIRATES);
        FactionAPI kol = sector.getFaction(Factions.KOL);
        FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
        FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
        FactionAPI nightcross = sector.getFaction(NightcrossID.NIGHTCROSS_ARMORY);

        nightcross.setRelationship(path.getId(), RepLevel.SUSPICIOUS);
        nightcross.setRelationship(hegemony.getId(), RepLevel.VENGEFUL);
        nightcross.setRelationship(pirates.getId(), RepLevel.HOSTILE);
        nightcross.setRelationship(tritachyon.getId(), RepLevel.SUSPICIOUS);
        nightcross.setRelationship(church.getId(), RepLevel.SUSPICIOUS);
        nightcross.setRelationship(kol.getId(), RepLevel.FAVORABLE);
    }

    boolean NightcrossGenerated = false;

    @Override
    public void generate(SectorAPI sector) {
        //SharedData.getData().getPersonBountyEventData().addParticipatingFaction("nightcross");

        generate_nightcross(sector);
    }

    public void generate_nightcross(SectorAPI sector) {
        initFactionRelationships(sector);

        new Pascal().generate(sector);

        NightcrossGenerated = true;
    }
}
