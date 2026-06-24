package data.scripts.campaign.enc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhostCreator;
import com.fs.starfarer.api.impl.campaign.ghosts.GhostFrequencies;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;
import com.fs.starfarer.api.impl.campaign.ghosts.types.ZigguratGhost;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public class NA_StargazerShroudGhostCreator extends BaseSensorGhostCreator {

    @Override
    public List<SensorGhost> createGhost(SensorGhostManager manager) {
        if (!Global.getSector().getCurrentLocation().isHyperspace()) return null;
        List<SensorGhost> result = new ArrayList<SensorGhost>();
        NA_StargazerShroudGhost g = new NA_StargazerShroudGhost(manager);
        if (!g.isCreationFailed()) {
            result.add(g);
        }
        return result;
    }

    @Override
    public float getFrequency(SensorGhostManager manager) {
        if (Global.getSector().getPlayerFleet() == null) return 0f;
        if (manager.hasGhostOfClass(NA_StargazerShroudGhost.class)) {
            return 0f;
        }
        boolean found = false;
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getHullSpec().hasTag("stargazer")) {
                found = true;
                break;
            }
        }
        if (!found) return (Math.min(1, Misc.getAbyssalDepth(Global.getSector().getPlayerFleet().getLocation()))/2)*(0.00f + NA_StargazerGhostManager.getAbyssInterest()*1.2f);
        return 2.4f + NA_StargazerGhostManager.getAbyssInterest()*3.45f;
    }

    @Override
    public float getTimeoutDaysOnSuccessfulCreate(SensorGhostManager manager) {
        return 15f + manager.getRandom().nextFloat() * 20f;
    }



    @Override
    public boolean canSpawnWhilePlayerInAbyss() {
        return true;
    }
    @Override
    public boolean canSpawnWhilePlayerOutsideAbyss() {
        return false;
    }


}
