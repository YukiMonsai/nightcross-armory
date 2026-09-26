package data.scripts.campaign.rulecmd.nca;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SalvageDefenderModificationPlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

public class NA_RelicDefenderPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {

    public float getStrength(SalvageGenFromSeed.SDMParams p, float strength, Random random, boolean withOverride) {
        // doesn't matter, just something non-zero so we end up with a fleet
        // the auto-generated fleet will get replaced by this anyway
        return strength + 100;
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
        Global.getSector().getMemoryWithoutUpdate().set("$na_defeatedAutoUnitFleet", true);
    }

    public void modifyFleet(SalvageGenFromSeed.SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {

        Misc.addDefeatTrigger(fleet, "DefeatedAutonomousUnits");

        fleet.setNoFactionInName(true);
        fleet.setName("Autonomous Defense Force #" + (int) Math.ceil(Math.random() * 9999));

        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);

        fleet.getFleetData().clear();
        fleet.getFleetData().setShipNameRandom(random);

        FleetMemberAPI member = fleet.getFleetData().addFleetMember("na_losulci_auto");
        member.setShipName("Halimede");
        PersonAPI person = plugin.createPerson(Commodities.ALPHA_CORE, fleet.getFaction().getId(), random);
        person.getStats().setSkipRefresh(true);
        person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        person.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        person.getStats().setSkipRefresh(false);

        member.setCaptain(person);
        ShipVariantAPI v = member.getVariant().clone();
        v.setSource(VariantSource.REFIT);
        v.addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
        v.addTag(Tags.UNRECOVERABLE);
        v.addPermaMod("na_fulldive");
        v.addPermaMod(HullMods.AUTOMATED);
        member.setVariant(v, false, true);
        fleet.setCommander(person);

        for (int i = 0; i < (Global.getSector().getMemoryWithoutUpdate().contains("$na_defeatedAutoUnitFleet") ? 14 : 7); i++) {
            addAutomated(fleet, "na_autonomousunit_auto", null, Commodities.ALPHA_CORE, random);
        }
        for (int i = 0; i < (Global.getSector().getMemoryWithoutUpdate().contains("$na_defeatedAutoUnitFleet") ? 13 : 6); i++) {
            addAutomated(fleet, "na_autonomousunit_auto2", null, Commodities.BETA_CORE, random);
        }

        fleet.getFleetData().sort();

        for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
            curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
        }

        for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
            v = curr.getVariant().clone();
            v.setSource(VariantSource.REFIT);
            curr.setVariant(v, false, false);
        }

        if (fleet.getInflater() instanceof DefaultFleetInflater) {
            DefaultFleetInflater dfi = (DefaultFleetInflater) fleet.getInflater();
            DefaultFleetInflaterParams dfip = (DefaultFleetInflaterParams)dfi.getParams();
            //dfip.allWeapons = true;
            dfip.averageSMods = 3;
            dfip.quality = 0.9f;

            // what a HACK
            DModManager.assumeAllShipsAreAutomated = true;
            fleet.inflateIfNeeded();
            fleet.setInflater(null);
            DModManager.assumeAllShipsAreAutomated = false;
        }

        for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
            curr.getVariant().addPermaMod(HullMods.AUTOMATED);
            curr.getVariant().setVariantDisplayName("");
            curr.getVariant().addTag(Tags.UNRECOVERABLE);
        }
    }

    public static void addAutomated(CampaignFleetAPI fleet, String variantId, String shipName, String aiCore, Random random) {
        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);

        FleetMemberAPI member = fleet.getFleetData().addFleetMember(variantId);
        member.setId("xivtf_" + random.nextLong());

        //System.out.println("ID for " + variantId + ": " + member.getId());

        //member.setId("xivtf_" + random.nextLong());
        if (shipName != null) {
            member.setShipName(shipName);
        }
        if (aiCore != null) {
            PersonAPI person = plugin.createPerson(aiCore, fleet.getFaction().getId(), random);
            member.setCaptain(person);
        }
    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SalvageGenFromSeed.SDMParams)) return 0;
        SalvageGenFromSeed.SDMParams p = (SalvageGenFromSeed.SDMParams) params;

        if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(
                "$na_relic_sop")) {
            return 2;
        }
        if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(
                "$na_relic_mare")) {
            return 2;
        }
        return -1;
    }
    public float getQuality(SalvageGenFromSeed.SDMParams p, float quality, Random random, boolean withOverride) {
        return quality;
    }
}



