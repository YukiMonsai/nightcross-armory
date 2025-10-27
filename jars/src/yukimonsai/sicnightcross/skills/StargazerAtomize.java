package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.NAUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

import java.awt.*;

public class StargazerAtomize extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }


    private static final float BONUS_DMG = 3f;
    private static final float MISSING_HP_DMG = 10f;


    public static final String ID = "na_sic_atomize";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("Up to %s bonus damage against ships based on their missing health.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(MISSING_HP_DMG) + "%");
        tooltipMakerAPI.addPara("%s to damage against ship wreckages", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(100f*BONUS_DMG) + "%");


    }


    public static class NA_AtomizeMod implements DamageDealtModifier {
        protected ShipAPI ship;
        public NA_AtomizeMod(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!ship.isAlive() || target == null) {
                return null;
            }
            //if (ship.getCaptain() == null || (ship.getCaptain().isDefault())) return null;

            if (target instanceof CombatAsteroidAPI || (target instanceof ShipAPI tship && (tship.isHulk() || !tship.isAlive()))) {
                damage.getModifier().modifyPercent(ID, 100f * BONUS_DMG);
                if (Math.random() * damage.getBaseDamage() > 0.04f * target.getHitpoints()) {
                    Global.getCombatEngine().spawnEmpArc(ship,
                            target.getLocation(),
                            target,
                            target,
                            DamageType.ENERGY,
                            0,
                            0, // emp
                            target.getCollisionRadius(), // max range
                            null, //"tachyon_lance_emp_impact",
                            20f, // thickness
                            new Color(
                                    255,
                                    175,
                                    175, 147),
                            new Color(
                                    255,
                                    0,
                                    0, 98)
                    );
                }
                return ID;
            } else if (target instanceof ShipAPI tship
                && tship.getHullLevel() < 1f
            ) {
                damage.getModifier().modifyPercent(ID, MISSING_HP_DMG * (1f - tship.getHullLevel()));
                return ID;
            }
            return null;
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        //if (ship.getCaptain() == null || (ship.getCaptain().isDefault())) return;
        if (!ship.hasListenerOfClass(NA_AtomizeMod.class)) {
            ship.addListener(new NA_AtomizeMod(ship));
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
