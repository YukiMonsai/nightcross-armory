package data.scripts.world.nightcross;


import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.combat.threat.ThreatFleetBehaviorScript;
import data.scripts.campaign.ids.NightcrossID;
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
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
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

        NA_StargazerBehavior behavior = new NA_StargazerBehavior(f, currSpawnLoc, currSpawnLoc.getStar());
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
        }

        FactionAPI faction = Global.getSector().getFaction(NightcrossID.FACTION_STARGAZER);
        f.setName(faction.getFleetTypeName(params.fleetType));

        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        f.getMemoryWithoutUpdate().set(MemFlags.MAY_GO_INTO_ABYSS, true);


        return f;
    }

}








