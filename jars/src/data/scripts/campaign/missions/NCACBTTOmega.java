package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats;
import com.fs.starfarer.api.impl.campaign.missions.hub.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.skills.OfficerTraining;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.enc.NA_StargazerBH;
import exerelin.utilities.NexUtilsFleet;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class NCACBTTOmega extends BaseCustomBountyCreator {

    public static float PROB_SMALL_FLEET = 0.5f;
    public static float PROB_SOME_PHASE_IN_SMALL_FLEET = 0.5f;
    public static float PROB_CARRIER_BASED_LARGE_FLEET = 0.33f;
    public static float PROB_SOME_PHASE_IN_LARGE_FLEET = 0.5f;


    public static WeightedRandomPicker<String> FlagshipPicker = new WeightedRandomPicker<String>();
    static {
        FlagshipPicker.add("fury_na_omega", 3f);
        FlagshipPicker.add("aurora_na_omega", 3f);
        FlagshipPicker.add("apogee_na_omega", 3f);
        FlagshipPicker.add("paragon_na_omega", 1f);
        FlagshipPicker.add("odyssey_na_omega", 1f);
        FlagshipPicker.add("anubis_na_omega", 2f);
        FlagshipPicker.add("anubis_na_omega2", 1f);
    }

    @Override
    public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
        return super.getFrequency(mission, difficulty) * CBStats.MERC_FREQ;
    }

    public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
        return " - Tri-Tachyon Testbed";
    }

    protected StarSystemAPI findSystem(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
//		mission.requireSystemTags(ReqMode.ANY, Tags.THEME_RUINS, Tags.THEME_MISC, Tags.THEME_REMNANT_SECONDARY,
//								  Tags.THEME_DERELICT, Tags.THEME_REMNANT_DESTROYED);
        mission.requireSystemInterestingAndNotUnsafeOrCore();
        mission.requireSystemNotHasPulsar();

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
        boolean smallUsePhase = mission.rollProbability(PROB_SOME_PHASE_IN_SMALL_FLEET);
        boolean largeUsePhase = mission.rollProbability(PROB_SOME_PHASE_IN_LARGE_FLEET);
        boolean largeUseCarriers = mission.rollProbability(PROB_CARRIER_BASED_LARGE_FLEET);

//		smallFleet = true;
//		largeUseCarriers = false;
//		largeUsePhase = true;
//		smallUsePhase = true;

        beginFleet(mission, data);
        if (smallFleet) {
            data.custom1 = true;
            if (difficulty <= 6) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.SMALL, FleetQuality.SMOD_3,
                        Factions.TRITACHYON));
                mission.triggerSetFleetMaxShipSize(1);
                mission.triggerSetFleetDoctrineOther(1, 4);
            } else if (difficulty <= 7) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.SMOD_3,
                        Factions.TRITACHYON));
                mission.triggerSetFleetMaxShipSize(2);
                mission.triggerSetFleetDoctrineOther(2, 4);
            } else if (difficulty <= 8) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.SMOD_3,
                        Factions.TRITACHYON));
                mission.triggerSetFleetMaxShipSize(3);
                mission.triggerSetFleetDoctrineOther(3, 4);
            } else if (difficulty <= 9) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.LARGE, FleetQuality.SMOD_3,
                        Factions.TRITACHYON));
                mission.triggerSetFleetDoctrineOther(4, 4);
            } else if (difficulty <= 10) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.LARGE, FleetQuality.SMOD_3,
                        Factions.TRITACHYON));
                mission.triggerSetFleetDoctrineOther(5, 4);
            }

            mission.triggerSetFleetMaxNumShips(12);

            if (smallUsePhase) {
                if (difficulty <= 8) {
                    mission.triggerSetFleetDoctrineComp(0, 0, 5);
                } else {
                    mission.triggerSetFleetDoctrineComp(4, 0, 3);
                }
            } else {
                mission.triggerSetFleetDoctrineComp(5, 0, 0);
            }

            mission.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1);
            mission.triggerFleetAddCommanderSkill(Skills.WOLFPACK_TACTICS, 1);
            mission.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1);
            mission.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1);
            mission.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1);
            mission.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.MORE, HubMissionWithTriggers.OfficerQuality.UNUSUALLY_HIGH);
        } else {
            if (difficulty <= 6) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.VERY_HIGH,
                        Factions.TRITACHYON));
            } else if (difficulty <= 7) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_MEDIUM, data.system.getLocation(), FleetSize.LARGE, FleetQuality.VERY_HIGH,
                        Factions.TRITACHYON));
            } else if (difficulty <= 8) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.VERY_LARGE, FleetQuality.VERY_HIGH,
                        Factions.TRITACHYON));
            } else if (difficulty <= 9) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.VERY_LARGE, FleetQuality.SMOD_1,
                        Factions.TRITACHYON));
            } else if (difficulty <= 10) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.HUGE, FleetQuality.SMOD_2,
                        Factions.TRITACHYON));
            }

            if (largeUseCarriers) {
                mission.triggerSetFleetDoctrineComp(3, 4, 0);
                mission.triggerFleetAddCommanderSkill(Skills.CARRIER_GROUP, 1);
                mission.triggerFleetAddCommanderSkill(Skills.FIGHTER_UPLINK, 1);
            } else {
                if (largeUsePhase) {
                    mission.triggerSetFleetDoctrineComp(3, 0, 2);
                    mission.triggerFleetAddCommanderSkill(Skills.PHASE_CORPS, 1);
                    mission.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1);
                } else {
                    mission.triggerSetFleetDoctrineComp(5, 0, 0);
                    mission.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1);
                    mission.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1);
                }
            }

            mission.triggerSetFleetDoctrineOther(3, 3);
            mission.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.MORE, HubMissionWithTriggers.OfficerQuality.HIGHER);
        }


        mission.triggerSetFleetNoCommanderSkills();
        mission.triggerFleetAddCommanderSkill(Skills.CREW_TRAINING, 1);
        mission.triggerSetFleetFaction(Factions.TRITACHYON);
        if (isAggro()) {
            mission.triggerMakeHostileAndAggressive();
            mission.triggerMakeNoRepImpact();
        }
        mission.triggerFleetAllowLongPursuit();
        mission.triggerDoNotShowFleetDesc();
        mission.triggerFleetSetAllWeapons();
        mission.setTimeLimit(BaseCustomBounty.Stage.FAILED, 120f, data.system);

        mission.triggerPickLocationAtInSystemJumpPoint(data.system);
        mission.triggerSpawnFleetAtPickedLocation(null, null);
        //mission.triggerFleetSetPatrolActionText("patrolling");
        mission.triggerOrderFleetPatrol(data.system, true, Tags.JUMP_POINT, Tags.SALVAGEABLE, Tags.PLANET, Tags.OBJECTIVE);

        data.fleet = createFleet(mission, data);
        if (data.fleet == null) return null;

        setRepChangesBasedOnDifficulty(data, difficulty);
        data.baseReward = CBStats.getBaseBounty(difficulty, CBStats.REMNANT_MULT + 0.25f, mission);

        return data;
    }

    @Override
    public void updateInteractionData(HubMissionWithBarEvent mission, CustomBountyData data) {
        String id = mission.getMissionId();
        if (data.custom1 != null) {
            mission.set("$" + id + "_ncattomega", data.difficulty);
            mission.set("$bcb_ncattomega", data.difficulty);
        } else {
            mission.unset("$" + id + "_ncattomega");
            mission.unset("$bcb_ncattomega");
        }
    }

    @Override
    public int getMaxDifficulty() {
        return super.getMaxDifficulty();
    }

    @Override
    public int getMinDifficulty() {
        return 5;
    }



    public static class CreateFleetAction extends HubMissionWithTriggers.CreateFleetAction implements MissionTrigger.TriggerAction {



        public CreateFleetAction(String type, Vector2f locInHyper,
                                 FleetSize fSize, FleetQuality fQuality, String factionId) {
            super(type, locInHyper, fSize, fQuality, factionId);
        }

        public void doAction(MissionTrigger.TriggerActionContext context) {
            //Random random = new Random(seed);
            Random random = null;
            if (context.mission != null) {
                random = ((BaseHubMission)context.mission).getGenRandom();
            } else {
                random = Misc.random;
            }
            FactionAPI faction = Global.getSector().getFaction(params.factionId);
            float maxPoints = faction.getApproximateMaxFPPerFleet(FactionAPI.ShipPickMode.PRIORITY_THEN_ALL);

            //strength = FleetStrength.MAXIMUM;

            //float fraction = fSize.maxFPFraction * (0.9f + random.nextFloat() * 0.2f);
            float min = fSize.maxFPFraction - (fSize.maxFPFraction - fSize.prev().maxFPFraction) / 2f;
            float max = fSize.maxFPFraction + (fSize.next().maxFPFraction - fSize.maxFPFraction) / 2f;
            float fraction = min + (max - min) * random.nextFloat();

            float excess = 0;

            if (fSizeOverride != null) {
                fraction = fSizeOverride * (0.95f + random.nextFloat() * 0.1f);
            }
            else {
                int numShipsDoctrine = 1;
                if (params.doctrineOverride != null) numShipsDoctrine = params.doctrineOverride.getNumShips();
                else if (faction != null) numShipsDoctrine = faction.getDoctrine().getNumShips();
                float doctrineMult = FleetFactoryV3.getDoctrineNumShipsMult(numShipsDoctrine);
                fraction *= 0.75f * doctrineMult;
                if (fraction > FleetSize.MAXIMUM.maxFPFraction) {
                    excess = fraction - FleetSize.MAXIMUM.maxFPFraction;
                    fraction = FleetSize.MAXIMUM.maxFPFraction;
                }
            }

            //fraction = 1f;

            float combatPoints = fraction * maxPoints;
            if (combatFleetPointsOverride != null) {
                combatPoints = combatFleetPointsOverride;
            }

            FactionDoctrineAPI doctrine = params.doctrineOverride;
            if (excess > 0) {
                if (doctrine == null) {
                    doctrine = faction.getDoctrine().clone();
                }
                int added = (int)Math.round(excess / 0.1f);
                if (added > 0) {
                    doctrine.setOfficerQuality(Math.min(5, doctrine.getOfficerQuality() + added));
                    doctrine.setShipQuality(doctrine.getShipQuality() + added);
                }
            }
//			if (fraction > 0.5f && false) {
//				if (doctrine == null) {
//					doctrine = faction.getDoctrine().clone();
//				}
//				int added = (int)Math.round((fraction - 0.5f) / 0.1f);
//				if (added > 0) {
//					doctrine.setNumShips(Math.max(1, doctrine.getNumShips() - added));
//					doctrine.setOfficerQuality(Math.min(5, doctrine.getOfficerQuality() + added));
//					doctrine.setShipQuality(doctrine.getShipQuality() + added);
//				}
//			}

            if (freighterMult == null) freighterMult = 0.05f;
            if (tankerMult == null) tankerMult = 0.05f;
            if (linerMult == null) linerMult = 0f;
            if (transportMult == null) transportMult = 0.1f;
            if (utilityMult == null) utilityMult = 0.05f;
            if (qualityMod == null) qualityMod = 0f;

            params.combatPts = combatPoints;
            params.freighterPts = combatPoints * freighterMult;
            params.tankerPts = combatPoints * tankerMult;
            params.transportPts = combatPoints * transportMult;
            params.linerPts = combatPoints * linerMult;
            params.utilityPts = combatPoints * utilityMult;
            params.qualityMod = qualityMod;


//			if (damage != null && damage > 0) {
//				if (damage > 1) damage = 1f;
//				float mult1 = 1f - damage;
//				float mult2 = 1f - damage * 0.5f;
//				params.combatPts *= mult1;
//				params.freighterPts *= mult2;
//				params.tankerPts *= mult2;
//				params.transportPts *= mult2;
//				params.linerPts *= mult2;
//				params.utilityPts *= mult2;
//			}

            //params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL;
            params.doctrineOverride = doctrine;
            params.random = random;


            if (fQuality != null) {
                switch (fQuality) {
                    case VERY_LOW:
                        if (fQualityMod != null) {
                            params.qualityMod += fQuality.qualityMod;
                        } else {
                            params.qualityOverride = 0f;
                        }
                        break;
                    case LOWER:
                        params.qualityMod += fQuality.qualityMod;
                        break;
                    case DEFAULT:
                        params.qualityMod += fQuality.qualityMod;
                        break;
                    case HIGHER:
                        params.qualityMod += fQuality.qualityMod;
                        break;
                    case VERY_HIGH:
                        if (fQualityMod != null) {
                            params.qualityMod += fQuality.qualityMod;
                        } else {
                            params.qualityMod += fQuality.qualityMod;
                            //params.qualityOverride = 1f;
                        }
                        break;
                    case SMOD_1:
                        params.qualityMod += fQuality.qualityMod;
                        params.averageSMods = fQuality.numSMods;
                        break;
                    case SMOD_2:
                        params.qualityMod += fQuality.qualityMod;
                        params.averageSMods = fQuality.numSMods;
                        break;
                    case SMOD_3:
                        params.qualityMod += fQuality.qualityMod;
                        params.averageSMods = fQuality.numSMods;
                        break;
                }
            }
            if (fQualityMod != null) {
                params.qualityMod += fQualityMod;
            }
            if (fQualitySMods != null) {
                params.averageSMods = fQualitySMods;
            }

            if (oNum != null) {
                switch (oNum) {
                    case NONE:
                        params.withOfficers = false;
                        break;
                    case FC_ONLY:
                        params.officerNumberMult = 0f;
                        break;
                    case FEWER:
                        params.officerNumberMult = 0.5f;
                        break;
                    case DEFAULT:
                        break;
                    case MORE:
                        params.officerNumberMult = 1.5f;
                        break;
                    case ALL_SHIPS:
                        params.officerNumberBonus = Global.getSettings().getInt("maxShipsInAIFleet");
                        break;
                }
            }

            if (oQuality != null) {
                switch (oQuality) {
                    case LOWER:
                        params.officerLevelBonus = -3;
                        params.officerLevelLimit = Global.getSettings().getInt("officerMaxLevel") - 1;
                        params.commanderLevelLimit = Global.getSettings().getInt("maxAIFleetCommanderLevel") - 2;
                        if (params.commanderLevelLimit < params.officerLevelLimit) {
                            params.commanderLevelLimit = params.officerLevelLimit;
                        }
                        break;
                    case DEFAULT:
                        break;
                    case HIGHER:
                        params.officerLevelBonus = 2;
                        params.officerLevelLimit = Global.getSettings().getInt("officerMaxLevel") + (int) OfficerTraining.MAX_LEVEL_BONUS;
                        break;
                    case UNUSUALLY_HIGH:
                        params.officerLevelBonus = 4;
                        params.officerLevelLimit = SalvageSpecialAssigner.EXCEPTIONAL_PODS_OFFICER_LEVEL;
                        break;
                    case AI_GAMMA:
                    case AI_BETA:
                    case AI_BETA_OR_GAMMA:
                    case AI_ALPHA:
                    case AI_MIXED:
                    case AI_OMEGA:
                        params.aiCores = oQuality;
                        break;
                }
                if (doNotIntegrateAICores != null) {
                    params.doNotIntegrateAICores = doNotIntegrateAICores;
                }
            }

            if (shipPickMode != null) {
                params.modeOverride = shipPickMode;
            }

            params.updateQualityAndProducerFromSourceMarket();
            if (qualityOverride != null) {
                params.qualityOverride = qualityOverride + params.qualityMod;;
            }

            context.fleet = FleetFactoryV3.createFleet(params);
            context.fleet.setFacing(random.nextFloat() * 360f);


            float combatPointsRemnant = combatPoints * 0.5f;
            for (int i = 0; i < 100 && combatPointsRemnant > 0; i++) {


                String size = ShipRoles.COMBAT_LARGE;
                float sizeMod = MathUtils.getRandomNumberInRange(0f, 1f);
                if (sizeMod < 0.4f) size = ShipRoles.COMBAT_SMALL;
                else if (sizeMod < 0.7f) size = ShipRoles.COMBAT_MEDIUM;

                String vid = pickRemnantVariant(Factions.REMNANTS, size, random);
                ShipVariantAPI var = Global.getSettings().getVariant(vid);
                if (var != null) {
                    int fp = var.getHullSpec().getFleetPoints();
                    if (combatPointsRemnant >= fp) {
                        combatPointsRemnant -= fp;
                        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, var);
                        member.setCaptain(Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, Factions.REMNANTS, random));
                        context.fleet.getFleetData().addFleetMember(member);
                    } else {
                        combatPointsRemnant--;
                    }
                }
            }

            String vid = FlagshipPicker.pick();
            ShipVariantAPI var = Global.getSettings().getVariant(vid).clone();

            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, var);

            var.addTag(Tags.NO_BATTLE_SALVAGE);
            var.setSource(VariantSource.REFIT);
            var.addTag(Tags.TAG_NO_AUTOFIT
            );


            member.setVariant(member.getVariant().clone(), false, false);
            context.fleet.getFleetData().addFleetMember(member);
            context.fleet.getFleetData().setFlagship(member);
            member.setCaptain(context.fleet.getCommander());



            context.fleet.getFleetData().sort();
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
        public String pickRemnantVariant(String factionId, String shipRole, Random random) {
            FactionAPI.ShipPickParams params = new FactionAPI.ShipPickParams(FactionAPI.ShipPickMode.ALL);
            List<ShipRolePick> picks = Global.getSector().getFaction(factionId).pickShip(shipRole, params, null, random);
            if (picks == null || picks.isEmpty()) return null;
            return picks.get(0).variantId;

        }
    }



}






