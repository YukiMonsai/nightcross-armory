package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NAUtils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NA_StargazerListener extends BaseEveryFrameCombatPlugin {

    public boolean destroyed = false;
    private String key;
    WeaponAPI weapon;
    Object source;

    public Vector2f lastLocation;

    public NA_StargazerListener(WeaponAPI weapon, String id, Object source) {
        super();
        this.weapon = weapon;
        this.key = id;
        this.lastLocation = weapon.getLocation();
        this.source = source;

        Global.getCombatEngine().addPlugin(this);

        if (layerRenderer != null) {
            if (!layerRenderer.eyes.containsKey(this)) {
                layerRenderer.eyes.put(this, this);
            }
        }
    }


    static NA_StargazerRenderer layerRenderer = null;

    @Override
    public void init(CombatEngineAPI engine) {
        layerRenderer = new NA_StargazerRenderer();
        engine.addLayeredRenderingPlugin(layerRenderer);
    }



    public void destroy() {
        // removes the plugin
        Global.getCombatEngine().removePlugin(this);
        Global.getCombatEngine().getCustomData().remove(key);
    }

    public boolean doEndOnce = false;

    public boolean armed = false;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine().isPaused()) return;

        // check if the projectile is alive or not
        if (destroyed || weapon == null || weapon.getShip() == null || !weapon.getShip().isAlive() || !Global.getCombatEngine().isEntityInPlay(weapon.getShip())) {
            if (!destroyed || doEndOnce) {
                destroyed = true;



            }
        } else {
            this.lastLocation = weapon.getLocation();

            if (layerRenderer != null) {
                layerRenderer.eyes.put(this, this);
            }
        }
    }

}
