package data.scripts.combat.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public class NA_CampaignEveryframe implements EveryFrameScript {


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
    }
}
