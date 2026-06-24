package data.scripts.campaign.enc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.AbyssalLightEntityPlugin;
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility;
import com.fs.starfarer.api.impl.campaign.ghosts.*;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.world.MoteParticleScript;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.GBFollow;
import com.fs.starfarer.api.impl.campaign.ghosts.GBGoAwayFrom;
import com.fs.starfarer.api.impl.campaign.ghosts.GBITooClose;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.world.MoteParticleScript;
import com.fs.starfarer.api.util.Misc;

public class NA_StargazerShroudGhost extends BaseSensorGhost {

    protected BaseGhostBehavior followBehavior = null;


    public NA_StargazerShroudGhost(SensorGhostManager manager) {
        super(manager, 30);

        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        //initEntity(1000f, 200f);
        initEntity(2000f, pf.getRadius());
        if (!placeNearPlayer(700f, 1200f)) {
            setCreationFailed();
            return;
        }

        despawnOutsideSector = false;
        despawnInAbyss = false;
        entity.addScript(new NA_StargazerShroudNebulaScript(entity, this, 1f));
        entity.addTag("na_stargazer_ghost");

        addBehavior(new GBFollow(pf, 1000f, 3, 1000f, 2000f));
        //addBehavior(new GBIRunEveryFrame(0f, this));
        addInterrupt(new GBITooClose(0f, pf, 100f));
        addBehavior(new GBGoAwayFrom(10f, pf, 10));

    }


    public void kill() {
        reportDespawning(BaseSensorGhost.DespawnReason.SCRIPT_ENDED, null);
        entity = null;
    }


    @Override
    public void advance(float amount) {
        if (entity == null) {
            return;
        }
        if (!entity.hasTag(Tags.FADING_OUT_AND_EXPIRING)) {
            if (script.isEmpty() ||
                    (despawnOutsideSector && Misc.isOutsideSector(entity.getLocation())) ||
                    (despawnInAbyss && Misc.isInAbyss(entity))
            ) {
                Misc.fadeAndExpire(entity, 1f);
                reportDespawning(DespawnReason.SCRIPT_ENDED, null);
                entity = null;
                return;
            }
        }


        if (!script.isEmpty()) {
            GhostBehavior curr = script.get(0);
            curr.advance(amount, this);

            if (curr.isDone()) {
                script.remove(curr);
            }
        }


        movement.advance(amount);
//		if (this instanceof LeviathanGhost) {
//			Vector2f prev = entity.getLocation();
//			Vector2f next = movement.getLocation();
//			if (Misc.getDistance(prev, next) > 100f) {
//				System.out.println("LOCATION JUMP");
//				movement.advance(amount);
//			}
//		}


        entity.getLocation().set(movement.getLocation());
        entity.getVelocity().set(movement.getVelocity());
        //entity.getVelocity().set(0f, 0f);
    }

}
