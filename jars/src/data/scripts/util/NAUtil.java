package data.scripts.util;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.impl.combat.SquallOnFireEffect;
import data.scripts.NAUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;
import org.dark.shaders.util.ShaderLib;

import java.util.List;

public class NAUtil {
    public static boolean OFFSCREEN = false;
    public static final float OFFSCREEN_GRACE_CONSTANT = 500f;
    public static final float OFFSCREEN_GRACE_FACTOR = 2f;
    public static boolean isOnscreen(Vector2f point, float radius) {
        return OFFSCREEN || ShaderLib.isOnScreen(point, radius * OFFSCREEN_GRACE_FACTOR + OFFSCREEN_GRACE_CONSTANT);
    }


}
