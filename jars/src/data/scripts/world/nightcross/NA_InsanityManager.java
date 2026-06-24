package data.scripts.world.nightcross;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostCreator;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossPeople;
import data.scripts.hullmods.NA_ProjectGhost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NA_InsanityManager extends BaseCampaignEventListener implements EveryFrameScript {

    IntervalUtil timer = new IntervalUtil(0.1f, 0.1f);

    public NA_InsanityManager(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }


    public static Map<String, Float> AI_MULTS = new HashMap<>();
    static {
        AI_MULTS.put(NightcrossPeople.GHOST_CORE, 1f);
        AI_MULTS.put(NightcrossPeople.GHOST_MATRIX, 3f);
        AI_MULTS.put(NightcrossPeople.GHOST_GRID, 2f);
    }


    public static float CR_PENALTY_PER_DP = 0.05f;

    public static float InsanityPenalty = 0;

    @Override
    public void advance(float amount) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        if (pf == null) {
            return;
        }

        InsanityPenalty = 0;
        for (FleetMemberAPI member : pf.getFleetData().getMembersListCopy()) {
            boolean isGhost = member.getVariant().hasHullMod(NA_ProjectGhost.ID);
            if (isGhost && member.getFleetData() != null && member.getFleetData().getFleet() != null
                    && member.getFleetData().getFleet().isPlayerFleet()) {

                float mult = 0;
                if (member.getCaptain() != null && member.getCaptain().isAICore()) {
                    if (AI_MULTS.containsKey(member.getCaptain().getAICoreId())) {
                        mult = AI_MULTS.get(member.getCaptain().getAICoreId());
                    }
                }
                if (mult == 0) continue;;
                var dp = member.getDeploymentPointsCost();
                InsanityPenalty += CR_PENALTY_PER_DP * mult * -0.01f * dp;
                /*for (FleetMemberAPI fm : member.getFleetData().getFleet().getFleetData().getMembersListCopy()) {
                    var fmstats = fm.getStats();
                    if (!Misc.isAutomated(fm)
                            && !(fm.getCaptain() != null && fm.getCaptain().isAICore())) {
                        //InsanityPenalty +=
                        //fmstats.getMaxCombatReadiness().modifyFlatAlways(fm.getId() + member.getId() + fm.getId() + "insanity" + member.getId(), CR_PENALTY_PER_DP * mult * -0.01f * dp, "Insanity - " + member.getShipName());
                    }
                }*/
            }
        }

        if (InsanityPenalty < 0) {
            for (FleetMemberAPI m : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
                if (m.getVariant()==null) continue;
                if (m.isFighterWing()) continue;
                if (m.getVariant().hasHullMod("na_insanitystats")) continue;

                m.getVariant().addMod("na_insanitystats");

                // refresh to avoid problems according to kissa
                for (FleetMemberAPI mm : pf.getMembersWithFightersCopy()) {
                    if (mm.isFighterWing()) continue;

                    mm.getVariant().setSource(VariantSource.REFIT);
                    mm.setVariant(mm.getVariant(), false, false);
                }
            }
        }
    }
}
