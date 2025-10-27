package data.scripts.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.io.IOException;
import java.util.List;

public class NA_BlackholeRenderer extends BaseCombatLayeredRenderingPlugin {
    public static SpriteAPI sprite;
    public static SpriteAPI spriteflare;
    static boolean doOnce = true;
    public HashMap<NA_CorrosionListener, NA_CorrosionListener> blackholes = new HashMap<>();
    public HashMap<NA_CorrosionListener, NA_CorrosionListener> missiles = new HashMap<>();

    public static final float FLARE_TIME = 0.35f;

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (doOnce){
            doOnce = false;
            try {
                Global.getSettings().loadTexture("graphics/fx/na_blackhole.png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sprite = Global.getSettings().getSprite("graphics/fx/na_blackhole.png");
            sprite.setSize(512, 512);


            try {
                Global.getSettings().loadTexture("graphics/fx/na_corrosionflare.png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            spriteflare = Global.getSettings().getSprite("graphics/fx/na_corrosionflare.png");
            spriteflare.setSize(512, 64);
        }
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER){
            for (NA_CorrosionListener bh : blackholes.values()){
                if (bh != null){
                    float progress = bh.endTimer.getElapsed()/NA_CorrosionListener.SINGULARITY_DURATION;
                    progress = Math.min(1f, 2*progress) - Math.max(0, 1.5f*(progress - 0.67f));
                    if (progress > 0.001) {
                        sprite.setNormalBlend();
                        sprite.setAlphaMult(progress*progress);
                        sprite.setWidth(Math.max(4f, 1250f));
                        sprite.setHeight(Math.max(4f, 1250f));
                        sprite.setAngle(-((float) Math.PI) * bh.endTimer.getElapsed());
                        sprite.setColor(new Color(0, 0, 0,255));
                        sprite.renderAtCenter(bh.lastLocation.x,bh.lastLocation.y);
                    }

                }
            }
        }
        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER){
            HashMap<NA_CorrosionListener, NA_CorrosionListener> newMap = new HashMap<>();
            for (NA_CorrosionListener bh : missiles.values()){
                if (bh != null && bh.armed && !bh.destroyed){
                    newMap.put(bh, bh);
                    float progress = Math.min(1f, bh.flareTimer.getElapsed()/FLARE_TIME);
                    if (progress > 0.001) {
                        spriteflare.setNormalBlend();
                        spriteflare.setAlphaMult(progress);
                        if (bh.proj instanceof MissileAPI && ((MissileAPI) bh.proj).getSpec() != null) {
                            Vector2f engineLocation = MathUtils.getPointOnCircumference(bh.proj.getLocation(),
                                    Math.max(5, ((MissileAPI) bh.proj).getCollisionRadius()),
                                    bh.proj.getFacing() + 180);
                            spriteflare.renderAtCenter(engineLocation.x,engineLocation.y);
                        } else spriteflare.renderAtCenter(bh.lastLocation.x,bh.lastLocation.y);

                    }

                }
            }
            missiles = newMap;
        }
    }

    public float getRenderRadius() {
        return 9.9999999E14F;
    }

    public EnumSet<CombatEngineLayers> getActiveLayers() {
        EnumSet<CombatEngineLayers> set = EnumSet.noneOf(CombatEngineLayers.class);
        set.add(CombatEngineLayers.BELOW_SHIPS_LAYER);
        set.add(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        return set;
    }
}