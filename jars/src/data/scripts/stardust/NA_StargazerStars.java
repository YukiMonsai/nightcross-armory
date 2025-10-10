package data.scripts.stardust;

import java.util.ArrayList;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.ColorShifterUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import org.magiclib.util.MagicIncompatibleHullmods;

/**
 * Hullmod that creates a fragment swarm around the ship. This swarm is required to power "fragment" weapons.
 *
 * @author Alex
 *
 */
public class NA_StargazerStars extends BaseHullMod {

    public static String STANDARD_STARDUST_EXCHANGE_CLASS = "standard_stardust_exchange_class";
    public static String STANDARD_STARDUST_FLOCKING_CLASS = "standard_stardust_flocking_class";


    public static float SMOD_CR_PENALTY = 0.2f;
    public static float SMOD_MAINTENANCE_PENALTY = 50f;
    public static float BASE_FLASH = 0.15f;



    public static Object STATUS_KEY1 = new Object();


    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        boolean sMod = isSMod(stats);
        if (sMod) {
            stats.getMaxCombatReadiness().modifyFlat(id, -Math.round(SMOD_CR_PENALTY * 100f) * 0.01f, "Stardust");
            stats.getSuppliesPerMonth().modifyPercent(id, SMOD_MAINTENANCE_PENALTY);
        }
    }

    protected final String ID = "na_stargazerstars";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().getHullMods().contains(HullMods.FRAGMENT_SWARM)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.FRAGMENT_SWARM, ID);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (amount <= 0f || ship == null) return;

        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
        if (swarm == null) {
            swarm = createSwarmFor(ship);
        }

        if (ship.isFighter()) return;

        boolean playerShip = Global.getCurrentState() == GameState.COMBAT &&
                Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship;


        NA_StargazerStardust.StardustParams params = swarm.params;
        params.baseMembersToMaintain = (int) ship.getMutableStats().getDynamic().getValue(
                Stats.FRAGMENT_SWARM_SIZE_MOD, getBaseSwarmSize(ship.getHullSize()));
        params.memberRespawnRate = getBaseSwarmRespawnRateMult(ship.getHullSize()) *
                ship.getMutableStats().getDynamic().getValue(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT);


        params.maxNumMembersToAlwaysRemoveAbove = (int) (params.baseMembersToMaintain * 1.5f);
        params.initialMembers = params.baseMembersToMaintain;


        if (playerShip) {
            int active = swarm.getNumActiveMembers();

            int maxRequired = 0;
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getEffectPlugin() instanceof NA_StardustWeapon) {
                    NA_StardustWeapon fw = (NA_StardustWeapon) w.getEffectPlugin();
                    maxRequired = Math.max(maxRequired, fw.getNumFragmentsToFire());
                }
            }

            boolean debuff = active < maxRequired;
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUS_KEY1,
                    Global.getSettings().getSpriteName("ui", "icon_tactical_fragment_swarm"),
                    spec.getDisplayName(),
                    "STARDUST: " + active,
                    debuff);
        }
    }


    public static NA_StargazerStardust createSwarmFor(ShipAPI ship) {
        NA_StargazerStardust existing = NA_StargazerStardust.getSwarmFor(ship);
        if (existing != null) return existing;

//		if (true) {
//			return NA_StardustNA_StardustSwarmLauncherEffect.createTestDwellerSwarmFor(ship);
//		}

        NA_StargazerStardust.StardustParams params = new NA_StargazerStardust.StardustParams();
        if (ship.isFighter()) {
            float radius = 20f;
            int numMembers = 50;

            String wingId = ship.getWing() == null ? null : ship.getWing().getWingId();
            if (NA_StardustSwarmLauncherEffect.SWARM_RADIUS.containsKey(wingId)) {
                radius = NA_StardustSwarmLauncherEffect.SWARM_RADIUS.get(wingId);
            }
            if (NA_StardustSwarmLauncherEffect.FRAGMENT_NUM.containsKey(wingId)) {
                numMembers = NA_StardustSwarmLauncherEffect.FRAGMENT_NUM.get(wingId);
            }

            params.memberExchangeClass = STANDARD_STARDUST_EXCHANGE_CLASS;
            params.flockingClass = STANDARD_STARDUST_FLOCKING_CLASS;
            params.maxSpeed = ship.getMaxSpeedWithoutBoost() +
                    Math.max(ship.getMaxSpeedWithoutBoost() * 0.25f + 50f, 100f);

            params.flashRateMult = 0.25f;
            params.flashCoreRadiusMult = 0f;
            params.flashRadius = 120f;
            params.flashFringeColor = new Color(94,0, 255,40);
            params.flashCoreColor = new Color(235, 226, 239, 182);

            // if this is set to true and the swarm is glowing, missile-fragments pop over the glow and it looks bad
            //params.renderFlashOnSameLayer = true;

            params.maxOffset = radius;
            params.initialMembers = numMembers;
            params.baseMembersToMaintain = params.initialMembers;
        } else {
            params.memberExchangeClass = STANDARD_STARDUST_EXCHANGE_CLASS;
            params.maxSpeed = ship.getMaxSpeedWithoutBoost() +
                    Math.max(ship.getMaxSpeedWithoutBoost() * 0.25f + 50f, 100f) +
                    ship.getMutableStats().getZeroFluxSpeedBoost().getModifiedValue();

            params.flashRateMult = 0.25f;
            params.flashCoreRadiusMult = 0f;
            params.flashRadius = 120f;
            params.flashFringeColor = new Color(153,0, 255,40);
            params.flashCoreColor = new Color(235, 226, 239, 182);

            // if this is set to true and the swarm is glowing, missile-fragments pop over the glow and it looks bad
            //params.renderFlashOnSameLayer = true;

            params.minOffset = 0f;
            params.maxOffset = Math.min(100f, ship.getCollisionRadius() * 0.5f);
            params.generateOffsetAroundAttachedEntityOval = true;
            params.despawnSound = null; // ship explosion does the job instead
            params.spawnOffsetMult = 0.33f;
            params.spawnOffsetMultForInitialSpawn = 1f;

            params.baseMembersToMaintain = getBaseSwarmSize(ship.getHullSize());
            params.memberRespawnRate = getBaseSwarmRespawnRateMult(ship.getHullSize());
            params.maxNumMembersToAlwaysRemoveAbove = params.baseMembersToMaintain * 2;

            //params.offsetRerollFractionOnMemberRespawn = 0.05f;

            params.initialMembers = 0;
            params.initialMembers = params.baseMembersToMaintain;
            params.removeMembersAboveMaintainLevel = false;
        }

        List<WeaponAPI> glowWeapons = new ArrayList<>();
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.usesAmmo() && w.getSpec().hasTag(Tags.FRAGMENT_GLOW)) {
                glowWeapons.add(w);
            }
            if (w.getSpec().hasTag(Tags.OVERSEER_CHARGE) ||
                    (ship.isFighter() && w.getSpec().hasTag(Tags.OVERSEER_CHARGE_FIGHTER))) {
                w.setAmmo(0);
            }
        }

//		if (ship.hasTag(Tags.FRAGMENT_SWARM_START_WITH_ZERO_FRAGMENTS)) {
//			params.initialMembers = 0;
//		}

        return new NA_StargazerStardust(ship, params) {
            protected ColorShifterUtil glowColorShifter = new ColorShifterUtil(new Color(0, 0, 0, 0));
            protected boolean resetFlash = false;

            @Override
            public int getNumMembersToMaintain() {
                if (ship.isFighter()) {
                    return (int)Math.round(((0.2f + 0.8f * ship.getHullLevel()) * super.getNumMembersToMaintain()));
                }
                return super.getNumMembersToMaintain();
            }

            @Override
            public void advance(float amount) {
                super.advance(amount);

                glowColorShifter.advance(amount);

                // this is actually QUITE performance-intensive on the rendering, at least doubles the cost per swarm
                // (comment was from when flashFrequency was *10 with a shorter flashRateMult; *2 is pretty ok -am
                /*if (VoltaicDischargeOnFireEffect.isSwarmPhaseMode(ship)) {
                    params.flashFrequency = 4f;
                    params.flashProbability = 1f;
                    resetFlash = true;
                } else {*/
                    if (!glowWeapons.isEmpty()) {
                        float ammoFractionTotal = 0f;
                        float totalOP = 0f;
                        for (WeaponAPI w : glowWeapons) {
                            float f = w.getAmmo() / Math.max(1f, w.getMaxAmmo());
                            Color glowColor = w.getSpec().getGlowColor();
                            //						if (f > 0) {
                            //							glowColorShifter.shift(w, glowColor, 0.5f, 0.5f, 1f);
                            //						}
                            glowColorShifter.shift(w, glowColor, 0.5f, 0.5f, 1f);
                            float weight = w.getSpec().getOrdnancePointCost(null);
                            ammoFractionTotal += f * weight;
                            totalOP += weight;
                        }

                        float ammoFraction = ammoFractionTotal / Math.max(1f, totalOP);
                        params.flashFrequency = (1f + ammoFraction) * 2f;
                        params.flashFrequency *= Math.max(1f, Math.min(2f, params.baseMembersToMaintain / 50f));
                        params.flashProbability = 1f;
                        if (ammoFraction <= 0f) {
                            params.flashProbability = BASE_FLASH;
                        }
                        //params.flashFringeColor = new Color(255,0,0,(int)(30f + 30f * ammoFraction));
                        //float glowAlphaBase = 50f;
                        float glowAlphaBase = 30f;
                        if (ship.isFighter()) {
                            glowAlphaBase = 18f;
                        }

                        float extraGlow = (totalOP - 10f) / 90f;
                        if (extraGlow < 0) extraGlow = 0;
                        if (extraGlow > 1f) extraGlow = 1f;

                        int glowAlpha = (int)(glowAlphaBase + glowAlphaBase * (ammoFraction + extraGlow * 0.5f));
                        if (glowAlpha > 255) glowAlpha = 255;
                        //params.flashFringeColor = Misc.setAlpha(glowColorShifter.getCurr(), glowAlpha);
                        params.flashFringeColor = Misc.setBrightness(glowColorShifter.getCurr(), 255);
                        params.flashFringeColor = Misc.setAlpha(params.flashFringeColor, glowAlpha);

                        resetFlash = true;
                    } else {
                        //if (ThreatSwarmAI.isAttackSwarm(ship)) {
                        params.flashFrequency = 1f;
                        params.flashProbability = BASE_FLASH;
                        if (resetFlash) {
                            params.flashProbability = BASE_FLASH;
                            resetFlash = false;
                        }
                    }
                //}

//				int flashing = 0;
//				for (SwarmMember p : members) {
//					if (p.flash != null) {
//						flashing++;
//					}
//				}
//				System.out.println("Flashing: " + flashing + ", total: " + members.size());
            }

        };
    }




    public static int getBaseSwarmSize(ShipAPI.HullSize size) {
        switch (size) {
            case CAPITAL_SHIP: return 60;
            case CRUISER: return 45;
            case DESTROYER: return 30;
            case FRIGATE: return 15;
            case FIGHTER: return 10;
            case DEFAULT: return 10;
            default: return 10;
        }
    }

    public static float getBaseSwarmRespawnRateMult(ShipAPI.HullSize size) {
        switch (size) {
            case CAPITAL_SHIP: return 4f;
            case CRUISER: return 2f;
            case DESTROYER: return 1f;
            case FRIGATE: return 0.5f;
            case FIGHTER: return 0.2f;
            case DEFAULT: return 1f;
            default: return 0f;
        }
    }


    @Override
    public CargoStackAPI getRequiredItem() {
        return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL,
                new SpecialItemData(NightcrossID.STARDUST_CORE, null), null);
    }

    public static boolean hasShroudedOrThreatHullmods(ShipAPI ship) {
        if (ship == null || ship.getVariant() == null) return false;
        for (String id : ship.getVariant().getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (spec != null && (spec.hasTag(Tags.SHROUDED) || spec.hasTag(Tags.FRAGMENT))) return true;
        }
        return false;
    }
    public static boolean hasStardustHull(ShipAPI ship) {
        if (ship == null || ship.getVariant() == null) return false;
        for (String id : ship.getVariant().getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (spec != null && (spec.getId().equals("na_stargazerhullmod"))) return true;
        }
        return false;
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship != null && ship.getHullSpec().isPhase()) {
            return false;
        }
        if (hasShroudedOrThreatHullmods(ship)) return false;
        if (hasStardustHull(ship)) return false;

        return true;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (ship != null && ship.getHullSpec().isPhase()) {
            return "Can not be installed on a phase ship due to instability";
        }
        return hasStardustHull(ship) ? "This ship already possesses a Stardust Nebula" : "Incompatible with Shrouded or Threat hullmods";
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)getBaseSwarmSize(ShipAPI.HullSize.FRIGATE);
        if (index == 1) return "" + (int)getBaseSwarmSize(ShipAPI.HullSize.DESTROYER);
        if (index == 2) return "" + (int)getBaseSwarmSize(ShipAPI.HullSize.CRUISER);
        if (index == 3) return "" + (int)getBaseSwarmSize(ShipAPI.HullSize.CAPITAL_SHIP);

        if (index == 4) return "" + (int)(60*getBaseSwarmRespawnRateMult(ShipAPI.HullSize.FRIGATE));
        if (index == 5) return "" + (int)(60*getBaseSwarmRespawnRateMult(ShipAPI.HullSize.DESTROYER));
        if (index == 6) return "" + (int)(60*getBaseSwarmRespawnRateMult(ShipAPI.HullSize.CRUISER));
        if (index == 7) return "" + (int)(60*getBaseSwarmRespawnRateMult(ShipAPI.HullSize.CAPITAL_SHIP));

        return null;
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int) Math.round(SMOD_CR_PENALTY * 100f) + "%";
        if (index == 1) return "" + (int) Math.round(SMOD_MAINTENANCE_PENALTY) + "%";
        return null;
    }

    @Override
    public boolean isSModEffectAPenalty() {
        return true;
    }
}











