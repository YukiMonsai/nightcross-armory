package data.scripts.world.nightcross;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.world.nightcross.pascal.Pascal;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.scripts.world.nightcross.NA_StargazerWandererManager.createStargazerFleet;

public class NA_StargazerGen implements SectorGeneratorPlugin {
    // generates happy friendly ai fleets to greet the player on their adventures
    public static void initFactionRelationships(SectorAPI sector) {
    }

    boolean StargazerGenerated = false;

    @Override
    public void generate(SectorAPI sector) {

        generate_stargazer(sector);
    }
    public void init(SectorAPI sector) {
        // nothing for now, they all generate in procgen systems
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

    // max amount - is less than minimum amount. set to 0 to disable respawn
    public static final int MAX_STARGAZER_WANDERERS = 0;

    // number to generate at start
    public static final int INIT_STARGAZER_WANDERERS = 16;

    // frequency with which they spawn until max number, in cycles, one at a time
    public static final float PERIOD_STARGAZER_WANDERERS = 30;

    public void generate_stargazer(SectorAPI sector) {
        initFactionRelationships(sector);
        createWanderers(sector);

        StargazerGenerated = true;
    }
    public void createWanderers(SectorAPI sector) {
        List<StarSystemAPI> availableSystems = getStargazerOrigins(sector);
        for (int i = 0; i < 10*INIT_STARGAZER_WANDERERS; i++) {

            int index = MathUtils.getRandomNumberInRange(0, availableSystems.size() - 1);
            if (availableSystems.size() > 0) {
                StarSystemAPI system = availableSystems.get(index);
                if (place_stargazer(sector, system)) i += 9; // soft retry
            }
        }
    }


    public List<StarSystemAPI> getStargazerOrigins(SectorAPI sector) {
        List<LocationAPI> locations = sector.getAllLocations();
        List<StarSystemAPI> blackholes = new ArrayList<>();
        for (LocationAPI loc : locations) {
            if (loc instanceof StarSystemAPI && ((StarSystemAPI) loc).getStar() != null) {
                if (((StarSystemAPI) loc).getStar().getSpec() != null && ((StarSystemAPI) loc).getStar().getSpec().isBlackHole()) {
                    if (((StarSystemAPI) loc).isProcgen() && !loc.isDeepSpace()) {
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
        return blackholes;
    }

    public boolean place_stargazer(SectorAPI sector, StarSystemAPI system) {

            // get the star
            PlanetAPI star = system.getStar();

            if (star.getSpec() != null) {
                float radius = star.getRadius() + star.getSpec().getCoronaSize() * star.getRadius() + MathUtils.getRandomNumberInRange(300, 500); // they are gazing at it

                NA_StargazerWandererManager.StargazerFleetParams params = new NA_StargazerWandererManager.StargazerFleetParams(
                        null,
                        null, // loc in hyper; don't need if have market
                        NightcrossID.FACTION_STARGAZER,
                        -1.5f, // quality override
                        FleetTypes.PATROL_SMALL,
                        MathUtils.getRandomNumberInRange(40, 220), // combatPts
                        0, // freighterPts
                        0, // tankerPts
                        0f, // transportPts
                        0f, // linerPts
                        0f, // utilityPts
                        0.1f
                );
                params.averageSMods = 2;
                //params.random = random;
                params.random = new Random(); //for easier testing
                params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_ONLY;

                CampaignFleetAPI f = createStargazerFleet(params, null);

                system.addEntity(f);

                //float radius = 100f + star.getRadius() + star.getSpec().getCoronaSize();
                Vector2f loc = Misc.getPointAtRadius(star.getLocation(), radius);
                f.setLocation(loc.x, loc.y);

                f.getMemoryWithoutUpdate().set("$combatMusicSetId","na_stargazer_battle");


                NA_StargazerBehavior behavior = new NA_StargazerBehavior(f, system, star, true, true, false);
                behavior.setSeenByPlayer();
                f.addScript(behavior);

                return true;
            }

            return false;
    }





}
