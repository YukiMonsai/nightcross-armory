package data.scripts.campaign.enc;

import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.enc.*;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPluginImpl;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;

public class NA_StargazerBH extends AbyssalRogueStellarObjectEPEC {

    public static final float DEPTH_THRESHOLD_FOR_ABYSSAL_STARGAZER_ENC = 0.4f;
    public static final float STARGAZER_FREQ = 1f;
    public static final float STARGAZER_BASE_POWER = 5f;

    public static enum StargazerBHType {
        LONE_SHIP,
        GROUP,
        NAMED,
        VICTIM,
        BATTLE,
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
        STARGAZER_TYPES.add(StargazerBHType.NAMED, 10f);
        STARGAZER_TYPES.add(StargazerBHType.VICTIM, 5f);
        STARGAZER_TYPES.add(StargazerBHType.BATTLE, 8f);
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

        if (!isPointSuited(point, true, DEPTH_THRESHOLD_FOR_ABYSSAL_STARGAZER_ENC)) {
            return 0f;
        }
        if (DebugFlags.ABYSSAL_GHOST_SHIPS_DEBUG) {
            return 1000000000f;
        }
        return STARGAZER_FREQ;
    }


    @Override
    protected void addSpecials(StarSystemAPI system, EncounterManager manager, EncounterPoint point, HyperspaceAbyssPluginImpl.AbyssalEPData data) {
        WeightedRandomPicker<StargazerBHType> picker = new WeightedRandomPicker<StargazerBHType>(data.random);
        picker.addAll(STARGAZER_TYPES);

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

//			type = AbyssalDireHintType.MINING_OP;
//			type = AbyssalDireHintType.GAS_GIANT_TURBULENCE;
//			type = AbyssalDireHintType.BLACK_HOLE_READINGS;
            if (type == StargazerBHType.LONE_SHIP) {
                LoneShipType ghostType = ghostShipPicker.pickAndRemove();
                if (ghostType != null) {
                    done = addGhostShip(system, ghostType, point, data, null);
                }
            } else if (type == StargazerBHType.NAMED) {
                LoneShipType ghostType = namedShipPicker.pickAndRemove();
                if (ghostType != null) {
                    done = addGhostShip(system, ghostType, point, data, NamedShipname.get(ghostType));
                }
            } else if (type == StargazerBHType.VICTIM) {
                done = addVictim(system, STARGAZER_VICTIM_TYPES_LONE.pick(), point, data);
            } else if (type == StargazerBHType.GROUP) {
                int count = MathUtils.getRandomNumberInRange(2, 4);
                for (int i = 0; i < count; i++) {
                    LoneShipType ghostType = ghostShipPicker.pick();
                    if (ghostType != null) {
                        done = addGhostShip(system, ghostType, point, data, null);
                    }
                }
            } else if (type == StargazerBHType.BATTLE) {
                VictimType vt = STARGAZER_VICTIM_TYPES.pick();
                int count = MathUtils.getRandomNumberInRange(1, (int) Math.max(1, 2.5f* VictimTypePower.get(vt)/STARGAZER_BASE_POWER));
                for (int i = 0; i < count; i++) {
                    LoneShipType ghostType = ghostShipPicker.pick();
                    if (ghostType != null) {
                        done = addGhostShip(system, ghostType, point, data, null);
                    }
                }
                int vcount = MathUtils.getRandomNumberInRange(1, (int) Math.max(1, 2.5f* STARGAZER_BASE_POWER/VictimTypePower.get(vt)));
                for (int i = 0; i < vcount; i++) {
                    done = addVictim(system, vt, point, data);
                }


            }
        } while (!picker.isEmpty() && !done);
    }



    protected boolean addGhostShip(StarSystemAPI system,LoneShipType type, EncounterPoint point, HyperspaceAbyssPluginImpl.AbyssalEPData data, String shipname) {
        WeightedRandomPicker<PlanetAPI> picker = new WeightedRandomPicker<PlanetAPI>(data.random);
        for (PlanetAPI planet : system.getPlanets()) {
            picker.add(planet);
        }
        PlanetAPI planet = picker.pick();
        if (planet == null) return false;


        if (type == LoneShipType.TESSERA) {
            String variantId = "naai_tessera_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), shipname, data.random, false);
        } else
        if (type == LoneShipType.TEMPUS) {
            String variantId = "naai_tempus_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.KASEI) {
            String variantId = "naai_kasei_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.FOSSA) {
            String variantId = "naai_fossa_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.MACULA) {
            String variantId = "naai_macula_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.PRISTINE, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.SOP) {
            String variantId = "naai_sop_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.PRISTINE, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.MARE) {
            String variantId = "naai_mare_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.PRISTINE, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.LOSULCI) {
            String variantId = "naai_losulci_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.BATTERED, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.ELYURIAS) {
            String variantId = "naai_elyurias_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.BATTERED, type.name(), shipname, data.random, false);
        }else
        if (type == LoneShipType.NAMMU) {
            String variantId = "naai_nammu_corrupted";
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), shipname, data.random, false);
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
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), data.random, true);
        } else if (type == VictimType.TRITACH) {
            String variantId = pickVariant(Factions.TRITACHYON, size, data.random);
            if (variantId == null) return false;
            CustomCampaignEntityAPI ship = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.AVERAGE, type.name(), data.random, true);
            if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$player.nca_abyss_sgvs_tritach")) {
                addStargazerWeapons(ship, 3);
            }
        } else if (type == VictimType.PIRATE) {
            String variantId = pickVariant(Factions.PIRATES, size, data.random);
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.WRECKED, type.name(), data.random, true);
        } else if (type == VictimType.MERC) {
            String variantId = pickVariant(Factions.MERCENARY, size, data.random);
            if (variantId == null) return false;
            addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.GOOD, type.name(), data.random, true);
        } else if (type == VictimType.THREAT) {
            String variantId = pickVariant(Factions.THREAT, size, data.random);
            if (variantId == null) return false;
            CustomCampaignEntityAPI ship = addShipAroundPlanet(planet, variantId, ShipRecoverySpecial.ShipCondition.BATTERED, type.name(), data.random, true);
            if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$player.nca_abyss_sgvs_threat")) {
                addThreatWeapons(ship, 3);
            }
        }
        Global.getSector().getMemoryWithoutUpdate().set(type.getTimeoutKey(), true, 90f);

        return true;
    }

    public CustomCampaignEntityAPI addShipAroundPlanet(SectorEntityToken planet, String variantId, ShipRecoverySpecial.ShipCondition condition,
                                    String gsType, Random random, boolean pruneWeapons) {
        return this.addShipAroundPlanet(planet, variantId, condition, gsType, null, random, pruneWeapons);
    }
    public CustomCampaignEntityAPI addShipAroundPlanet(SectorEntityToken planet, String variantId, ShipRecoverySpecial.ShipCondition condition,
                                    String gsType, String shipName, Random random, boolean pruneWeapons) {
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
        float orbitRadius = planet.getRadius() + 500f + random.nextFloat() * 100f;
        float orbitDays = orbitRadius / (10f + random.nextFloat() * 5f);
        ship.setCircularOrbit(planet, random.nextFloat() * 360f, orbitRadius, orbitDays);

        ship.setLocation(planet.getLocation().x, planet.getLocation().y);
        ship.getVelocity().set(planet.getVelocity());

        ship.getMemoryWithoutUpdate().set("$gsType", "NA_" + gsType);

        return ship;
    }

    public String pickVariant(String factionId, String shipRole, Random random) {
        FactionAPI.ShipPickParams params = new FactionAPI.ShipPickParams(FactionAPI.ShipPickMode.ALL);
        List<ShipRolePick> picks = Global.getSector().getFaction(factionId).pickShip(shipRole, params, null, random);
        if (picks == null || picks.isEmpty()) return null;
        return picks.get(0).variantId;

    }
}

















