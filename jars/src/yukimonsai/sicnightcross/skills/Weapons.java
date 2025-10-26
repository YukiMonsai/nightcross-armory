package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

import java.awt.*;

public class Weapons extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships with officers";
    }


    private static final float BONUS_DMG = 20f;
    private static final float BONUS_DMG_RANGE_SCALE = 200f;
    private static final float BONUS_DMG_RANGE_SCALE_END = 50f;


    public static final String ID = "na_sic_weapons";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("Up to %s bonus ballistic and energy damage dealt, based on the distance the projectile or beam travelled*." +
                        "\n effect scales from no bonus at %s units before the max range, to full bonus at exactly the max range of the weapon, and scaling back to no effect within %s units.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(BONUS_DMG) + "%",
                "" + (int)(BONUS_DMG_RANGE_SCALE) + " su",
                "" + (int)(BONUS_DMG_RANGE_SCALE_END) + " su");

        tooltipMakerAPI.addSpacer(10f);

        tooltipMakerAPI.addPara("Has no effect on missiles or weapons fired by missiles.", 0f, Misc.getGrayColor(), Misc.getGrayColor());

    }


    public static class NA_WeaponMod implements DamageDealtModifier {
        protected ShipAPI ship;
        public NA_WeaponMod(ShipAPI ship) {
            this.ship = ship;
        }


        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            float scale = 0;
            if (!ship.isAlive() || target == null) {
                return null;
            }
            if (ship.getCaptain() == null || (ship.getCaptain().isDefault())) return null;
            if (param instanceof DamagingProjectileAPI proj) {
                if (proj.getWeapon() != null && proj.getWeapon().getType() != WeaponAPI.WeaponType.MISSILE && proj.getWeapon().getShip() == ship) {

                    float dist = MathUtils.getDistance(point, proj.getWeapon().getLocation());
                    float range = Math.max(1f, proj.getWeapon().getRange());
                    float minscale = BONUS_DMG_RANGE_SCALE;
                    float minscalemod = (proj.getSource() != null ? Math.max(proj.getSource().getCollisionRadius() * 0.5f, target.getCollisionRadius()*0.5f) : 0);
                    float maxscale = BONUS_DMG_RANGE_SCALE_END;
                    float maxscalemod = (proj.getSource() != null ? proj.getSource().getCollisionRadius() * 0.5f : 0);
                    if (dist > range) {
                        scale = Math.max(0f, Math.min(1f,
                                1f + Math.min(0, (range + maxscalemod - dist) / maxscale)
                        ));
                    } else if (dist < range) {
                        scale = Math.max(0f, Math.min(1f,
                                1f + Math.min(0, (dist - range - minscalemod) / minscale)
                        ));
                    } else scale = 1;
                }

            } else if (param instanceof BeamAPI beam) {
                if (beam.getWeapon() != null && beam.getWeapon().getType() != WeaponAPI.WeaponType.MISSILE && beam.getWeapon().getShip() == ship) {
                    float dist = beam.getLength();
                    float range = Math.max(1f, beam.getWeapon().getRange());
                    float minscale = BONUS_DMG_RANGE_SCALE;
                    float minscalemod = (beam.getSource() != null ? Math.max(beam.getSource().getCollisionRadius() * 0.5f, target.getCollisionRadius() * 0.5f) : 0);
                    float maxscale = BONUS_DMG_RANGE_SCALE_END;
                    float maxscalemod = (beam.getSource() != null ? beam.getSource().getCollisionRadius() * 0.5f : 0);
                    if (dist > range) {
                        scale = Math.max(0f, Math.min(1f,
                                1f + Math.min(0, (range + maxscalemod - dist) / maxscale)
                        ));
                    } else if (dist < range) {
                        scale = Math.max(0f, Math.min(1f,
                                1f + Math.min(0, (dist - range - minscalemod) / minscale)
                        ));
                    } else scale = 1;

                }
            }
            if (scale > 0) {
                damage.getModifier().modifyPercent(ID, scale * BONUS_DMG);
                float dmg = damage.getDamage();
                if (param instanceof BeamAPI beam) {
                    dmg *= beam.getDamage().getDpsDuration();
                }
                if (Math.random() *100f < dmg)
                    Global.getCombatEngine().addSmokeParticle(point, Misc.ZERO, Math.min(1f, 0.55f + 0.45f*dmg/100f) * (25+25*scale), 0.12f, 0.5f, (param instanceof BeamAPI beam) ? beam.getCoreColor()
                            : (param instanceof DamagingProjectileAPI proj ? proj.getProjectileSpec().getGlowColor() : Color.WHITE));
                return ID;
            }
            return null;
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (ship.getCaptain() == null || (ship.getCaptain().isDefault())) return;
        if (!ship.hasListenerOfClass(NA_WeaponMod.class)) {
            ship.addListener(new NA_WeaponMod(ship));
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
