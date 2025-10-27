package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.plugins.NA_CombatECMPlugin;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class ReactivePlating extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }

    public static float ARMOR_BOOST_PERCENT = 0.1f;
    public static float HULL_DMG_THRESH = 0.33f;
    public static float HULL_DMG_THRESH_RED = 0.25f;

    public static final String ID = "na_sic_plating";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("While shields are offline, gain effective armor equal to %s of the ship hull's base armor rating.*", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(ARMOR_BOOST_PERCENT * 100) + "%");
        tooltipMakerAPI.addPara("When taking damage over %s of the ship's maximum hitpoints, the portion above that value is reduced by %s.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(HULL_DMG_THRESH * 100) + "%",
                (int)(HULL_DMG_THRESH_RED * 100) + "%");
        tooltipMakerAPI.addSpacer(10f);
        tooltipMakerAPI.addPara("*Does not apply while overloaded or venting, or to ships without shields.", 0f, Misc.getGrayColor(), Misc.getHighlightColor());

    }


    @Override
    public void advanceInCombat(SCData data, ShipAPI ship, Float amount) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (stats != null) {
            if (ship.getShield() != null && ship.getShield().isOff()
                && !ship.getFluxTracker().isOverloadedOrVenting()) {
                stats.getEffectiveArmorBonus().modifyFlat(ID, ARMOR_BOOST_PERCENT * ship.getHullSpec().getArmorRating());
            } else stats.getEffectiveArmorBonus().unmodify(ID);

        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

    }



    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (ship.getCaptain() == null  || (ship.getCaptain().isDefault())) return;
        if (!ship.hasListenerOfClass(NA_ReactivePlatingListener.class)) {
            ship.addListener(new NA_ReactivePlatingListener(ship));
        }
    }

    public static class NA_ReactivePlatingListener implements DamageTakenModifier {
        protected ShipAPI ship;
        public NA_ReactivePlatingListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!shieldHit && damage.getDamage() > 0 && damage.getDamage() > ship.getMaxHitpoints() * HULL_DMG_THRESH) {
                float over = damage.getDamage() - ship.getMaxHitpoints() * HULL_DMG_THRESH;
                over *= 1f - HULL_DMG_THRESH_RED;
                float mult = (ship.getMaxHitpoints() * HULL_DMG_THRESH + over) / damage.getDamage();
                damage.getModifier().modifyMult(ID, mult);
                return ID;
            }

            return null;
        }
    }
}
