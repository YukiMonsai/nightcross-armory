package data.scripts.campaign.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.fleets.SDFBase;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.world.nightcross.NA_BlackcatGen;
import data.scripts.world.nightcross.NA_StargazerFleets;
import org.lwjgl.util.vector.Vector2f;
import yukimonsai.sicnightcross.scripts.world.NightcrossColonyWatcher;

import java.util.List;


public class NA_Stargazer_Lunar_Fleet extends SDFBase {

    public NA_Stargazer_Lunar_Fleet() {
    }

    @Override
    protected String getFactionId() {
        return NightcrossID.FACTION_STARGAZER;
    }

    protected SkillPickPreference getCommanderShipSkillPreference() {
        return SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_NO_DEFENSE;
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0 || isDone()) return;

        if (fleet != null && !fleet.isAlive()) {
            fleet = null;
        }

        if (fleet == null) {
            float days = Global.getSector().getClock().convertToDays(amount);
            currDelay -= days;
            if (currDelay <= 0f) {
                currDelay = 0f;

                if (shouldScriptBeRemoved() || getPerson() == null) {
                    done = true;
                    return;
                }

                if (canSpawnFleetNow()) {
                    fleet = spawnFleet();
                    if (fleet != null) {
                        origFP = fleet.getFleetPoints();
                        fleet.addEventListener(this);
                        if (defeatTrigger != null) {
                            Misc.addDefeatTrigger(fleet, defeatTrigger);
                        }
                    }
                }
                if (fleet == null) {
                    currDelay = minFailedSpawnRespawnDelayDays +
                            (maxFailedSpawnRespawnDelayDays - minFailedSpawnRespawnDelayDays) * random.nextFloat();
                }
            }
        }


        if (getFleet() != null && isRedundant()) {
            if (!Misc.isFleetReturningToDespawn(fleet)) {
                Misc.giveStandardReturnToSourceAssignments(fleet);
            }
        }
    }


    @Override
    protected MarketAPI getSourceMarket() {
        return Global.getSector().getEconomy().getMarket("na_researchbase");
    }

    @Override
    protected String getDefeatTriggerToUse() {
        return "SDFStargazerLunarFleetDefeated";
    }


    @Override
    public boolean canSpawnFleetNow() {
        return !isRedundant() && NA_BlackcatGen.blackcatstation != null && NA_BlackcatGen.lunargravitywell != null;
    }

    public boolean isRedundant() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        return mem.contains("SDFStargazerLunarFleetDefeated");
    }

    @Override
    public CampaignFleetAPI spawnFleet() {

        SectorEntityToken NA_Elevator = NA_BlackcatGen.blackcatstation;

        FleetCreatorMission m = new FleetCreatorMission(random);
        m.beginFleet();

        Vector2f loc = NA_Elevator.getLocationInHyperspace();

        m.triggerCreateFleet(FleetSize.HUGE, FleetQuality.SMOD_3, NightcrossID.FACTION_STARGAZER, FleetTypes.PATROL_LARGE, loc);



        m.triggerSetFleetSizeFraction(0.9f);

        m.triggerSetFleetOfficers( OfficerNum.ALL_SHIPS, OfficerQuality.UNUSUALLY_HIGH);
        m.triggerSetFleetDoctrineComp(4, 1, 0);
        m.triggerSetFleetCommander(getPerson());

        m.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1);
        m.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1);
        m.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1);
        m.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1);
        m.triggerFleetAddCommanderSkill(Skills.WOLFPACK_TACTICS, 1);
        m.triggerFleetAddCommanderSkill(Skills.CREW_TRAINING, 1);
        m.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1);
        m.triggerFleetAddCommanderSkill(Skills.OFFICER_TRAINING, 1);

        m.triggerSetPatrol();
        //m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, NA_Elevator);
        m.triggerSetFleetMemoryValue("$SDFStargazerLunarFleet", true);
        m.triggerFleetSetNoFactionInName();
        m.triggerSetFleetFaction(Factions.INDEPENDENT);
        m.triggerFleetSetName("Existential Threat");
        m.triggerPatrolAllowTransponderOff();
        m.triggerFleetSetPatrolActionText("Holding open the gate to freedom");
        m.triggerOrderFleetPatrol(NA_BlackcatGen.lunargravitywell);



        CampaignFleetAPI fleet = m.createFleet();

        FactionAPI faction = Global.getSector().getFaction(Factions.INDEPENDENT);

        FactionAPI.ShipPickParams p = new FactionAPI.ShipPickParams(FactionAPI.ShipPickMode.PRIORITY_THEN_ALL);
        p.blockFallback = true;
        p.maxFP = (int) (fleet.getFleetPoints() * 0.8f);

        for (int i = 0; i < 9; i++) {
            List<ShipRolePick> picks = faction.pickShip(ShipRoles.COMBAT_MEDIUM, p, null, random);
            for (ShipRolePick pick : picks) {
                fleet.getFleetData().addFleetMember(pick.variantId);
            }
        }
        for (int i = 0; i < 6; i++) {
            List<ShipRolePick> picks = faction.pickShip(ShipRoles.COMBAT_LARGE, p, null, random);
            for (ShipRolePick pick : picks) {
                fleet.getFleetData().addFleetMember(pick.variantId);
            }
        }


        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getFleetData().sort();

        fleet.getMemoryWithoutUpdate().set("$combatMusicSetId","na_silence_dummy");
        fleet.getMemoryWithoutUpdate().set("$na_customCombatMusic1","na_shootthemoon");
        fleet.getMemoryWithoutUpdate().set("$na_customMusicPhases",1);

        fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
        NA_Elevator.getContainingLocation().addEntity(fleet);
        fleet.setLocation(NA_Elevator.getLocation().x, NA_Elevator.getLocation().y);
        fleet.setFacing((float) random.nextFloat() * 360f);

        NA_StargazerFleets.modifyStargazerFleet(fleet, random);


        return fleet;
    }
}





