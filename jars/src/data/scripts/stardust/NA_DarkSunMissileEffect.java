package data.scripts.stardust;

import java.awt.Color;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class NA_DarkSunMissileEffect extends NA_BaseStardustMissile {

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        super.onFire(projectile, weapon, engine);
    }

    protected void configureMissileSwarmParams(NA_StargazerStardust.StardustParams params) {
//		params.flashFringeColor = new Color(255,50,50,255);
//		params.flashFringeColor = new Color(255,165,30,255);
        params.flashFringeColor = new Color(193, 0, 66,255);
        params.flashCoreColor = Color.white;
        params.flashRadius = 140f;
        params.flashCoreRadiusMult = 0.25f;
    }

    protected void swarmCreated(MissileAPI missile, NA_StargazerStardust missileSwarm, NA_StargazerStardust sourceSwarm) {
        if (!missileSwarm.members.isEmpty()) {
            NA_StargazerStardust.SwarmMember p = missileSwarm.members.get(0);
            p.scaler.setBrightness(p.scale);
            p.scaler.setBounceDown(false);
            p.scaler.fadeIn();
        }
    }

    protected int getNumOtherMembersToTransfer() {
        return 14;
        //return 0;
        //return 12;
    }

    protected int getEMPResistance() {
        return 6;
    }

    protected boolean explodeOnFizzling() {
        return false;
    }



//	protected String getExplosionSoundId() {
//		return "devastator_explosion";
//	}


}








