package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.plugins.NA_CombatECMPlugin;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class Flux extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }

    public static float SPEED_BOOST = 20f;
    public static float SPEED_BOOST_CAP = 10f;
    public static float FLUX_THRESH = 0.01f;
    public static float FLUX_BOOST_MAX = 0.1f;
    public static float SHIELD_BOOST_MAX = 0.1f;
    public static float SHIELD_THRESH = 0.1f;

    public static final String ID = "na_sic_flux";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("Increases the zero-flux speed boost by %s (%s for capital ships) while flux is below %s.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(SPEED_BOOST) + " su",
                (int)(SPEED_BOOST_CAP) + " su",
                (int)(FLUX_THRESH * 100f) + "%");
        tooltipMakerAPI.addPara("Decreases shield damage taken by %s while flux is low (max below %s).", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(SHIELD_BOOST_MAX * 100) + "%",
                (int)(SHIELD_THRESH * 100) + "%");
        tooltipMakerAPI.addPara("Increases flux dissipation by up to %s while flux is high (max at %s).", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(FLUX_BOOST_MAX * 100) + "%",
                (int)(100f) + "%");

    }



    @Override
    public void advanceInCombat(SCData data, ShipAPI ship, Float amount) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (stats != null) {
            float level = ship.getFluxLevel();
            float shieldLevel = 1f - Math.max(0f, Math.min(1f, (level - SHIELD_THRESH) / (1f - SHIELD_THRESH)));

            if (level < FLUX_THRESH) {
                stats.getZeroFluxSpeedBoost().modifyFlat(ID, ship.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP ? SPEED_BOOST_CAP : SPEED_BOOST);
            } else {
                stats.getZeroFluxSpeedBoost().unmodify(ID);
            }
            if (shieldLevel > 0) {
                stats.getShieldDamageTakenMult().modifyFlat(ID, shieldLevel * SHIELD_BOOST_MAX);
            } else {
                stats.getShieldDamageTakenMult().unmodify(ID);
            }
            if (level > 0) {
                stats.getFluxDissipation().modifyPercent(ID, FLUX_BOOST_MAX * level);
            } else {
                stats.getFluxDissipation().unmodify(ID);
            }
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

    }
}
