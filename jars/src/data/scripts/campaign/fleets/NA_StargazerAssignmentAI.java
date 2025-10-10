package data.scripts.campaign.fleets;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.missions.hub.TriggerFleetAssignmentAI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.ai.CampaignFleetAI;
import data.scripts.world.nightcross.NA_StargazerGen;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class NA_StargazerAssignmentAI implements EveryFrameScript {

    public SectorEntityToken target = null;
    public StarSystemAPI systemtarget = null;
    public CampaignFleetAPI fleet = null;
    public float offset = 150f;
    public boolean wanderSystem = true;
    public boolean despawn = false;
    public IntervalUtil gazeTime = new IntervalUtil(1f, 13);
    public IntervalUtil systemTime = new IntervalUtil(15f, 45f);
    public NA_StargazerAssignmentAI(CampaignFleetAPI fleet, SectorEntityToken target, StarSystemAPI systemtarget, boolean wanderSystem, boolean despawn) {
        this.fleet = fleet;
        this.target = target;
        this.systemtarget = systemtarget;
        this.offset = MathUtils.getRandomNumberInRange(100f, 250f);
        this.wanderSystem = wanderSystem;
        this.despawn = despawn;
    }

    @Override
    public void advance(float amount) {
        if (!fleet.isAlive()) return;

        float days = Global.getSector().getClock().convertToDays(amount);

        this.gazeTime.advance(days);
        this.systemTime.advance(days);

        if (this.gazeTime.intervalElapsed() || (target instanceof FleetAPI && !target.isVisibleToSensorsOf(fleet))
            || (Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().getBoolean("$na_stargazer_warn")) && (target != Global.getSector().getPlayerFleet()) && Global.getSector().getPlayerFleet() != null &&
                Global.getSector().getPlayerFleet().isVisibleToSensorsOf(fleet)) {
            this.gazeTime.randomize();
            if (fleet.getStarSystem() != null && !fleet.getStarSystem().isHyperspace()) {
                if (Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getStarSystem() != null && Global.getSector().getPlayerFleet().getStarSystem() == fleet.getStarSystem()) {
                    this.target = findNewTarget();
                } else if (MathUtils.getRandomNumberInRange(0, 30f) < 5f) {
                    // save perfoemance
                    this.target = findNewTarget();
                }
            }


        } else if (wanderSystem) {
            if (this.systemTime.intervalElapsed() ) {
                this.systemTime.randomize();
                if (Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getStarSystem() != null && Global.getSector().getPlayerFleet().getStarSystem() == fleet.getStarSystem()) {
                    // do nothing, they wont leave
                } else if (MathUtils.getRandomNumberInRange(0, 30f) < 15f) {
                    // save perfoemance
                    this.target = null;
                    this.systemtarget = findNewSystem();
                }

            }
        } else if (despawn) {
            if (this.systemTime.intervalElapsed() ) {
                this.systemTime.randomize();
                if (Global.getSector().getPlayerFleet() != null && fleet.getStarSystem() != Global.getSector().getPlayerFleet().getStarSystem()) {
                    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, fleet.getStarSystem().getCenter(), 300f, "vanishing");
                }
            }
        }

        if (fleet.isVisibleToSensorsOf(Global.getSector().getPlayerFleet()) && fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE)) {

            fleet.getStats().getDetectedRangeMod().unmodify("na_travelsneak");
        } else
         if (fleet.getStarSystem() != null && !fleet.getStarSystem().isHyperspace() && fleet.getStarSystem() == systemtarget && fleet.getStarSystem().getAllEntities().contains(target)) {
             float dd = Math.max(50f, MathUtils.getDistance(target.getLocation(), fleet.getLocation()));
            float distance = target.getRadius() + 50f;
            if (target instanceof PlanetAPI && ((PlanetAPI) target).getSpec() != null && ((PlanetAPI) target).getSpec().getCoronaSize() > 0)
                distance += ((PlanetAPI) target).getSpec().getCoronaSize() * target.getRadius();

            distance += offset;

            float angle = VectorUtils.getAngle(target.getLocation(), fleet.getLocation());
            Vector2f loc = MathUtils.getPointOnCircumference(target.getLocation(), distance, angle + 15f * Math.min(1f, offset/dd));
            fleet.setMoveDestination(loc.x, loc.y);
            if (!fleet.getMemoryWithoutUpdate().getBoolean("$na_stargazersuspicion") && (!(MathUtils.getDistanceSquared(target.getLocation(), loc) > (distance + 100f) * (distance + 100f))
                && !(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE) && Global.getSector().getPlayerFleet() != null && fleet.isHostileTo(Global.getSector().getPlayerFleet())))) {
                fleet.goSlowOneFrame();
            }
            fleet.getStats().getDetectedRangeMod().unmodify("na_travelsneak");

             if (fleet.getCurrentAssignment() == null || fleet.getCurrentAssignment().getAssignment() != FleetAssignment.PATROL_SYSTEM ||
                fleet.getCurrentAssignment().getTarget() != target) {
                 if (fleet.getCurrentAssignment() != null && fleet.getCurrentAssignment().getAssignment() == FleetAssignment.PATROL_SYSTEM && fleet.getCurrentAssignment().getTarget() != target) {
                     fleet.getCurrentAssignment().expire();
                 }
                 fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, target, 1f);
             }
             if (fleet.getCurrentAssignment() != null && !fleet.getMemoryWithoutUpdate().getBoolean("$na_stargazersuspicion")) {
                 String text = (target instanceof PlanetAPI && ((PlanetAPI) target).isBlackHole()) ? "staring into the abyss" : (
                         (target instanceof PlanetAPI && ((PlanetAPI) target).isStar()) ? "stargazing" : (
                                 (target instanceof PlanetAPI) ? "observing" : "watching"
                         )
                 );
                 fleet.getCurrentAssignment().setActionText(text);
             }
        } else if (wanderSystem && fleet.getStarSystem() != systemtarget) {
             if (fleet.getCurrentAssignment() != null && fleet.getCurrentAssignment().getAssignment() != FleetAssignment.PATROL_SYSTEM)
                 fleet.getCurrentAssignment().expire();
             if (fleet.getCurrentAssignment() == null || fleet.getCurrentAssignment().getAssignment() != FleetAssignment.GO_TO_LOCATION)
                 fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, systemtarget.getStar(), 300f, "wandering");
             else if (fleet.getCurrentAssignment() != null && fleet.getCurrentAssignment().getAssignment() == FleetAssignment.GO_TO_LOCATION) {

                 if (
                 !fleet.getMemoryWithoutUpdate().getBoolean("$na_stargazersuspicion") && (
                         !(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE) && Global.getSector().getPlayerFleet() != null && fleet.isHostileTo(Global.getSector().getPlayerFleet())))) {
                     fleet.goSlowOneFrame();
                     fleet.getStats().getDetectedRangeMod().modifyMult("na_travelsneak", 0.35f);
                 } else {
                     fleet.getStats().getDetectedRangeMod().unmodify("na_travelsneak");
                 }
             }
         }
    }

    public SectorEntityToken findNewTarget() {
        if (fleet.getStarSystem() == null) return null;
        WeightedRandomPicker<SectorEntityToken> interestingObjects = new WeightedRandomPicker<SectorEntityToken>();
        StarSystemAPI system = fleet.getStarSystem();

        // add the stars and planets
        if (system.getStar() != null) interestingObjects.add(system.getStar(), 100f);
        if (system.getSecondary() != null) interestingObjects.add(system.getSecondary(), 50f);
        if (system.getTertiary() != null) interestingObjects.add(system.getTertiary(), 50f);
        for (PlanetAPI planet : system.getPlanets()) {
            interestingObjects.add(planet, 25f);
        }

        // add non-stargazer fleets nearby
        for (CampaignFleetAPI f2 : system.getFleets()) {
            if (f2.isVisibleToSensorsOf(fleet)) {
                if (!fleet.isHostileTo(f2) && fleet.getFaction() != f2.getFaction()) {
                    interestingObjects.add(f2, f2.isPlayerFleet() ? 1000f : 175f);
                }
            }
        }

        return interestingObjects.pick();
    }
    public StarSystemAPI findNewSystem() {
        WeightedRandomPicker<StarSystemAPI> interestingSystems = new WeightedRandomPicker<StarSystemAPI>();


        for (StarSystemAPI system: Global.getSector().getStarSystems()) {
            if ((system).getStar() != null) {
                if (system.getStar().getSpec() != null && (
                        system.getStar().getSpec().isBlackHole()
                        || (system.getStar().getSpec().isStar()
                        && (
                                system.getSecondary() == null // avoid binaries
                                ))
                        )) {
                    if (system.isProcgen() && !system.isDeepSpace()) {
                        boolean filtered = false;
                        for (String tag : NA_StargazerGen.BLACKLISTED_SYSTEM_TAGS) {
                            if (system.getTags().contains(tag)) {
                                filtered = true;
                                break;
                            }
                        }
                        for (String name : NA_StargazerGen.BLACKLISTED_SYSTEMS) {
                            if (system.getName().equals(name)) {
                                filtered = true;
                                break;
                            }
                        }

                        // behavior is go to system => go to black hole => go to system etc
                        if (fleet.getStarSystem() != null && fleet.getStarSystem().getStar() != null && !fleet.getStarSystem().getStar().isBlackHole()) {
                            if (!system.getStar().isBlackHole()) filtered = true;
                        }

                        // only procgen blackholes
                        // weigh closer stars
                        if (!filtered)
                        {
                            float w = 1;
                            if (fleet.getStarSystem() != null && fleet.getStarSystem().isHyperspace()) {
                                w = 100f / (100f + MathUtils.getDistance(fleet.getLocation(), system.getLocation()));
                            } else if (fleet.getStarSystem() != null) {
                                w = 100f / (100f + MathUtils.getDistance(fleet.getStarSystem().getLocation(), system.getLocation()));
                            }
                            interestingSystems.add(system, w);
                        }
                    }
                }
            }
        }




        return interestingSystems.pick();
    }

    @Override
    public boolean isDone() {
        return !fleet.isAlive();
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }


}



