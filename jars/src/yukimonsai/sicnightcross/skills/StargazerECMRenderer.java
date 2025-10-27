package yukimonsai.sicnightcross.skills;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class StargazerECMRenderer extends BaseCombatLayeredRenderingPlugin {
    public static SpriteAPI sprite;
    static boolean doOnce = true;
    public HashMap<CombatEntityAPI, Integer> toRender = new HashMap<>();


    public CombatEngineAPI engine;
    public StargazerECMRenderer(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (Global.getCombatEngine() != engine) {
            // AVOID MEMORY LEAK
            engine = null;
            return;
        }
        if (doOnce){
            try {
                Global.getSettings().loadTexture("graphics/fx/na_ECMCircle.png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sprite = Global.getSettings().getSprite("graphics/fx/na_ECMCircle.png");
            sprite.setSize(512, 512);
            doOnce = false;
        }
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        sprite.setAdditiveBlend();
        float frameTime = Global.getCombatEngine().getElapsedInLastFrame();
        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER && frameTime > 0){


            for (Map.Entry<CombatEntityAPI, Integer> entry : toRender.entrySet()){
                if (entry.getKey() != null && Global.getCombatEngine().isEntityInPlay(entry.getKey())
                        && Global.getCombatEngine().isUIShowingHUD()
                ){
                    if (entry.getValue() != null && Global.getCombatEngine().isEntityInPlay(entry.getKey())) {
                        sprite.setAngle(0);
                        sprite.setColor(new Color(75, 100, 255,255));
                        float size = 2*StargazerECM.AURA_SIZE;
                        sprite.setSize(size, size);
                        sprite.renderAtCenter(
                                entry.getKey().getLocation().x,
                                entry.getKey().getLocation().y);
                    }

                }
            }


            if (!engine.isPaused())
                toRender = new HashMap<>();

        }
    }

    public float getRenderRadius() {
        return 9.9999999E14F;
    }

    public EnumSet<CombatEngineLayers> getActiveLayers() {
        EnumSet<CombatEngineLayers> set = EnumSet.noneOf(CombatEngineLayers.class);
        set.add(CombatEngineLayers.BELOW_SHIPS_LAYER);
        return set;
    }
}