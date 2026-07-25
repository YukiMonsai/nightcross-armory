package data.scripts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

public class NA_Flickerfield extends BaseShipSystemScript {

    public static float MAGNITUDE = 0.7f;
    public static float HFLUX_REDUCTION = 0.5f;
    public static String TIDAL_PAUSE = "na_tidalpause";

    protected Object STATUSKEY1 = new Object();
    protected NA_FlickerfieldDamageListener dmglistener;

    //public static final float INCOMING_DAMAGE_MULT = 0.25f;
    //public static final float INCOMING_DAMAGE_CAPITAL = 0.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        effectLevel = 1f;

        float mult = MAGNITUDE;
        stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - mult) * effectLevel);
        stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - mult) * effectLevel);
        stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - mult) * effectLevel);
        stats.getDynamic().getStat(TIDAL_PAUSE).modifyFlat(id, 1f - (1f - mult) * effectLevel);


        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        }
        if (player) {
            ShipSystemAPI system = getDamper(ship);
            if (system != null) {
                float percent = (1f - mult) * effectLevel * 100;
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                        system.getSpecAPI().getIconSpriteName(), system.getDisplayName(),
                        (int) Math.round(percent) + "% less damage taken", false);
            }
        }

        if (effectLevel > 0) {
            if (dmglistener == null) {

                dmglistener = new NA_FlickerfieldDamageListener(ship);
                ship.addListener(dmglistener);
            }
        } else {
            if (dmglistener != null) {
                ship.removeListener(dmglistener);
                dmglistener = null;
            }
        }


        if (dmglistener != null) {
            dmglistener.level = effectLevel;
        }

    }

    public static ShipSystemAPI getDamper(ShipAPI ship) {
//		ShipSystemAPI system = ship.getSystem();
//		if (system != null && system.getId().equals("damper")) return system;
//		if (system != null && system.getId().equals("damper_omega")) return system;
//		if (system != null && system.getSpecAPI() != null && system.getSpecAPI().hasTag(Tags.SYSTEM_USES_DAMPER_FIELD_AI)) return system;
//		return ship.getPhaseCloak();
        ShipSystemAPI system = ship.getPhaseCloak();
        if (system != null && system.getId().equals("na_flickerfield")) return system;
        if (system != null && system.getSpecAPI() != null && system.getSpecAPI().hasTag(Tags.SYSTEM_USES_DAMPER_FIELD_AI)) return system;
        return ship.getSystem();
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getDynamic().getStat(TIDAL_PAUSE).unmodify(id);
    }


//	public StatusData getStatusData(int index, State state, float effectLevel) {
//		float mult = (Float) mag.get(HullSize.CRUISER);
//		if (stats.getVariant() != null) {
//			mult = (Float) mag.get(stats.getVariant().getHullSize());
//		}
//		effectLevel = 1f;
//		float percent = (1f - INCOMING_DAMAGE_MULT) * effectLevel * 100;
//		if (index == 0) {
//			return new StatusData((int) percent + "% less damage taken", false);
//		}
//		return null;
//	}

    public class NA_FlickerfieldDamageListener implements DamageTakenModifier {

        public static final String ID = "NA_FlickerfieldDamageListener";
        protected ShipAPI ship;
        public float level = 0;
        public NA_FlickerfieldDamageListener(ShipAPI ship) {
            this.ship = ship;this.level = 0;
        }


        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!shieldHit && damage.getDamage() > 0 && level > 0) {
                float amt = damage.getDamage() * HFLUX_REDUCTION;
                if (ship.isAlive() && ship.getHardFluxLevel() > 0) {
                    ship.getFluxTracker().setHardFlux(Math.max(0, ship.getFluxTracker().getHardFlux() - amt));
                }
                return ID;
            }

            return null;
        }
    }
}
