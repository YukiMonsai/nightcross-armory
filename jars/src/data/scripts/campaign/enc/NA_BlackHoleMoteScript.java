package data.scripts.campaign.enc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class NA_BlackHoleMoteScript implements EveryFrameScript {

	protected float moteSpawnRate = 1f;
	protected SectorEntityToken entity;
	protected IntervalUtil moteSpawn = new IntervalUtil(0.01f, 0.1f);

	public NA_BlackHoleMoteScript(SectorEntityToken entity, float moteSpawnRate) {
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

	public static Color MOTE_COLOR = new Color(255, 68, 222,175);
	
	public static void spawnMote(SectorEntityToken from) {
		if (!from.isInCurrentLocation()) return;
		float dur = 3f + 2f * (float) Math.random();
		dur *= 2f;
		float size = 4f + (float) Math.random() * 5f;
		size *= 3f;
		Color color = MOTE_COLOR;

		float ang = MathUtils.getRandomNumberInRange(0, 360);
		Vector2f loc = MathUtils.getPointOnCircumference(from.getLocation(), from.getRadius() + MathUtils.getRandomNumberInRange(0, 100f), ang);
		Vector2f vel = Misc.getUnitVectorAtDegreeAngle(ang + 90f);
		vel.scale(0.5f + (float) Math.random() * 2f);
		vel.scale(0.25f);
		Vector2f.add(vel, from.getVelocity(), vel);
		Misc.addGlowyParticle(from.getContainingLocation(), loc, vel, size, dur * 0.33f, dur, color);
	}

	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}
}












