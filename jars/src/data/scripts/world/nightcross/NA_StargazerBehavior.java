package data.scripts.world.nightcross;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DisposableFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.terrain.Planet;
import data.scripts.campaign.fleets.NA_FleetAssignmentAI;
import data.scripts.campaign.fleets.NA_StargazerAssignmentAI;
import org.lazywizard.lazylib.MathUtils;

public class NA_StargazerBehavior implements EveryFrameScript {

    public static float MIN_SECONDS_TO_PURSUE_AFTER_SEEN_BY_PLAYER = 30f;
    public static float MAX_SECONDS_TO_PURSUE_AFTER_SEEN_BY_PLAYER = 120f;
    protected StarSystemAPI system;
    protected PlanetAPI target;
    protected CampaignFleetAPI fleet;
    protected DisposableFleetManager manager;

    protected float seenByPlayerTimeout = 0f;


    public NA_StargazerBehavior(CampaignFleetAPI fleet, StarSystemAPI system, PlanetAPI target) {
        this.fleet = fleet;
        this.system = system;
        this.target = target;


        fleet.addScript(new NA_StargazerAssignmentAI(fleet, target, system, true));
        pickNext();
    }

    protected void pickNext() {
        float days = MathUtils.getRandomNumberInRange(10, 30);


        /* PlanetAPI largestStar = system.getStar();
        if (largestStar != null) {
            if (system.getSecondary() != null && system.getSecondary().getRadius() > largestStar.getRadius()) largestStar = system.getSecondary();
            if (system.getTertiary() != null && system.getTertiary().getRadius() > largestStar.getRadius()) largestStar = system.getTertiary();
        }

         */

        //float distFromTarget = Math.max(Misc.getDistance(fleet, target), largestStar != null ? largestStar.getRadius() + 100f : 100f);

    }

    public void advance(float amount) {
        if (fleet.getCurrentAssignment() == null) {
            pickNext();
        }

        //seenByPlayerTimeout -= amount;

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;



        /*boolean visibleToPlayer = fleet.isVisibleToPlayerFleet() && player.isVisibleToSensorsOf(fleet);
        if (!Global.getSettings().isCampaignSensorsOn() && fleet.isInCurrentLocation()) {
            float dist = Misc.getDistance(fleet, player);
            dist -= fleet.getRadius() + player.getRadius();
            boolean asb = player.getAbility(Abilities.SENSOR_BURST) != null &&
                    player.getAbility(Abilities.SENSOR_BURST).isActive();
            visibleToPlayer = dist < 150f || asb && dist < 500f;
        }


        if (visibleToPlayer) {
            setSeenByPlayer();
        }
        if (seenByPlayerTimeout > 0f) {
            visibleToPlayer = true;
        }
        //visibleToPlayer = false;

        if (!visibleToPlayer) {
            if (fleet.getAI() instanceof ModularFleetAIAPI) {
                ModularFleetAIAPI ai = (ModularFleetAIAPI) fleet.getAI();
                for (int i = 0; i < 3; i++) {
                    ai.getNavModule().avoidEntity(player, 3000f, 5000f, 0.2f);
                }

                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, false);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
            }
        } else {
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, false);
        }*/
    }

    public void setSeenByPlayer() {
        seenByPlayerTimeout = MIN_SECONDS_TO_PURSUE_AFTER_SEEN_BY_PLAYER +
                (MAX_SECONDS_TO_PURSUE_AFTER_SEEN_BY_PLAYER - MIN_SECONDS_TO_PURSUE_AFTER_SEEN_BY_PLAYER) * (float) Math.random();
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return false;
    }


}










