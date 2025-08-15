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
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class NA_StargazerAssignmentAI implements EveryFrameScript {

    public SectorEntityToken target = null;
    public CampaignFleetAPI fleet = null;
    public float offset = 150f;
    public IntervalUtil gazeTime = new IntervalUtil(1f, 13);
    public NA_StargazerAssignmentAI(CampaignFleetAPI fleet, SectorEntityToken target) {
        this.fleet = fleet;
        this.target = target;
        this.offset = MathUtils.getRandomNumberInRange(100f, 250f);
    }

    @Override
    public void advance(float amount) {
        if (!fleet.isAlive()) return;

        float days = Global.getSector().getClock().convertToDays(amount);

        this.gazeTime.advance(days);

        if (this.gazeTime.intervalElapsed() || (target instanceof FleetAPI && !target.isVisibleToSensorsOf(fleet))) {
            this.gazeTime.randomize();
            if (Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getStarSystem() != null && Global.getSector().getPlayerFleet().getStarSystem() == fleet.getStarSystem()) {
                this.target = findNewTarget();
            } else if (MathUtils.getRandomNumberInRange(0, 30f) < 1f) {
                // save perfoemance
                this.target = findNewTarget();
            }

        }

         if (fleet.getStarSystem() != null && fleet.getStarSystem().getAllEntities().contains(target)) {
            float distance = target.getRadius() + 50f;
            if (target instanceof PlanetAPI && ((PlanetAPI) target).getSpec() != null && ((PlanetAPI) target).getSpec().getCoronaSize() > 0)
                distance += ((PlanetAPI) target).getSpec().getCoronaSize() * target.getRadius();

            distance += offset;

            float angle = VectorUtils.getAngle(target.getLocation(), fleet.getLocation());
            Vector2f loc = MathUtils.getPointOnCircumference(target.getLocation(), distance, angle + 15f);
            fleet.setMoveDestination(loc.x, loc.y);
            if (!(MathUtils.getDistanceSquared(target.getLocation(), loc) > (distance + 100f) * (distance + 100f))) {
                fleet.goSlowOneFrame();
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

    @Override
    public boolean isDone() {
        return !fleet.isAlive();
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }


}



