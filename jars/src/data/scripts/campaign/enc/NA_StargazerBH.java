package data.scripts.campaign.enc;

import java.awt.*;
import java.util.*;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.enc.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPluginImpl;
import com.fs.starfarer.api.impl.campaign.world.GateHaulerLocation;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.plugins.NAModPlugin;
import data.scripts.campaign.plugins.NA_SettingsListener;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.world.nightcross.NA_StargazerBehavior;
import data.scripts.world.nightcross.NA_StargazerWandererManager;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import static data.scripts.world.nightcross.NA_StargazerFleets.createStargazerFleet;

public class NA_StargazerBH extends AbyssalRogueStellarObjectEPEC {

    public static final float DEPTH_THRESHOLD_FOR_ABYSSAL_STARGAZER_ENC = 0.15f;
    public static final float STARGAZER_FREQ = 7f;
    public static final float STARGAZER_BASE_POWER = 5f;

    public static WeightedRandomPicker<NA_RogueStellarObjectType> ALT_STELLAR_OBJECT_TYPES = new WeightedRandomPicker<NA_RogueStellarObjectType>();
    public static enum NA_RogueStellarObjectType {
        PLANETOID,
        GAS_GIANT,
        BLACK_HOLE,
        BROWN_DWARF,
        BLACK_DWARF,
    }



    static {

        ALT_STELLAR_OBJECT_TYPES.add(NA_RogueStellarObjectType.PLANETOID, 75);
        ALT_STELLAR_OBJECT_TYPES.add(NA_RogueStellarObjectType.GAS_GIANT, 12f);
        ALT_STELLAR_OBJECT_TYPES.add(NA_RogueStellarObjectType.BLACK_HOLE, 5f);
        ALT_STELLAR_OBJECT_TYPES.add(NA_RogueStellarObjectType.BROWN_DWARF, 8f);
        ALT_STELLAR_OBJECT_TYPES.add(NA_RogueStellarObjectType.BLACK_DWARF, 7f);
        // black holes much more common
    }


    public static float PROB_BLACK_HOLE_ORBITERS = 0.14f;
    public static float PROB_BROWN_DWARF_ORBITERS = 0.45f;
    public static float PROB_BLACK_DWARF_ORBITERS = 0.75f;
    public static float CACHE_CHANCE = 0.2f;


    @Override
    public void createEncounter(EncounterManager manager, EncounterPoint point) {
        HyperspaceAbyssPluginImpl.AbyssalEPData data = (HyperspaceAbyssPluginImpl.AbyssalEPData) point.custom;
        SectorAPI sector = Global.getSector();

        StarSystemAPI system = sector.createStarSystem("Deep Space");
        system.setProcgen(true);
        //system.setType(StarSystemType.NEBULA);
        system.setName("Deep Space"); // to get rid of "Star System" at the end of the name
        system.setOptionalUniqueId(Misc.genUID());
        system.setType(StarSystemGenerator.StarSystemType.DEEP_SPACE);
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.addTag(Tags.TEMPORARY_LOCATION);
        system.addTag(Tags.SYSTEM_ABYSSAL);


        if (data.nearest == null) {
            float prob = (data.depth - 1f) / (MAX_THREAT_PROB_DEPTH - 1f) * MAX_THREAT_PROB;
            if (prob > MAX_THREAT_PROB) prob = MAX_THREAT_PROB;
            if (prob > 0 && prob < MIN_THREAT_PROB) prob = 0.25f;

            String failKey = "$threatSpawnsFailedToRoll";
            float numFails = Global.getSector().getMemoryWithoutUpdate().getInt(failKey);
            float probBonus = 0f;
            if (data.depth >= MIN_DEPTH_FOR_GUARANTEED) {
                probBonus = numFails * BONUS_PROB_PER_FAIL;
            }
            if (data.random.nextFloat() < prob + probBonus) {
                system.addTag(Tags.SYSTEM_CAN_SPAWN_THREAT);
                numFails = 0;
            } else {
                numFails++;
            }
            Global.getSector().getMemoryWithoutUpdate().set(failKey, (int) numFails);
//			public static float MIN_DEPTH_FOR_GUARANTEED = 3f;
//			public static float BONUS_PROB_PER_FAIL = 0.25f;
        }


        // threat spawn-in animation looks best against this bg
        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");

//		if (data.random.nextFloat() < 0.5f) {
//			system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
//		} else {
//			system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");
//		}

        system.getLocation().set(point.loc.x, point.loc.y);

        SectorEntityToken center = system.initNonStarCenter();

        system.setLightColor(GateHaulerLocation.ABYSS_AMBIENT_LIGHT_COLOR);
        center.addTag(Tags.AMBIENT_LS);

        NA_RogueStellarObjectType objectType = ALT_STELLAR_OBJECT_TYPES.pick(data.random);
        if (objectType == null) return;

        WeightedRandomPicker<StarAge> agePicker = new WeightedRandomPicker<StarAge>(data.random);
        agePicker.add(StarAge.OLD, 10f);

        StarAge age = agePicker.pick();
        String nebulaId = Planets.NEBULA_CENTER_OLD;
        if (age == StarAge.AVERAGE) {
            nebulaId = Planets.NEBULA_CENTER_AVERAGE;
        } else if (age == StarAge.YOUNG) {
            nebulaId = Planets.NEBULA_CENTER_YOUNG;
        }


        StarGenDataSpec starData = (StarGenDataSpec)
                Global.getSettings().getSpec(StarGenDataSpec.class, nebulaId, false);

        StarSystemGenerator.CustomConstellationParams params = new StarSystemGenerator.CustomConstellationParams(age);
        Random prev = StarSystemGenerator.random;
        StarSystemGenerator.random = data.random;

        StarSystemGenerator gen = new StarSystemGenerator(params);
        gen.init(system, age);

        StarSystemGenerator.GenContext context = new StarSystemGenerator.GenContext(gen, system, system.getCenter(), starData,
                null, 0, age.name(), 0, 1000, null, -1);
        // just so it doesn't try to add things at the planet's lagrange points
        context.lagrangeParent = new StarSystemGenerator.GeneratedPlanet(null, null, false, 0, 0, 0);

        context.excludeCategories.add(StarSystemGenerator.CAT_HAB5);
        context.excludeCategories.add(StarSystemGenerator.CAT_HAB4);
        context.excludeCategories.add(StarSystemGenerator.CAT_HAB3);
        context.excludeCategories.add(StarSystemGenerator.CAT_HAB2);

        PlanetAPI main = null;


        if (objectType == NA_RogueStellarObjectType.BLACK_HOLE) {
            main = addBlackHole(system, context, data);

            if (main != null) {
                system.setStar(main);
                system.setCenter(main);
                system.removeEntity(center);
                center = main;

                if (data.random.nextFloat() < PROB_BLACK_HOLE_ORBITERS) {
                    context.starData = (StarGenDataSpec)
                            Global.getSettings().getSpec(StarGenDataSpec.class, StarTypes.BLACK_HOLE, false);
                    StarSystemGenerator.addOrbitingEntities(system, main, age, 1, 3, 500, 0, false, false);
                }
            }
        } else  if (objectType == NA_RogueStellarObjectType.BROWN_DWARF) {
            main = addBrownDwarf(system, context, data);

            if (main != null) {
                system.setStar(main);
                system.setCenter(main);
                system.removeEntity(center);
                center = main;

                if (data.random.nextFloat() < PROB_BROWN_DWARF_ORBITERS) {
                    context.starData = (StarGenDataSpec)
                            Global.getSettings().getSpec(StarGenDataSpec.class, StarTypes.BROWN_DWARF, false);
                    StarSystemGenerator.addOrbitingEntities(system, main, age, 1, 1, 400 + main.getRadius(), 0, false, false);
                }
            }
        } else  if (objectType == NA_RogueStellarObjectType.BLACK_DWARF) {
            main = addBlackDwarf(system, context, data);

            if (main != null) {
                system.setStar(main);
                system.setCenter(main);
                system.removeEntity(center);
                center = main;

                if (data.random.nextFloat() < PROB_BLACK_DWARF_ORBITERS) {
                    context.starData = (StarGenDataSpec)
                            Global.getSettings().getSpec(StarGenDataSpec.class, StarTypes.WHITE_DWARF, false);
                    StarSystemGenerator.addOrbitingEntities(system, main, age, 1, 2, 2000, 0, false, false);
                }
            }
        } else {
            String planetType;
            if (objectType == NA_RogueStellarObjectType.PLANETOID) {
                planetType = PLANETOID_TYPES.pick(data.random);
            } else {
                planetType = GAS_GIANT_TYPES.pick(data.random);
            }
            //planetType = Planets.CRYOVOLCANIC;
            //planetType = Planets.GAS_GIANT;
            PlanetGenDataSpec planetData = (PlanetGenDataSpec) Global.getSettings().getSpec(PlanetGenDataSpec.class, planetType, false);

            StarSystemGenerator.GenResult result = gen.addPlanet(context, planetData, false, true);
            if (result == null || result.entities.isEmpty() ||
                    !(result.entities.get(0) instanceof PlanetAPI)) return;
            main = (PlanetAPI) result.entities.get(0);
        }

        if (main == null) return;

        main.setOrbit(null);
        main.setLocation(0, 0);

        boolean multiple = context.generatedPlanets.size() > 1;
        int index = data.random.nextInt(20);

//		List<GeneratedPlanet> sorted = new ArrayList<GeneratedPlanet>(context.generatedPlanets);
//		Collections.sort(sorted, new Comparator<GeneratedPlanet>() {
//			public int compare(GeneratedPlanet o1, GeneratedPlanet o2) {
//				return (int) Math.signum(o2.planet.getRadius() - o1.planet.getRadius());
//			}
//		});
        List<PlanetAPI> sorted = new ArrayList<PlanetAPI>(system.getPlanets());
        Collections.sort(sorted, new Comparator<PlanetAPI>() {
            public int compare(PlanetAPI o1, PlanetAPI o2) {
                return (int) Math.signum(o2.getRadius() - o1.getRadius());
            }
        });

//		for (GeneratedPlanet curr : sorted) {
//			PlanetAPI planet = curr.planet;
        for (PlanetAPI planet : sorted) {
            Misc.CatalogEntryType type = Misc.CatalogEntryType.PLANET;
            if (planet.isGasGiant()) type = Misc.CatalogEntryType.GIANT;
            if (planet.getSpec().isBlackHole()) type = Misc.CatalogEntryType.BLACK_HOLE;

            String firstChar = null;
            if (multiple) {
                firstChar = "" + Character.valueOf((char) ('A' + (index % 26)));
                index++;
            }

            String name = Misc.genEntityCatalogId(firstChar, -1, -1, -1, type);
            planet.setName(name);
            if (planet.getMarket() != null) {
                planet.getMarket().setName(name);

                planet.getMarket().removeCondition(Conditions.RUINS_SCATTERED);
                planet.getMarket().removeCondition(Conditions.RUINS_WIDESPREAD);
                planet.getMarket().removeCondition(Conditions.RUINS_EXTENSIVE);
                planet.getMarket().removeCondition(Conditions.RUINS_VAST);
                planet.getMarket().removeCondition(Conditions.DECIVILIZED);
                planet.getMarket().removeCondition(Conditions.DECIVILIZED_SUBPOP);
                planet.getMarket().removeCondition(Conditions.POLLUTION);
            }

            // the standard "barren" description mentions a primary star
            if (planet.getSpec().getDescriptionId().equals(Planets.BARREN)) {
                planet.setDescriptionIdOverride("barren_deep_space");
            }

        }


        StarSystemGenerator.random = prev;

        system.autogenerateHyperspaceJumpPoints(true, false, false);

        setAbyssalDetectedRanges(system);

        system.addScript(new AbyssalLocationDespawner(system));

        addSpecials(system, manager, point, data);
    }


    public static enum StargazerBHType {
        LONE_SHIP,
        GROUP,
        NAMED,
        VICTIM,
        BATTLE,
        ALIVE,
        SWARM,
        NONE,
    }


    /**
     * Not related to sensor ghosts OR IS IT
     *
     * @author Alex
     *
     * Copyright 2023 Fractal Softworks, LLC
     */
    public static enum VictimType {
        NIGHTCROSS,
        TRITACH,
        PIRATE,
        MERC,
        THREAT;

        public String getTimeoutKey() {
            return "$" + name() + "_timeout";
        }
    }

    /**
     * Not related to sensor ghosts OR IS IT
     *
     * @author Alex
     *
     * Copyright 2023 Fractal Softworks, LLC
     */
    public static enum LoneShipType {
        TESSERA,
        TEMPUS,
        LOSULCI,
        MARE,
        KASEI,
        MACULA,
        FOSSA,
        ELYURIAS,
        NAMMU,
        SOP;

        public String getTimeoutKey() {
            return "$" + name() + "_timeout";
        }
    }
    public static Map<LoneShipType, String> NamedShipname = new HashMap();
    static {
        NamedShipname.put(LoneShipType.TESSERA, "Flames of the Sky");
        NamedShipname.put(LoneShipType.TEMPUS, "End of Days");
        NamedShipname.put(LoneShipType.LOSULCI, "Chandrasekhar Limit");
        NamedShipname.put(LoneShipType.MARE, "Roche Limit");
        NamedShipname.put(LoneShipType.KASEI, "Thousand Steps");
        NamedShipname.put(LoneShipType.SOP, "Inhuman");
        NamedShipname.put(LoneShipType.MACULA, "Eternal War");
        NamedShipname.put(LoneShipType.FOSSA, "The Abyss Stares Back");
        NamedShipname.put(LoneShipType.ELYURIAS, "It Came From Beyond");
        NamedShipname.put(LoneShipType.NAMMU, "Tzitzimitl");
    }

    public static Map<VictimType, Float> VictimTypePower = new HashMap();
    static {
        VictimTypePower.put(VictimType.NIGHTCROSS, 2f);
        VictimTypePower.put(VictimType.TRITACH, 2.5f);
        VictimTypePower.put(VictimType.MERC, 3.0f);
        VictimTypePower.put(VictimType.PIRATE, 1.0f);
        VictimTypePower.put(VictimType.THREAT, 8.0f);
    }

    public static WeightedRandomPicker<StargazerBHType> STARGAZER_TYPES = new WeightedRandomPicker<StargazerBHType>();
    static {
        STARGAZER_TYPES.add(StargazerBHType.LONE_SHIP, 4f);
        STARGAZER_TYPES.add(StargazerBHType.GROUP, 3f);
        STARGAZER_TYPES.add(StargazerBHType.NAMED, 5f);
        STARGAZER_TYPES.add(StargazerBHType.VICTIM, 5f);
        STARGAZER_TYPES.add(StargazerBHType.BATTLE, 9f);
        STARGAZER_TYPES.add(StargazerBHType.ALIVE, 6f);
        STARGAZER_TYPES.add(StargazerBHType.NONE, 100f);
    }


    public static HashMap<StargazerBHType, String> MUSIC_CHOICE = new HashMap<StargazerBHType, String>();
    static {
        //MUSIC_CHOICE.put(StargazerBHType.LONE_SHIP, "mekaloton_Numbered_Rooms");
        MUSIC_CHOICE.put(StargazerBHType.GROUP, "kocaeli_nightcross_remnant");
        //MUSIC_CHOICE.put(StargazerBHType.NAMED, "mekaloton_Numbered_Rooms");
        MUSIC_CHOICE.put(StargazerBHType.ALIVE, "mekaloton_Off_Air");
        MUSIC_CHOICE.put(StargazerBHType.SWARM, "mekaloton_Red_Maskq");
        MUSIC_CHOICE.put(StargazerBHType.BATTLE, "kocaeli_nightcross_remnant");
    }
    public static WeightedRandomPicker<VictimType> STARGAZER_VICTIM_TYPES = new WeightedRandomPicker<VictimType>();
    static {
        STARGAZER_VICTIM_TYPES.add(VictimType.NIGHTCROSS, 10f);
        STARGAZER_VICTIM_TYPES.add(VictimType.TRITACH, 2f);
        STARGAZER_VICTIM_TYPES.add(VictimType.PIRATE, 3f);
        STARGAZER_VICTIM_TYPES.add(VictimType.MERC, 5f);
        STARGAZER_VICTIM_TYPES.add(VictimType.THREAT, 1f);
    }
    public static WeightedRandomPicker<VictimType> STARGAZER_VICTIM_TYPES_LONE = new WeightedRandomPicker<VictimType>();
    static {
        STARGAZER_VICTIM_TYPES_LONE.add(VictimType.NIGHTCROSS, 7f);
        STARGAZER_VICTIM_TYPES_LONE.add(VictimType.TRITACH, 3f);
    }


    public static boolean isPointSuited(EncounterPoint point, boolean allowNearStar, float depthRequired) {
        if (!HyperspaceAbyssPluginImpl.EP_TYPE_ABYSSAL.equals(point.type)) return false;
        HyperspaceAbyssPluginImpl.AbyssalEPData data = (HyperspaceAbyssPluginImpl.AbyssalEPData) point.custom;
        if (data.depth < depthRequired) return false;
        if (!allowNearStar && data.nearest != null) return false;
        return true;
    }


    public float getFrequencyForPoint(EncounterManager manager, EncounterPoint point) {
        if (NAModPlugin.hasLunaLib)
        {
            if (!NA_SettingsListener.na_stargazer_abyss) return 0;
        }

        if (!isPointSuited(point, true, DEPTH_THRESHOLD_FOR_ABYSSAL_STARGAZER_ENC)) {
            return 0f;
        }
        if (DebugFlags.ABYSSAL_GHOST_SHIPS_DEBUG) {
            return 1000000000f;
        }
        return STARGAZER_FREQ + 1.5f * NA_StargazerGhostManager.getAbyssInterest();
    }


    @Override
    protected void addSpecials(StarSystemAPI system, EncounterManager manager, EncounterPoint point, HyperspaceAbyssPluginImpl.AbyssalEPData data) {
        WeightedRandomPicker<StargazerBHType> picker = new WeightedRandomPicker<StargazerBHType>(data.random);

        if (system.getStar() != null && system.getStar().isBlackHole()) {

            picker.add(StargazerBHType.BATTLE, 9f);
            picker.add(StargazerBHType.ALIVE, 6f);
            picker.add(StargazerBHType.SWARM, 1f);
        } else if (system.getStar() != null && system.getStar().isStar()) {

            picker.add(StargazerBHType.VICTIM, 4f);
            picker.add(StargazerBHType.LONE_SHIP, 5f);
            picker.add(StargazerBHType.BATTLE, 4f);
            picker.add(StargazerBHType.ALIVE, 6f);
            picker.add(StargazerBHType.SWARM, 4f);
            picker.addAll(STARGAZER_TYPES);
        } else {
            picker.addAll(STARGAZER_TYPES);
        }

        if (picker.getItems().contains(StargazerBHType.NONE)) {
            picker.remove(StargazerBHType.NONE);
            picker.add(StargazerBHType.NONE, STARGAZER_TYPES.getWeight(StargazerBHType.NONE) / (1f + NA_StargazerGhostManager.getAbyssInterest()));
        }
        if (picker.getItems().contains(StargazerBHType.VICTIM)) {
            picker.remove(StargazerBHType.VICTIM);
            picker.add(StargazerBHType.VICTIM, STARGAZER_TYPES.getWeight(StargazerBHType.VICTIM) / (1f + NA_StargazerGhostManager.getAbyssInterest()));
        }




        SharedData.UniqueEncounterData ueData = SharedData.getData().getUniqueEncounterData();
        WeightedRandomPicker<LoneShipType> namedShipPicker = new WeightedRandomPicker<LoneShipType>(data.random);
        WeightedRandomPicker<LoneShipType> ghostShipPicker = new WeightedRandomPicker<LoneShipType>(data.random);
        for (LoneShipType type : EnumSet.allOf(LoneShipType.class)) {
            ghostShipPicker.add(type);
            if (ueData.wasInteractedWith(type.name())) {
                continue;
            }
            if (Global.getSector().getMemoryWithoutUpdate().contains(type.getTimeoutKey())) {
                continue;
            }
            namedShipPicker.add(type);
        }

        if (namedShipPicker.isEmpty()) {
            picker.remove(StargazerBHType.NAMED);
        }


        boolean done = false;
        do {
            StargazerBHType type = picker.pickAndRemove();

            if (MUSIC_CHOICE.containsKey(type)) {
                system.getMemoryWithoutUpdate().set("$musicSetId",MUSIC_CHOICE.get(type));
            }

//			type = AbyssalDireHintType.MINING_OP;
//			type = AbyssalDireHintType.GAS_GIANT_TURBULENCE;
//			type = AbyssalDireHintType.BLACK_HOLE_READINGS;
            if (type == StargazerBHType.LONE_SHIP) {
                system.removeTag(Tags.SYSTEM_CAN_SPAWN_THREAT);
                LoneShipType ghostType = ghostShipPicker.pickAndRemove();
                if (ghostType != null) {
                    done = addGhostShip(system, ghostType, point, data, null, 4);
                }
            } else if (type == StargazerBHType.NONE) {
                // nope!!
                if (!system.hasTag(Tags.SYSTEM_CAN_SPAWN_THREAT)) {
                    // only for empty systems we add the music
                    if (system.getStar() != null && (system.getStar().isBlackHole()
                            || system.getStar().hasTag("na_whitedwarf"))) {
                        system.getMemoryWithoutUpdate().set("$musicSetId", "kocaeli_blackhole");
                        system.getStar().addScript(new NA_BlackHoleMoteScript(system.getStar(), 0.05f));
                    }
                    if (data.random.nextFloat() < CACHE_CHANCE + (1f - 1f / (1f + NA_StargazerGhostManager.getAbyssInterest()))) {
                        NA_StargazerGhostManager.addAbyssInterest(5f);
                        // chance to add a stargazer cache
                        addEntity(system, Entities.SUPPLY_CACHE, data, 2f, 3);
                    }
                }
                done = true;
            } else if (type == StargazerBHType.NAMED) {
                system.removeTag(Tags.SYSTEM_CAN_SPAWN_THREAT);
                LoneShipType ghostType = namedShipPicker.pickAndRemove();
                if (ghostType != null) {
                    done = addGhostShip(system, ghostType, point, data, NamedShipname.get(ghostType), 5);
                }
            } else if (type == StargazerBHType.VICTIM) {
                done = addVictim(system, STARGAZER_VICTIM_TYPES_LONE.pick(), point, data);
            } else if (type == StargazerBHType.GROUP) {
                int count = MathUtils.getRandomNumberInRange(2, 4);
                for (int i = 0; i < count; i++) {
                    LoneShipType ghostType = ghostShipPicker.pick();
                    if (ghostType != null) {
                        done = addGhostShip(system, ghostType, point, data, null, 2);
                    }
                }
            } else if (type == StargazerBHType.BATTLE) {
                NA_StargazerGhostManager.addAbyssInterest(4.5f);
                VictimType vt = STARGAZER_VICTIM_TYPES.pick();
                int count = MathUtils.getRandomNumberInRange(1, (int) Math.max(1, 2.5f* VictimTypePower.get(vt)/STARGAZER_BASE_POWER));
                for (int i = 0; i < count; i++) {
                    LoneShipType ghostType = ghostShipPicker.pick();
                    if (ghostType != null) {
                        done = addGhostShip(system, ghostType, point, data, null, 0f);
                    }
                }
                int vcount = MathUtils.getRandomNumberInRange(1, (int) Math.max(1, 2.5f* STARGAZER_BASE_POWER/VictimTypePower.get(vt)));
                for (int i = 0; i < vcount; i++) {
                    done = addVictim(system, vt, point, data);
                }


            } else if (type == StargazerBHType.ALIVE || type == StargazerBHType.SWARM) {
                NA_StargazerGhostManager.addAbyssInterest(10f);
                system.removeTag(Tags.SYSTEM_CAN_SPAWN_THREAT);
                int count = 1;
                if (type == StargazerBHType.SWARM) count = MathUtils.getRandomNumberInRange(6, 10);

                for (int i = 0; i < count; i++) {
                    float size = MathUtils.getRandomNumberInRange(50, 100);
                    if (type == StargazerBHType.SWARM) size = MathUtils.getRandomNumberInRange(70, 240 - count*10);
                    NA_StargazerWandererManager.StargazerFleetParams params = new NA_StargazerWandererManager.StargazerFleetParams(
                            null,
                            null, // loc in hyper; don't need if have market
                            NightcrossID.FACTION_STARGAZER,
                            -1.5f, // quality override
                            FleetTypes.PATROL_SMALL,
                            MathUtils.getRandomNumberInRange(10, (float) (size + 70*Math.min(data.depth, Math.pow(data.depth, 0.33)))), // combatPts
                            0, // freighterPts
                            0, // tankerPts
                            0f, // transportPts
                            0f, // linerPts
                            0f, // utilityPts
                            0.1f
                    );
                    params.averageSMods = Math.max(0, Math.min(3, Math.round(1 + data.depth)));
                    //params.random = random;
                    params.random = new Random(); //for easier testing
                    params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_ONLY;

                    CampaignFleetAPI f = createStargazerFleet(params, null);

                    system.addEntity(f);
                    if (system.getStar() != null && system.getStar().isBlackHole())
                        f.getMemoryWithoutUpdate().set("$combatMusicSetId","Kocaeli_Core");

                    //float radius = 100f + star.getRadius() + star.getSpec().getCoronaSize();
                    Vector2f loc = Misc.getPointAtRadius(system.getCenter().getLocation(), MathUtils.getRandomNumberInRange(600f, 1300f));
                    f.setLocation(loc.x, loc.y);

                    NA_StargazerBehavior behavior = new NA_StargazerBehavior(f, system, system.getStar(), false, true, true);
                    behavior.setSeenByPlayer();
                    f.addScript(behavior);
                }


                if (type == StargazerBHType.SWARM) {
                    picker = new WeightedRandomPicker<>();
                    picker.add(StargazerBHType.BATTLE, 100f);
                } else {
                    picker = new WeightedRandomPicker<>();
                    picker.add(StargazerBHType.BATTLE, 30f);
                    picker.add(StargazerBHType.VICTIM, 60f);
                }

            }

        } while (!picker.isEmpty() && !done);
    }




    public CustomCampaignEntityAPI addEntity(StarSystemAPI system, String entityType, HyperspaceAbyssPluginImpl.AbyssalEPData data, float distMult, float abyssInterest) {

        WeightedRandomPicker<PlanetAPI> picker = new WeightedRandomPicker<PlanetAPI>(data.random);
        for (PlanetAPI planet : system.getPlanets()) {
            picker.add(planet);
        }
        PlanetAPI planet = picker.pick();
        if (planet == null) return null;

        Random random = data.random;

        CustomCampaignEntityAPI ship = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                data.random, planet.getContainingLocation(), entityType, Factions.NEUTRAL);
        //SalvageSpecialAssigner.assignSpecials(ship, false, data.random);
        //ship.addTag(Tags.EXPIRES);

        ship.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest);

        ship.setDiscoverable(true);
        float orbitRadius = planet.getRadius() + (500f + random.nextFloat() * 100f) * distMult;
        float orbitDays = orbitRadius / (10f + random.nextFloat() * 5f);
        ship.setCircularOrbit(planet, random.nextFloat() * 360f, orbitRadius, orbitDays);

        ship.setLocation(planet.getLocation().x, planet.getLocation().y);
        ship.getVelocity().set(planet.getVelocity());

        ship.getMemoryWithoutUpdate().set("$gsType", "NA_stargazerabyss");

        return ship;
    }


    protected boolean addGhostShip(StarSystemAPI system,LoneShipType type, EncounterPoint point, HyperspaceAbyssPluginImpl.AbyssalEPData data, String shipname, float abyssInterest) {
        WeightedRandomPicker<PlanetAPI> picker = new WeightedRandomPicker<PlanetAPI>(data.random);
        for (PlanetAPI planet : system.getPlanets()) {
            picker.add(planet);
        }
        PlanetAPI planet = picker.pick();
        if (planet == null) return false;


        if (type == LoneShipType.TESSERA) {
            String variantId = "naai_tessera_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), shipname, data.random, false, 3f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops_cru", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest + 0.5);

        } else
        if (type == LoneShipType.TEMPUS) {
            String variantId = "naai_tempus_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), shipname, data.random, false, 4f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops_cru", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest + 0.5);
        }else
        if (type == LoneShipType.KASEI) {
            String variantId = "naai_kasei_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), shipname, data.random, false, 2f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest);
        }else
        if (type == LoneShipType.FOSSA) {
            String variantId = "naai_fossa_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), shipname, data.random, false, 1.5f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest);
        }else
        if (type == LoneShipType.MACULA) {
            String variantId = "naai_macula_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.PRISTINE, type.name(), shipname, data.random, false, 1f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops_cru", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest + 1);
        }else
        if (type == LoneShipType.SOP) {
            String variantId = "naai_sop_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.PRISTINE, type.name(), shipname, data.random, false, 1f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest);
        }else
        if (type == LoneShipType.MARE) {
            String variantId = "naai_mare_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.PRISTINE, type.name(), shipname, data.random, false, 1f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest);
        }else
        if (type == LoneShipType.LOSULCI) {
            String variantId = "naai_losulci_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.BATTERED, type.name(), shipname, data.random, false, 5f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops_cap", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest + 3);
        }else
        if (type == LoneShipType.ELYURIAS) {
            String variantId = "naai_elyurias_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.BATTERED, type.name(), shipname, data.random, false, 2f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops_cap", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest + 1);
        }else
        if (type == LoneShipType.NAMMU) {
            String variantId = "naai_nammu_corrupted";
            if (variantId == null) return false;
            CustomCampaignEntityAPI e = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), shipname, data.random, false, 1.5f,"kocaeli_nightcross_remnant");
            e.addDropRandom("na_stargazer_drops", 1);
            e.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest);
        }




        Global.getSector().getMemoryWithoutUpdate().set(type.getTimeoutKey(), true, 90f);

        return true;
    }


    public void addStargazerWeapons(CustomCampaignEntityAPI ship, int chances) {
        ship.addDropRandom("na_weps_stargazer", chances);
        ship.addDropRandom("na_weps_stargazer_loot", chances);
    }
    public void addThreatWeapons(CustomCampaignEntityAPI ship, int chances) {
        ship.addDropRandom("na_weps_threat", chances);

    }

    protected boolean addVictim(StarSystemAPI system,VictimType type, EncounterPoint point, HyperspaceAbyssPluginImpl.AbyssalEPData data) {
        WeightedRandomPicker<PlanetAPI> picker = new WeightedRandomPicker<PlanetAPI>(data.random);
        for (PlanetAPI planet : system.getPlanets()) {
            picker.add(planet);
        }
        PlanetAPI planet = picker.pick();
        if (planet == null) return false;

        String size = ShipRoles.COMBAT_LARGE;
        float sizeMod = MathUtils.getRandomNumberInRange(0f, 1f);
        if (sizeMod < 0.4f) size = ShipRoles.COMBAT_SMALL;
        else if (sizeMod < 0.7f) size = ShipRoles.COMBAT_MEDIUM;

        if (type == VictimType.NIGHTCROSS) {
            String variantId = pickVariant("nightcross", size, data.random);
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), data.random, true, 1f, null, 3);
        } else if (type == VictimType.TRITACH) {
            String variantId = pickVariant(Factions.TRITACHYON, size, data.random);
            if (variantId == null) return false;
            CustomCampaignEntityAPI ship = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), data.random, true, 1f, null, 3);
            if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$player.nca_abyss_sgvs_tritach")) {
                addStargazerWeapons(ship, 3);
            }
        } else if (type == VictimType.PIRATE) {
            String variantId = pickVariant(Factions.PIRATES, size, data.random);
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.WRECKED, type.name(), data.random, true, 1f, null, 3);
        } else if (type == VictimType.MERC) {
            String variantId = pickVariant(Factions.MERCENARY, size, data.random);
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), data.random, true, 1f, null, 3);
        } else if (type == VictimType.THREAT) {
            String variantId = pickVariant(Factions.THREAT, size, data.random);
            if (variantId == null) return false;
            CustomCampaignEntityAPI ship = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.BATTERED, type.name(), data.random, true, 1f, "Kocaeli_Coreq", 3);
            if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$player.nca_abyss_sgvs_threat")) {
                addThreatWeapons(ship, 3);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().set(type.getTimeoutKey(), true, 90f);

        return true;
    }

    public CustomCampaignEntityAPI addShipAroundPlanet(SectorEntityToken planet, String variantId, ShipRecoverySpecial.ShipCondition condition,
                                    String gsType, Random random, boolean pruneWeapons, float distMult, String music, float abyssInterest) {
        CustomCampaignEntityAPI ship = addShipAroundPlanet(planet, variantId, condition, gsType, null, random, pruneWeapons, distMult, music);
        ship.getMemoryWithoutUpdate().set(NA_StargazerGhostManager.ABYSS_INTEREST_KEY, abyssInterest);

        return ship;
    }
    public CustomCampaignEntityAPI addShipAroundPlanet(SectorEntityToken planet, String variantId, ShipRecoverySpecial.ShipCondition condition,
                                    String gsType, String shipName, Random random, boolean pruneWeapons, float distMult, String music) {
        ShipRecoverySpecial.PerShipData psd = new ShipRecoverySpecial.PerShipData(variantId, condition, 0f);
        if (shipName != null) {
            psd.shipName = shipName;
            psd.nameAlwaysKnown = true;
            psd.pruneWeapons = pruneWeapons;
        }
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(psd, true);

        CustomCampaignEntityAPI ship = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                random, planet.getContainingLocation(), Entities.WRECK, Factions.NEUTRAL, params);
        //SalvageSpecialAssigner.assignSpecials(ship, false, data.random);
        //ship.addTag(Tags.EXPIRES);

        ship.setDiscoverable(true);
        float orbitRadius = planet.getRadius() + (500f + random.nextFloat() * 100f) * distMult;
        float orbitDays = orbitRadius / (10f + random.nextFloat() * 5f);
        ship.setCircularOrbit(planet, random.nextFloat() * 360f, orbitRadius, orbitDays);

        ship.setLocation(planet.getLocation().x, planet.getLocation().y);
        ship.getVelocity().set(planet.getVelocity());

        ship.getMemoryWithoutUpdate().set("$gsType", "NA_" + gsType);
        if (music != null)
            ship.getMemoryWithoutUpdate().set("$musicSetId", music);

        return ship;
    }

    public String pickVariant(String factionId, String shipRole, Random random) {
        FactionAPI.ShipPickParams params = new FactionAPI.ShipPickParams(FactionAPI.ShipPickMode.ALL);
        List<ShipRolePick> picks = Global.getSector().getFaction(factionId).pickShip(shipRole, params, null, random);
        if (picks == null || picks.isEmpty()) return null;
        return picks.get(0).variantId;

    }



    public PlanetAPI addBrownDwarf(StarSystemAPI system, StarSystemGenerator.GenContext context, HyperspaceAbyssPluginImpl.AbyssalEPData data) {

        StarGenDataSpec starData = (StarGenDataSpec)
                Global.getSettings().getSpec(StarGenDataSpec.class, StarTypes.BROWN_DWARF, false);

        system.setLightColor(starData.getLightColorMin());

        float radius = starData.getMinRadius() +
                (starData.getMaxRadius() - starData.getMinRadius()) * data.random.nextFloat();

        PlanetAPI planet = system.addPlanet(null, null, null, StarTypes.BROWN_DWARF, 0, radius, 0, 0);

        // add ring disk
        if (data.random.nextFloat() < 0.33)
            system.addRingBand(planet, "misc", "rings_dust0", 256f, 3, Color.red, 128 + data.random.nextFloat() * 256f, radius + 100 + data.random.nextFloat() * 300, 30 + data.random.nextFloat() * 30f, null, null);

        StarSystemGenerator.GeneratedPlanet p = new StarSystemGenerator.GeneratedPlanet(null, planet, false, 0, 0, 0);
        context.generatedPlanets.add(p);

        return planet;
    }

    public static final WeightedRandomPicker<String> BLACK_DWARF_TYPES = new WeightedRandomPicker<String>();
    static {
        BLACK_DWARF_TYPES.add("na_whitedwarf", 3f);
        BLACK_DWARF_TYPES.add("na_blackdwarf", 4f);
        BLACK_DWARF_TYPES.add("na_blackdwarf2", 5f);
    }

    public PlanetAPI addBlackDwarf(StarSystemAPI system, StarSystemGenerator.GenContext context, HyperspaceAbyssPluginImpl.AbyssalEPData data) {
        String bdType = BLACK_DWARF_TYPES.pick();
        StarGenDataSpec starData = (StarGenDataSpec)
                Global.getSettings().getSpec(StarGenDataSpec.class,  bdType, false);

        system.setLightColor(starData.getLightColorMin());

        float radius = starData.getMinRadius() +
                (starData.getMaxRadius() - starData.getMinRadius()) * data.random.nextFloat();

        PlanetAPI planet = system.addPlanet(null, null, null, bdType, 0, radius, 0, 0);

        planet.addTag("na_whitedwarf");

        StarSystemGenerator.GeneratedPlanet p = new StarSystemGenerator.GeneratedPlanet(null, planet, false, 0, 0, 0);
        context.generatedPlanets.add(p);

        return planet;
    }
}

















