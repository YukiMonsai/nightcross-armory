package data.scripts.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class NA_RKKVRenderer extends BaseCombatLayeredRenderingPlugin {
    public static SpriteAPI sprite;
    static boolean doOnce = true;
    public HashMap<MissileAPI, CombatEntityAPI> missiles = new HashMap<>();
    public HashMap<CombatEntityAPI, IntervalUtil> targetProgress = new HashMap<>();

    public final float TARGET_TIME = 0.5f;


    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (doOnce){
            doOnce = false;
            try {
                Global.getSettings().loadTexture("graphics/fx/na_RKKVTarget.png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sprite = Global.getSettings().getSprite("graphics/fx/na_RKKVTarget.png");
            sprite.setSize(512, 512);
        }
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        sprite.setAdditiveBlend();
        float frameTime = Global.getCombatEngine().getElapsedInLastFrame();
        if (layer == CombatEngineLayers.ABOVE_PARTICLES && frameTime > 0){

            HashMap<CombatEntityAPI, IntervalUtil> drawn = new HashMap<>();
            HashMap<MissileAPI, Boolean> toRemove = new HashMap<>();

            for (Map.Entry<MissileAPI, CombatEntityAPI> entry : missiles.entrySet()){
                if (entry.getKey() != null && Global.getCombatEngine().isEntityInPlay(entry.getKey())
                        && Global.getCombatEngine().isUIShowingHUD()
                ){
                    if (entry.getValue() != null && Global.getCombatEngine().isEntityInPlay(entry.getValue())) {
                        if (!drawn.containsKey(entry.getValue())) {
                            if (!targetProgress.containsKey(entry.getValue())) {
                                targetProgress.put(entry.getValue(), new IntervalUtil(TARGET_TIME, TARGET_TIME));
                            } else {
                                if (!targetProgress.get(entry.getValue()).intervalElapsed())
                                    targetProgress.get(entry.getValue()).advance(frameTime);
                            }

                            drawn.put(entry.getValue(), targetProgress.get(entry.getValue()));
                            sprite.setAngle(0);
                            sprite.setColor(new Color(75, 100, 255,255));
                            float size = Math.max(32f, Math.min(1f, targetProgress.get(entry.getValue()).getElapsed()/TARGET_TIME)
                                    * 2f * entry.getValue().getCollisionRadius());
                            sprite.setSize(size, size);
                            sprite.renderAtCenter(
                                    entry.getValue().getLocation().x,
                                    entry.getValue().getLocation().y);
                        }
                    }

                } else {
                    toRemove.put(entry.getKey(), true);
                }
            }

            for (Map.Entry<MissileAPI, Boolean> entry : toRemove.entrySet()) {
                if (missiles.containsKey(entry.getKey()))
                    missiles.remove((entry.getKey()));
            }

            targetProgress = drawn;


        }
    }

    public float getRenderRadius() {
        return 9.9999999E14F;
    }

    public EnumSet<CombatEngineLayers> getActiveLayers() {
        EnumSet<CombatEngineLayers> set = EnumSet.noneOf(CombatEngineLayers.class);
        set.add(CombatEngineLayers.ABOVE_PARTICLES);
        return set;
    }
}