package data.scripts.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class NA_StargazerRenderer extends BaseCombatLayeredRenderingPlugin {
    public static SpriteAPI sprite;
    public static SpriteAPI spriteflare;
    static boolean doOnce = true;
    public HashMap<NA_StargazerListener, NA_StargazerListener> eyes = new HashMap<>();

    public static final float FLARE_TIME = 0.35f;

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (doOnce){


            try {
                Global.getSettings().loadTexture("graphics/ships/naai/naai_eye.png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            spriteflare = Global.getSettings().getSprite("graphics/ships/naai/naai_eye.png");
            spriteflare.setSize(240, 34);
        }
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER){
            HashMap<NA_StargazerListener, NA_StargazerListener> newMap = new HashMap<>();
            for (NA_StargazerListener bh : eyes.values()){
                if (bh != null && !bh.destroyed){
                    newMap.put(bh, bh);
                    spriteflare.setNormalBlend();
                    spriteflare.setAlphaMult(1f);
                    float scale = 1f;
                    if (bh.weapon.getShip() != null) {
                        scale = (float) ((180f + Math.random() * 5f))/Math.max(100f, bh.weapon.getShip().getCollisionRadius());
                    }

                    spriteflare.setSize((int) (240f * scale), (int) (34f * scale));
                    //if (bh.weapon instanceof WeaponAPI) {
                    spriteflare.renderAtCenter(bh.weapon.getLocation().x,bh.weapon.getLocation().y);
                   // }// else spriteflare.renderAtCenter(bh.lastLocation.x,bh.lastLocation.y);


                }
            }
            eyes = newMap;
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