import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.campaign.plugins.NAModPlugin;
import data.scripts.everyframe.Nightcross_Homing;
import data.scripts.weapons.NA_TrailHomingWeapon;
import data.scripts.weapons.NA_StunpulseHoming;
import java.lang.reflect.*;
import java.util.*;

public class HomingRegression {
    static final String KEY = "Nightcross_Homing";
    static int cases;
    static class Battle implements InvocationHandler {
        final Map<String,Object> data = new HashMap<>();
        int homingAdds;
        boolean reject;
        final CombatEngineAPI api = (CombatEngineAPI) Proxy.newProxyInstance(
            CombatEngineAPI.class.getClassLoader(), new Class[]{CombatEngineAPI.class}, this);
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("getCustomData")) return data;
            if (method.getName().equals("addPlugin")) {
                if (args[0] instanceof Nightcross_Homing) {
                    if (reject) throw new IllegalStateException("simulated registration failure");
                    homingAdds++;
                }
                ((EveryFrameCombatPlugin)args[0]).init(api);
                return null;
            }
            if (method.getName().equals("toString")) return "TestBattle";
            throw new AssertionError("Unexpected engine call: "+method);
        }
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static Battle battle() { Battle b = new Battle(); Global.setCombatEngine(b.api); return b; }
    public static void main(String[] args) {
        NAModPlugin.hasMagicLib = true;
        Global.setCombatEngine(null);
        Nightcross_Homing.createIfNeeded(); cases++;
        Battle first = battle();
        NA_TrailHomingWeapon gun = new NA_TrailHomingWeapon();
        for(int i=0;i<50;i++) gun.onFire(null,null,first.api);
        System.out.println("50 real weapon callbacks registered "+first.homingAdds+" homing handlers");
        check(first.homingAdds==1,"Expected one handler, got "+first.homingAdds); cases++;
        check(first.data.containsKey(KEY),"Successful registration lacks marker"); cases++;
        new NA_StunpulseHoming().onFire(null,null,first.api);
        check(first.homingAdds==1,"Second weapon callback duplicated handler"); cases++;
        Battle second = battle(); Nightcross_Homing.createIfNeeded();
        check(second.homingAdds==1 && first.homingAdds==1,"Handler must be per combat engine"); cases++;
        Battle disabled = battle(); NAModPlugin.hasMagicLib=false; Nightcross_Homing.createIfNeeded();
        check(disabled.homingAdds==0 && !disabled.data.containsKey(KEY),"Disabled dependency registered handler"); cases++;
        NAModPlugin.hasMagicLib=true;
        Battle failing = battle(); failing.reject=true;
        try { Nightcross_Homing.createIfNeeded(); throw new AssertionError("Expected failure"); }
        catch(IllegalStateException expected) { }
        check(!failing.data.containsKey(KEY),"Failed registration marked successful");
        failing.reject=false; Nightcross_Homing.createIfNeeded(); Nightcross_Homing.createIfNeeded();
        check(failing.homingAdds==1,"Failed registration must be retryable exactly once"); cases++;
        Global.setCombatEngine(null);
        System.out.println("PASS: "+cases+" homing registration cases");
    }
}
