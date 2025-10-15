package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.combat.plugins.NA_CombatECMPlugin;
import data.scripts.stardust.NA_StargazerHull;
import data.scripts.stardust.NA_StargazerStardust;

import java.awt.*;

public class NAFulldiveOfficer_Ghost extends NAFulldiveOfficer  {

    public static float ARMORMULT = -15f;
    public static float ARMORMULT2 = -20f;
    public static float OVERLOAD_DUR = 30f;

    public static float EMP_SCALE = 100f;
    public static float EMP_SCALE_MAXAT = 20f;

    public static float OVERLOAD_RATE = .1f;
    public static float VENT_RATE = 200f;

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


    public static class Level3 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.addListener(new NA_GhostCoreListenerECM(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.removeListenerOfClass(NA_GhostCoreListenerECM.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getDamageToTargetWeaponsMult().unmodify(ECM_BONUS_ID);
            stats.getDamageToTargetEnginesMult().unmodify(ECM_BONUS_ID);
        }

        public String getEffectDescription(float level) {
            //return "+1-4" + "% to ECM rating of ships, depending on ship size";
            return null;//"Up to +" + (int)EMP_SCALE + "% damage to engines and weapons, depending on ECM advantage. Max at +10% net ECM rating.";
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);


            info.addPara("Up to %s damage to engines and weapons based on ECM advantage. Max at %s net ECM rating.", 0f, hc, hc,
                    "+" + (int)(EMP_SCALE) + "%", (int)(100*EMP_SCALE_MAXAT) + "%");
        }

        public String getEffectPerLevelDescription() {
            return null;
        }
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }

        public static String ECM_BONUS_ID = "na_fulldiveghostecmbuff";

        public static class NA_GhostCoreListenerECM implements AdvanceableListener {
            protected ShipAPI ship;
            public NA_GhostCoreListenerECM(ShipAPI ship) {
                this.ship = ship;
            }

            public void advance(float amount) {
                Integer [] player = getTotalAndMaximum(0);
                Integer [] enemy = getTotalAndMaximum(1);
                float pTotal = player[0];
                float eTotal = enemy[0];
                float ecmdmgboost = (int) Math.round(Math.max(0, Math.min(1f, (pTotal - eTotal)/EMP_SCALE_MAXAT)));

                if (ecmdmgboost < 0) ecmdmgboost = 0;

                ship.getMutableStats().getDamageToTargetWeaponsMult().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*EMP_SCALE/100f);
                ship.getMutableStats().getDamageToTargetEnginesMult().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*EMP_SCALE/100f);

                String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
                if (ship == Global.getCombatEngine().getPlayerShip()) Global.getCombatEngine().maintainStatusForPlayerShip("NA_FulldiveGhost", icon, "Ghost Core",
                        ((int)(ecmdmgboost * EMP_SCALE)) + "% damage to weapon/engines due to ECM bonus", false);


            }


            //yoinked from ElectronicWarfareScript.java
            private Integer [] getTotalAndMaximum(int owner) {
                return NA_CombatECMPlugin.get(owner);
            }
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
            stats.getOverloadTimeMod().modifyPercent(BONUS_ID, OVERLOAD_DUR);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getOverloadTimeMod().unmodify(BONUS_ID);
        }

        public String getEffectDescription(float level) {
            return null;//"+" + (int)(OVERLOAD_RATE) + "% flux dissipation while overloaded, +" + (int)(VENT_RATE) + "% while venting";
        }

        Color hc2 = new Color(255, 75, 125);
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);


            info.addPara("\nIf the ship has a %s, rapidly consumes %s while venting or overloaded to restore " + (int)(VENT_RATE) + " flux and 0.1s of overload time, respectively. " +
                            "\nAlso increases overload duration by " + ((int)OVERLOAD_DUR) + "%%.", 0f, hc, NA_StargazerHull.STARGAZER_RED,
                    "Stardust Nebula", "Stardust");
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
            public IntervalUtil ConsumptionTimer = new IntervalUtil(0.05f, 0.12f);

            // since overloadtime is not exposed in API we do this in a stupid hacky way
            private float overloadTime = 0f;
            private float overloadProgress = 0f;

            public void advance(float amount) {
                // finish up the first bit of the first line... bad form
                if (ship.getFluxTracker().isOverloaded()) {
                    ship.getMutableStats().getHullDamageTakenMult().modifyPercent(BONUS_ID, ARMORMULT2);
                } else {
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(BONUS_ID);
                }


                NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);


                if (swarm != null) {
                    ship.getMutableStats().getOverloadTimeMod().modifyFlat(BONUS_ID, OVERLOAD_DUR);
                } else {
                    ship.getMutableStats().getOverloadTimeMod().unmodify(BONUS_ID);
                }

                if (swarm == null || swarm.getNumActiveMembers() < 1f) return;
                if (!ship.getFluxTracker().isOverloadedOrVenting()) return;

                if (!ship.getFluxTracker().isOverloaded()) {
                    overloadTime = 0f;
                    overloadProgress = 0f;
                }
                else if (overloadTime == 0f) {
                    overloadTime = ship.getFluxTracker().getOverloadTimeRemaining();
                    overloadProgress = 0f;
                } else {
                    overloadProgress = overloadTime - ship.getFluxTracker().getOverloadTimeRemaining();
                }

                if (ConsumptionTimer.intervalElapsed()) {
                    ConsumptionTimer.randomize();
                    ConsumptionTimer.setElapsed(0f);

                    NA_StargazerStardust.SwarmMember fragment = swarm.getPicker(true, true).pick();

                    if (fragment != null) {
                        if (ship.getFluxTracker().isOverloaded()) {
                            ship.getFluxTracker().setOverloadProgress(overloadProgress + OVERLOAD_RATE);
                            if (ship.getFluxTracker().getOverloadTimeRemaining() <= 0f) ship.getFluxTracker().stopOverload();
                        }

                        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                        params.flickerRateMult = 0.5f;
                        boolean rand = Math.random() < 0.5f;
                        EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
                                rand ? ship.getLocation() : fragment.loc, ship,
                                rand ? fragment.loc : ship.getLocation(),
                                ship, 15f,
                                ship.getShield().getInnerColor(),
                                new Color(255, 222, 234, 150), params
                        );
                        arc.setSingleFlickerMode(true);

                        swarm.removeMember(fragment);
                    }
                }

                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    ship.getMutableStats().getFluxDissipation().modifyFlat(BONUS_ID, VENT_RATE);
                } else {
                    ship.getMutableStats().getFluxDissipation().unmodify(BONUS_ID);
                }

            }
        }
    }
}

