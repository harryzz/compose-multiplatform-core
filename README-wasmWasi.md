# wasmWasi port of compose-multiplatform-core

This fork adds a `wasmWasi` target to the Compose Multiplatform module set so
that Compose UI, foundation, animation, material3, etc. compile to klibs
consumable by Kotlin/Wasm-WASI guests (e.g. wasmtime). It targets the
[wasi-android-runtime](https://github.com/harryzz/wasm-android-runtime) host
which renders via skia-safe on real Android GPU hardware.

This is **Option A** of the wasi-bring-up effort: in-tree port. Option B
(sibling modules at `~/wart/compose-*-wasi/`) remains for
comparison but is no longer the active path.

## What got built

All artifacts publish to `~/.m2/repository/`. Three groups:

### 16 JetBrains compatibility-stubs (`9999.0.0-SNAPSHOT`)

Stubs that on JVM/native/web redirect to maven `androidx.*` artifacts. For
`wasmWasi` they build from in-tree upstream sources via `srcDirs`.

| Group | Module |
|---|---|
| `org.jetbrains.compose.annotation-internal` | `annotation-wasm-wasi` |
| `org.jetbrains.compose.collection-internal` | `collection-wasm-wasi` |
| `org.jetbrains.compose.runtime` | `runtime-wasm-wasi`, `runtime-saveable-wasm-wasi` |
| `org.jetbrains.androidx.lifecycle` | `lifecycle-{common, runtime, viewmodel, viewmodel-savedstate, runtime-compose, viewmodel-compose}-wasm-wasi` |
| `org.jetbrains.androidx.navigation` | `navigation-common-wasm-wasi`, `navigation-runtime-wasm-wasi` |
| `org.jetbrains.androidx.navigationevent` | `navigationevent-compose-wasm-wasi` |
| `org.jetbrains.androidx.savedstate` | `savedstate-wasm-wasi`, `savedstate-compose-wasm-wasi` |
| `org.jetbrains.androidx.window` | `window-core-wasm-wasi` |

### 13 real-source compose modules (`9999.0.0-SNAPSHOT`)

Actual source modules in this fork, extended with a `wasmWasi` target.

| Group | Modules |
|---|---|
| `org.jetbrains.compose.ui` | `ui-util`, `ui-geometry`, `ui-unit`, `ui-graphics`, `ui-text`, `ui-backhandler`, `ui` |
| `org.jetbrains.compose.foundation` | `foundation-layout`, `foundation` |
| `org.jetbrains.compose.animation` | `animation-core`, `animation` |
| `org.jetbrains.compose.material` | `material-ripple` |
| `org.jetbrains.compose.material3` | `material3` |

### 2 supporting real-source modules

Pulled into the build chain because compose modules transitively require them.

| GAV |
|---|
| `androidx.compose.runtime:runtime-retain-wasm-wasi:1.12.0-alpha02` |
| `androidx.graphics:graphics-shapes-wasm-wasi:1.1.0-rc01` |

The skia C++ shim used by the ui-graphics/ui-text/ui klibs is
`org.jetbrains.skiko:skiko-wasm-wasi:0.0.0-SNAPSHOT` — built from a
companion fork at `~/skiko/`.

## Strategy: stubs vs real-source modules

The fork already shipped two kinds of modules. We extended both:

* **Compatibility-stub** (`X-compatibility-stub/`): on JVM/Android/native/web
  this just exposes `api("androidx.X:Y:VERSION")` so consumers get the
  maven-published klib. There is no source in the stub itself.

  For `wasmWasi`, no maven variant exists. Each stub now adds:
  * `wasmWasi()` to `androidXMultiplatform`
  * An intermediate `wasmWasiUpstreamCommon` source set that pulls the
    real upstream module's `commonMain/kotlin` via `kotlin.srcDirs +=`
  * `wasmWasiMain` with `dependsOn(wasmWasiUpstreamCommon)` plus
    `nonJvmMain/kotlin` and (where applicable) `webMain/kotlin` srcDirs
  * A `configurations.matching { it.name.startsWith("wasmWasi") }.all { exclude … }`
    block to drop the maven `androidx.X:Y` transitively pulled by
    `commonMain`'s `api(...)`

* **Real-source module** (`compose/ui/ui/`, etc.): genuine sources, already
  building for jvm/android/desktop/ios/native/wasmJs. Adding `wasmWasi`
  meant:
  * `wasmWasi()` in `androidXMultiplatform`
  * `wasmWasiMain.dependsOn(nonJvmMain)` — mirrors the `nativeMain`
    pattern, inheriting `skikoMain` content (compose's skia-using actuals)
  * A `resolutionStrategy.dependencySubstitution { ... }` block that
    forces every legacy `androidx.*` and JetBrains-fork-published
    `org.jetbrains.androidx.*` maven coord to redirect to our locally
    published `*-wasm-wasi` artifacts

## Cross-cutting changes

### `buildSrc/private/.../AndroidXMultiplatformExtension.kt`

* Added `fun wasmWasi(block: Action<KotlinWasmTargetDsl>? = null)`. It
  registers `PlatformIdentifier.WASM_WASI`, calls
  `kotlinExtension.wasmWasi { binaries.library() }`, and invokes
  `configurePinnedKotlinLibraries(platform)` so the wasm-wasi stdlib pins
  to the compiler's ABI level (otherwise old maven deps drag in
  `kotlin-stdlib-wasm-wasi:2.3.0` and the compile fails with
  "Kotlin/Wasm standard library has the ABI version (2.3.0)…").
* `configurePinnedKotlinLibraries` now handles `WASM_WASI` (skipping the
  test source set, which the fork doesn't wire on wasi).
* `applyAndroidXDefaultHierarchyTemplate` adds
  `group("wasmWasi") { withWasmWasi() }` under `nonJvm` (separate from
  `web` so wasmWasi doesn't pick up browser-only Kotlin sources).

### `gradle/libs.versions.toml` + buildSrc

* Bumped Kotlin to **2.4.0-RC**.
* Bumped `composeCompilerPlugin`, `kotlinGradlePluginAnnotations`,
  `kotlinGradlePluginApi`, `kotlinNativeUtils`, `kotlinToolingCore` to
  the matching 2.4.0-RC versions.
* `buildSrc/shared.gradle`: `languageVersion` to `KOTLIN_2_2`
  (`KOTLIN_2_1` is deprecated in 2.4).
* `JetBrainsCompatibilityVersions.kt`: `JETBRAINS_COMPILE_KOTLIN_VERSION = KOTLIN_2_3`.
* `AndroidXConfiguration.kt`: added `KOTLIN_2_4` entry, set `LATEST` to
  it.

### `settings.gradle`

Added three new project registrations needed by the wasi compile chain:

```
includeProject(":compose:runtime:runtime-retain")
includeProject(":graphics:graphics-shapes")
```

(`runtime-annotation` and `navigationevent` base modules remain
unregistered — their commonMain sources are pulled via `srcDirs` from
the projects that need them.)

### skiko (separate fork at `~/skiko/`)

Added `org/jetbrains/skia/icu/CharProperties.wasi.kt` — a stub of
the ICU binary-property lookup that `compose-foundation`'s
`StringHelpers.skiko.kt` needs for emoji-aware cursor stepping. The stub
returns `false` for all property checks, so cursor stepping degrades to
codepoint-boundary stepping (no extended emoji clustering) on wasi.

## Recipe details, in one place

### Stub pattern

```groovy
plugins {
    id("AndroidXPlugin")
    id("JetBrainsAndroidXPlugin")
    // alias(libs.plugins.kotlinSerialization)   // add if upstream uses kotlinx.serialization
}

androidXMultiplatform {
    // ... existing targets ...
    wasmWasi()

    sourceSets {
        commonMain { /* existing api(maven) deps */ }

        wasmWasiUpstreamCommon {
            dependsOn(commonMain)
            kotlin.srcDirs += [
                "$rootDir/path/to/upstream/src/commonMain/kotlin",
            ]
            // optInToExperimentalContracts() if upstream uses it
        }
        wasmWasiMain {
            // KGP emits a misleading "redundant dependsOn" warning here.
            // Without this edge, wasmWasiMain can't see upstream commonMain expects.
            dependsOn(wasmWasiUpstreamCommon)
            kotlin.srcDirs += [
                "$rootDir/path/to/upstream/src/nonJvmMain/kotlin",
                "$rootDir/path/to/upstream/src/webMain/kotlin",
            ]
            dependencies {
                // direct refs to other published *-wasm-wasi klibs
            }
        }
    }
}

configurations.matching { it.name.startsWith("wasmWasi") }.all {
    exclude group: "androidx.X", module: "Y"
    // …one exclude per legacy maven coord with no wasi variant…
}
```

### Real-source pattern

```groovy
androidXMultiplatform {
    // … existing targets …
    wasmWasi()

    sourceSets {
        wasmWasiMain {
            dependsOn(nonJvmMain)   // mirrors nativeMain
            dependencies {
                api("org.jetbrains.skiko:skiko-wasm-wasi:0.0.0-SNAPSHOT")
            }
        }
    }
}

tasks.matching { it.name == "compileKotlinWasmWasi" }.configureEach { task ->
    task.compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        allWarningsAsErrors.set(false)
        suppressWarnings.set(true)
    }
    // task.exclude("…") for inherited skiko/web files that won't compile
}

configurations.matching { it.name.startsWith("wasmWasi") }.all {
    exclude group: "androidx.X", module: "Y"
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.X:Y")).using(module("…:Y-wasm-wasi:VERSION"))
    }
}
```

### Stripping JVM-only annotations from upstream sources

Kotlin 2.4 forbids `@OptionalExpectation` annotations
(`@JvmField`, `@JvmName`, `@JvmInline`, `@JvmStatic`, `@JvmSuppressWildcards`,
`@JvmMultifileClass`, `@Throws`, etc.) outside `commonMain`-style fragments.
Upstream `*.nonJvm.kt` / `*.nonAndroid.kt` files often use these. For each
such file we:

1. `kotlin.exclude(...)` or `task.exclude(...)` the upstream copy
2. Drop a sed-stripped copy under our own `src/wasmWasiMain/kotlin/...`
   (renamed to `*.wasmWasi.kt` so its name doesn't collide with the
   exclude pattern)

A reusable sed script lives at `/tmp/strip_jvm_annots.sed` (see
BUILD-wasmWasi.md).

For some real-source modules we relied on the compiler-args trick
(`-Xexpect-actual-classes` + `suppressWarnings.set(true)`) instead of
file-by-file patching; that's faster but only works for warnings, not for
the hard `OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY` error.

## Hand-written wasi-specific actuals

Some upstream `expect` declarations have no JS- or native-equivalent we
can reuse on wasi. The fork ships small wasi-only actuals:

| Where | What | Strategy |
|---|---|---|
| `compose/runtime/runtime/.../WeakReference.wasmWasi.kt` | `WeakReference<T>` | Strong-reference fallback (wasi has no `WeakRef`) |
| `compose/runtime/runtime/.../Utils.wasmWasi.kt` | `identityHashCode()` | `Any.hashCode()` (no `WeakMap`-backed memoization) |
| `compose/runtime/runtime/.../MonotonicFrameClock.wasmWasi.kt` | `DefaultMonotonicFrameClock` | Deprecated no-op — host wires its own clock via the coroutine context |
| `compose/runtime/runtime/.../SnapshotId.wasmWasi.kt` | `SnapshotId`+helpers | Copy of upstream `wasmJsMain` impl (pure-Long, no JS deps) |
| `compose/runtime/runtime/.../ComposableLambda.wasmWasi.kt` | `ComposableLambda` | Copy of upstream `wasmJsMain` impl |
| `compose/runtime/runtime/.../OldIdentityHashCode.wasmWasi.kt` | Legacy public helper | Copy of upstream `wasmJsMain` impl |
| `compose/ui/ui-util/.../Trace.wasmWasi.kt` | `trace`+`traceValue` | No-op (wasi has no `android.os.Trace`/JS console.time) |
| `compose/ui/ui-graphics/.../ImageAsset.wasmWasi.kt` | `ByteArray.putBytesInto` | BGRA→ARGB pixel copy (single hand-written file) |
| `compose/ui/ui-text/src/wasmWasiMain/...` | 7 wasi actuals (Misc, PlatformString, PlatformFont, Locale, IsRtl, FontSynthesis, FontFamilyResolver) | Copied from `~/wart/compose-ui-text-wasi/` |
| `compose/ui/ui/src/wasmWasiMain/...` | 10 wasi actuals (UiActuals, Clipboard, UriHandler, WasiFrameDispatcher, IdentityHashCode, WeakReference, SemanticsRegion, DragAndDrop, InteropView, Synchronization) | Copied from `~/wart/compose-ui-wasi/` |
| `compose/foundation/foundation/src/wasmWasiMain/...` | 15 wasi actuals + replacements | Copied from `~/wart/compose-foundation-wasi/` |
| `compose/material3/material3/.../PlatformDateFormat.wasmWasi.kt`, `IdentityHashCode.wasmWasi.kt` | DateFormat + identity hash | Copied from `~/wart/compose-material3-wasi/` |

## Known limitations / loss of fidelity vs JVM/native

* `WeakReference` is a strong reference — anything held by a Compose
  internal `WeakReference` lives until the wrapper itself drops. Acceptable
  for short-lived render state; might leak under sustained UI churn.
* `identityHashCode(x)` returns `x.hashCode()`. Compose runtime treats this
  as opaque so the behavioural difference is only visible if app code
  reads identity hashes for its own bookkeeping.
* `DefaultMonotonicFrameClock.withFrameNanos` always reports `0L`. Real
  hosts must inject their own `MonotonicFrameClock` via the coroutine
  context (the wasi-android-runtime host does).
* Emoji-aware cursor stepping (`canBeEmojiOrPictographic`) always returns
  `false`. Cursor stepping falls back to plain codepoint boundaries.
  Real ICU `CharProperties` would need to be wired through the skiko C++
  shim.
* `ImageComposeScene` (testing helper) and accessibility
  `SemanticsRegion` are dropped from skikoMain — they need
  `Surface.makeRasterN32Premul` and ICU `IRect`/`Region`, neither in
  the wasi skia shim.
* `BrowserHistory.kt` (in navigation-runtime/webMain) is excluded — uses
  `external interface` for browser DOM, which isn't valid on wasi.
* AWT/desktop-specific files in foundation/desktopMain (~19 files
  related to clipboard, context menus, popup positioning, etc.) are
  excluded; their expects either don't matter on wasi or get hand-written.

## Files modified this branch

A non-exhaustive list of the touched paths. The diff is mostly additive
(new `wasmWasi { ... }` blocks and `src/wasmWasiMain/kotlin/...` files)
plus the Kotlin-2.4 version bumps in `buildSrc/`.

```
gradle/libs.versions.toml                                  # 2.4.0-RC bump
buildSrc/shared.gradle                                     # languageVersion KOTLIN_2_2
buildSrc/private/...AndroidXMultiplatformExtension.kt      # wasmWasi() registration
buildSrc/private/...JetBrainsCompatibilityVersions.kt      # KOTLIN_2_3
buildSrc/public/...AndroidXConfiguration.kt                # KOTLIN_2_4 entry + LATEST
buildSrc/private/...TestSourceSetsHelper.kt                # @Suppress("DEPRECATION")
settings.gradle                                            # 3 new includeProject lines
annotation/annotation-compatibility-stub/build.gradle      # +wasmWasi block
collection/collection-compatibility-stub/build.gradle      # +wasmWasi block
collection/collection-compatibility-stub/src/wasm…         # patched .wasmWasi.kt files + annotation stubs
window/window-core-compatibility-stub/build.gradle
lifecycle/lifecycle-{common, runtime, viewmodel, viewmodel-savedstate, runtime-compose, viewmodel-compose}-compatibility-stub/build.gradle
savedstate/savedstate-compatibility-stub/build.gradle
savedstate/savedstate-compose-compatibility-stub/build.gradle
navigation/navigation-{common, runtime}-compatibility-stub/build.gradle
navigationevent/navigationevent-compose-compatibility-stub/build.gradle
compose/runtime/runtime-compatibility-stub/build.gradle
compose/runtime/runtime-saveable-compatibility-stub/build.gradle
compose/runtime/runtime-retain/build.gradle                # real-source, prev. unregistered
graphics/graphics-shapes/build.gradle                      # real-source, prev. unregistered, API style updated
compose/ui/ui-util/build.gradle                            # +wasmWasi()
compose/ui/ui-geometry/build.gradle
compose/ui/ui-unit/build.gradle
compose/ui/ui-graphics/build.gradle
compose/ui/ui-text/build.gradle
compose/ui/ui-backhandler/build.gradle
compose/ui/ui/build.gradle
compose/foundation/foundation-layout/build.gradle
compose/foundation/foundation/build.gradle
compose/animation/animation-core/build.gradle
compose/animation/animation/build.gradle
compose/material/material-ripple/build.gradle
compose/material3/material3/build.gradle
+ src/wasmWasiMain/kotlin/... for all of the above where wasi-specific actuals exist
```

## See also

* `BUILD-wasmWasi.md` — exact gradle commands to reproduce the publish
* `~/skiko/` — the skiko fork supplying `skiko-wasm-wasi`
* `~/wart/compose-*-wasi/` — the Option B siblings (now
  superseded by this in-tree port)
