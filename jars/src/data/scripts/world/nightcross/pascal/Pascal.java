package data.scripts.world.nightcross.pascal;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin.AsteroidFieldParams;

import com.fs.starfarer.api.util.Misc;
import data.scripts.NAModPlugin;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.world.nightcross.addMarketplace;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

public class Pascal {



    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("Pascal");
        system.getLocation().set(-20000, 17600);
        system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, NightcrossID.NIGHTCROSS_PASCAL_BG);
        system.setBackgroundTextureFilename("graphics/Backgrounds/background5.jpg");

        PlanetAPI pascal = system.initStar("pascal", "star_rayet", 700f, 1000f);
        pascal.setCustomDescriptionId("wolf_rayet");

        SectorEntityToken pascal_nebula = Misc.addNebulaFromPNG("data/campaign/terrain/nightcross_nebula.png",
                                                                0, 0, // center of nebula
                                                                system, // location to add to
                                                                "terrain", "nebula_blue",
                                                                4, 4, StarAge.YOUNG); // number of cells in texture

        SectorEntityToken field = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldParams(500f, // terrain effect band width
                        2000, // terrain effect middle radius
                        pascal, // entity that it's around
                        1750f, // visual band start
                        2250f, // visual band end
                        new Color(50, 20, 100, 40), // base color
                        1f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
                        new Color(50, 20, 110, 130),
                        new Color(150, 30, 120, 150),
                        new Color(200, 50, 130, 190),
                        new Color(250, 70, 150, 240),
                        new Color(200, 80, 130, 255),
                        new Color(75, 0, 160),
                        new Color(127, 0, 255)
                ));
        field.setCircularOrbit(pascal, 0, 0, 150);

        /* ----------SCENERY------------- */



        PlanetAPI watergiant = system.addPlanet("na_watergiant", pascal, "Hurricane", "na_watergiant", 60, 300, 4000, 37697); //
        watergiant.getSpec().setGlowColor(new Color(54, 119, 84, 84));
        watergiant.getSpec().setUseReverseLightForGlow(true);
        watergiant.applySpecChanges();
        watergiant.setCustomDescriptionId("na_watergiant_desc");

        SectorEntityToken field2 = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldParams(100, // terrain effect band width
                        400, // terrain effect middle radius
                        pascal, // entity that it's around
                        350f, // visual band start
                        450f, // visual band end
                        new Color(50, 20, 100, 40), // base color
                        1f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
                        new Color(50, 20, 110, 130),
                        new Color(150, 30, 120, 150),
                        new Color(200, 50, 130, 190),
                        new Color(250, 70, 150, 240),
                        new Color(200, 80, 130, 255),
                        new Color(75, 0, 160),
                        new Color(127, 0, 255)
                ));
        field2.setCircularOrbit(watergiant, 0, 0, 150);


        system.addRingBand(watergiant, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 520, 20, Terrain.ASTEROID_BELT, null);

        Misc.initConditionMarket(watergiant);
        watergiant.getMarket().addCondition(Conditions.VOLATILES_PLENTIFUL);
        watergiant.getMarket().addCondition(Conditions.HOT);
        watergiant.getMarket().addCondition(Conditions.EXTREME_WEATHER);
        watergiant.getMarket().addCondition(Conditions.HIGH_GRAVITY);


        // trojans - L4 leads, L5 follows
        SectorEntityToken watergiant_aruL4 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldParams(
                        300f, // min radius
                        500f, // max radius
                        16, // min asteroid count
                        24, // max asteroid count
                        4f, // min asteroid radius
                        16f, // max asteroid radius
                        "Hurricane L4 Asteroids")); // null for default name

        SectorEntityToken watergiant_aruL5 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldParams(
                        300f, // min radius
                        500f, // max radius
                        16, // min asteroid count
                        24, // max asteroid count
                        4f, // min asteroid radius
                        16f, // max asteroid radius
                        "Hurricane L5 Asteroids")); // null for default name

        watergiant_aruL4.setCircularOrbit(pascal, 120, 4000, 37697);
        watergiant_aruL5.setCircularOrbit(pascal, 0, 4000, 37697);


        PlanetAPI icegiant = system.addPlanet("na_icegiant", pascal, "Einstein", "ice_giant", 210, 240, 10000, 149010); //

        Misc.initConditionMarket(icegiant);
        icegiant.getMarket().addCondition(Conditions.VOLATILES_PLENTIFUL);
        icegiant.getMarket().addCondition(Conditions.COLD);
        icegiant.getMarket().addCondition(Conditions.EXTREME_WEATHER);
        icegiant.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
        icegiant.getMarket().addCondition(Conditions.HIGH_GRAVITY);

        SectorEntityToken field3 = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldParams(80, // terrain effect band width
                        300, // terrain effect middle radius
                        pascal, // entity that it's around
                        260f, // visual band start
                        340f, // visual band end
                        new Color(50, 20, 100, 40), // base color
                        1f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
                        new Color(50, 20, 110, 130),
                        new Color(150, 30, 120, 150),
                        new Color(200, 50, 130, 190),
                        new Color(250, 70, 150, 240),
                        new Color(200, 80, 130, 255),
                        new Color(75, 0, 160),
                        new Color(127, 0, 255)
                ));
        field3.setCircularOrbit(icegiant, 0, 0, 150);



        system.addAsteroidBelt(icegiant, 90, 2650, 500, 150, 300, Terrain.ASTEROID_BELT,  null);

        system.addRingBand(icegiant, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 700, 55, null, null);
        system.addRingBand(icegiant, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 1100, 65, null, null);

        system.addAsteroidBelt(icegiant, 15, 700, 255, 45, 55);
        system.addAsteroidBelt(icegiant, 35, 1100, 350, 60, 70);




        PlanetAPI icegiantmoon = system.addPlanet("na_icegiantmoon", icegiant, "Laplace", "toxic", 210, 55, 1700, 90); //

        icegiantmoon.setCustomDescriptionId("na_icegiantmoon");
        Misc.initConditionMarket(icegiantmoon);
        icegiantmoon.getMarket().addCondition(Conditions.VOLATILES_ABUNDANT);
        icegiantmoon.getMarket().addCondition(Conditions.ORGANICS_PLENTIFUL);
        icegiantmoon.getMarket().addCondition(Conditions.COLD);
        icegiantmoon.getMarket().addCondition(Conditions.TECTONIC_ACTIVITY);
        icegiantmoon.getMarket().addCondition(Conditions.LOW_GRAVITY);
        icegiantmoon.getMarket().addCondition(Conditions.RUINS_SCATTERED);
        icegiantmoon.getMarket().getFirstCondition(Conditions.RUINS_SCATTERED).setSurveyed(true);


        PlanetAPI icegiantmoon2 = system.addPlanet("na_icegiantmoon2", icegiant, "Mach", "cryovolcanic", 50, 70, 1300, 40); //

        icegiantmoon2.setCustomDescriptionId("na_icegiantmoon2");
        Misc.initConditionMarket(icegiantmoon2);
        icegiantmoon2.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);
        icegiantmoon2.getMarket().addCondition(Conditions.ORE_SPARSE);
        icegiantmoon2.getMarket().addCondition(Conditions.VERY_COLD);
        icegiantmoon2.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);
        icegiantmoon2.getMarket().addCondition(Conditions.LOW_GRAVITY);



        // Trojan moon
        PlanetAPI icegiantmoon3 = system.addPlanet("na_icegiantmoon3", pascal, "Kepler", "frozen", 270, 60, 10000, 149010); //

        icegiantmoon3.setCustomDescriptionId("na_icegiantmoon3");
        Misc.initConditionMarket(icegiantmoon3);
        icegiantmoon3.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);
        icegiantmoon3.getMarket().addCondition(Conditions.RARE_ORE_ABUNDANT);
        icegiantmoon3.getMarket().addCondition(Conditions.ORE_MODERATE);
        icegiantmoon3.getMarket().addCondition(Conditions.VERY_COLD);
        icegiantmoon3.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);
        icegiantmoon3.getMarket().addCondition(Conditions.LOW_GRAVITY);
        icegiantmoon3.getMarket().addCondition(Conditions.RUINS_SCATTERED);
        icegiantmoon3.getMarket().getFirstCondition(Conditions.RUINS_SCATTERED).setSurveyed(true);
        if (Global.getSettings().getModManager().isModEnabled("US")) {
            // Add space elevator if Unknown Skies installed
            icegiantmoon3.getMarket().addCondition("US_bedrock");
        }

        // trojans - L4 leads, L5 follows
        SectorEntityToken icegiantL4 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldParams(
                        500f, // min radius
                        600f, // max radius
                        16, // min asteroid count
                        24, // max asteroid count
                        4f, // min asteroid radius
                        16f, // max asteroid radius
                        "Einstein L4 Asteroids")); // null for default name

        SectorEntityToken icegiantL5 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldParams(
                        300f, // min radius
                        500f, // max radius
                        16, // min asteroid count
                        24, // max asteroid count
                        4f, // min asteroid radius
                        16f, // max asteroid radius
                        "Einstein L5 Asteroids")); // null for default name

        icegiantL4.setCircularOrbit(pascal, 210 +60, 10000, 149010);
        icegiantL5.setCircularOrbit(pascal, 210 -60, 10000, 149010);



        PlanetAPI cenotaph = system.addPlanet("na_cenotaph", pascal, "Monolith", "na_cenotaph", 140, 80, 8500, 116773); //
        cenotaph.getSpec().setGlowColor(new Color(54, 119, 84, 84));
        cenotaph.getSpec().setUseReverseLightForGlow(true);
        cenotaph.applySpecChanges();
        cenotaph.setCustomDescriptionId("na_cenotaph");

        Misc.initConditionMarket(cenotaph);
        cenotaph.getMarket().addCondition(Conditions.COLD);
        cenotaph.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);




        system.addAsteroidBelt(pascal, 120, 17000, 1000, 188248, 228248);
        system.addRingBand(pascal, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 16500, 200000, null, "Outer Graveyard");
        system.addRingBand(pascal, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 17780, 195000, null, "Outer Graveyard");




        system.addAsteroidBelt(pascal, 120, 3000, 300, 25697, 28248);
        system.addRingBand(pascal, "misc", "rings_asteroids0", 256f, 2, Color.white, 256f, 3000, 27000, null, null);

        /* ----------FUN STUFF------------- */









        /* ----------OBJECTS------------- */

        // create relay
        SectorEntityToken relay = system.addCustomEntity("na_relay", // unique id
                "Pascal Relay", // name - if null, defaultName from custom_entities.json will be used
                "comm_relay_makeshift", // type of object, defined in custom_entities.json
                "independent"); // faction
        relay.setCircularOrbit(pascal, 0, 4000, 37697);

        // create relay
        SectorEntityToken nav = system.addCustomEntity("na_nav", // unique id
                "Pascal Nav Buoy", // name - if null, defaultName from custom_entities.json will be used
                "nav_buoy_makeshift", // type of object, defined in custom_entities.json
                "independent"); // faction
        nav.setCircularOrbit(pascal, 120, 4000, 37697);

        /* ----------PLANETS------------- */

        SectorEntityToken graveyard
                = system.addCustomEntity("na_graveyard_station", "Graveyard Station", "station_side06",
                "independent");
        graveyard.setCircularOrbitPointingDown(watergiant, 120, 800, 30);
        graveyard.setCustomDescriptionId("na_graveyard_station");
        graveyard.setInteractionImage("illustrations", "industrial_megafacility");

        //if (!NAModPlugin.isExerelin) {
            MarketAPI naGraveyardMarket = addMarketplace.addMarketplace("independent", graveyard,
                    null,
                    "Graveyard Station",
                    5,
                    new ArrayList<>(Arrays.asList(
                            Conditions.NO_ATMOSPHERE,
                            Conditions.INDUSTRIAL_POLITY,
                            Conditions.POPULATION_5)),
                    new ArrayList<>(Arrays.asList(
                            Industries.POPULATION,
                            Industries.SPACEPORT,
                            Industries.HEAVYBATTERIES,
                            Industries.ORBITALWORKS,
                            Industries.HIGHCOMMAND,
                            "na_armory_hq",
                            Industries.BATTLESTATION_HIGH)),
                    new ArrayList<>(Arrays.asList(
                            Submarkets.SUBMARKET_STORAGE,
                            Submarkets.SUBMARKET_BLACK,
                            "na_market_nca",
                            Submarkets.SUBMARKET_OPEN)),
                    0.3f);
        //}

        naGraveyardMarket.getMemoryWithoutUpdate().set("$musicSetId","nightcross_market_friendly");

        SectorEntityToken aria
                = system.addCustomEntity("na_researchbase", "Aria Station", "station_side00",
                "pirates");
        aria.setCircularOrbitPointingDown(cenotaph, 270, 400, 20);
        aria.setCustomDescriptionId("na_researchbase");
        aria.setInteractionImage("illustrations", "pirate_station");

        //if (!NAModPlugin.isExerelin) {
            MarketAPI naAriaMarket = addMarketplace.addMarketplace("pirates", aria,
                    null,
                    "Aria Station",
                    3,
                    new ArrayList<>(Arrays.asList(
                            Conditions.NO_ATMOSPHERE,
                            Conditions.FREE_PORT,
                            Conditions.ORGANIZED_CRIME,
                            Conditions.POPULATION_3)),
                    new ArrayList<>(Arrays.asList(
                            Industries.POPULATION,
                            Industries.SPACEPORT,
                            Industries.WAYSTATION,
                            Industries.MILITARYBASE,
                            Industries.GROUNDDEFENSES,
                            "na_aria_hq",
                            "battlestation_aria")),
                    new ArrayList<>(Arrays.asList(
                            Submarkets.SUBMARKET_STORAGE,
                            Submarkets.SUBMARKET_BLACK,
                            "na_market_aria",
                            Submarkets.SUBMARKET_OPEN)),
                    0.3f);
        //}
        naAriaMarket.getIndustry("battlestation_aria").setAICoreId(Commodities.ALPHA_CORE);
        naAriaMarket.getMemoryWithoutUpdate().set("$musicSetId","nightcross_market_hostile");
        PlanetAPI nightcross = system.addPlanet("nightcross", pascal, "Nightcross", "na_nightcross", -30, 100, 7000,
                87269);
        nightcross.setInteractionImage("illustrations", "jangala_station");
        nightcross.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "sindria"));
        nightcross.getSpec().setGlowColor(new Color(255, 255, 255, 255));
        nightcross.getSpec().setUseReverseLightForGlow(true);
        nightcross.applySpecChanges();
        nightcross.setCustomDescriptionId("nightcross_nightcrossplanet");

        SectorEntityToken NAstation
                = system.addCustomEntity("na_elevator", "Nightcross Space Elevator", "pascal_na_station",
                "independent");
        NAstation.setCircularOrbitPointingDown(nightcross, 100, 235, 30f);
        nightcross.setInteractionImage("illustrations", "jangala_station");

        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("pascal_jp", "Nightcross Passage");
        OrbitAPI orbit = Global.getFactory().createCircularOrbit(nightcross, 90, 1500, 35);
        jumpPoint.setOrbit(orbit);
        jumpPoint.setRelatedPlanet(nightcross);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);

        Misc.initConditionMarket(nightcross);
        nightcross.getMarket().addCondition(Conditions.ORE_RICH);
        nightcross.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
        nightcross.getMarket().addCondition(Conditions.IRRADIATED);
        nightcross.getMarket().addCondition(Conditions.HABITABLE);
        nightcross.getMarket().addCondition(Conditions.MILD_CLIMATE);
        nightcross.getMarket().addCondition(Conditions.DECIVILIZED);
        nightcross.getMarket().addCondition(Conditions.RUINS_VAST);
        if (Global.getSettings().getModManager().isModEnabled("US")) {
            // Add space elevator if Unknown Skies installed
            nightcross.getMarket().addCondition("US_elevator");
        }
        nightcross.getMarket().getFirstCondition(Conditions.RUINS_VAST).setSurveyed(true);

        //if (!NAModPlugin.isExerelin) {
        MarketAPI naNightcrossMarket = addMarketplace.addMarketplace("independent", NAstation,
                null,
                "Nightcross Space Elevator",
                5,
                new ArrayList<>(Arrays.asList(
                        Conditions.NO_ATMOSPHERE,
                        Conditions.FREE_PORT,
                        Conditions.DISSIDENT,
                        Conditions.POPULATION_5)),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.WAYSTATION,
                        Industries.MEGAPORT,
                        Industries.HEAVYBATTERIES,
                        Industries.PATROLHQ,
                        Industries.ORBITALSTATION_HIGH)),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN)),
                0.3f);
        //}

        naNightcrossMarket.getMemoryWithoutUpdate().set("$musicSetId","nightcross_market_neutral");

        PlanetAPI moon = system.addPlanet("na_moon", nightcross, "Vanth", "rocky_metallic", 30, 50, 800,
                42);
        moon.setInteractionImage("illustrations", "vacuum_colony");
        moon.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "sindria"));
        moon.getSpec().setGlowColor(new Color(255, 255, 255, 255));
        moon.getSpec().setUseReverseLightForGlow(true);
        moon.applySpecChanges();
        moon.setCustomDescriptionId("na_moon");

        Misc.initConditionMarket(moon);
        moon.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);
        moon.getMarket().addCondition(Conditions.ORE_SPARSE);
        moon.getMarket().addCondition(Conditions.VERY_COLD);
        moon.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
        moon.getMarket().addCondition(Conditions.LOW_GRAVITY);
        moon.getMarket().addCondition(Conditions.RUINS_SCATTERED);
        moon.getMarket().getFirstCondition(Conditions.RUINS_SCATTERED).setSurveyed(true);

        /*MarketAPI moonMarket = addMarketplace.addMarketplace("nightcross", moon,
                null,
                "Vanth",
                5,
                new ArrayList<>(Arrays.asList(
                        Conditions.FARMLAND_BOUNTIFUL,
                        Conditions.ORE_SPARSE,
                        Conditions.IRRADIATED,
                        Conditions.MILD_CLIMATE,
                        Conditions.POPULATION_5)),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.MEGAPORT,
                        Industries.HEAVYBATTERIES,
                        Industries.MILITARYBASE,
                        Industries.REFINING)),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_OPEN)),
                0.3f);
        moonMarket.addIndustry(Industries.ORBITALWORKS, new ArrayList<>(Arrays.asList(Items.CORRUPTED_NANOFORGE)));*/



        /*float radiusAfter = StarSystemGenerator.addOrbitingEntities(system, pascal, StarAge.YOUNG,
                                                                    2, 4, // min/max entities to add
                                                                    14200, // radius to start adding at
                                                                    3, // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
                                                                    true); // whether to use custom or system-name based names
        */
        system.autogenerateHyperspaceJumpPoints(true, true);
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }
}


/*
        SectorEntityToken relay = system.addCustomEntity("gneiss_relay", "Gneiss Relay", "comm_relay",
                                                         "blackrock_driveyards");
        relay.setCircularOrbit(gneiss, 220, 3650, 215);

                SectorEntityToken stableloc2 = system.addCustomEntity(null,null, "stable_location",Factions.NEUTRAL);
		stableloc2.setCircularOrbitPointingDown(gneiss, 40, 3650, 215f);

                SectorEntityToken stableloc3 = system.addCustomEntity(null,null, "stable_location",Factions.NEUTRAL);
		stableloc3.setCircularOrbitPointingDown(gneiss, 310, 3650, 215f);

        PlanetAPI creir = system.addPlanet("creir", gneiss, "Creir", "toxic", 100, 130, 6500, 360);
                Misc.initConditionMarket(creir);
        creir.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
        creir.getMarket().addCondition(Conditions.HOT);


        PlanetAPI lydia = system.addPlanet("lydia", creir, "Lydia", "barren_iron", 40, 60, 850, 45); // 0.004 AU
        lydia.setInteractionImage("illustrations", "vacuum_colony");
        lydia.setCustomDescriptionId("blackrock_lydia");

        system.addAsteroidBelt(gneiss, 70, 5600, 128, 440, 470);

        PlanetAPI nanoplanet = system.addPlanet("nanoplanet", gneiss, "Verge", "br_nanoplanet", 230, 340, 9500, 800); //
        nanoplanet.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "banded"));
        nanoplanet.getSpec().setGlowColor(new Color(54, 119, 84, 84));
        nanoplanet.getSpec().setUseReverseLightForGlow(true);
        nanoplanet.applySpecChanges();

                Misc.initConditionMarket(nanoplanet);
        nanoplanet.getMarket().addCondition(NA_conditions.VERGE_CON);
        nanoplanet.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
        nanoplanet.getMarket().addCondition(Conditions.HIGH_GRAVITY);

        system.addRingBand(nanoplanet, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 360, 7.9f);
        system.addRingBand(nanoplanet, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 370, 9.95f);
        system.addRingBand(nanoplanet, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 380, 11.45f);

        system.addAsteroidBelt(nanoplanet, 70, 900, 128, 10, 16);

        SectorEntityToken vigil = system.addCustomEntity("brstation2", "Vigil Station", "br_station",
                "blackrock_driveyards");
        vigil.setCircularOrbitPointingDown(system.getEntityById("nanoplanet"), 90, 540, 11);
        vigil.setInteractionImage("illustrations", "blackrock_vigil_station");
        vigil.setCustomDescriptionId("blackrock_vigil");

        PlanetAPI preclusion = system.addPlanet("preclusion", gneiss, "Preclusion", "cryovolcanic", 260, 30, 12200, 480);
        preclusion.setInteractionImage("illustrations", "abandoned_station");
        preclusion.setCustomDescriptionId("blackrock_preclusion");*/