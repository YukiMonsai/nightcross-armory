package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.world.nightcross.NA_InsanityManager;

public class NA_InsanityStats extends BaseHullMod {

    static void log(final String message) {
        Global.getLogger(NA_InsanityStats.class).info(message);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

        if (stats == null) return;
        FleetMemberAPI fm = stats.getFleetMember();
        if (fm == null) return;
        if (NA_InsanityManager.InsanityPenalty < 0) {
            if (!Misc.isAutomated(fm)
                    && !(fm.getCaptain() != null && fm.getCaptain().isAICore())) {
                fm.getStats().getMaxCombatReadiness().modifyFlatAlways(fm.getId() + "insanity", NA_InsanityManager.InsanityPenalty, "Insanity");
            }
        }

    }

    private float timer = 0f;

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

        timer += amount;
        //logic taken from kissa
        if (Global.getSector().isPaused()) timer += 1f;

        if (timer > 1f) {
            timer = 0f;

            if (member == null) return;
            if (member.getFleetData() == null) {
                return;
            }
            if (member.getFleetData().getFleet() == null) {
                return;
            }
            if (member.getFleetData().getFleet() != Global.getSector().getPlayerFleet()
                || NA_InsanityManager.InsanityPenalty == 0 || !(!Misc.isAutomated(member)
                    && !(member.getCaptain() != null && member.getCaptain().isAICore()))) {
                if (member.getVariant() != null) {
                    member.getVariant().removeMod("na_insanitystats");
                }
            }
        }
    }



}