package data.scripts.everyframe;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.NAModPlugin;
import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;
import data.scripts.util.NAUtil;

public class Nightcross_Trails extends BaseEveryFrameCombatPlugin {

    private static final String AMP_PROJ_ID = "na_heavyparticlecannon_shot";
    private static final Color AMP_TRAIL_COLOR_START = new Color(50, 125, 250);
    private static final Color AMP_TRAIL_COLOR_END = new Color(25, 125, 225);


    private static final String LASER_PROJ_ID = "na_laser_shot";
    private static final Color LASER_TRAIL_COLOR_START = new Color(125, 50, 250);
    private static final Color LASER_TRAIL_COLOR_END = new Color(75, 25, 175);


    private static final String PYRO_PROJ_ID = "na_pyrokinetic_shot";
    private static final Color PYRO_TRAIL_COLOR_START = new Color(255, 225, 50);
    private static final Color PYRO_TRAIL_COLOR_END = new Color(255, 175, 0);
    private static final String META_PROJ_ID = "na_metahelium_shot";
    private static final String MINIRAZOR_ID = "na_minirazor_shot";
    private static final Color META_TRAIL_COLOR_START = new Color(255, 45, 25);
    private static final Color META_TRAIL_COLOR_END = new Color(72, 15, 0);


    private static final String SUPERBLASTER_PROJ_ID = "na_superblaster_shot";
    private static final Color SUPERBLASTER_TRAIL_COLOR_START = new Color(205, 45, 255);
    private static final Color SUPERBLASTER_TRAIL_COLOR_END = new Color(0, 125, 255);


    private static final String PYROWISP_LARGE_PROJ_ID = "na_pyrowisp_large_shot";
    private static final Color PYROWISP_LARGE_TRAIL_COLOR_START = new Color(255, 125, 25);
    private static final Color PYROWISP_LARGE_TRAIL_COLOR_END = new Color(200, 25, 0);

    private static final String WAVEFRONT_SUB = "na_wavefront_warhead2";
    private static final Color WAVEFRONT_SUB_TRAIL_COLOR_START = new Color(225, 225, 255);
    private static final Color WAVEFRONT_SUB_TRAIL_COLOR_END = new Color(150, 202, 255);

    private static final String HARDLIGHT_SHOT = "na_hardlightrifle_shot";
    private static final Color HARDLIGHT_SHOT_TRAIL_COLOR_START = new Color(225, 225, 255);
    private static final Color HARDLIGHT_SHOT_TRAIL_COLOR_END = new Color(150, 202, 255);
    private static final Color HARDLIGHT_SHOT_TRAIL2_COLOR_START = new Color(125, 175, 255);
    private static final Color HARDLIGHT_SHOT_TRAIL2_COLOR_END = new Color(75, 125, 255);



    private static final float SIXTY_FPS = 1f / 60f;

    private static final String DATA_KEY = "Nightcross_Trails";

    private CombatEngineAPI engine;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) {
            return;
        }
        if (engine.isPaused()) {
            return;
        }

        final LocalData localData = (LocalData) engine.getCustomData().get(DATA_KEY);
        final Map<DamagingProjectileAPI, TrailData> trailMap = localData.trailMap;

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        int size = projectiles.size();
        double trailCount = 0f;
        for (int i = 0; i < size; i++) {
            DamagingProjectileAPI projectile = projectiles.get(i);
            if (projectile.getProjectileSpecId() == null) {
                continue;
            }

            switch (projectile.getProjectileSpecId()) {
                case AMP_PROJ_ID:
                    if (NAUtil.isOnscreen(projectile.getLocation(), projectile.getVelocity().length() * 0.2f)) {
                        trailCount += 2f;
                    }
                    break;
                case HARDLIGHT_SHOT:
                    if (NAUtil.isOnscreen(projectile.getLocation(), projectile.getVelocity().length() * 0.2f)) {
                        trailCount += 2f;
                    }
                    break;
                case PYRO_PROJ_ID:
                case META_PROJ_ID:
                case MINIRAZOR_ID:

                case PYROWISP_LARGE_PROJ_ID:
                case WAVEFRONT_SUB:
                case SUPERBLASTER_PROJ_ID:
                case LASER_PROJ_ID:
                    if (NAUtil.isOnscreen(projectile.getLocation(), projectile.getVelocity().length() * 0.2f)) {
                        trailCount += 1f;
                    }
                    break;
                default:
                    break;
            }
        }

        float trailFPSRatio = Math.min(3f, (float) Math.max(1f, (trailCount / 30f)));

        for (int i = 0; i < size; i++) {
            DamagingProjectileAPI proj = projectiles.get(i);
            String spec = proj.getProjectileSpecId();
            TrailData data;
            if (spec == null) {
                continue;
            }

            switch (spec) {
                case LASER_PROJ_ID:
                case PYRO_PROJ_ID:
                case META_PROJ_ID:
                case MINIRAZOR_ID:
                case SUPERBLASTER_PROJ_ID:
                case PYROWISP_LARGE_PROJ_ID:
                case WAVEFRONT_SUB:
                case HARDLIGHT_SHOT:
                case AMP_PROJ_ID:
                    data = trailMap.get(proj);
                    if (data == null) {
                        data = new TrailData();
                        data.id = MagicTrailPlugin.getUniqueID();

                        switch (spec) {
                            case AMP_PROJ_ID:
                                data.id2 = MagicTrailPlugin.getUniqueID();
                                break;
                            case HARDLIGHT_SHOT:
                                data.id2 = MagicTrailPlugin.getUniqueID();
                                break;

                            default:
                                break;
                        }
                    }

                    trailMap.put(proj, data);
                    break;

                default:
                    continue;
            }

            if (!data.enabled) {
                continue;
            }

            float fade = 1f;
            if (proj.getBaseDamageAmount() > 0f) {
                fade = proj.getDamageAmount() / proj.getBaseDamageAmount();
            }

            if (fade <= 0f) {
                continue;
            }

            fade = Math.max(0f, Math.min(1f, fade));

            Vector2f projVel = new Vector2f(proj.getVelocity());
            Vector2f projBodyVel = VectorUtils.rotate(new Vector2f(projVel), -proj.getFacing());
            Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
            Vector2f sidewaysVel = VectorUtils.rotate(new Vector2f(projLateralBodyVel), proj.getFacing());

            Vector2f spawnPosition = new Vector2f(proj.getLocation());
            if (proj.getSpawnType() != ProjectileSpawnType.BALLISTIC_AS_BEAM) {
                spawnPosition.x += sidewaysVel.x * amount * -1.05f;
                spawnPosition.y += sidewaysVel.y * amount * -1.05f;
            }

            switch (spec) {
                case AMP_PROJ_ID:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                35f, /* startSize */
                                10f, /* endSize */
                                AMP_TRAIL_COLOR_START, /* startColor */
                                AMP_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                0.35f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                0f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id2, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrail"), /* sprite */
                                spawnPosition, /* position */
                                0, /* startSpeed */
                                0, /* endSpeed */
                                proj.getFacing(), /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                75f, /* startSize */
                                35f, /* endSize */
                                AMP_TRAIL_COLOR_START, /* startColor */
                                AMP_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                0.9f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                25f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                case HARDLIGHT_SHOT:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                30f, /* startSize */
                                15f, /* endSize */
                                HARDLIGHT_SHOT_TRAIL_COLOR_START, /* startColor */
                                HARDLIGHT_SHOT_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0.2f, /* inDuration */
                                0.1f, /* mainDuration */
                                0.8f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                0f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id2, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrail"), /* sprite */
                                spawnPosition, /* position */
                                0, /* startSpeed */
                                0, /* endSpeed */
                                proj.getFacing(), /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                50f, /* startSize */
                                50f, /* endSize */
                                HARDLIGHT_SHOT_TRAIL2_COLOR_START, /* startColor */
                                HARDLIGHT_SHOT_TRAIL2_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                0.9f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                25f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                case LASER_PROJ_ID:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                15f, /* startSize */
                                5f, /* endSize */
                                LASER_TRAIL_COLOR_START, /* startColor */
                                LASER_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0.3f, /* mainDuration */
                                1.1f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                0f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                case PYRO_PROJ_ID:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                7f, /* startSize */
                                3f, /* endSize */
                                PYRO_TRAIL_COLOR_START, /* startColor */
                                PYRO_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0.1f, /* mainDuration */
                                0.25f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                0f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                case META_PROJ_ID:
                case MINIRAZOR_ID:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_smoketrail"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                8f, /* startSize */
                                32f, /* endSize */
                                META_TRAIL_COLOR_START, /* startColor */
                                META_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0.22f, /* mainDuration */
                                0.35f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE_MINUS_SRC_ALPHA, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                16f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                case SUPERBLASTER_PROJ_ID:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_smoketrail"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                32f, /* startSize */
                                8f, /* endSize */
                                SUPERBLASTER_TRAIL_COLOR_START, /* startColor */
                                SUPERBLASTER_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0.22f, /* mainDuration */
                                0.35f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE_MINUS_SRC_ALPHA, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                16f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                case PYROWISP_LARGE_PROJ_ID:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                42f, /* startSize */
                                24f, /* endSize */
                                PYROWISP_LARGE_TRAIL_COLOR_START, /* startColor */
                                PYROWISP_LARGE_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0.1f, /* mainDuration */
                                0.25f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                0f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                case WAVEFRONT_SUB:
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 10f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                spawnPosition, /* position */
                                100f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing(), /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                10f, /* startSize */
                                5f, /* endSize */
                                WAVEFRONT_SUB_TRAIL_COLOR_START, /* startColor */
                                WAVEFRONT_SUB_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0.2f, /* mainDuration */
                                0.8f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                0f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                    break;
                default:
                    break;
            }
        }

        /* Clean up */
        Iterator<DamagingProjectileAPI> iter = trailMap.keySet().iterator();
        while (iter.hasNext()) {
            DamagingProjectileAPI proj = iter.next();
            if (!engine.isEntityInPlay(proj)) {
                iter.remove();
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public static void createIfNeeded() {
        if (!NAModPlugin.hasMagicLib) {
            return;
        }

        if (Global.getCombatEngine() != null) {
            if (!Global.getCombatEngine().getCustomData().containsKey(DATA_KEY)) {
                Global.getCombatEngine().getCustomData().put(DATA_KEY, new LocalData());
                Global.getCombatEngine().addPlugin(new Nightcross_Trails());
            }
        }
    }

    private static final class LocalData {
        final Map<DamagingProjectileAPI, TrailData> trailMap = new LinkedHashMap<>(100);
    }

    private static final class TrailData {
        Float id = null;
        Float id2 = null;
        IntervalUtil interval = null;
        boolean cut = false;
        boolean enabled = true;
    }
}
