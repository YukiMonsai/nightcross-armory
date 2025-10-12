package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class NAFulldiveOfficer_Teto extends NAFulldiveOfficer {


    public static float ZSPEED_BOOST = 15f;
    public static float DMG_BOOST = 25f;
    public static float BASE_SPEED = 80f;



    public static class Level2 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getZeroFluxSpeedBoost().modifyPercent(id, ZSPEED_BOOST);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getZeroFluxSpeedBoost().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "+" + (int)(ZSPEED_BOOST) + "% to zero flux bonus";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class Level3 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.addListener(new NA_TetoListener_pleaselisten(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.removeListenerOfClass(NA_TetoListener_pleaselisten.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getBallisticAmmoRegenMult().unmodify(DMG_BONUS_ID);
            stats.getEnergyAmmoRegenMult().unmodify(DMG_BONUS_ID);
            stats.getBallisticWeaponDamageMult().unmodify(DMG_BONUS_ID);
            stats.getEnergyWeaponDamageMult().unmodify(DMG_BONUS_ID);
        }

        public String getEffectDescription(float level) {
            //return "+1-4" + "% to ECM rating of ships, depending on ship size";
            return null;//"+" + (int) DMG_BOOST + "% ballistic and energy damage and ammo regen, based on forward speed. Max when moving at least 80 u/s in the forward vector.";
        }

        Color hc2 = new Color(255, 5, 5);

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            info.addPara("%s ballistic and energy damage and ammo regen, based on forward speed. Max when moving at least %s in the forward vector.", 0f, hc, hc,
                    "+" + (int)(DMG_BOOST) + "%", (int)(BASE_SPEED) + " su");
        }

        public String getEffectPerLevelDescription() {
            return null;
        }
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }

        public static String DMG_BONUS_ID = "na_fulldivetetodmgbuff";

        public static class NA_TetoListener_pleaselisten implements AdvanceableListener {
            protected ShipAPI ship;
            public NA_TetoListener_pleaselisten(ShipAPI ship) {
                this.ship = ship;
            }

            public void advance(float amount) {
                CombatEngineAPI engine = Global.getCombatEngine();

                float bonus = Math.min(1f, Math.max(0, Vector2f.dot(ship.getVelocity(), MathUtils.getPointOnCircumference(Misc.ZERO, 1f, ship.getFacing()))/BASE_SPEED));


                ship.getMutableStats().getBallisticAmmoRegenMult().modifyMult(DMG_BONUS_ID, 1f + 0.01f * bonus * DMG_BOOST);
                ship.getMutableStats().getEnergyAmmoRegenMult().modifyMult(DMG_BONUS_ID, 1f + 0.01f * bonus * DMG_BOOST);
                ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(DMG_BONUS_ID, 1f + 0.01f * bonus * DMG_BOOST);
                ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult(DMG_BONUS_ID, 1f + 0.01f * bonus * DMG_BOOST);


                String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
                if (ship == Global.getCombatEngine().getPlayerShip()) Global.getCombatEngine().maintainStatusForPlayerShip("NA_FulldiveTeto", icon, "No Regrets!",
                        (int)(bonus * DMG_BOOST) + "% increased ballistic/energy damage/regen from speed", false);


            }
        }
    }

}

