package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;
import java.text.DecimalFormat;

public class NAFulldiveOfficer_Ghost extends NAFulldiveOfficer  {

    public static float ARMORMULT = -15f;
    public static float ARMORMULT2 = -45f;
    public static float OVERLOAD_DUR = 30f;
    public static float OVERLOAD_RATE = 100f;
    public static float VENT_RATE = 40f;


    public static class Level2 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getArmorDamageTakenMult().modifyPercent(id, ARMORMULT);
            stats.getHullDamageTakenMult().modifyPercent(id, ARMORMULT);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getHullDamageTakenMult().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return (int)(ARMORMULT) + "% armor and hull damage taken, additional " + (int)(ARMORMULT2) + "% hull damage taken while overloaded.";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }
    public static class Level3 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getOverloadTimeMod().modifyPercent(id, OVERLOAD_DUR);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getZeroFluxSpeedBoost().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "+" + (int)(OVERLOAD_DUR) + "% overload duration";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }
    public static class Level4 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.addListener(new NA_GhostCoreListener(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.removeListenerOfClass(NA_GhostCoreListener.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getVentRateMult().modifyPercent(BONUS_ID, OVERLOAD_RATE);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getVentRateMult().unmodify(BONUS_ID);
            stats.getFluxDissipation().unmodify(BONUS_ID);

            stats.getZeroFluxSpeedBoost().unmodify(BONUS_ID);
        }

        public String getEffectDescription(float level) {
            return null;//"+" + (int)(OVERLOAD_RATE) + "% flux dissipation while overloaded, +" + (int)(VENT_RATE) + "% while venting";
        }

        Color hc2 = new Color(255, 75, 125);
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);


            info.addPara("+%s flux dissipation while overloaded, +%s while venting", 0f, hc, hc,
                    "" + (int)(OVERLOAD_RATE) + "%", (int)(VENT_RATE) + "%");
        }
        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }


        public static String BONUS_ID = "na_fulldiveghost_ol_buff";

        public static class NA_GhostCoreListener implements AdvanceableListener {
            protected ShipAPI ship;
            public NA_GhostCoreListener(ShipAPI ship) {
                this.ship = ship;
            }

            public void advance(float amount) {
                CombatEngineAPI engine = Global.getCombatEngine();

                if (ship.getFluxTracker().isOverloaded()) {
                    ship.getMutableStats().getFluxDissipation().modifyMult(BONUS_ID, OVERLOAD_RATE);
                    ship.getMutableStats().getHullDamageTakenMult().modifyPercent(BONUS_ID, ARMORMULT2);
                } else {
                    ship.getMutableStats().getFluxDissipation().unmodify(BONUS_ID);
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(BONUS_ID);
                }

            }
        }
    }
}

