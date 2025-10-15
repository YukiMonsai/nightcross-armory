package data.scripts.campaign.enc;

import com.fs.starfarer.api.impl.campaign.enc.*;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPluginImpl;
import data.scripts.campaign.plugins.NAModPlugin;
import data.scripts.campaign.plugins.NA_SettingsListener;

public class NA_StargazerDrifter extends BaseEPEncounterCreator {
    public void createEncounter(EncounterManager manager, EncounterPoint point) {
        boolean angry = false;
        HyperspaceAbyssPluginImpl.AbyssalEPData data = (HyperspaceAbyssPluginImpl.AbyssalEPData) point.custom;
        if (data.depth >= DEPTH_THRESHOLD_FOR_ANGRY_DRIFTER)
            angry = manager.getRandom().nextFloat() < 0.1 * Math.sqrt(NA_StargazerGhostManager.getAbyssInterest()) + 1f - (ANGRY_CHANCE + ANGRY_CHANCE/(1f + 0.1f * data.depth)); // jank
        NA_StargazerGhostCreator.genList.add(new NA_StargazerGhostCreator.StargazerGhostEncounterGenerationParams(
                angry
        )); // jank

        // so much jank
    }

    public float getFrequencyForPoint(EncounterManager manager, EncounterPoint point) {
        if (NAModPlugin.hasLunaLib)
        {
            if (!NA_SettingsListener.na_stargazer_abyss) return 0;
        }

        return getStargazerDriferFreq(manager, point);
    }


    public static float getStargazerDriferFreq(EncounterManager manager, EncounterPoint point) {
        if (!isPointSuited(point, false, DEPTH_THRESHOLD_FOR_DRIFTER)) {
            return 0f;
        }
        return STARGAZER_DRIFER_FREQ + NA_StargazerGhostManager.getAbyssInterest()*2f;
    }


    public static float DEPTH_THRESHOLD_FOR_DRIFTER = 3f;
    public static float DEPTH_THRESHOLD_FOR_ANGRY_DRIFTER = 8f;
    public static float STARGAZER_DRIFER_FREQ = 0.25f;
    public static float ANGRY_CHANCE = 0.4f;

    public static boolean isPointSuited(EncounterPoint point, boolean allowNearStar, float depthRequired) {
        if (!HyperspaceAbyssPluginImpl.EP_TYPE_ABYSSAL.equals(point.type)) return false;
        HyperspaceAbyssPluginImpl.AbyssalEPData data = (HyperspaceAbyssPluginImpl.AbyssalEPData) point.custom;
        if (data.depth < depthRequired) return false;
        if (!allowNearStar && data.nearest != null) return false;
        return true;
    }
}












