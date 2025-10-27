package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class StargazerOverride extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all automated ships";
    }

    private Object KEY = new Object();


    private static final float FLUX_DISS = 0.2f;
    private static final float RANGE_RED = 0.15f;
    private static final float PEAK_RED = 0.5f;


    public static final String ID = "na_sic_voerride";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s flux dissipation, doubling as the ship's peak performance time expires", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(FLUX_DISS * 100) + "%");
        tooltipMakerAPI.addPara("%s ballistic and energy weapon range", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(),
                "-" + (int)(RANGE_RED * 100) + "%");
        tooltipMakerAPI.addPara("%s peak performance time", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(),
                "-" + (int)(PEAK_RED * 100) + "%");

        tooltipMakerAPI.addSpacer(10f);

        tooltipMakerAPI.addPara("These effects do not apply to ships with the Safety Overrides hullmod", 0f, Misc.getGrayColor(), Misc.getNegativeHighlightColor());

    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (!Misc.isAutomated(ship)) return;
        if (!ship.hasListenerOfClass(NA_StargazerOverrideListener.class)) {
            ship.addListener(new NA_StargazerOverrideListener(ship));
        }
    }



    public static class NA_StargazerOverrideListener implements AdvanceableListener {
        protected ShipAPI ship;
        protected IntervalUtil afterimageTimer = new IntervalUtil(0.25f, 0.5f);
        public NA_StargazerOverrideListener(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {

            if (ship.isAlive()) {


                if (!(ship.getHullSpec().getNoCRLossTime() < 10000 || ship.getHullSpec().getCRLossPerSecond(ship.getMutableStats()) > 0)) {
                    ship.getMutableStats().getFluxDissipation().modifyPercent(ID, 100f * FLUX_DISS);
                    return;
                }

                float remaining = ship.getPeakTimeRemaining();
                float max = ship.getMutableStats().getPeakCRDuration().computeEffective(ship.getHullSpec().getNoCRLossTime());
                float level = 0;
                if (max > 0) {
                    level = 1f - remaining/max;
                }
                ship.getMutableStats().getFluxDissipation().modifyPercent(ID, 100f * (1f + level) * FLUX_DISS);

                afterimageTimer.advance(amount * level);
                if (level > 0.25 && afterimageTimer.intervalElapsed()) {
                    // do gfx
                    ship.addAfterimage(ship.getVentFringeColor(), 0f, 0f,
                            -ship.getVelocity().x, -ship.getVelocity().y, 10, 0.2f, 0.37f, 0.1f, true, true, false);
                }
            }




        }

    }


    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
        stats.getPeakCRDuration().modifyMult(ID, 1f - PEAK_RED);
        stats.getEnergyWeaponRangeBonus().modifyMult(ID, 1f - RANGE_RED);
        stats.getBallisticWeaponRangeBonus().modifyMult(ID, 1f - RANGE_RED);



    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
