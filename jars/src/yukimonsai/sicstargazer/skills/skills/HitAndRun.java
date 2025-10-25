package yukimonsai.sicstargazer.skills.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class HitAndRun extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all frigates, destroyers, and cruisers";
    }


    private static final float BONUS_SPEED_MAX = 20f;
    private static final float BONUS_SPEED_DECAY = 4f;
    private static final float BONUS_SPEED_RATIO_FRIG = 1200f;
    private static final float BONUS_SPEED_RATIO_DEST = 2000f;
    private static final float BONUS_SPEED_RATIO_CRUISER = 4000f;
    private static final float SYSTEM_CD = 0.15f;



    public static final String ID = "na_sic_heavysupport";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s ship system cooldown and recharge time, if the ship has a mobility system.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(100f * SYSTEM_CD) + "%"

        );
        tooltipMakerAPI.addPara("Dealing non-missile damage (including EMP damage) grants up to %s increased top speed and acceleration, decaying at %s per second. Max effect is reached after dealing %s/%s/%s points of damage, depending on hull size.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(BONUS_SPEED_MAX) + "%",
                (int)(BONUS_SPEED_DECAY) + "%",
                (int)(BONUS_SPEED_RATIO_FRIG) + "",
                (int)(BONUS_SPEED_RATIO_DEST) + "",
                (int)(BONUS_SPEED_RATIO_CRUISER) + ""

        );
        tooltipMakerAPI.addPara("Damage dealt by beams is not counted, unless the damage is dealt to hull or armor.", 0f, Misc.getGrayColor(), Misc.getHighlightColor()

        );


    }


    public static class NA_HitAndRunMod implements DamageDealtModifier, AdvanceableListener {
        protected ShipAPI ship;
        protected float dmgStored = 0;
        protected float dmg_scale = BONUS_SPEED_RATIO_FRIG;
        public NA_HitAndRunMod(ShipAPI ship) {
            this.ship = ship;
            if (ship.getHullSize() == ShipAPI.HullSize.DESTROYER) dmg_scale = BONUS_SPEED_RATIO_DEST;
            else if (ship.getHullSize() == ShipAPI.HullSize.CRUISER) dmg_scale = BONUS_SPEED_RATIO_CRUISER;
        }

        boolean wasZero = false;


        @Override
        public void advance(float amount) {
            if (dmgStored > 0) {
                float amt = dmgStored/dmg_scale;
                ship.getMutableStats().getMaxSpeed().modifyPercent(ID, BONUS_SPEED_MAX*amt);
                ship.getMutableStats().getDeceleration().modifyPercent(ID, BONUS_SPEED_MAX*amt);
                ship.getMutableStats().getAcceleration().modifyPercent(ID, BONUS_SPEED_MAX*amt);
                ship.getMutableStats().getTurnAcceleration().modifyPercent(ID, BONUS_SPEED_MAX*amt);


                float ratedecay = BONUS_SPEED_DECAY / BONUS_SPEED_MAX * dmg_scale;

                dmgStored = Math.max(dmgStored - ratedecay*amount, 0);
            } else {
                ship.getMutableStats().getMaxSpeed().unmodify(ID);
                ship.getMutableStats().getDeceleration().unmodify(ID);
                ship.getMutableStats().getAcceleration().unmodify(ID);
                ship.getMutableStats().getTurnAcceleration().unmodify(ID);
            }
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!(target instanceof ShipAPI tship)) return null;
            if (!ship.isAlive() || !tship.isAlive() || tship.isFighter()) {
                return null;
            }
            if (param instanceof DamagingProjectileAPI proj) {
                if (proj.getWeapon() != null && proj.getWeapon().getType() != WeaponAPI.WeaponType.MISSILE && proj.getWeapon().getShip() == ship) {
                    dmgStored += damage.getDamage();

                    if (dmgStored > dmg_scale) dmgStored = dmg_scale;
                }

            } else if (param instanceof BeamAPI beam) {
                if (beam.getWeapon() != null && beam.getWeapon().getType() != WeaponAPI.WeaponType.MISSILE && beam.getWeapon().getShip() == ship) {
                    if (shieldHit) return null;

                    float dur = beam.getDamage().getDpsDuration();
                    // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
                    if (!wasZero) dur = 0;
                    wasZero = beam.getDamage().getDpsDuration() <= 0;

                    dmgStored += beam.getDamage().computeDamageDealt(dur);
                    if (dmgStored > dmg_scale) dmgStored = dmg_scale;

                }
            }

            return null;
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (ship.getCaptain() == null || (ship.getCaptain().getPortraitSprite().equals("graphics/portraits/portrait_generic_grayscale.png"))) return;
        if (!ship.hasListenerOfClass(NA_HitAndRunMod.class)) {
            ship.addListener(new NA_HitAndRunMod(ship));
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
        ShipSystemSpecAPI systemSpec = null;
        if (variant.getHullSpec() != null && variant.getHullSpec().getShipSystemId() != null) {
            systemSpec = Global.getSettings().getShipSystemSpec(variant.getHullSpec().getShipSystemId());
        } else if (variant.getHullSpec().getBaseHull() != null && variant.getHullSpec().getBaseHull().getShipDefenseId() != null) {
            systemSpec = Global.getSettings().getShipSystemSpec(variant.getHullSpec().getBaseHull().getShipDefenseId());
        }

        if (systemSpec != null) {
            if (systemSpec.hasTag(Tags.SHIP_SYSTEM_MOVEMENT)) {
                stats.getSystemCooldownBonus().modifyMult(ID, 1f - SYSTEM_CD);
                stats.getSystemRegenBonus().modifyPercent(ID, 100f * SYSTEM_CD);
            }
        }

    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
