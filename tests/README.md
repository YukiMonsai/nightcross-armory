# Combat regression tests

These tests execute the actual Nightcross homing handler, weapon callbacks, and both RKKV AIs against proxies of the Starsector API. They do not start the game or require an OpenGL context.

Requirements: Python 3.9+, JDK 17+, a licensed Starsector 0.98a installation, and LazyLib, MagicLib, and GraphicsLib. Game and dependency JARs are not included. The repository's `jars/nightcross.jar` supplies unchanged Nightcross support classes; the three affected classes are compiled from source.

Run from the repository root, replacing these example paths with your installation paths:

```powershell
python tests/run-regressions --java-home "C:/path/to/jdk-17" --lib-dir "E:/Fractal Softworks/Starsector/starsector-core" --lib-dir "E:/Fractal Softworks/Starsector/mods/LazyLib/jars" --lib-dir "E:/Fractal Softworks/Starsector/mods/MagicLib/jars" --lib-dir "E:/Fractal Softworks/Starsector/mods/GraphicsLib/jars"
```

The same runner works on Linux/macOS with the appropriate paths. Omit `--java-home` if `java` and `javac` are on PATH. Compilation uses Java 17 bytecode and execution enables `-Xverify:all`.

Add `--baseline-ref 2de9c304cf4cca71e3eb3643b3b79ef59ed8581e` to compile and run the pre-fix source too. Add `--output-dir /path/to/new-results-directory` to retain compiled classes and a JSON result report. Baseline failures are comparison evidence; a patched-suite failure returns a failing exit code. The runner never modifies the supplied game installation or any JAR.

## Coverage

- **7 homing-registration cases:** 50 actual weapon callbacks, a second triggering weapon, engine-local lifetime, absent engine, disabled dependency, and failed registration followed by retry. A test-only `NAModPlugin` exposes the dependency flag without initializing campaign content; it is never packaged into the mod.
- **502 RKKV cases:** distance and world-space lead calculations, startup prediction, invalid current/inherited targets and fallback acquisition, aligned acceleration, and angular convergence at 16, 33, 67, 100, and 200 ms updates. Both projectile types use their shipped nominal turning parameters. All AI/library code is real; the combat entities and angular engine update are test doubles.

The angular model accounts for the engine applying queued turn acceleration or automatic braking after the AI. It omits battle translation, collisions, ECM, rendering, and HE payload detonation. Passing these tests does not establish live FPS improvement or complete behavior at accelerated combat speeds. Retest a fresh station battle and both projectile types in-game before release.

New test code follows the repository's [GPLv3 code license](../jars/LICENSE.txt).
