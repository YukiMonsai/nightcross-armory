package data.scripts.util;

import org.lwjgl.util.vector.Vector2f;
import org.dark.shaders.util.ShaderLib;

public class NAUtil {
    public static boolean OFFSCREEN = false;
    public static final float OFFSCREEN_GRACE_CONSTANT = 500f;
    public static final float OFFSCREEN_GRACE_FACTOR = 2f;
    public static boolean isOnscreen(Vector2f point, float radius) {
        return OFFSCREEN || ShaderLib.isOnScreen(point, radius * OFFSCREEN_GRACE_FACTOR + OFFSCREEN_GRACE_CONSTANT);
    }


}
