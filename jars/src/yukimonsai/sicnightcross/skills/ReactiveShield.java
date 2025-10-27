package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicRender;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

import java.awt.*;

public class ReactiveShield extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships with shields";
    }

    public static float SHIELD_BONUS_TIME = 1f;
    public static float SHIELD_BONUS_AMT = 0.33f;
    public static float SHIELD_ACCEL_BUFF = 25f;

    public static final String ID = "na_sic_shielding";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s to shield unfold and turn rate.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" +(int)(SHIELD_ACCEL_BUFF) + "%");
        tooltipMakerAPI.addPara("For the first second after deploying shields, gain a rapidly decaying %s shield damage taken multiplier.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" +(int)(SHIELD_BONUS_AMT * 100) + "%");

    }


    @Override
    public void advanceInCombat(SCData data, ShipAPI ship, Float amount) {
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

        stats.getShieldUnfoldRateMult().modifyPercent(ID, SHIELD_ACCEL_BUFF);
        stats.getShieldTurnRateMult().modifyPercent(ID, SHIELD_ACCEL_BUFF);
    }



    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (ship.getCaptain() == null || (ship.getCaptain().isDefault())) {
            return;
        } else if (ship.getCaptain() == null) {
            return;
        }
        if (!ship.hasListenerOfClass(NA_ReactiveShieldListener.class)) {
            ship.addListener(new NA_ReactiveShieldListener(ship));
        }
    }

    public static class NA_ReactiveShieldListener implements AdvanceableListener {
        protected ShipAPI ship;
        protected float strengthTime = 1f;
        public NA_ReactiveShieldListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (ship.getShield() != null) {
                if (ship.getShield().isOff()) {
                    strengthTime = Math.min(1.1f, strengthTime + amount/SHIELD_BONUS_TIME);

                    float buff = Math.max(0f, Math.min(1f, strengthTime)) * SHIELD_BONUS_AMT;
                    ship.getMutableStats().getShieldDamageTakenMult().modifyMult(ID, 1f - buff);
                } else {
                    float buff = Math.max(0f, Math.min(1f, strengthTime)) * SHIELD_BONUS_AMT;
                    strengthTime = Math.max(0f, strengthTime - amount/SHIELD_BONUS_TIME);
                    ship.getMutableStats().getShieldDamageTakenMult().modifyMult(ID, (float) (1f - Math.sqrt(buff)));

                    ship.setJitterShields(true);
                    ship.setJitter(this, ship.getShield().getInnerColor(), strengthTime * 0.4f, 9, 72f);
                }
            } else {
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(ID);
            }
        }
    }
}
