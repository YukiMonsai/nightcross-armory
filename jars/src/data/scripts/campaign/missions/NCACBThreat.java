package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats;
import com.fs.starfarer.api.impl.campaign.missions.hub.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.skills.OfficerTraining;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT;
import static com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT_ID;

public class NCACBThreat extends BaseCustomBountyCreator {

    public static float PROB_SMALL_FLEET = 0.5f;

    @Override
    public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
        return super.getFrequency(mission, difficulty) * 0.5f;
    }

    public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
        return " - Unknown Threat";
    }

    public static void addShips(CampaignFleetAPI fleet, int num, String role, Random random) {
        FactionAPI faction = Global.getSector().getFaction(Factions.THREAT);

        FactionAPI.ShipPickParams p = new FactionAPI.ShipPickParams(FactionAPI.ShipPickMode.ALL);
        p.blockFallback = true;
        p.maxFP = 1000000;

        for (int i = 0; i < num; i++) {
            List<ShipRolePick> picks = faction.pickShip(role, p, null, random);
            for (ShipRolePick pick : picks) {
                fleet.getFleetData().addFleetMember(pick.variantId);
            }
        }
    }
    public static CampaignFleetAPI createThreatFleet(DisposableThreatFleetManager.ThreatFleetCreationParams params, Random random) {
        CampaignFleetAPI f = Global.getFactory().createEmptyFleet(Factions.THREAT, "Host", true);
        f.setInflater(null);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_TYPE, params.fleetType);

        addShips(f, params.numFabricators, ShipRoles.THREAT_FABRICATOR, random);
        addShips(f, params.numHives, ShipRoles.THREAT_HIVE, random);
        addShips(f, params.numOverseers, ShipRoles.THREAT_OVERSEER, random);
        addShips(f, params.numCapitals, ShipRoles.COMBAT_CAPITAL, random);
        addShips(f, params.numCruisers, ShipRoles.COMBAT_LARGE, random);
        addShips(f, params.numDestroyers, ShipRoles.COMBAT_MEDIUM, random);
        addShips(f, params.numFrigates, ShipRoles.COMBAT_SMALL, random);

        f.getFleetData().setSyncNeeded();
        f.getFleetData().syncIfNeeded();
        f.getFleetData().sort();

        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
        }

        FactionAPI faction = Global.getSector().getFaction(Factions.THREAT);
        f.setName(faction.getFleetTypeName(params.fleetType));


        f.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new ThreatFIDConfig());
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        f.getMemoryWithoutUpdate().set(MemFlags.MAY_GO_INTO_ABYSS, true);
        f.getDetectedRangeMod().modifyMult(THREAT_DETECTED_RANGE_MULT_ID, 0.5f, "Low emission drives");
        f.getSensorRangeMod().modifyMult(THREAT_DETECTED_RANGE_MULT_ID, 8.0f, "I see you");


        return f;
    }
    public static CampaignFleetAPI createThreatFleet(int numFabricators,
                                                     int minOtherCapitals, int maxOtherCapitals, DisposableThreatFleetManager.FabricatorEscortStrength escorts, Random random) {
        if (random == null) random = Misc.random;

        int minHives = 0;
        int maxHives = 0;
        int minOverseers = 0;
        int maxOverseers = 0;
        int minCruisers = 0;
        int maxCruisers = 0;
        int minDestroyers = 0;
        int maxDestroyers = 0;
        int minFrigates = 0;
        int maxFrigates = 0;

        switch (escorts) {
            case NONE:
                break;
            case LOW:
                minOverseers = 0;
                maxOverseers = 1;
                minDestroyers = 0;
                maxDestroyers = 1;
                minFrigates = 2;
                maxFrigates = 4;
                if (numFabricators <= 0) {
                    minOverseers = 1;
                }
                break;
            case MEDIUM:
                minHives = 0;
                maxHives = 1;
                minOverseers = 1;
                maxOverseers = 1;
                minCruisers = 0;
                maxCruisers = 1;
                minDestroyers = 1;
                maxDestroyers = 2;
                minFrigates = 2;
                maxFrigates = 4;
                if (numFabricators <= 0) {
                    minHives = 1;
                }
                break;
            case HIGH:
                minHives = 2;
                maxHives = 3;
                minOverseers = 2;
                maxOverseers = 2;
                minCruisers = 2;
                maxCruisers = 3;
                minDestroyers = 3;
                maxDestroyers = 6;
                minFrigates = 7;
                maxFrigates = 11;

                break;
            case MAXIMUM:
                minHives = 3;
                maxHives = 4;
                minOverseers = 3;
                maxOverseers = 3;
                minCruisers = 4;
                maxCruisers = 5;
                minDestroyers = 5;
                maxDestroyers = 6;
                minFrigates = 5;
                maxFrigates = 6;
                break;
        }

        DisposableThreatFleetManager.ThreatFleetCreationParams params = new DisposableThreatFleetManager.ThreatFleetCreationParams();
        params.numFabricators = numFabricators;
        params.numHives = minHives + random.nextInt(maxHives - minHives + 1);
        params.numOverseers = minOverseers + random.nextInt(maxOverseers - minOverseers + 1);
        params.numCapitals = minOtherCapitals + random.nextInt(maxOtherCapitals - minOtherCapitals + 1);
        params.numCruisers = minCruisers + random.nextInt(maxCruisers - minCruisers + 1);
        params.numDestroyers = minDestroyers + random.nextInt(maxDestroyers - minDestroyers + 1);
        params.numFrigates = minFrigates + random.nextInt(maxFrigates - minFrigates + 1);

        params.fleetType = FleetTypes.PATROL_SMALL;
        if (numFabricators >= 3 ||
                (numFabricators >= 2 && escorts.ordinal() >= DisposableThreatFleetManager.FabricatorEscortStrength.HIGH.ordinal())) {
            params.fleetType = FleetTypes.PATROL_LARGE;
        } else if (numFabricators >= 2 ||
                (numFabricators >= 1 && escorts.ordinal() >= DisposableThreatFleetManager.FabricatorEscortStrength.HIGH.ordinal())) {
            params.fleetType = FleetTypes.PATROL_MEDIUM;
        }

        return createThreatFleet(params, random);
    }


    public static class CreateFleetAction extends HubMissionWithTriggers.CreateFleetAction implements MissionTrigger.TriggerAction {

        public CreateFleetAction(String type, Vector2f locInHyper,
                                 FleetSize fSize, FleetQuality fQuality, String factionId) {
            super(type, locInHyper, fSize, fQuality, factionId);
            seed = Misc.genRandomSeed();
            params = new FleetParamsV3(locInHyper, factionId, null, type, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
            params.ignoreMarketFleetSizeMult = true;

            this.fSize = fSize;
            this.fQuality = fQuality;

            freighterMult = 0f;
            tankerMult = 0f;
        }

        public void doAction(MissionTrigger.TriggerActionContext context) {
            //Random random = new Random(seed);
            Random random = null;
            if (context.mission != null) {
                random = ((BaseHubMission) context.mission).getGenRandom();
            } else {
                random = Misc.random;
            }
            FactionAPI faction = Global.getSector().getFaction(params.factionId);
            //float maxPoints = faction.getApproximateMaxFPPerFleet(FactionAPI.ShipPickMode.PRIORITY_THEN_ALL);

            int nf = 0;
            int nc = 0;
            int mc = 0;

            params.combatPts = faction.getApproximateMaxFPPerFleet(FactionAPI.ShipPickMode.PRIORITY_THEN_ALL);
            DisposableThreatFleetManager.FabricatorEscortStrength escort = DisposableThreatFleetManager.FabricatorEscortStrength.LOW;

            if (fQuality == FleetQuality.VERY_HIGH)
                escort = DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM;

            if (fSize == FleetSize.MEDIUM) {
                if (random.nextFloat() < 0.5f && fQuality != FleetQuality.VERY_HIGH) {
                    nf = 1;
                } else {
                    nc = 1;
                    mc = 1;
                    if (fQuality == FleetQuality.VERY_HIGH)
                        escort = DisposableThreatFleetManager.FabricatorEscortStrength.HIGH;
                    else
                        escort = DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM;
                }
            } else if (fSize == FleetSize.LARGE) {
                if (random.nextFloat() < 0.5f && fQuality != FleetQuality.VERY_HIGH) {
                    nf = 2;
                    escort = DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM;
                } else {
                    nf = 1;
                    nc = 1;
                    mc = 1;
                    if (fQuality == FleetQuality.VERY_HIGH)
                        escort = DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM;
                    else
                        escort = DisposableThreatFleetManager.FabricatorEscortStrength.HIGH;
                }
            } else if (fSize == FleetSize.VERY_LARGE) {
                if (random.nextFloat() < 0.5f && fQuality != FleetQuality.VERY_HIGH) {
                    nf = 2;
                    escort = DisposableThreatFleetManager.FabricatorEscortStrength.HIGH;
                } else {
                    nf = 1;
                    nc = 1;
                    mc = 2;

                    escort = DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM;
                }
            } else if (fSize == FleetSize.HUGE) {
                if (random.nextFloat() < 0.5f && fQuality != FleetQuality.VERY_HIGH) {
                    nf = 3;
                    escort = DisposableThreatFleetManager.FabricatorEscortStrength.HIGH;
                } else {
                    nf = 2;
                    nf = 1;
                    nc = 1;
                    mc = 2;
                    escort = DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM;
                }
            } else if (fSize == FleetSize.MAXIMUM) {
                nf = 3;
                nc = 2;
                mc = 3;
                escort = DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM;
            }

            context.fleet = createThreatFleet(nf, nc, mc,  escort, random);
            context.fleet.setFacing(random.nextFloat() * 360f);

            if (this.faction != null) {
                context.fleet.setFaction(this.faction, true);
            }

            if (this.nameOverride != null) {
                context.fleet.setName(this.nameOverride);
            }
            if (this.noFactionInName != null && this.noFactionInName) {
                context.fleet.setNoFactionInName(noFactionInName);
            }

            if (removeInflater != null && removeInflater) {
                context.fleet.setInflater(null);
            } else {
                if (context.fleet.getInflater() instanceof DefaultFleetInflater) {
                    DefaultFleetInflater inflater = (DefaultFleetInflater) context.fleet.getInflater();
                    if (inflater.getParams() instanceof DefaultFleetInflaterParams) {
                        DefaultFleetInflaterParams p = (DefaultFleetInflaterParams) inflater.getParams();
                        if (allWeapons != null) {
                            p.allWeapons = allWeapons;
                        }
                        if (shipPickMode != null) {
                            p.mode = shipPickMode;
                        }
                    }
                }
            }

            context.fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_BUSY, true);
            //context.fleet.getMemoryWithoutUpdate().set("$LP_titheAskedFor", true);

            context.allFleets.add(context.fleet);

            if (!context.fleet.hasScriptOfClass(MissionFleetAutoDespawn.class)) {
                context.fleet.addScript(new MissionFleetAutoDespawn(context.mission, context.fleet));
            }

            if (damage != null) {
                FleetFactoryV3.applyDamageToFleet(context.fleet, damage, false, random);
            }

//			if (Factions.PIRATES.equals(params.factionId)) {
//
//			}
        }
    }


    protected StarSystemAPI findSystem(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
        mission.requireSystemNotHasPulsar();
        float sectorWidth = 10000;
        float sectorHeight = 10000;
        try {
            sectorWidth = Global.getSettings().getFloat("sectorWidth");
            sectorHeight = Global.getSettings().getFloat("sectorHeight");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mission.requireSystemInDirectionFrom(new Vector2f(0, 0), 225, 90);
        mission.requireSystemOutsideRangeOf(new Vector2f(0, 0), 27);

        StarSystemAPI system = mission.pickSystem();
        return system;
    }

    protected boolean isAggro() {
        return true;
    }

    @Override
    public CustomBountyData createBounty(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
        CustomBountyData data = new CustomBountyData();
        data.difficulty = difficulty;

        data.system = findSystem(createdAt, mission, difficulty, bountyStage);
        if (data.system == null) return null;

//		FleetSize size = FleetSize.MEDIUM;
//		FleetSize sizeWolfpack = FleetSize.MEDIUM;
//		FleetQuality quality = FleetQuality.VERY_HIGH;
//		FleetQuality qualityWolfpack = FleetQuality.SMOD_3;

        boolean smallFleet = mission.rollProbability(PROB_SMALL_FLEET);

//		smallFleet = true;
//		largeUseCarriers = false;
//		largeUsePhase = true;
//		smallUsePhase = true;

        beginFleet(mission, data);
        if (smallFleet || difficulty <= 4) {
            data.custom1 = true;
            if (difficulty <= 6) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.SMALL, FleetQuality.VERY_HIGH,
                        Factions.THREAT));
                mission.triggerSetFleetMaxShipSize(1);
                mission.triggerSetFleetDoctrineOther(1, 4);
            } else if (difficulty <= 7) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_MEDIUM, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.VERY_HIGH,
                        Factions.THREAT));
                mission.triggerSetFleetMaxShipSize(2);
                mission.triggerSetFleetDoctrineOther(2, 4);
            } else if (difficulty <= 8) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_MEDIUM, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.VERY_HIGH,
                        Factions.THREAT));
                mission.triggerSetFleetMaxShipSize(3);
                mission.triggerSetFleetDoctrineOther(3, 4);
            } else if (difficulty <= 9) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.LARGE, FleetQuality.VERY_HIGH,
                        Factions.THREAT));
                mission.triggerSetFleetDoctrineOther(4, 4);
            } else {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.LARGE, FleetQuality.VERY_HIGH,
                        Factions.THREAT));
                mission.triggerSetFleetDoctrineOther(5, 4);
            }

            mission.triggerSetFleetDoctrineComp(5, 0, 0);

        } else {
            if (difficulty <= 6) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_MEDIUM, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.DEFAULT,
                        Factions.THREAT));
            } else if (difficulty <= 7) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.LARGE, FleetQuality.DEFAULT,
                        Factions.THREAT));
            } else if (difficulty <= 8) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.VERY_LARGE, FleetQuality.DEFAULT,
                        Factions.THREAT));
            } else if (difficulty <= 9) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.VERY_LARGE, FleetQuality.DEFAULT,
                        Factions.THREAT));
            } else {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.HUGE, FleetQuality.DEFAULT,
                        Factions.THREAT));
            }

            mission.triggerSetFleetDoctrineComp(5, 0, 0);

        }


        mission.triggerSetFleetNoCommanderSkills();
        mission.triggerSetFleetFaction(Factions.THREAT);
        if (isAggro()) {
            mission.triggerMakeHostileAndAggressive();
            mission.triggerMakeNoRepImpact();
        }
        mission.triggerFleetAllowLongPursuit();
        mission.triggerDoNotShowFleetDesc();
        mission.triggerFleetSetAllWeapons();

        mission.triggerPickLocationAtInSystemJumpPoint(data.system);
        mission.triggerSpawnFleetAtPickedLocation(null, null);
        //mission.triggerFleetSetPatrolActionText("patrolling");
        mission.triggerOrderFleetPatrol(data.system, true, Tags.GAS_GIANT, Tags.SALVAGEABLE, Tags.PLANET, Tags.OBJECTIVE);

        data.fleet = createFleet(mission, data);
        if (data.fleet == null) return null;

        setRepChangesBasedOnDifficulty(data, difficulty);
        data.baseReward = CBStats.getBaseBounty(difficulty, 1.5f, mission);

        return data;
    }

    @Override
    public void addIntelAssessment(TextPanelAPI text, HubMissionWithBarEvent mission, CustomBountyData data) {
        float opad = 10f;
        int max = 7;
        int cols = 7;
        float iconSize = 440 / cols;
        Color h = Misc.getHighlightColor();


        TooltipMakerAPI info = text.beginTooltip();
        info.setParaSmallInsignia();
        info.addPara(Misc.ucFirst(mission.getPerson().getHeOrShe()) + " taps a data pad, and " +
                "a sample of the anomalous energy readings shows up on your tripad.", 0f);

        if (data.difficulty >= 9) {
            info.addPara("\"Based on the readings, we'd expect several capital ships. Be careful out there.\"", opad, h, "several capital ships");
        } else if (data.difficulty >= 6) {
            info.addPara("\"Based on the readings, we'd expect at least one capital class vessel. It's best to be prepared.\"", opad, h, "capital class vessel");
        } else {
            info.addPara("\"Based on the readings, we'd expect a relatively small fleet. Don't let your guard down, though.\"", opad, h, "relatively small");
        }
        text.addTooltip();
        return;
    }




    @Override
    public void addFleetDescription(TooltipMakerAPI info, float width, float height, HubMissionWithBarEvent mission, CustomBountyData data) {
//		if (hideoutLocation != null) {
//			SectorEntityToken fake = hideoutLocation.getContainingLocation().createToken(0, 0);
//			fake.setOrbit(Global.getFactory().createCircularOrbit(hideoutLocation, 0, 1000, 100));
//
//			String loc = BreadcrumbSpecial.getLocatedString(fake);
//			loc = loc.replaceAll("orbiting", "hiding out near");
//			loc = loc.replaceAll("located in", "hiding out in");
//			String sheIs = "She is";
//			if (person.getGender() == Gender.MALE) sheIs = "He is";
//			info.addPara(sheIs + " rumored to be " + loc + ".", opad);
//		}

        PersonAPI person = data.fleet.getCommander();
        FactionAPI faction = person.getFaction();
        int cols = 7;
        float iconSize = width / cols;
        float opad = 10f;
        Color h = Misc.getHighlightColor();


        List<FleetMemberAPI> members = data.fleet.getFleetData().getMembersListCopy();

        info.addPara("The bounty posting does not contain any info about the threat.", opad);
    }

    @Override
    public void updateInteractionData(HubMissionWithBarEvent mission, CustomBountyData data) {
        String id = mission.getMissionId();
        if (data.custom1 != null) {
            mission.set("$" + id + "_ncathreat", data.difficulty);
            mission.set("$bcb_ncathreat", data.difficulty);
        } else {
            mission.unset("$" + id + "_ncathreat");
            mission.unset("$bcb_ncathreat");
        }
    }

    @Override
    public int getMaxDifficulty() {
        return super.getMaxDifficulty();
    }

    @Override
    public int getMinDifficulty() {
        return 4;
    }

}






