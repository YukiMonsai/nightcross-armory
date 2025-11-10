package data.scripts.campaign.fleets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.SDFBase;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import yukimonsai.sicnightcross.scripts.world.NightcrossColonyWatcher;

import java.util.List;


public class NA_SDF_Nightcross extends SDFBase {

    public NA_SDF_Nightcross() {
    }

    @Override
    protected String getFactionId() {
        return Factions.INDEPENDENT;
    }

    protected OfficerManagerEvent.SkillPickPreference getCommanderShipSkillPreference() {
        return SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_NO_DEFENSE;
    }

    @Override
    protected MarketAPI getSourceMarket() {
        return Global.getSector().getEconomy().getMarket("na_elevator");
    }

    @Override
    protected String getDefeatTriggerToUse() {
        return "SDFNightcrossRoyalDefeated";
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        if (getFleet() != null && isRedundant()) {
            if (!Misc.isFleetReturningToDespawn(fleet)) {
                Misc.giveStandardReturnToSourceAssignments(fleet);
            }
        }
    }

    @Override
    public boolean canSpawnFleetNow() {
        MarketAPI source = getSourceMarket();
        if (source == null || source.hasCondition(Conditions.DECIVILIZED)) return false;
        if (!source.getFactionId().equals(getFactionId())) return false;
        return !isRedundant();
    }

    public boolean isRedundant() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        return mem.contains(NightcrossColonyWatcher.MEMKEY_NIGHTCROSS_DECIVILIZED) || (
                mem.contains(NightcrossColonyWatcher.MEMKEY_NIGHTCROSS_COLONIZED) &&
                        !getSourceMarket().getFaction().equals(Factions.INDEPENDENT)
                );
    }

    @Override
    public CampaignFleetAPI spawnFleet() {

        MarketAPI NA_Elevator = getSourceMarket();

        FleetCreatorMission m = new FleetCreatorMission(random);
        m.beginFleet();

        Vector2f loc = NA_Elevator.getLocationInHyperspace();

        m.triggerCreateFleet(FleetSize.LARGE, HubMissionWithTriggers.FleetQuality.SMOD_1, "nightcross", FleetTypes.PATROL_LARGE, loc);



        m.triggerSetFleetSizeFraction(0.9f);

        m.triggerSetFleetOfficers( HubMissionWithTriggers.OfficerNum.ALL_SHIPS, HubMissionWithTriggers.OfficerQuality.HIGHER);
        m.triggerSetFleetDoctrineComp(5, 0, 0);
        m.triggerSetFleetCommander(getPerson());

        m.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1);
        m.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1);
        m.triggerFleetAddCommanderSkill(Skills.CREW_TRAINING, 1);
        m.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1);
        m.triggerFleetAddCommanderSkill(Skills.OFFICER_TRAINING, 1);

        m.triggerSetPatrol();
        m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, NA_Elevator);
        m.triggerSetFleetMemoryValue("$SDFNightcrossRoyal", true);
        m.triggerFleetSetNoFactionInName();
        m.triggerSetFleetFaction(Factions.INDEPENDENT);
        m.triggerFleetSetName("Nightcross Royal Fleet");
        m.triggerPatrolAllowTransponderOff();
        //m.triggerFleetSetPatrolActionText("patrolling");
        //m.triggerOrderFleetPatrol(NA_Elevator.getStarSystem());
        if (Global.getSector().getEntityById("nightcross") != null)
            m.triggerOrderFleetPatrol(Global.getSector().getEntityById("nightcross"));
        else m.triggerOrderFleetPatrol(NA_Elevator.getPrimaryEntity());

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

        fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
        NA_Elevator.getContainingLocation().addEntity(fleet);
        fleet.setLocation(NA_Elevator.getPrimaryEntity().getLocation().x, NA_Elevator.getPrimaryEntity().getLocation().y);
        fleet.setFacing((float) random.nextFloat() * 360f);


        return fleet;
    }
}





