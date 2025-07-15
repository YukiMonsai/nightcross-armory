package data.scripts.campaign.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import java.util.List;

public class NA_FleetAssignmentAI extends RouteFleetAssignmentAI {

    protected String faction;
    protected IntervalUtil factionCheck = new IntervalUtil(0.2f, 0.4f);
    public NA_FleetAssignmentAI(CampaignFleetAPI fleet, RouteManager.RouteData route, String faction) {
        super(fleet, route);
        this.faction = faction;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        if (faction != null) {
            float days = Global.getSector().getClock().convertToDays(amount);
            factionCheck.advance(days);
            if (factionCheck.intervalElapsed()) {
                doFactionCheck();
            }
        }
    }

    protected void doFactionCheck() {
        if (fleet.getBattle() != null) return;


        boolean isCurrentlyNightcross = fleet.getFaction().getId().equals(faction);

        if (fleet.isTransponderOn()
                && Global.getSector().getPlayerFleet() != null
                && Global.getSector().getPlayerFleet().isVisibleToSensorsOf(fleet)) {
            if (!isCurrentlyNightcross) {
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, false);
                fleet.setFaction(faction, true);
            }
            return;
        } else {
            if (isCurrentlyNightcross) {
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
                fleet.setFaction(Factions.INDEPENDENT, true);
            }
            return;
        }

    }

}



