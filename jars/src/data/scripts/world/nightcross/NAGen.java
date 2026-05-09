package data.scripts.world.nightcross;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CircularOrbit;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NACampaignPlugin;
import data.scripts.world.nightcross.pascal.Pascal;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NAGen implements SectorGeneratorPlugin {

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
        FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
        FactionAPI pirates = sector.getFaction(Factions.PIRATES);
        FactionAPI kol = sector.getFaction(Factions.KOL);
        FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
        FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
        FactionAPI nightcross = sector.getFaction(NightcrossID.NIGHTCROSS_ARMORY);
        FactionAPI stargazer = sector.getFaction(NightcrossID.FACTION_STARGAZER);

        nightcross.setRelationship(path.getId(), RepLevel.SUSPICIOUS);
        nightcross.setRelationship(hegemony.getId(), RepLevel.VENGEFUL);
        nightcross.setRelationship(pirates.getId(), RepLevel.HOSTILE);
        nightcross.setRelationship(tritachyon.getId(), RepLevel.SUSPICIOUS);
        nightcross.setRelationship(church.getId(), RepLevel.SUSPICIOUS);
        nightcross.setRelationship(kol.getId(), RepLevel.SUSPICIOUS);

        stargazer.setRelationship(kol.getId(), RepLevel.VENGEFUL);
        stargazer.setRelationship(church.getId(), RepLevel.VENGEFUL);
        stargazer.setRelationship(sector.getFaction(Factions.SCAVENGERS).getId(), RepLevel.HOSTILE);
        stargazer.setRelationship(sector.getFaction(Factions.PIRATES).getId(), RepLevel.HOSTILE);
        stargazer.setRelationship(sector.getFaction(Factions.INDEPENDENT).getId(), RepLevel.SUSPICIOUS);
        stargazer.setRelationship(sector.getFaction(Factions.PLAYER).getId(), RepLevel.SUSPICIOUS);
        stargazer.setRelationship(sector.getFaction(Factions.REMNANTS).getId(), RepLevel.SUSPICIOUS);
        stargazer.setRelationship(sector.getFaction(Factions.THREAT).getId(), RepLevel.HOSTILE);
    }

    boolean NightcrossGenerated = false;
    boolean MareGenerated = false;
    boolean SopGenerated = false;
    public static float WR_NEBULA_DIST = 8000f; // same constellation only
    public static float WR_BASE_DENSITY = 0.8f;

    @Override
    public void generate(SectorAPI sector) {
        //SharedData.getData().getPersonBountyEventData().addParticipatingFaction("nightcross");

        generate_nightcross(sector);
    }


    public void genplanetarynebulas(SectorAPI sector) {
        //SharedData.getData().getPersonBountyEventData().addParticipatingFaction("nightcross");

        // Go thru all procgen wolf-rayets and make them have a nebula

        List<StarSystemAPI> locations = sector.getStarSystems();
        List<StarSystemAPI> pn_stars = new ArrayList<>();


        for (StarSystemAPI loc : locations) {
            if (((StarSystemAPI) loc).getStar() != null) {
                if (((StarSystemAPI) loc).getStar().getSpec() != null
                        && ((StarSystemAPI) loc).getStar().getSpec().getDescriptionId() != null
                        && ((StarSystemAPI) loc).getStar().getSpec().getDescriptionId().equals("star_white")) {
                    if (((StarSystemAPI) loc).isProcgen() && !loc.isDeepSpace()) {
                        pn_stars.add((StarSystemAPI) loc);
                    }
                } else if (((StarSystemAPI) loc).getSecondary() != null
                        && ((StarSystemAPI) loc).getSecondary().getSpec() != null
                        && ((StarSystemAPI) loc).getSecondary().getSpec().getDescriptionId() != null
                        && ((StarSystemAPI) loc).getSecondary().getSpec().getDescriptionId().equals("star_white")) {
                    if (((StarSystemAPI) loc).isProcgen() && !loc.isDeepSpace()) {
                        pn_stars.add((StarSystemAPI) loc);
                    }
                } else if (((StarSystemAPI) loc).getTertiary() != null
                        && ((StarSystemAPI) loc).getTertiary().getSpec() != null
                        && ((StarSystemAPI) loc).getTertiary().getSpec().getDescriptionId() != null
                        && ((StarSystemAPI) loc).getTertiary().getSpec().getDescriptionId().equals("star_white")) {
                    if (((StarSystemAPI) loc).isProcgen() && !loc.isDeepSpace()) {
                        pn_stars.add((StarSystemAPI) loc);
                    }
                }
            }
        }

        for (StarSystemAPI sys : pn_stars) {
            // 10% of white dwarfs get a planetary nebula

            float basedensity = WR_BASE_DENSITY;
            if (!sys.hasSystemwideNebula() && random.nextFloat() < 0.1) {
                addSystemwideNebula(sys, basedensity, NEBULA_BLUE);
                sys.setHasSystemwideNebula(true);

                SectorEntityToken pascal_nebula = Misc.addNebulaFromPNG("data/campaign/terrain/nightcross_nebula.png",
                        0, 0, // center of nebula
                        sys, // location to add to
                        "terrain", NEBULA_BLUE,
                        4, 4, StarAge.YOUNG); // number of cells in texture

            }

        }
    }
    public void fixwolfrayet(SectorAPI sector) {
        //SharedData.getData().getPersonBountyEventData().addParticipatingFaction("nightcross");

        // Go thru all procgen wolf-rayets and make them have a nebula

        List<StarSystemAPI> locations = sector.getStarSystems();
        List<StarSystemAPI> wr_stars = new ArrayList<>();



        for (StarSystemAPI loc : locations) {
            if (((StarSystemAPI) loc).getStar() != null) {
                if (((StarSystemAPI) loc).getStar().getSpec() != null
                        && ((StarSystemAPI) loc).getStar().getSpec().getDescriptionId() != null
                        && ((StarSystemAPI) loc).getStar().getSpec().getDescriptionId().equals("star_rayet")) {
                    if (((StarSystemAPI) loc).isProcgen() && !loc.isDeepSpace()) {
                        wr_stars.add((StarSystemAPI) loc);
                    }
                } else if (((StarSystemAPI) loc).getSecondary() != null
                        && ((StarSystemAPI) loc).getSecondary().getSpec() != null
                        && ((StarSystemAPI) loc).getSecondary().getSpec().getDescriptionId() != null
                        && ((StarSystemAPI) loc).getSecondary().getSpec().getDescriptionId().equals("star_rayet")) {
                    if (((StarSystemAPI) loc).isProcgen() && !loc.isDeepSpace()) {
                        wr_stars.add((StarSystemAPI) loc);
                    }
                } else if (((StarSystemAPI) loc).getTertiary() != null
                        && ((StarSystemAPI) loc).getTertiary().getSpec() != null
                        && ((StarSystemAPI) loc).getTertiary().getSpec().getDescriptionId() != null
                        && ((StarSystemAPI) loc).getTertiary().getSpec().getDescriptionId().equals("star_rayet")) {
                    if (((StarSystemAPI) loc).isProcgen() && !loc.isDeepSpace()) {
                        wr_stars.add((StarSystemAPI) loc);
                    }
                }
            }
        }

        for (StarSystemAPI sys : wr_stars) {
            float basedensity = WR_BASE_DENSITY;
            addSystemwideNebula(sys, basedensity, NEBULA_WOLF);
            sys.setHasSystemwideNebula(true);

            SectorEntityToken pascal_nebula = Misc.addNebulaFromPNG("data/campaign/terrain/nightcross_nebula.png",
                    0, 0, // center of nebula
                    sys, // location to add to
                    "terrain", "nebula_wolf_rayet",
                    4, 4, StarAge.YOUNG); // number of cells in texture


            var constellation = sys.getConstellation();
            if (constellation != null) {
                for (StarSystemAPI sys2 : constellation.getSystems()) {
                    var dist = Misc.getDistance(sys.getLocation(), sys2.getLocation());
                    var density = WR_BASE_DENSITY * dist/WR_NEBULA_DIST;
                    if (density > 0.1f && !sys2.hasSystemwideNebula()) {
                        addSystemwideNebula(sys2, density, NEBULA_WOLF);
                        sys2.setHasSystemwideNebula(true);
                    }
                }
            }

        }
    }
    public static Random random = new Random();
    public static final String NEBULA_WOLF = "nebula_wolf_rayet";
    public static final String NEBULA_BLUE = "nebula_blue";

    protected void addSystemwideNebula(StarSystemAPI system, float density, String nebulaType) {


        int w = 128;
        int h = 128;

        StringBuilder string = new StringBuilder();
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                string.append("x");
            }
        }
        var toRemove = new ArrayList<SectorEntityToken>();
        for (SectorEntityToken token : system.getAllEntities()) {
            if (token instanceof CampaignTerrainAPI && ((CampaignTerrainAPI) token).getType() != null && ((CampaignTerrainAPI) token).getType().equals(Terrain.NEBULA)) {
                if (token.getLocation().x == 0 && token.getLocation().y == 0) {
                    toRemove.add(token);
                }
            }
        }
        for (SectorEntityToken token : toRemove) {
            system.removeEntity(token);
        }
        SectorEntityToken nebula = system.addTerrain(Terrain.NEBULA, new BaseTiledTerrain.TileParams(string.toString(),
                w, h,
                "terrain", nebulaType, 4, 4, null));
        nebula.getLocation().set(0, 0);

        NebulaTerrainPlugin nebulaPlugin = (NebulaTerrainPlugin)((CampaignTerrainAPI)nebula).getPlugin();
        NebulaEditor editor = new NebulaEditor(nebulaPlugin);

        editor.regenNoise();

        // good medium thickness: 0.6
        //editor.noisePrune(0.8f);

        // yes, star age here, despite using constellation age to determine if a nebula to all exists
        // basically: young star in old constellation will have lots of nebula, but of the constellation-age color
        editor.noisePrune(density);
        //editor.noisePrune(0.75f);
        //editor.noisePrune(0.1f);

//		for (float f = 0.1f; f <= 0.9f; f += 0.05f) {
//			editor.noisePrune(f);
//		}

        editor.regenNoise();

        for (PlanetAPI planet : system.getPlanets()) {

            if (planet.getOrbit() != null && planet.getOrbit().getFocus() != null &&
                    planet.getOrbit().getFocus().getOrbit() != null) {
                // this planet is orbiting something that's orbiting something
                // its motion will be relative to its parent moving
                // don't clear anything out for this planet
                continue;
            }

            float clearThreshold = 0f; // clear everything by default
            float clearInnerRadius = 0f;
            float clearOuterRadius = 0f;
            Vector2f clearLoc = null;


            if (!planet.isStar() && !planet.isGasGiant()) {
                clearThreshold = 1f - Math.min(0f, planet.getRadius() / 300f);
                if (clearThreshold > 0.5f) clearThreshold = 0.5f;
            }

            Vector2f loc = planet.getLocation();
            if (planet.getOrbit() != null && planet.getOrbit().getFocus() != null) {
                Vector2f focusLoc = planet.getOrbit().getFocus().getLocation();
                float dist = Misc.getDistance(planet.getOrbit().getFocus().getLocation(), loc);
                float width = planet.getRadius() * 2f + 100f;
                if (planet.isStar()) {
                    StarCoronaTerrainPlugin corona = Misc.getCoronaFor(planet);
                    if (corona != null) {
                        width = corona.getParams().bandWidthInEngine * 1f;
                    }
                    PulsarBeamTerrainPlugin pulsar = Misc.getPulsarFor(planet);
                    if (pulsar != null) {
                        width = Math.max(width, pulsar.getParams().bandWidthInEngine * 0.5f);
                    }
                }
                clearLoc = focusLoc;
                clearInnerRadius = dist - width / 2f;
                clearOuterRadius = dist + width / 2f;
            } else if (planet.getOrbit() == null) {
                float width = planet.getRadius() * 1f + 100f;
                if (planet.isStar()) {
                    StarCoronaTerrainPlugin corona = Misc.getCoronaFor(planet);
                    if (corona != null) {
                        width = corona.getParams().bandWidthInEngine * 0.75f;
                    }
                    PulsarBeamTerrainPlugin pulsar = Misc.getPulsarFor(planet);
                    if (pulsar != null) {
                        width = Math.max(width, pulsar.getParams().bandWidthInEngine * 0.5f);
                    }
                }
                clearLoc = loc;
                clearInnerRadius = 0f;
                clearOuterRadius = width;
            }

            if (clearLoc != null) {
                float min = nebulaPlugin.getTileSize() * 2f;
                if (clearOuterRadius - clearInnerRadius < min) {
                    clearOuterRadius = clearInnerRadius + min;
                }
                editor.clearArc(clearLoc.x, clearLoc.y, clearInnerRadius, clearOuterRadius, 0, 360f, clearThreshold);
            }
        }

        // add a spiral going from the outside towards the star
        float angleOffset = random.nextFloat() * 360f;
        editor.clearArc(0f, 0f, 30000, 31000 + 1000f * random.nextFloat(),
                angleOffset + 0f, angleOffset + 360f * (2f + random.nextFloat() * 2f), 0.01f, 0f);

        // do some random arcs
        int numArcs = (int) (8f + 6f * random.nextFloat());
        //int numArcs = 11;

        for (int i = 0; i < numArcs; i++) {
            //float dist = 4000f + 10000f * random.nextFloat();
            float dist = 15000f + 15000f * random.nextFloat();
            float angle = random.nextFloat() * 360f;

            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
            dir.scale(dist - (2000f + 8000f * random.nextFloat()));

            //float tileSize = nebulaPlugin.getTileSize();
            //float width = tileSize * (2f + 4f * random.nextFloat());
            float width = density * 3200f * (1f + 2f * random.nextFloat());

            float clearThreshold = 0f + 0.5f * random.nextFloat();
            //clearThreshold = 0f;

            editor.clearArc(dir.x, dir.y, dist - width/2f, dist + width/2f, 0, 360f, clearThreshold);
        }
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

        int index = MathUtils.getRandomNumberInRange(0, blackholes.size() - 1);
        if (blackholes.size() > 0) {
            StarSystemAPI system = blackholes.get(index);

            // place the mare crisium wreck
            PlanetAPI star = system.getStar();

            float distance = star.getRadius() + 50f; // right on event horizon
            SectorEntityToken ship = addDerelict(system, "na_mare_proto_relic", ShipRecoverySpecial.ShipCondition.AVERAGE, true, null);
            ship.setCircularOrbit(star, MathUtils.getRandomNumberInRange(0f, 360f), distance, distance/10f);
            ship.setId("na_mare_x_wreck");
        }

        MareGenerated = true;
    }


    public void place_sop(SectorAPI sector) {
        List<LocationAPI> locations = sector.getAllLocations();
        List<StarSystemAPI> neutronstar = new ArrayList<>();
        for (LocationAPI loc : locations) {
            if (loc instanceof StarSystemAPI && ((StarSystemAPI) loc).getStar() != null) {
                if (((StarSystemAPI) loc).getStar().getSpec() != null && ((StarSystemAPI) loc).getStar().getSpec().isPulsar()) {
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

                        // only procgen pulsars
                        if (!filtered)
                            neutronstar.add((StarSystemAPI) loc);
                    }
                }
            }
        }

        int index = MathUtils.getRandomNumberInRange(0, neutronstar.size() - 1);
        if (neutronstar.size() > 0) {
            StarSystemAPI system = neutronstar.get(index);

            // place the string of pearls wreck
            PlanetAPI star = system.getStar();

            float distance = star.getRadius() + 100f; // close
            SectorEntityToken ship = addDerelict(system, "na_sop_proto_relic", ShipRecoverySpecial.ShipCondition.AVERAGE, true, null);
            ship.setCircularOrbit(star, MathUtils.getRandomNumberInRange(0f, 360f), distance, distance/10f);
            ship.setId("na_sop_x_wreck");
        }

        SopGenerated = true;
    }


    public void generate_nightcross(SectorAPI sector) {

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
