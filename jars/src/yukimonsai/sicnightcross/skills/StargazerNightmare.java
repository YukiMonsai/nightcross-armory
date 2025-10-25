package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.stardust.NA_StargazerHull;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

import java.awt.*;

public class StargazerNightmare extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships piloted by AI cores";
    }




    public static final String ID = "na_sic_stargazernightmare";
    public static float HULL_THRESH_LOWER = 0.25f;
    public static float HULL_THRESH_UPPER = 0.4f;
    public static float SHIELD_NERF = 0.20f;
    public static float HULL_BUFF = 0.3f;
    public static float FLUX_BUFF = 0.33f;
    public static float SYSTEM_BUFF = 0.33f;

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("When a ship takes damage that takes it below 25%% hitpoints, it enters a Terminal State unless its hull goes back above 40%%:", 0f, Misc.getHighlightColor(), NA_StargazerHull.STARGAZER_RED,
                "Terminal State"

        );
        tooltipMakerAPI.addPara(" - Peak performance time ends immediately", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor()
        );
        tooltipMakerAPI.addPara(" - Takes %s increased shield damage", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(),
                (int)(100f * SHIELD_NERF) + "%"
        );
        tooltipMakerAPI.addPara(" - Cannot vent", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor()
        );
        tooltipMakerAPI.addPara(" - Takes %s reduced hull damage", 0f, Misc.getTextColor(), Misc.getHighlightColor(),
                (int)(100f * HULL_BUFF) + "%"
        );
        tooltipMakerAPI.addPara(" - Flux dissipation increased by %s", 0f, Misc.getTextColor(), Misc.getHighlightColor(),
                (int)(100f * FLUX_BUFF) + "%"
        );
        tooltipMakerAPI.addPara(" - Ship system cools down and recharges %s faster", 0f, Misc.getTextColor(), Misc.getHighlightColor(),
                (int)(100f * SYSTEM_BUFF) + "%"
        );
        tooltipMakerAPI.addPara(" - If the ship has a %s, doubles the %s regeneration rate.", 0f, Misc.getTextColor(), Misc.getHighlightColor(),
            "Stardust Nebula",
                "Stardust"
        );


    }


    public static class NA_NightmareListener implements DamageTakenModifier, AdvanceableListener {
        protected ShipAPI ship;
        protected boolean terminal = false;
        protected IntervalUtil afterimageTimer = new IntervalUtil(0.25f, 0.25f);
        public NA_NightmareListener(ShipAPI ship) {
            this.ship = ship;
        }


        @Override
        public void advance(float amount) {
            if (ship.getHullLevel() > HULL_THRESH_UPPER) terminal = false;
            if (terminal && ship.isAlive()) {
                afterimageTimer.advance(amount);
                if (afterimageTimer.intervalElapsed()) {
                    // do gfx
                    ship.addAfterimage(ship.getVentCoreColor(), ship.getLocation().x, ship.getLocation().y,
                            -ship.getVelocity().x, -ship.getVelocity().y, 10, 0.1f, 0.25f, 0.1f, true, true, false);
                }
                ship.getMutableStats().getShieldDamageTakenMult().modifyPercent(ID, 100*SHIELD_NERF);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(ID, 1f - HULL_BUFF);
                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                ship.getMutableStats().getFluxDissipation().modifyPercent(ID, 100*FLUX_BUFF);
                ship.getMutableStats().getSystemRegenBonus().modifyPercent(ID, 100*SYSTEM_BUFF);
                ship.getMutableStats().getSystemCooldownBonus().modifyPercent(ID, 1f - SYSTEM_BUFF);
            } else {
                terminal = false;
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(ID);
                ship.getMutableStats().getHullDamageTakenMult().unmodify(ID);
                ship.getMutableStats().getFluxDissipation().unmodify(ID);
                ship.getMutableStats().getSystemRegenBonus().unmodify(ID);
                ship.getMutableStats().getSystemCooldownBonus().unmodify(ID);
            }




        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (target == ship && ship.isAlive()) {
                if (!terminal && ship.getHullLevel() < HULL_THRESH_LOWER) {
                    // Do fancy
                    terminal = true;
                    Global.getCombatEngine().addNegativeSwirlyNebulaParticle(ship.getLocation(), ship.getVelocity(), ship.getCollisionRadius(), 1.25f, 0.25f, 0.5f, 1.0f,
                            RiftLanceEffect.getColorForDarkening(ship.getVentCoreColor()));
                    Global.getCombatEngine().addFloatingText(ship.getLocation(), "terminal state",
                            48, new Color(255, 22, 146), ship, 1f, 5f);
                }
            }
            return null;
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (ship.getCaptain() == null || (ship.getCaptain().getPortraitSprite().equals("graphics/portraits/portrait_generic_grayscale.png"))) return;
        if (!ship.hasListenerOfClass(NA_NightmareListener.class)) {
            ship.addListener(new NA_NightmareListener(ship));
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
