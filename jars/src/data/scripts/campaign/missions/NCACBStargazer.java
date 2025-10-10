package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
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
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.skills.OfficerTraining;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT_ID;

public class NCACBStargazer extends BaseCustomBountyCreator {

    public static float PROB_SMALL_FLEET = 0.5f;

    @Override
    public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
        return super.getFrequency(mission, difficulty) * 0.75f;
    }

    public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
        return " - Unknown";
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

            this.oQuality = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
            this.oNum = HubMissionWithTriggers.OfficerNum.ALL_SHIPS;
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

            context.fleet.getMemoryWithoutUpdate().set("$combatMusicSetId","na_stargazer_battlebounty");
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
        mission.preferSystemUnexplored();

        StarSystemAPI system = mission.pickSystem();
        return system;
    }

    protected boolean isAggro() {
        return false;
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


        beginFleet(mission, data);
        if (smallFleet || difficulty <= 4) {
            data.custom1 = true;
            if (difficulty <= 6) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_SMALL, data.system.getLocation(), FleetSize.SMALL, FleetQuality.VERY_HIGH,
                        NightcrossID.FACTION_STARGAZER));
                mission.triggerSetFleetMaxShipSize(1);
                mission.triggerSetFleetDoctrineOther(1, 4);
            } else if (difficulty <= 7) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_MEDIUM, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.VERY_HIGH,
                        NightcrossID.FACTION_STARGAZER));
                mission.triggerSetFleetMaxShipSize(2);
                mission.triggerSetFleetDoctrineOther(2, 4);
            } else if (difficulty <= 8) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_MEDIUM, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.VERY_HIGH,
                        NightcrossID.FACTION_STARGAZER));
                mission.triggerSetFleetMaxShipSize(3);
                mission.triggerSetFleetDoctrineOther(3, 4);
            } else if (difficulty <= 9) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.LARGE, FleetQuality.VERY_HIGH,
                        NightcrossID.FACTION_STARGAZER));
                mission.triggerSetFleetDoctrineOther(4, 4);
            } else {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.LARGE, FleetQuality.VERY_HIGH,
                        NightcrossID.FACTION_STARGAZER));
                mission.triggerSetFleetDoctrineOther(5, 4);
            }

            mission.triggerSetFleetDoctrineComp(5, 0, 0);

        } else {
            if (difficulty <= 6) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_MEDIUM, data.system.getLocation(), FleetSize.MEDIUM, FleetQuality.DEFAULT,
                        NightcrossID.FACTION_STARGAZER));
            } else if (difficulty <= 7) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.LARGE, FleetQuality.DEFAULT,
                        NightcrossID.FACTION_STARGAZER));
            } else if (difficulty <= 8) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.VERY_LARGE, FleetQuality.DEFAULT,
                        NightcrossID.FACTION_STARGAZER));
            } else if (difficulty <= 9) {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.VERY_LARGE, FleetQuality.DEFAULT,
                        NightcrossID.FACTION_STARGAZER));
            } else {
                mission.triggerCustomAction(new CreateFleetAction(FleetTypes.PATROL_LARGE, data.system.getLocation(), FleetSize.HUGE, FleetQuality.DEFAULT,
                        NightcrossID.FACTION_STARGAZER));
            }

            mission.triggerSetFleetDoctrineComp(5, 0, 0);

        }


        mission.triggerSetFleetNoCommanderSkills();
        mission.triggerSetFleetFaction(NightcrossID.FACTION_STARGAZER);
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
        mission.triggerOrderFleetPatrol(data.system, true, Tags.GAS_GIANT, Tags.SALVAGEABLE, Tags.PLANET, Tags.OBJECTIVE, Tags.STABLE_LOCATION);

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

        info.addPara("The bounty posting does not contain any info about the target.", opad);
    }

    @Override
    public void updateInteractionData(HubMissionWithBarEvent mission, CustomBountyData data) {
        String id = mission.getMissionId();
        if (data.custom1 != null) {
            mission.set("$" + id + "_ncastargazer", data.difficulty);
            mission.set("$bcb_ncastargazer", data.difficulty);
        } else {
            mission.unset("$" + id + "_ncastargazer");
            mission.unset("$bcb_ncastargazer");
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






