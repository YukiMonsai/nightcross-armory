package data.scripts.world.nightcross;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DisposableFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.terrain.Planet;
import data.scripts.campaign.fleets.NA_FleetAssignmentAI;
import data.scripts.campaign.fleets.NA_StargazerAssignmentAI;
import data.scripts.campaign.ids.NightcrossID;
import org.lazywizard.lazylib.MathUtils;

public class NA_StargazerBehavior implements EveryFrameScript {

    public static float MIN_SECONDS_TO_PURSUE_AFTER_SEEN_BY_PLAYER = 30f;
    public static float MAX_SECONDS_TO_PURSUE_AFTER_SEEN_BY_PLAYER = 120f;
    protected StarSystemAPI system;
    protected PlanetAPI target;
    protected CampaignFleetAPI fleet;
    protected DisposableFleetManager manager;

    protected float seenByPlayerTimeout = 0f;
    protected float seenByPlayerTime = 0f;
    protected float seenByPlayerTimeTotal = 0f;
    protected float WARNING_TIME = 125f;
    protected float WARNING_TIME_CD = 75f;
    protected float KILL = 250f;
    protected float KILL_TIME_CD = 200f;
    protected float NO_FORGET = 4f;




    public NA_StargazerBehavior(CampaignFleetAPI fleet, StarSystemAPI system, PlanetAPI target, boolean wanderSystem, boolean wanderTarget, boolean despawn) {
        this.fleet = fleet;
        this.system = system;
        this.target = target;


        fleet.addScript(new NA_StargazerAssignmentAI(fleet, target, system, wanderSystem, wanderTarget, despawn));
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

        seenByPlayerTimeout -= amount;

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;



        boolean visibleToPlayer = fleet.isVisibleToPlayerFleet() && player.isVisibleToSensorsOf(fleet);
        if (!Global.getSettings().isCampaignSensorsOn() && fleet.isInCurrentLocation()) {
            float dist = Misc.getDistance(fleet, player);
            dist -= fleet.getRadius() + player.getRadius();
            boolean asb = player.getAbility(Abilities.SENSOR_BURST) != null &&
                    player.getAbility(Abilities.SENSOR_BURST).isActive();
            visibleToPlayer = dist < 150f || asb && dist < 500f;
        }


        if (visibleToPlayer) {
            setSeenByPlayer();
            seenByPlayerTime += amount;
            seenByPlayerTimeTotal += amount;

        } else {
            seenByPlayerTimeout = 0;
        }
        if (seenByPlayerTimeout > 0f) {
            visibleToPlayer = true;
        }
        //visibleToPlayer = false;
        if (!visibleToPlayer) {
            if (fleet.getAI() instanceof ModularFleetAIAPI) {
                if (seenByPlayerTimeTotal < KILL * NO_FORGET || Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().getBoolean("$na_stargazer_kill")) {
                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, false);
                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
                }
            }
        } else if (!(Global.getSector().getFaction(NightcrossID.FACTION_STARGAZER).getRelationship(Factions.PLAYER) > 0.1f)) {
            if (seenByPlayerTime > KILL || seenByPlayerTimeTotal > KILL * NO_FORGET) {

                if (MathUtils.getRandomNumberInRange(1, 10) < 3) {
                    if (!Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE)
                            && (Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().getExpire("$na_stargazer_kill") < KILL_TIME_CD * 0.5f
                            || !Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().getBoolean("$na_stargazer_kill"))) {
                        Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().set("$na_stargazer_kill", true, KILL_TIME_CD);
                        Global.getSoundPlayer().playSound("NA_SFX_GRAVE_MISTAKE", 1f, 1.2f, Global.getSector().getPlayerFleet().getLocation(), Misc.ZERO);
                    }

                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, false);

                    fleet.getMemoryWithoutUpdate().set("$na_stargazersuspicion", true);

                }
                if (fleet.getCurrentAssignment() != null) {
                    String text = "//INTERRUPT kill kill kill";
                    fleet.getCurrentAssignment().setActionText(text);
                }

                if (Global.getSector().getFaction(NightcrossID.FACTION_STARGAZER).isAtWorst(Factions.PLAYER, RepLevel.SUSPICIOUS)) {
                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
                } else {
                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, false);
                }

            } else
            if (seenByPlayerTime > WARNING_TIME) {
                if (!Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().getBoolean("$na_stargazer_warn")) {
                    Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().set("$na_stargazer_warn", true, WARNING_TIME_CD);
                    Global.getSoundPlayer().playSound("NA_SFX_BAD_OMEN", 1f, 1.2f, Global.getSector().getPlayerFleet().getLocation(), Misc.ZERO);
                }
                if (fleet.getCurrentAssignment() != null) {
                    String text = "//INTERRUPT ???";
                    fleet.getCurrentAssignment().setActionText(text);
                }
                fleet.getMemoryWithoutUpdate().set("$na_stargazersuspicion", true);

                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, false);
            } else {
                fleet.getMemoryWithoutUpdate().set("$na_stargazersuspicion", false);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, false);
            }
        } else {

            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, false);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, false);
            fleet.getMemoryWithoutUpdate().set("$na_stargazersuspicion", false);
        }
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










