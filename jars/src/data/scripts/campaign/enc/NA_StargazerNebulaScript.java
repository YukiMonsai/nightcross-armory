package data.scripts.campaign.enc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class NA_StargazerNebulaScript implements EveryFrameScript {

	protected float moteSpawnRate = 1f;
	protected SectorEntityToken entity;
	protected IntervalUtil moteSpawn = new IntervalUtil(0.01f, 0.1f);

	public NA_StargazerNebulaScript(SectorEntityToken entity, float moteSpawnRate) {
		super();
		this.entity = entity;
		this.moteSpawnRate = moteSpawnRate;
	}

	public void advance(float amount) {
		float days = Misc.getDays(amount);
		float mult = moteSpawnRate;
		moteSpawn.advance(days * mult);
		if (moteSpawn.intervalElapsed()) {
			spawnMote(entity);
		}
	}

	public static Color MOTE_COLOR = new Color(131, 3, 33,175);
	
	public static void spawnMote(SectorEntityToken from) {
		if (!from.isInCurrentLocation()) return;
		float dur = 1f + 2f * (float) Math.random();
		dur *= 2f;
		float size = 3f + (float) Math.random() * 5f;
		size *= 3f;
		Color color = MOTE_COLOR;
		
		Vector2f loc = Misc.getPointWithinRadius(from.getLocation(), from.getRadius());
		Vector2f vel = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
		vel.scale(2f + (float) Math.random() * 4f);
		vel.scale(0.25f);
		Vector2f.add(vel, from.getVelocity(), vel);
		Misc.addGlowyParticle(from.getContainingLocation(), loc, vel, size, 0.5f, dur, color);
	}

	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}
}












