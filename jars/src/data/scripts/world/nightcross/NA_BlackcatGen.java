package data.scripts.world.nightcross;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.enc.NA_BlackHoleMoteScript;
import data.scripts.campaign.ids.NightcrossID;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.scripts.world.nightcross.NA_StargazerFleets.createStargazerFleet;

public class NA_BlackcatGen implements SectorGeneratorPlugin {
    // generates happy friendly ai fleets to greet the player on their adventures
    public static void initFactionRelationships(SectorAPI sector) {
    }

    public boolean BlackcatGenerated = false;

    @Override
    public void generate(SectorAPI sector) {


        generate_blackcat(sector);
    }
    public void init(SectorAPI sector) {
        
    }


    public static StarSystemAPI blackcatsystem = null;
    public static PlanetAPI lunargravitywell = null;


    public boolean generate_blackcat(SectorAPI sector) {


        StarSystemAPI system = sector.createStarSystem("Omega-41");
        system.setOptionalUniqueId("na_blackcatsystem");
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_INTERESTING);
        float w = Global.getSettings().getFloat("sectorWidth");
        float h = Global.getSettings().getFloat("sectorHeight");
        system.getLocation().set(-w/2f - 12300f, -h/2f + 4100f);


        LocationAPI hyper = Global.getSector().getHyperspace();

        system.setBackgroundTextureFilename("graphics/backgrounds/na_blackbg3.jpg");


        Random random = StarSystemGenerator.random;
        // create the star and generate the hyperspace anchor for this system
        PlanetAPI star = system.initStar("na_blackcatstar", // unique id for this star
                "star_neutron",  // id in planets.json
                100f, 		  // radius (in pixels at default zoom)
                500); // corona radius, from star edge
        setPulsarIfNeutron(system, star, random);;


        PlanetAPI gravitywell = system.addPlanet("na_lunargravitywell", // unique id for this star
                star,
                "Lunar Gravity Well",
                "black_hole",  // id in planets.json
                50f,
                100, 9000, 5000);
        setBlackHoleIfBlackHole(system, gravitywell, random);
        lunargravitywell = gravitywell;
        gravitywell.setCustomDescriptionId("na_lunargravitywell");

        gravitywell.addScript(new NA_BlackHoleMoteScript(gravitywell, 0.15f));


        system.setLightColor(new Color(162, 140, 205)); // light color in entire system, affects all entities


        PlanetAPI planet = system.addPlanet("na_blackcatsystem_planet", star, "Nix", Planets.BARREN_VENUSLIKE, 215, 90, 1700, 75);
        planet.getMemoryWithoutUpdate().set("$na_blackcatsystem_planet", true);
        planet.setCustomDescriptionId("na_blackcatsystem_planet");
        planet.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
        planet.getMarket().addCondition(Conditions.VERY_COLD);
        planet.getMarket().addCondition(Conditions.POOR_LIGHT);
        planet.getMarket().addCondition(Conditions.IRRADIATED);
        planet.getMarket().addCondition(Conditions.RUINS_SCATTERED);

        BaseThemeGenerator.EntityLocation loc = new BaseThemeGenerator.EntityLocation();
        loc.orbit = Global.getFactory().createCircularOrbit(star, 215,
                1850, 75);
        BaseThemeGenerator.AddedEntity added = BaseThemeGenerator.addEntity(null, system, loc, "naai_blackcatstation", Factions.NEUTRAL);
        //added.entity.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, "na_blackcatstation");
        added.entity.setName("Nightcross Research Station");
        added.entity.getMemoryWithoutUpdate().set("$na_blackcatstation", true);
        added.entity.setCustomDescriptionId("na_blackcatstation");


        int maxFleets = 7;
        StargazerStation activeFleets = new StargazerStation(
                added.entity, 1f, 0, maxFleets, 15f, 8, 24);
        system.addScript(activeFleets);



        //CargoAPI cargo = Global.getFactory().createCargo(true);
        //cargo.addCommodity(Commodities.RARE_METALS, 200f + random.nextInt(101));
        //BaseSalvageSpecial.addExtraSalvage(added.entity, cargo);


        planet = system.addPlanet("na_blackcatsystem_planet2", star, "Charon", Planets.BARREN_CASTIRON, 70, 45, 2300, 135);
        planet.setCustomDescriptionId("na_blackcatsystem_planet");
        planet.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
        planet.getMarket().addCondition(Conditions.VERY_COLD);
        planet.getMarket().addCondition(Conditions.POOR_LIGHT);
        planet.getMarket().addCondition(Conditions.IRRADIATED);
        planet.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);


        StarSystemGenerator.addStableLocations(system, 1);


        system.autogenerateHyperspaceJumpPoints(true, true);

        blackcatsystem = system;
        //BlackcatGenerated = true;
        return BlackcatGenerated;
    }

    protected void setPulsarIfNeutron(StarSystemAPI system, PlanetAPI star, Random random) {
        if (star == null) return;

        if (star.getSpec().getPlanetType().equals("star_neutron")) {
            StarCoronaTerrainPlugin coronaPlugin = Misc.getCoronaFor(star);
            if (coronaPlugin != null) {
                system.removeEntity(coronaPlugin.getEntity());
            }

            system.addCorona(star,
                    300, // radius
                    3, // wind
                    0, // flares
                    3); // cr loss


            StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, star.getSpec().getPlanetType(), false);
            float corona = star.getRadius() * (starData.getCoronaMult() + starData.getCoronaVar() * (random.nextFloat() - 0.5f));
            if (corona < starData.getCoronaMin()) corona = starData.getCoronaMin();

            SectorEntityToken eventHorizon = system.addTerrain(Terrain.PULSAR_BEAM,
                    new StarCoronaTerrainPlugin.CoronaParams(star.getRadius() + corona, (star.getRadius() + corona) / 2f,
                            star, starData.getSolarWind(),
                            (float) (starData.getMinFlare() + (starData.getMaxFlare() - starData.getMinFlare()) * random.nextFloat()),
                            starData.getCrLossMult()));
            eventHorizon.setCircularOrbit(star, 0, 0, 100);
        }
    }



    protected void setBlackHoleIfBlackHole(StarSystemAPI system, PlanetAPI star, Random random) {
        if (star == null) return;

        if (star.getSpec().getPlanetType().equals("black_hole")) {
            StarCoronaTerrainPlugin coronaPlugin = Misc.getCoronaFor(star);
            if (coronaPlugin != null) {
                system.removeEntity(coronaPlugin.getEntity());
            }

            StarGenDataSpec starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, star.getSpec().getPlanetType(), false);
            float corona = star.getRadius() * (starData.getCoronaMult() + starData.getCoronaVar() * (random.nextFloat() - 0.5f));
            if (corona < starData.getCoronaMin()) corona = starData.getCoronaMin();

            SectorEntityToken eventHorizon = system.addTerrain(Terrain.EVENT_HORIZON,
                    new StarCoronaTerrainPlugin.CoronaParams(star.getRadius() + corona, (star.getRadius() + corona) / 2f,
                            star, starData.getSolarWind(),
                            (float) (starData.getMinFlare() + (starData.getMaxFlare() - starData.getMinFlare()) * random.nextFloat()),
                            starData.getCrLossMult()));
            eventHorizon.setCircularOrbit(star, 0, 0, 100);
        }
    }


}
