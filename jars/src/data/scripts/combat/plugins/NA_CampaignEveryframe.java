package data.scripts.combat.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.rulecmd.nca.NA_ZGRTurnIn;

public class NA_CampaignEveryframe implements EveryFrameScript {

    IntervalUtil timer = new IntervalUtil(30, 30);
    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (NA_CombatPlugin.musicalOverrideDone) {
            NA_CombatPlugin.musicalOverrideDone = false;
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
            Global.getSoundPlayer().pauseCustomMusic();
        }
        timer.advance(Global.getSector().getClock().convertToDays(amount));
        if (timer.intervalElapsed()) {
            for (var core : NA_ZGRTurnIn.stargazerCores.keySet()) {
                if (Global.getSector() != null
                        && Global.getSector().getPlayerFleet() != null
                        && Global.getSector().getPlayerFleet().getCargo() != null
                        && Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(core) > 0
                )
                    for (var faction : Global.getSector().getAllFactions()) {
                        if (!faction.isIllegal(core)) {
                            faction.makeCommodityIllegal(core);
                        }
                    }
            }

        }
    }
}
