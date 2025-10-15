package data.scripts.world.nightcross;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.SurveyDataSpecial;
import com.fs.starfarer.api.plugins.SurveyPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.plugins.NAModPlugin;
import data.scripts.campaign.plugins.NA_SettingsListener;
import data.scripts.world.NightcrossTags;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class NA_NightcrossThemeGenerator extends BaseThemeGenerator {




    public static final String NIGHTCROSS_RESEARCH_OUTPOST = "na_research_outpost";
    public static final String NIGHTCROSS_RESEARCH_STATION = "naai_research_stargazerstation";

    @Override
    public float getWeight() {
        if (NAModPlugin.hasLunaLib)
        {
            if (!NA_SettingsListener.na_stargazer_gen) return 0;
        }
        return 50;
    }

    public static final float BASE_LINK_FRACTION = 0.25f;
    public static final float SALVAGE_SPECIAL_FRACTION = 0.5f;
    public static final float ABYSSDATA_FRACTION = 0.2f;

    public static final int BRANCHES_PER_STARSTATION_MIN = 2;
    public static final int BRANCHES_PER_STARSTATION_MAX = 3;

    public static final int BRANCHES_PER_RESEARCHSTATION_MIN = 1;
    public static final int BRANCHES_PER_RESEARCHSTATION_MAX = 3;


    public static class SystemGenData {
        public int numMotherships;
        public int numSurveyShips;
        public int numProbes;
    }


    public String getThemeId() {
        return NightcrossTags.THEME_NIGHTCROSS;
    }


    @Override
    public void generateForSector(ThemeGenContext context, float allowedUnusedFraction) {

        float total = (float) (context.constellations.size() - context.majorThemes.size()) * allowedUnusedFraction;
        if (total <= 0) return;

        float avg1 = (BRANCHES_PER_STARSTATION_MIN + BRANCHES_PER_STARSTATION_MAX) / 2f;
        float avg2 = (BRANCHES_PER_RESEARCHSTATION_MIN + BRANCHES_PER_RESEARCHSTATION_MAX) / 2f;
        float perChain = 1 + avg1 + (avg1 * avg2);

        float num = total / perChain;
        if (num < 1) num = 1;
        if (num > 3) num = 3;

        if (num > 1 && num < 2) {
            num = 2;
        } else {
            num = Math.round(num);
        }

        List<BaseThemeGenerator.AddedEntity> mothershipsSoFar = new ArrayList<BaseThemeGenerator.AddedEntity>();

        for (int i = 0; i < num; i++) {
            addStargazerStationChain(context, mothershipsSoFar);
        }
        // future: add other stuff like beacons, caches, etc
        /*

        WeightedRandomPicker<StarSystemAPI> cryoSystems = new WeightedRandomPicker<StarSystemAPI>(StarSystemGenerator.random);
        WeightedRandomPicker<StarSystemAPI> backup = new WeightedRandomPicker<StarSystemAPI>(StarSystemGenerator.random);
        OUTER: for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            float w = 0f;
            if (system.hasTag(NightcrossTags.THEME_NIGHTCROSS_SHIPS)) {
                w = 10f;
            } else if (system.hasTag(NightcrossTags.THEME_NIGHTCROSS_STATION)) {
                w = 10f;
            } else if (system.hasTag(NightcrossTags.THEME_NIGHTCROSS_STARGAZERSTATION)) {
                w = 10f;
            } else if (system.hasTag(NightcrossTags.THEME_NIGHTCROSS)) {
                w = 10f;
            } else {
                continue;
            }

            int numPlanets = 0;
            boolean hasHab = false;
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                if (planet.getSpec().isPulsar()) continue OUTER;
                hasHab |= planet.getMarket() != null && planet.getMarket().hasCondition(Conditions.HABITABLE);
                numPlanets++;
            }

            WeightedRandomPicker<StarSystemAPI> use = cryoSystems;
            if (!hasHab || numPlanets < 3) {
                use = backup;
            }

            if (hasHab) w += 5;
            w += numPlanets;

            if (use == backup) {
                w *= 0.0001f;
            }
            use.add(system, w);
        }


        int numCryo = 2;
        if (cryoSystems.isEmpty() || cryoSystems.getItems().size() < numCryo + 1) {
            cryoSystems.addAll(backup);
        }

        int added = 0;
        WeightedRandomPicker<String> cryosleeperNames = new WeightedRandomPicker<String>(random);
        cryosleeperNames.add("Calypso");
        cryosleeperNames.add("Tantalus");
        while (added < numCryo && !cryoSystems.isEmpty()) {
            StarSystemAPI pick = cryoSystems.pickAndRemove();
            String name = cryosleeperNames.pickAndRemove();
            //AddedEntity cryo = addCryosleeper(pick, name);
            //if (cryo != null) {
            //    added++;
            //}
        }
*/
    }

    protected void addStargazerStationChain(ThemeGenContext context, List<BaseThemeGenerator.AddedEntity> stargazerStationsSoFar) {

        List<BaseThemeGenerator.AddedEntity> all = new ArrayList<BaseThemeGenerator.AddedEntity>();


        Vector2f center = new Vector2f();
        for (BaseThemeGenerator.AddedEntity e : stargazerStationsSoFar) {
            Vector2f.add(center, e.entity.getLocationInHyperspace(), center);
        }
        center.scale(1f / (float)(stargazerStationsSoFar.size() + 1f));

        List<Constellation> constellations = getSortedAvailableConstellations(context, false, center, null);
        WeightedRandomPicker<Constellation> picker = new WeightedRandomPicker<Constellation>(StarSystemGenerator.random);
        for (int i = 0; i < constellations.size() / 3; i++) {
            picker.add(constellations.get(i));
        }

        Constellation main = picker.pick();
        if (main == null) return;

        if (DEBUG) {
            System.out.println("Picked for stargazer station chain start: [" + main.getNameWithType() + "] (" + (int)main.getLocation().x + ", " + (int)main.getLocation().y + ")");
        }

        constellations.remove(main);
        context.majorThemes.put(main, NightcrossTags.NIGHTCROSS);


        StarSystemAPI mainSystem = main.getSystemWithMostPlanets();
        if (mainSystem == null) return;

        //mainSystem.addTag(NightcrossTags.THEME_NIGHTCROSS);
        for (StarSystemAPI system : main.getSystems()) {
            system.addTag(NightcrossTags.THEME_NIGHTCROSS);
        }

//		if (mainSystem.getName().toLowerCase().contains("valac")) {
//			System.out.println("HERE13123123123");
//		}

        BaseThemeGenerator.AddedEntity stargazerStation = addStargazerStation(mainSystem);
        if (stargazerStation == null) return;

        all.add(stargazerStation);

        if (DEBUG) {
            System.out.println("  Added mothership to [" + mainSystem.getNameWithLowercaseType() + "]");
        }

        //if (true) return;

        // probes in mothership system
        //int probesNearMothership = (int) Math.round(StarSystemGenerator.getRandom(2, 4));
        int derelictsNearStargazerStation = getNumProbesForSystem(stargazerStation.entity.getContainingLocation());
        List<BaseThemeGenerator.AddedEntity> added = addToSystem(mainSystem, Entities.WRECK, derelictsNearStargazerStation);
        all.addAll(added);


        linkFractionToParent(stargazerStation, added,
                BASE_LINK_FRACTION,
                NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_STARGAZERSTATION);

        // survey ships in mothership constellation
        int outpostsNearMainStation = (int) Math.round(StarSystemGenerator.getRandom(0, 3));
        if (outpostsNearMainStation > main.getSystems().size()) outpostsNearMainStation = main.getSystems().size();
        if (DEBUG) {
            System.out.println(String.format("Adding %d research stations near stargazerstation", outpostsNearMainStation));
        }
        List<BaseThemeGenerator.AddedEntity> addedShips = addToConstellation(main, NIGHTCROSS_RESEARCH_OUTPOST, outpostsNearMainStation, false);
        all.addAll(addedShips);

        // derelicts in each station with outpost
        for (BaseThemeGenerator.AddedEntity e : addedShips) {
            int derelictsNearOutpost = (int) Math.round(StarSystemGenerator.getRandom(1, 3));
            //int probesNearSurveyShip = getNumProbesForSystem(e.entity.getContainingLocation());
            added = addDerelicts((StarSystemAPI) e.entity.getContainingLocation(), derelictsNearOutpost);
            all.addAll(added);

            linkFractionToParent(e, added,
                    BASE_LINK_FRACTION,
                    NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_OUTPOST);
        }

        linkFractionToParent(stargazerStation, addedShips,
                1f,
                NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_STARGAZERSTATION);

        //if (true) return;

        constellations = getSortedAvailableConstellations(context, false, stargazerStation.entity.getLocationInHyperspace(), null);
        picker = new WeightedRandomPicker<Constellation>(StarSystemGenerator.random);
        for (int i = constellations.size() - 7; i < constellations.size(); i++) {
            if (i < 0) continue;
            picker.add(constellations.get(i));
        }

        int numSurveyShipsInNearConstellations = (int) Math.round(StarSystemGenerator.getRandom(BRANCHES_PER_STARSTATION_MIN, BRANCHES_PER_STARSTATION_MAX));
        if (DEBUG) {
            System.out.println(String.format("Adding up to %d research stations", numSurveyShipsInNearConstellations));
        }
        List<Constellation> constellationsForOutposts = new ArrayList<Constellation>();
        for (int i = 0; i < numSurveyShipsInNearConstellations && !picker.isEmpty(); i++) {
            constellationsForOutposts.add(picker.pickAndRemove());
        }
        List<BaseThemeGenerator.AddedEntity> outerShips = new ArrayList<BaseThemeGenerator.AddedEntity>();


        for (Constellation c : constellationsForOutposts) {
            context.majorThemes.put(c, NightcrossTags.NIGHTCROSS);

            if (DEBUG) {
                System.out.println("  Picked for research outpost: [" + c.getNameWithType() + "]");
            }

            addedShips = addToConstellation(c, NIGHTCROSS_RESEARCH_OUTPOST, 1, true);
            if (addedShips.isEmpty()) continue;

            all.addAll(addedShips);

            BaseThemeGenerator.AddedEntity ship = addedShips.get(0);
            outerShips.addAll(addedShips);

            //int probesNearSurveyShip = (int) Math.round(StarSystemGenerator.getRandom(1, 3));
            int derelictsNearOutpost = getNumProbesForSystem(ship.entity.getContainingLocation());
            added = addDerelicts((StarSystemAPI) ship.entity.getContainingLocation(), derelictsNearOutpost);
            all.addAll(added);

            linkFractionToParent(ship, added,
                    BASE_LINK_FRACTION,
                    NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_OUTPOST);

            int probesInSameConstellation = (int) Math.round(StarSystemGenerator.getRandom(2, 5));
            int max = c.getSystems().size() + 2;
            if (probesInSameConstellation > max) probesInSameConstellation = max;

            added = addToConstellation(c, Entities.WRECK, probesInSameConstellation, false);
            all.addAll(added);

            linkFractionToParent(ship, added,
                    BASE_LINK_FRACTION,
                    NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_OUTPOST);



            List<Constellation> c2 = getSortedAvailableConstellations(context, false, c.getLocation(), constellationsForOutposts);
            WeightedRandomPicker<Constellation> p2 = new WeightedRandomPicker<Constellation>(StarSystemGenerator.random);
            //for (int i = 0; i < constellations.size() / 3 && i < 7; i++) {
            for (int i = c2.size() - 3; i < c2.size(); i++) {
                if (i < 0) continue;
                p2.add(constellations.get(i));
            }

            int derelictSystemsNearOutpost = (int) Math.round(StarSystemGenerator.getRandom(BRANCHES_PER_RESEARCHSTATION_MIN, BRANCHES_PER_RESEARCHSTATION_MAX));
            int k = 0;
            if (DEBUG) {
                System.out.println(String.format("Adding derelicts to %d constellations near survey ship", derelictSystemsNearOutpost));
            }
            List<BaseThemeGenerator.AddedEntity> derelicts3 = new ArrayList<BaseThemeGenerator.AddedEntity>();
            while (k < derelictSystemsNearOutpost && !p2.isEmpty()) {
                Constellation pick = p2.pickAndRemove();
                k++;
                context.majorThemes.put(pick, Themes.NO_THEME);
                int probesInConstellation = (int) Math.round(StarSystemGenerator.getRandom(1, 3));
                derelicts3.addAll(addToConstellation(pick, Entities.WRECK, probesInConstellation, false));
            }

            all.addAll(derelicts3);
            linkFractionToParent(ship, derelicts3,
                    BASE_LINK_FRACTION * 1.33f,
                    NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_STARGAZERSTATION);
        }

        if (random.nextFloat() < BASE_LINK_FRACTION * 1.33f && addedShips.size() > 0) {
            linkFractionToParent(addedShips.get(random.nextInt(0, addedShips.size())), outerShips,
                    1f,
                    NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_OUTPOST);
        } else {

            linkFractionToParent(stargazerStation, outerShips,
                    1f,
                    NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_STARGAZERSTATION);
        }



        assignRandomSpecials(all);
    }

    public static Set<String> interestingConditions = new HashSet<String>();
    public static Set<String> interestingConditionsWithoutHabitable = new HashSet<String>();
    public static Set<String> interestingConditionsWithRuins = new HashSet<String>();
    static {
        //interestingConditions.add(Conditions.VOLATILES_ABUNDANT);
        interestingConditions.add(Conditions.VOLATILES_PLENTIFUL);
        //interestingConditions.add(Conditions.ORE_RICH);
        interestingConditions.add(Conditions.RARE_ORE_RICH);
        interestingConditions.add(Conditions.RARE_ORE_ULTRARICH);
        interestingConditions.add(Conditions.ORE_ULTRARICH);
        interestingConditions.add(Conditions.FARMLAND_BOUNTIFUL);
        interestingConditions.add(Conditions.FARMLAND_ADEQUATE);
        //interestingConditions.add(Conditions.ORGANICS_ABUNDANT);
        interestingConditions.add(Conditions.ORGANICS_PLENTIFUL);
        interestingConditions.add(Conditions.HABITABLE);

        interestingConditionsWithoutHabitable.addAll(interestingConditions);
        interestingConditionsWithoutHabitable.remove(Conditions.HABITABLE);

        interestingConditionsWithRuins.addAll(interestingConditions);
        interestingConditionsWithRuins.add(Conditions.RUINS_VAST);
        interestingConditionsWithRuins.add(Conditions.RUINS_EXTENSIVE);
    }



    protected void assignRandomSpecials(List<BaseThemeGenerator.AddedEntity> entities) {
        Set<PlanetAPI> usedPlanets = new HashSet<PlanetAPI>();
        Set<StarSystemAPI> usedSystems = new HashSet<StarSystemAPI>();

        for (BaseThemeGenerator.AddedEntity e : entities) {
            if (hasSpecial(e.entity)) continue;

            SurveyDataSpecial.SurveyDataSpecialType type = null;
/*
            if (StarSystemGenerator.random.nextFloat() < ABYSSDATA_FRACTION) {
                int min = 0;
                int max = 0;
                if (Entities.WRECK.equals(e.entityType)) {
                    min = HTPoints.LOW_MIN;
                    max = HTPoints.LOW_MAX;
                } else if (NIGHTCROSS_RESEARCH_OUTPOST.equals(e.entityType)) {
                    min = HTPoints.MEDIUM_MIN;
                    max = HTPoints.MEDIUM_MAX;
                } else if (NIGHTCROSS_RESEARCH_STATION.equals(e.entityType)) {
                    min = HTPoints.HIGH_MIN;
                    max = HTPoints.HIGH_MAX;
                }
                int points = min + StarSystemGenerator.random.nextInt(max - min + 1);
                if (points > 0) {
                    TopographicDataSpecial.TopographicDataSpecialData data = new TopographicDataSpecial.TopographicDataSpecialData(points);
                    Misc.setSalvageSpecial(e.entity, data);
                    continue;
                }
            }
            if (StarSystemGenerator.random.nextFloat() < SALVAGE_SPECIAL_FRACTION) {
                float pNothing = 0.1f;
                if (Entities.WRECK.equals(e.entityType)) {
                    pNothing = 0.5f;
                } else if (NIGHTCROSS_RESEARCH_OUTPOST.equals(e.entityType)) {
                    pNothing = 0.25f;
                } else if (NIGHTCROSS_RESEARCH_STATION.equals(e.entityType)) {
                    pNothing = 0f;
                }

                float r = StarSystemGenerator.random.nextFloat();
                if (r >= pNothing) {
                    type = SurveyDataSpecial.SurveyDataSpecialType.PLANET_SURVEY_DATA;
                }
            }
*/
            //type = SpecialType.PLANET_SURVEY_DATA;

            if (type == SurveyDataSpecial.SurveyDataSpecialType.PLANET_SURVEY_DATA) {
                PlanetAPI planet = findInterestingPlanet(e.entity.getConstellation().getSystems(), usedPlanets);
                if (planet != null) {
                    SurveyDataSpecial.SurveyDataSpecialData data = new SurveyDataSpecial.SurveyDataSpecialData(SurveyDataSpecial.SurveyDataSpecialType.PLANET_SURVEY_DATA);
                    data.entityId = planet.getId();
                    data.includeRuins = false;
                    Misc.setSalvageSpecial(e.entity, data);
                    usedPlanets.add(planet);

//					NightcrossThemeSpecialData special = new NightcrossThemeSpecialData(type);
//					special.entityId = planet.getId();
//					usedPlanets.add(planet);
//					e.entity.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPECIAL_DATA, special);
                }
            }
        }
    }


    public static StarSystemAPI findNearbySystem(SectorEntityToken from, Set<StarSystemAPI> exclude) {
        return findNearbySystem(from, exclude, null, 10000f);
    }

    public static StarSystemAPI findNearbySystem(SectorEntityToken from, Set<StarSystemAPI> exclude, Random random, float maxRange) {
        if (random == null) random = StarSystemGenerator.random;

        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<StarSystemAPI>(random);

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (exclude != null && exclude.contains(system)) continue;

            float dist = Misc.getDistance(from.getLocationInHyperspace(), system.getLocation());
            if (dist > maxRange) continue;
            if (systemIsEmpty(system)) continue;

            picker.add(system);
        }

        return picker.pick();
    }


    public static String getInterestingCondition(PlanetAPI planet, boolean includeRuins) {
        if (planet == null) return null;

        Set<String> conditions = interestingConditions;
        if (includeRuins) conditions = interestingConditionsWithRuins;

        for (MarketConditionAPI mc : planet.getMarket().getConditions()) {
            if (conditions.contains(mc.getId())) {
                return mc.getId();
            }
        }
        return null;
    }

    public static PlanetAPI findInterestingPlanet(List<StarSystemAPI> systems, Set<PlanetAPI> exclude) {
        return findInterestingPlanet(systems, exclude, true, false, null);
    }
    public static PlanetAPI findInterestingPlanet(List<StarSystemAPI> systems, Set<PlanetAPI> exclude, boolean includeKnown, boolean includeRuins, Random random) {
        if (random == null) random = StarSystemGenerator.random;

        WeightedRandomPicker<PlanetAPI> planets = new WeightedRandomPicker<PlanetAPI>(random);

        Set<String> conditions = interestingConditions;
        if (includeRuins) conditions = interestingConditionsWithRuins;

        SurveyPlugin plugin = (SurveyPlugin) Global.getSettings().getNewPluginInstance("surveyPlugin");

        for (StarSystemAPI system : systems) {
            if (system.hasTag(Tags.THEME_HIDDEN)) continue;

            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                if (exclude != null && exclude.contains(planet)) continue;
                if (planet.getMarket() == null || !planet.getMarket().isPlanetConditionMarketOnly()) continue;
                if (!includeKnown && planet.getMarket() != null && planet.getMarket().getSurveyLevel() == MarketAPI.SurveyLevel.FULL) {
                    continue;
                }
                //if (planet.getMarket().getSurveyLevel() == SurveyLevel.FULL) continue;

                String type = plugin.getSurveyDataType(planet);
                boolean classIV = Commodities.SURVEY_DATA_4.equals(type);
                boolean classV = Commodities.SURVEY_DATA_5.equals(type);

                if (!(classIV || classV || planet.getMarket().getHazardValue() <= 1f)) continue;

                float w = 1f;
                for (MarketConditionAPI mc : planet.getMarket().getConditions()) {
                    if (conditions.contains(mc.getId())) {
                        w += 1f;
                    }
                }
                if (classIV) w *= 0.5f;
                if (classV) w *= 4f;
                planets.add(planet, w);
            }
        }
        return planets.pick();
    }


    protected int getNumProbesForSystem(LocationAPI system) {
        int base = 1;
        int planets = system.getPlanets().size();

        if (planets <= 3) {
        } else if (planets <= 5) {
            base += 1;
        } else if (planets <= 8) {
            base += 2;
        } else {
            base += 3;
        }

        base += StarSystemGenerator.random.nextInt(2);

        return base;
    }
    protected void linkFractionToParent(BaseThemeGenerator.AddedEntity parent, List<BaseThemeGenerator.AddedEntity> children, float p, NA_NightcrossThemeSpecial.SpecialType type) {

        WeightedRandomPicker<BaseThemeGenerator.AddedEntity> picker = new WeightedRandomPicker<BaseThemeGenerator.AddedEntity>(StarSystemGenerator.random);
        for (BaseThemeGenerator.AddedEntity c : children) {
            if (!hasSpecial(c.entity)) {
                picker.add(c);
            }
        }

        int extraLinks = (int) Math.max(1, Math.round(children.size() * p * (1f + StarSystemGenerator.random.nextFloat() * 0.5f)));
        for (int i = 0; i < extraLinks && !picker.isEmpty(); i++) {
            BaseThemeGenerator.AddedEntity e = picker.pickAndRemove();
            linkToParent(e.entity, parent.entity, type);
        }
    }

//	protected AddedEntity getClosest(AddedEntity from, List<AddedEntity> choices) {
//		float min = Float.MAX_VALUE;
//		AddedEntity result = null;
//		for (AddedEntity e : choices) {
//
//		}
//
//		return result;
//	}

    protected void linkToParent(SectorEntityToken from, SectorEntityToken parent, NA_NightcrossThemeSpecial.SpecialType type) {
        if (hasSpecial(from)) return;

        NA_NightcrossThemeSpecial.NightcrossThemeSpecialData special = new NA_NightcrossThemeSpecial.NightcrossThemeSpecialData(type, from.getCustomEntityType());
        special.entityId = parent.getId();
        from.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPECIAL_DATA, special);
    }

    protected void linkToMothership(SectorEntityToken from, SectorEntityToken mothership) {
        if (hasSpecial(from)) return;

        NA_NightcrossThemeSpecial.NightcrossThemeSpecialData special = new NA_NightcrossThemeSpecial.NightcrossThemeSpecialData(NA_NightcrossThemeSpecial.SpecialType.LOCATION_NIGHTCROSS_STARGAZERSTATION
                , from.getCustomEntityType());
        special.entityId = mothership.getId();
        from.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPECIAL_DATA, special);
    }

    public static boolean hasSpecial(SectorEntityToken entity) {
        return entity.getMemoryWithoutUpdate().contains(MemFlags.SALVAGE_SPECIAL_DATA);
    }

    protected List<BaseThemeGenerator.AddedEntity> addToConstellation(Constellation c, String type, int num, boolean biggestFirst) {
        List<BaseThemeGenerator.AddedEntity> result = new ArrayList<BaseThemeGenerator.AddedEntity>();

        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<StarSystemAPI>(StarSystemGenerator.random);
        picker.addAll(c.getSystems());

        boolean first = true;
        for (int i = 0; i < num; i++) {
            StarSystemAPI system = picker.pick();
            if (biggestFirst && first) system = c.getSystemWithMostPlanets();
            first = false;

            if (system == null) continue;

            result.addAll(addToSystem(system, type, 1));
        }

        return result;
    }

    protected List<BaseThemeGenerator.AddedEntity> addToSystem(StarSystemAPI system, String type, int num) {
        List<BaseThemeGenerator.AddedEntity> result = new ArrayList<BaseThemeGenerator.AddedEntity>();
        if (system == null) return result;

        for (int i = 0; i < num; i++) {
            BaseThemeGenerator.AddedEntity e = null;
            if (NIGHTCROSS_RESEARCH_STATION.equals(type)) {
                e = addStargazerStation(system);
            } else if (NIGHTCROSS_RESEARCH_OUTPOST.equals(type)) {
                e = add_NightcrossResearchStation(system);
            } else if (Entities.WRECK.equals(type)) {
                result.addAll(addDerelicts(system, 1));
            }
            if (e != null) {
                result.add(e);
            }
        }
        return result;
    }



    protected BaseThemeGenerator.AddedEntity addStargazerStation(StarSystemAPI system) {
        LinkedHashMap<BaseThemeGenerator.LocationType, Float> weights = new LinkedHashMap<BaseThemeGenerator.LocationType, Float>();
        weights.put(BaseThemeGenerator.LocationType.PLANET_ORBIT, 10f);
        weights.put(BaseThemeGenerator.LocationType.NEAR_STAR, 1f);
        weights.put(BaseThemeGenerator.LocationType.OUTER_SYSTEM, 5f);
        weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_BELT, 10f);
        weights.put(BaseThemeGenerator.LocationType.IN_RING, 10f);
        weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD, 10f);
        weights.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 1f);
        weights.put(BaseThemeGenerator.LocationType.IN_SMALL_NEBULA, 1f);
        weights.put(BaseThemeGenerator.LocationType.L_POINT, 1f);
        WeightedRandomPicker<BaseThemeGenerator.EntityLocation> locs = getLocations(random, system, 100f, weights);

//		if (system.getName().toLowerCase().contains("valac")) {
//			for (int i = 0; i < 10; i++) {
//				//Random random = new Random(32895278947689263L);
//				StarSystemGenerator.random = new Random(32895278947689263L);
//				random = StarSystemGenerator.random;
//				locs = getLocations(random, system, 100f, weights);
//				EntityLocation loc = locs.pickAndRemove();
//				System.out.println("Location: " + loc.toString());
//			}
//		}

        BaseThemeGenerator.AddedEntity entity = addEntity(random, system, locs, NIGHTCROSS_RESEARCH_STATION, Factions.DERELICT);
        if (entity != null) {

            entity.entity.getMemoryWithoutUpdate().set("$musicSetId", "mekaloton_Numbered_Rooms");
            system.addTag(Tags.THEME_INTERESTING);
            system.addTag(NightcrossTags.THEME_NIGHTCROSS);
            system.addTag(NightcrossTags.THEME_NIGHTCROSS_STARGAZERSTATION);
        }

        if (DEBUG) {
            if (entity != null) {
                System.out.println(String.format("  Added mothership to %s", system.getNameWithLowercaseType()));
            } else {
                System.out.println(String.format("  Failed to add mothership to %s", system.getNameWithLowercaseType()));
            }
        }
        return entity;
    }



    protected BaseThemeGenerator.AddedEntity add_NightcrossResearchStation(StarSystemAPI system) {
        LinkedHashMap<BaseThemeGenerator.LocationType, Float> weights = new LinkedHashMap<BaseThemeGenerator.LocationType, Float>();
        weights.put(BaseThemeGenerator.LocationType.PLANET_ORBIT, 10f);
        weights.put(BaseThemeGenerator.LocationType.NEAR_STAR, 1f);
        weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_BELT, 10f);
        weights.put(BaseThemeGenerator.LocationType.IN_RING, 10f);
        weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD, 10f);
        weights.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 1f);
        weights.put(BaseThemeGenerator.LocationType.IN_SMALL_NEBULA, 1f);
        weights.put(BaseThemeGenerator.LocationType.L_POINT, 1f);
        WeightedRandomPicker<BaseThemeGenerator.EntityLocation> locs = getLocations(random, system, 100f, weights);

        BaseThemeGenerator.AddedEntity entity = addEntity(random, system, locs, NIGHTCROSS_RESEARCH_OUTPOST, Factions.DERELICT);

        if (entity != null) {
            system.addTag(Tags.THEME_INTERESTING);
            system.addTag(NightcrossTags.THEME_NIGHTCROSS);
            system.addTag(NightcrossTags.THEME_NIGHTCROSS_STATION);
            entity.entity.getMemoryWithoutUpdate().set("$musicSetId", "mekaloton_Quarters_and_Seconds");
        }

        if (DEBUG) {
            if (entity != null) {
                System.out.println(String.format("  Added survey ship to %s", system.getNameWithLowercaseType()));
            } else {
                System.out.println(String.format("  Failed to add survey ship to %s", system.getNameWithLowercaseType()));
            }
        }
        return entity;
    }

    public AddedEntity addWreckedShip(StarSystemAPI system, EntityLocation loc, NA_DerelictType type, ShipRecoverySpecial.ShipCondition condition,
                                                       String shipName, Random random, boolean pruneWeapons) {
        ShipRecoverySpecial.PerShipData psd = new ShipRecoverySpecial.PerShipData(type.id, condition, 0f);
        if (shipName != null) {
            psd.shipName = shipName;
            psd.nameAlwaysKnown = true;
            psd.pruneWeapons = pruneWeapons;
        }
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(psd, true);

        CustomCampaignEntityAPI ship = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                random, system, Entities.WRECK, Factions.NEUTRAL, params);

        if (loc.orbit != null) {
            ship.setOrbit(loc.orbit);
            loc.orbit.setEntity(ship);
        } else {
            ship.setOrbit(null);
            ship.getLocation().set(loc.location);
        }

        //SalvageSpecialAssigner.assignSpecials(ship, false, data.random);
        if (!type.recoverable)
            ship.addTag(Tags.UNRECOVERABLE);

        ship.setDiscoverable(true);

        return new AddedEntity(ship, loc, ship.getCustomEntityType());
    }

    protected static class NA_DerelictType {
        public String id;
        public boolean recoverable;
        public NA_DerelictType(String id, boolean recoverable) {
            this.id = id;
            this.recoverable = recoverable;
        }
    }

    public static WeightedRandomPicker<NA_DerelictType> derelictShipTypes = new WeightedRandomPicker<NA_DerelictType>();
    static {
        derelictShipTypes.add(new NA_DerelictType("na_nammu_support", true), 1.5f);
        derelictShipTypes.add(new NA_DerelictType("na_echo_assault", true), 0.25f);
        derelictShipTypes.add(new NA_DerelictType("na_sop_elite", true), 0.6f);
        derelictShipTypes.add(new NA_DerelictType("na_mare_exp", true), 0.5f);
        derelictShipTypes.add(new NA_DerelictType("na_zal_strike", true), 1.0f);
        derelictShipTypes.add(new NA_DerelictType("na_tempus_experimental", true), 0.1f);
        derelictShipTypes.add(new NA_DerelictType("na_fossa_sniper", true), 0.25f);
        derelictShipTypes.add(new NA_DerelictType("na_macula_overdriven", true), 0.3f);
        derelictShipTypes.add(new NA_DerelictType("na_xanthe_standard", true), 0.25f);
        derelictShipTypes.add(new NA_DerelictType("na_kasei_x_assault", true), 0.4f);
        // broken beyond repair
        derelictShipTypes.add(new NA_DerelictType("na_losulci_defense", false), 0.1f);
        derelictShipTypes.add(new NA_DerelictType("naai_sop_corrupted", false), 0.1f);
        derelictShipTypes.add(new NA_DerelictType("naai_mare_corrupted", false), 0.1f);
        derelictShipTypes.add(new NA_DerelictType("naai_macula_corrupted", false), 0.1f);
        derelictShipTypes.add(new NA_DerelictType("naai_nammu_corrupted", false), 0.1f);
    }


    protected List<BaseThemeGenerator.AddedEntity> addDerelicts(StarSystemAPI system, int num) {
        LinkedHashMap<BaseThemeGenerator.LocationType, Float> weights = new LinkedHashMap<BaseThemeGenerator.LocationType, Float>();
        weights.put(BaseThemeGenerator.LocationType.PLANET_ORBIT, 20f);
        weights.put(BaseThemeGenerator.LocationType.JUMP_ORBIT, 10f);
        weights.put(BaseThemeGenerator.LocationType.NEAR_STAR, 10f);
        //weights.put(BaseThemeGenerator.LocationType.OUTER_SYSTEM, 5f);
        weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_BELT, 5f);
        weights.put(BaseThemeGenerator.LocationType.IN_RING, 5f);
        weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD, 5f);
        weights.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 1f);
        weights.put(BaseThemeGenerator.LocationType.IN_SMALL_NEBULA, 1f);
        weights.put(BaseThemeGenerator.LocationType.L_POINT, 1f);
        WeightedRandomPicker<BaseThemeGenerator.EntityLocation> locs = getLocations(random, system, 100f, weights);

        List<BaseThemeGenerator.AddedEntity> result = new ArrayList<BaseThemeGenerator.AddedEntity>();
        for (int i = 0; i < num; i++) {

            BaseThemeGenerator.AddedEntity probe = addWreckedShip(system, locs.pick(), derelictShipTypes.pick(), ShipRecoverySpecial.ShipCondition.BATTERED,
                    null, random, false);
            if (probe != null) {
                result.add(probe);

                system.addTag(Tags.THEME_INTERESTING_MINOR);
                system.addTag(NightcrossTags.THEME_NIGHTCROSS);
                system.addTag(NightcrossTags.THEME_NIGHTCROSS_SHIPS);
            }

            if (DEBUG) {
                if (probe != null) {
                    System.out.println(String.format("  Added probe to %s", system.getNameWithLowercaseType()));
                } else {
                    System.out.println(String.format("  Failed to add probe to %s", system.getNameWithLowercaseType()));
                }
            }
        }
        return result;
    }


    /**
     * Sorted by *descending* distance from sortFrom.
     * @param context
     * @param sortFrom
     * @return
     */
    protected List<Constellation> getSortedAvailableConstellations(ThemeGenContext context, boolean emptyOk, final Vector2f sortFrom, List<Constellation> exclude) {
        List<Constellation> constellations = new ArrayList<Constellation>();
        for (Constellation c : context.constellations) {
            if (context.majorThemes.containsKey(c)) continue;
            if (!emptyOk && constellationIsEmpty(c)) continue;

            constellations.add(c);
        }

        if (exclude != null) {
            constellations.removeAll(exclude);
        }

        Collections.sort(constellations, new Comparator<Constellation>() {
            public int compare(Constellation o1, Constellation o2) {
                float d1 = Misc.getDistance(o1.getLocation(), sortFrom);
                float d2 = Misc.getDistance(o2.getLocation(), sortFrom);
                return (int) Math.signum(d2 - d1);
            }
        });
        return constellations;
    }


    public static boolean constellationIsEmpty(Constellation c) {
        for (StarSystemAPI s : c.getSystems()) {
            if (!systemIsEmpty(s)) return false;
        }
        return true;
    }
    public static boolean systemIsEmpty(StarSystemAPI system) {
        for (PlanetAPI p : system.getPlanets()) {
            if (!p.isStar()) return false;
        }
        //system.getTerrainCopy().isEmpty()
        return true;
    }




    @Override
    public int getOrder() {
        return 350;
    }


}
