package data.scripts.world.nightcross;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CircularOrbit;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NACampaignPlugin;
import data.scripts.world.nightcross.pascal.Pascal;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

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
    boolean MareGenerated = false;

    @Override
    public void generate(SectorAPI sector) {
        //SharedData.getData().getPersonBountyEventData().addParticipatingFaction("nightcross");

        generate_nightcross(sector);
    }



    // from Tahlan Shipworks
    public static final List<String> BLACKLISTED_SYSTEMS = new ArrayList<>();

    static {
        BLACKLISTED_SYSTEMS.add("spookysecretsystem_omega");
    }

    // from Tahlan Shipworks
    public static final List<String> BLACKLISTED_SYSTEM_TAGS = new ArrayList<>();

    static {
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_main");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_secondary");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_no_fleets");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_destroyed");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_suppressed");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_resurgent");
        BLACKLISTED_SYSTEM_TAGS.add("theme_magellan_homeworld");
        BLACKLISTED_SYSTEM_TAGS.add("theme_magellan_graveyard");
        BLACKLISTED_SYSTEM_TAGS.add("theme_magellan_leveller");
        BLACKLISTED_SYSTEM_TAGS.add("theme_magellan_leveller_home_nebula");
    }


    public void place_mare(SectorAPI sector) {
        List<LocationAPI> locations = sector.getAllLocations();
        List<StarSystemAPI> blackholes = new ArrayList<>();
        for (LocationAPI loc : locations) {
            if (loc instanceof StarSystemAPI && ((StarSystemAPI) loc).getStar() != null) {
                if (((StarSystemAPI) loc).getStar().isBlackHole()) {
                    if (((StarSystemAPI) loc).isProcgen()) {
                        boolean filtered = false;
                        for (String tag : BLACKLISTED_SYSTEM_TAGS) {
                            if (loc.getTags().contains(tag)) {
                                filtered = true;
                                break;
                            }
                        }
                        for (String name : BLACKLISTED_SYSTEMS) {
                            if (loc.getName().equals(name)) {
                                filtered = true;
                                break;
                            }
                        }

                        // only procgen blackholes
                        if (!filtered)
                        blackholes.add((StarSystemAPI) loc);
                    }
                }
            }
        }

        int index = MathUtils.getRandomNumberInRange(0, blackholes.size() - 1);
        if (blackholes.size() > 0) {
            StarSystemAPI system = blackholes.get(index);

            // place the mare crisium wreck
            PlanetAPI star = system.getStar();

            float distance = star.getRadius() + 50f; // right on event horizon
            SectorEntityToken ship = addDerelict(system, "na_mare_proto_relic", ShipRecoverySpecial.ShipCondition.AVERAGE, true, null);
            ship.setCircularOrbit(star, MathUtils.getRandomNumberInRange(0f, 360f), distance, distance/10f);
        }

        MareGenerated = true;
    }


    public void generate_nightcross(SectorAPI sector) {
        initFactionRelationships(sector);

        new Pascal().generate(sector);

        NightcrossGenerated = true;
    }



    // code based on Tahlan shipworks
    private static SectorEntityToken addDerelict(StarSystemAPI system, String variantId,
                                                 ShipRecoverySpecial.ShipCondition condition, boolean recoverable,
                                                 @Nullable DefenderDataOverride defenders) {

        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(
                new ShipRecoverySpecial.PerShipData(variantId, condition), false);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(true);

        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(
                    null, 0, 0, false, null, null);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        }
        if (defenders != null) {
            Misc.setDefenderOverride(ship, defenders);
        }
        return ship;
    }
}
