package data.scripts.world.nightcross;


import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;
import com.fs.starfarer.api.impl.combat.threat.ThreatFleetBehaviorScript;
import data.scripts.campaign.enc.NA_StargazerBH;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.hullmods.NA_ProjectGhost;
import data.scripts.stardust.NA_StargazerFIDConfig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.CurrentLocationChangedListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.enc.AbyssalRogueStellarObjectEPEC;
import com.fs.starfarer.api.impl.campaign.fleets.DisposableFleetManager;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;



public class NA_StargazerWandererManager extends DisposableFleetManager implements CurrentLocationChangedListener {

    public static int MIN_FLEETS = 1;
    public static int MAX_FLEETS = 6;


    public NA_StargazerWandererManager() {
        Global.getSector().getListenerManager().addListener(this);
    }

    protected Object readResolve() {
        super.readResolve();
        return this;
    }

    @Override
    protected String getSpawnId() {
        return NightcrossID.FACTION_STARGAZER;
    }


    public static WeightedRandomPicker<String> STARGAZER_WANDERER_NAMES = new WeightedRandomPicker<String>();

    static {
        STARGAZER_WANDERER_NAMES.add("Wanderers", 10f);
        STARGAZER_WANDERER_NAMES.add("Travelers", 10f);
        STARGAZER_WANDERER_NAMES.add("Observers", 10f);
        STARGAZER_WANDERER_NAMES.add("Stargazers", 30f);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);


        // want Threat fleets to basically "be there" not gradually spawn in
        if (spawnRateMult > 0) {
            spawnRateMult = 1000f;
        }
    }

    @Override
    protected CampaignFleetAPI spawnFleetImpl() {
        CampaignFleetAPI f = createStargazerFleet(new StargazerFleetParams(), null);

        f.getStats().getDetectedRangeMod().modifyMult("na_stargazer_hidden", 0.5f);

        NA_StargazerBehavior behavior = new NA_StargazerBehavior(f, currSpawnLoc, currSpawnLoc.getStar(), false, true, true);
        behavior.setSeenByPlayer();
        f.addScript(behavior);

        return null;
    }

    @Override
    public void reportCurrentLocationChanged(LocationAPI prev, LocationAPI curr) {
        if (tracker2 != null) {
            tracker2.forceIntervalElapsed();
        }
    }

    @Override
    protected float getExpireDaysPerFleet() {
        return 365f; // don't spawn again for a long time after reaching max
    }

    @Override
    protected int getDesiredNumFleetsForSpawnLocation() {
        String id = currSpawnLoc.getOptionalUniqueId();
        if (id == null) id = currSpawnLoc.getId();

        Random random = new Random(id.hashCode() * 1343890534L);

        float depth = Misc.getAbyssalDepth(currSpawnLoc.getLocation(), true);
        if (depth <= 1f) return 0;

        float maxDepth = AbyssalRogueStellarObjectEPEC.MAX_THREAT_PROB_DEPTH;

        float f = (depth - 1f) / (maxDepth - 1f);
        if (f > 1f) f = 1f;

        int minFleets = 1;
        int maxFleets = MIN_FLEETS +
                Math.round((MAX_FLEETS - MIN_FLEETS) * f);

        return minFleets + random.nextInt(maxFleets - minFleets + 1);
    }

    @Override
    protected boolean withReturnToSourceAssignments() {
        return false;
    }

    public static class StargazerFleetParams extends FleetParamsV3 {

        public String fleetType = FleetTypes.PATROL_SMALL;

        public StargazerFleetParams(MarketAPI source, Vector2f locInHyper, String factionId, Float qualityOverride, String fleetType,
                                    float combatPts, float freighterPts, float tankerPts,
                                    float transportPts, float linerPts,
                                    float utilityPts, float qualityMod) {
            if (source != null) {
                init(source, fleetType, factionId, combatPts, freighterPts, tankerPts, transportPts, linerPts, utilityPts, qualityMod);
                if (factionId != null) {
                    this.factionId = factionId;
                }
                this.qualityOverride = qualityOverride;
                this.locInHyper = locInHyper;
            } else {
                init(locInHyper, NightcrossID.FACTION_STARGAZER, qualityOverride, fleetType,
                        combatPts, freighterPts, tankerPts, transportPts, linerPts, utilityPts, qualityMod);
            }

            this.officerNumberMult = 5f;
        }

        public StargazerFleetParams() {
            this.factionId = NightcrossID.FACTION_STARGAZER;
        }
    }

    public static CampaignFleetAPI createStargazerFleet(StargazerFleetParams params, Random random) {


        CampaignFleetAPI f = FleetFactoryV3.createFleet(params);
        //f.setInflater(DefaultFleetInflater.);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_TYPE, params.fleetType);


        f.getFleetData().setSyncNeeded();
        f.getFleetData().syncIfNeeded();
        f.getFleetData().sort();


        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
            if (curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                f.addDropRandom("na_stargazer_drops_cap", 1);
            } else if (curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CRUISER) {
                f.addDropRandom("na_stargazer_drops_cru", 1);
            } else if (curr.getHullSpec() != null && curr.getHullSpec().getHullSize() != ShipAPI.HullSize.FIGHTER) {
                f.addDropRandom("na_stargazer_drops", 1);
            }

        }
        editStargazerFleetAICores(f, random);

        FactionAPI faction = Global.getSector().getFaction(NightcrossID.FACTION_STARGAZER);
        f.setName(faction.getFleetTypeName(params.fleetType));

        f.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new NA_StargazerFIDConfig());
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, false);
        f.getMemoryWithoutUpdate().set(MemFlags.MAY_GO_INTO_ABYSS, true);


        f.setName(STARGAZER_WANDERER_NAMES.pick());


        return f;
    }

    public static void editStargazerFleetAICores(CampaignFleetAPI f, Random random) {
        if (random == null) random = new Random();


        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            boolean keepPortrait = (curr.isFlagship()) ?
                    Math.random() < 0.75f :
                    Math.random() < 0.25f;

            float chance_matrix = 0;
            float chance_grid = 0.33f;
            float chance_ghost = 1f;

            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                chance_matrix = 0.33f;
                chance_grid = 0.5f;
            }
            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CRUISER) {
                chance_matrix = 0.25f;
                chance_grid = 0.6f;
            }
            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.DESTROYER) {
                chance_matrix = 0.1f;
                chance_grid = 0.33f;
            }
            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.FRIGATE) {
                chance_matrix = 0.05f;
                chance_grid = 25;
            }


            if (random.nextFloat() < chance_matrix) {
                setStargazerAICore(curr, NightcrossID.GHOST_MATRIX_ID, keepPortrait);
            } else if (random.nextFloat() < chance_grid) {
                setStargazerAICore(curr, NightcrossID.GHOST_CORE_ID, keepPortrait);
            } else if (random.nextFloat() < chance_ghost) {
                setStargazerAICore(curr, NightcrossID.GHOST_CORE_ID, keepPortrait);
            }
        }
    }


    public static void setStargazerAICore(FleetMemberAPI curr, String aiCoreID, boolean keepPortrait) {
        switch (aiCoreID) {
            case NightcrossID.GHOST_CORE_ID:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "teto"));
                    curr.getCaptain().setName(new FullName("Teto", "Kasane", FullName.Gender.FEMALE));
                }

                curr.getCaptain().addTag(NA_ProjectGhost.CAPTAIN_TAG);

                curr.getCaptain().getStats().setLevel(6);
                curr.getCaptain().getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE_TETO, 2);

                curr.getCaptain().getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                break;
            case NightcrossID.GHOST_MATRIX_ID:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "stargazermatrix"));
                    curr.getCaptain().setName(new FullName("Stargazer", "Matrix", FullName.Gender.ANY));
                }

                curr.getCaptain().addTag(NA_ProjectGhost.CAPTAIN_TAG);

                curr.getCaptain().getStats().setLevel(7);
                curr.getCaptain().getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE_MATRIX, 2);

                curr.getCaptain().getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                break;
            case NightcrossID.TETO_CORE:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "ghostcore"));
                    curr.getCaptain().setName(new FullName("Ghost", "Core", FullName.Gender.ANY));
                }

                curr.getCaptain().addTag(NA_ProjectGhost.CAPTAIN_TAG);

                curr.getCaptain().getStats().setLevel(5);
                curr.getCaptain().getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE_GHOST, 2);

                curr.getCaptain().getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                break;
        }
    }
}








