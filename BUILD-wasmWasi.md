# Building the wasmWasi klibs

End-to-end steps to produce every `*-wasm-wasi.klib` in `~/.m2/repository/`.

See `README-wasmWasi.md` for the design rationale.

## Prerequisites

* JDK 17+ on `PATH`
* `~/.m2/` writable (we use mavenLocal as our artifact store)
* The skiko fork checked out at `~/skiko/` (sibling to this
  compose-multiplatform-core checkout). The fork provides the
  `skiko-wasm-wasi` shim that every Stage-3 compose module's
  `skikoMain` source set depends on.
* Internet access on first run — Gradle fetches `kotlinx-coroutines-core-wasm-wasi`,
  `kotlinx-serialization-core-wasm-wasi`, `kotlinx-datetime`, and other
  published wasi variants from public repos.

## Conventions used in every command

* **`-Dorg.gradle.configureondemand=false`** — the fork's `gradle.properties`
  enables Configuration-on-Demand which is incompatible with Kotlin/Wasm
  targets (see KT-52074). For the *publish* phase you'll see
  `Could not determine the dependencies` errors without this flag.
  Compile-only tasks work without it.
* **`--no-daemon`** — keeps memory footprint predictable across the long
  publish chain.
* **`--no-configuration-cache`** — the fork's KMP setup currently isn't
  config-cache-safe.
* **`--console=plain`** — quieter logs (less noise scraping for errors).

A reusable env-var helper:

```bash
GRADLE_OPTS_WASI="-Dorg.gradle.configureondemand=false --console=plain --no-daemon --no-configuration-cache"
```

## Step 0 — Build & publish the skiko-wasm-wasi shim

```bash
cd ~/skiko/skiko
./gradlew publishWasmWasiPublicationToMavenLocal --console=plain --no-daemon
```

Expected: `~/.m2/repository/org/jetbrains/skiko/skiko-wasm-wasi/0.0.0-SNAPSHOT/`
with `.klib`, `.module`, `.pom`, `-sources.jar`.

Re-run this whenever you modify the skiko fork.

## Step 1 — Build & publish the 16 compatibility stubs

Order matters because each stub's `pom`/`.module` may reference earlier
ones (e.g. lifecycle-runtime → lifecycle-common). Stubs that *publish*
their target-specific artifacts only — no umbrella — are listed first.
Then we publish the **decorated** umbrellas for the half of them that
need umbrellas for transitive Gradle variant resolution.

```bash
cd ~/wart/compose-multiplatform-core
GRADLE_OPTS_WASI="-Dorg.gradle.configureondemand=false --console=plain --no-daemon --no-configuration-cache"

# 1.1 — leaf-first ordering so transitives resolve as we go.
for proj in \
    :annotation:annotation \
    :collection:collection \
    :window:window-core \
    :lifecycle:lifecycle-common \
    :lifecycle:lifecycle-runtime \
    :lifecycle:lifecycle-viewmodel \
    :lifecycle:lifecycle-viewmodel-savedstate \
    :savedstate:savedstate \
    :navigation:navigation-common \
    :navigation:navigation-runtime \
    :compose:runtime:runtime \
    :compose:runtime:runtime-saveable \
    :lifecycle:lifecycle-runtime-compose \
    :lifecycle:lifecycle-viewmodel-compose \
    :savedstate:savedstate-compose \
    :navigationevent:navigationevent-compose
do
    ./gradlew "${proj}:publishWasmWasiPublicationToMavenLocal" $GRADLE_OPTS_WASI
done

# 1.2 — umbrellas (only the stubs whose downstream consumers need an
# `org.jetbrains.androidx.X:Y:9999.0.0-SNAPSHOT` umbrella module file
# for Gradle variant resolution).
for proj in \
    :annotation:annotation \
    :collection:collection \
    :window:window-core \
    :lifecycle:lifecycle-common \
    :lifecycle:lifecycle-runtime \
    :lifecycle:lifecycle-viewmodel \
    :lifecycle:lifecycle-viewmodel-savedstate \
    :savedstate:savedstate \
    :navigation:navigation-common \
    :navigation:navigation-runtime \
    :compose:runtime:runtime
do
    ./gradlew "${proj}:publishKotlinMultiplatformDecoratedPublicationToMavenLocal" $GRADLE_OPTS_WASI
done
```

Verify any one:

```bash
ls ~/.m2/repository/org/jetbrains/compose/collection-internal/collection-wasm-wasi/9999.0.0-SNAPSHOT/
# collection-wasm-wasi-9999.0.0-SNAPSHOT.klib
# collection-wasm-wasi-9999.0.0-SNAPSHOT.module
# collection-wasm-wasi-9999.0.0-SNAPSHOT.pom
# collection-wasm-wasi-9999.0.0-SNAPSHOT-sources.jar
# maven-metadata-local.xml
```

## Step 2 — Build & publish the 2 supporting real-source modules

These are newly-registered in `settings.gradle` (`includeProject(...)`
lines). They aren't part of the original "13 real-source compose
modules" list but the compose modules need them.

```bash
./gradlew :compose:runtime:runtime-retain:publishWasmWasiPublicationToMavenLocal $GRADLE_OPTS_WASI
./gradlew :compose:runtime:runtime-retain:publishKotlinMultiplatformPublicationToMavenLocal $GRADLE_OPTS_WASI

./gradlew :graphics:graphics-shapes:publishWasmWasiPublicationToMavenLocal $GRADLE_OPTS_WASI
./gradlew :graphics:graphics-shapes:publishKotlinMultiplatformDecoratedPublicationToMavenLocal $GRADLE_OPTS_WASI
```

Note: `runtime-retain` uses standard `kotlinMultiplatform` publication
(no `Decorated` suffix) because it doesn't have `JetBrainsAndroidXPlugin`
applied. `graphics-shapes` does have it and uses the decorated variant
like the compose modules.

Expected:

```bash
ls ~/.m2/repository/androidx/compose/runtime/runtime-retain-wasm-wasi/1.12.0-alpha02/
ls ~/.m2/repository/androidx/graphics/graphics-shapes-wasm-wasi/1.1.0-rc01/
```

## Step 3 — Build & publish the 13 real-source compose modules

Dep order matters; this list is bottom-up:

```bash
for proj in \
    :compose:ui:ui-util \
    :compose:ui:ui-geometry \
    :compose:ui:ui-unit \
    :compose:ui:ui-graphics \
    :compose:ui:ui-text \
    :compose:ui:ui-backhandler \
    :compose:ui:ui \
    :compose:foundation:foundation-layout \
    :compose:animation:animation-core \
    :compose:animation:animation \
    :compose:foundation:foundation \
    :compose:material:material-ripple \
    :compose:material3:material3
do
    ./gradlew "${proj}:publishWasmWasiPublicationToMavenLocal" $GRADLE_OPTS_WASI
done
```

Each publish takes 1–4 minutes on a workstation; the full chain is
~30–40 minutes from a clean state.

Verify:

```bash
ls ~/.m2/repository/org/jetbrains/compose/material3/material3-wasm-wasi/9999.0.0-SNAPSHOT/
ls ~/.m2/repository/org/jetbrains/compose/foundation/foundation-wasm-wasi/9999.0.0-SNAPSHOT/
```

## Step 4 — Consuming the klibs

In a downstream wasmWasi project:

```kotlin
// build.gradle.kts
repositories {
    mavenLocal()
    mavenCentral()
    google()
}

kotlin {
    wasmWasi { binaries.library(); nodejs() }
    sourceSets {
        val wasmWasiMain by getting {
            dependencies {
                // Compose runtime & UI
                api("org.jetbrains.compose.runtime:runtime-wasm-wasi:9999.0.0-SNAPSHOT")
                api("org.jetbrains.compose.ui:ui-wasm-wasi:9999.0.0-SNAPSHOT")
                api("org.jetbrains.compose.foundation:foundation-wasm-wasi:9999.0.0-SNAPSHOT")
                api("org.jetbrains.compose.material3:material3-wasm-wasi:9999.0.0-SNAPSHOT")
                // The Skia shim every UI module links against
                api("org.jetbrains.skiko:skiko-wasm-wasi:0.0.0-SNAPSHOT")
            }
        }
    }
}
```

You'll typically need the same exclude+substitute boilerplate as the
in-tree modules to keep transitive `androidx.*` maven coords (no wasi
variants) from drifting onto the classpath. See any
`compose/*/build.gradle` for a copy-pasteable template.

## Step 5 — Incremental rebuilds

Most edits only require republishing the affected module:

```bash
./gradlew :compose:ui:ui:publishWasmWasiPublicationToMavenLocal $GRADLE_OPTS_WASI
```

If you change something in `:compose:runtime:runtime` (the stub) or in
the upstream `compose/runtime/runtime/src/commonMain/kotlin/...` files
that the stub pulls via `srcDirs`, you'll need to republish every
module that depends on runtime — that's roughly *all* of them. In
practice it's cheaper to re-run the whole Step-3 loop than to figure
out the transitive set.

**When you rebuild skiko (`~/skiko/`) you MUST republish
every compose module that consumed it**, otherwise the older klibs
reference symbols that have shifted in the skiko klib's ABI.
At minimum:

```bash
for proj in \
    :compose:ui:ui-graphics :compose:ui:ui-text :compose:ui:ui \
    :compose:foundation:foundation-layout :compose:foundation:foundation \
    :compose:animation:animation-core :compose:animation:animation \
    :compose:material:material-ripple :compose:material3:material3
do
    ./gradlew "${proj}:publishWasmWasiPublicationToMavenLocal" $GRADLE_OPTS_WASI
done
```

## Step 6 — Bootstrapping fresh from a wipe

If you nuke `~/.m2/repository/` (or move to a new machine) and want
everything wasi-related rebuilt:

```bash
# 1) skiko first
cd ~/skiko/skiko
./gradlew publishWasmWasiPublicationToMavenLocal --console=plain --no-daemon

# 2) all 16 stubs (Step 1.1 + 1.2 loops above)
# 3) runtime-retain + graphics-shapes (Step 2)
# 4) the 13 compose modules (Step 3)
```

End-to-end clean-rebuild time: ~50–70 minutes on an 8-core workstation
with warm Gradle daemon (which `--no-daemon` doesn't actually use, so
this estimate uses the daemon-off worst case).

## The sed helper for JVM-annotation stripping

A handful of upstream `*.nonAndroid.kt` / `*.nonJvm.kt` files use Kotlin
JVM annotations (`@file:JvmName(...)`, `@JvmStatic`, `@JvmInline`, etc.)
which Kotlin 2.4 forbids outside `commonMain`. We keep a sed script at
`/tmp/strip_jvm_annots.sed`. Re-create it if needed:

```bash
cat > /tmp/strip_jvm_annots.sed <<'EOF'
/^@file:Jvm[A-Za-z]/d
/^import kotlin\.jvm\.\(JvmName\|JvmMultifileClass\|JvmStatic\|JvmInline\|JvmField\|JvmSynthetic\|JvmOverloads\|JvmSuppressWildcards\|JvmDefaultWithCompatibility\|JvmDefaultWithoutCompatibility\|Synchronized\|Strictfp\|Transient\|Volatile\)$/d
/^import kotlin\.Throws$/d
s/@JvmName([^)]*)//g
s/@JvmOverloads//g
s/@JvmStatic//g
s/@JvmInline//g
s/@JvmField//g
s/@JvmSynthetic//g
s/@JvmSuppressWildcards//g
s/@JvmDefaultWithCompatibility//g
s/@JvmDefaultWithoutCompatibility//g
s/@Throws([^)]*)//g
s/@Synchronized//g
s/@Strictfp//g
s/@Transient//g
s/@Volatile//g
EOF
```

Usage — given an upstream file with JVM annotations, drop a stripped
copy into our `src/wasmWasiMain/kotlin/`:

```bash
sed -f /tmp/strip_jvm_annots.sed \
    .../upstream/src/nonAndroidMain/kotlin/.../Foo.nonAndroid.kt \
    > .../stub/src/wasmWasiMain/kotlin/.../Foo.wasmWasi.kt
```

Then `kotlin.exclude("**/Foo.nonAndroid.kt")` in the stub's
wasmWasiMain to keep the upstream copy out.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Could not resolve org.jetbrains.androidx.X:Y:9999.0.0-SNAPSHOT` | The umbrella isn't published. Either substitute to `Y-wasm-wasi:9999.0.0-SNAPSHOT` directly, or run that module's `publishKotlinMultiplatformDecoratedPublicationToMavenLocal`. |
| `Could not resolve androidx.X:Y:VERSION` (real androidx coords) | No wasi variant exists upstream. Add `exclude group: "androidx.X", module: "Y"` *and* a `resolutionStrategy.dependencySubstitution { substitute(...).using(...) }` redirect. |
| `KLIB loader: same 'unique_name=...' in more than one library` | Same module is on the classpath via both `project(...)` and an `m2` dep. Drop one — usually the m2 dep, since project deps take priority. |
| `Declaration annotated with '@OptionalExpectation' can only be used in common module sources.` | An `@Jvm*`/`@Throws` annotation in a non-commonMain file. Either strip it (sed helper) or add `freeCompilerArgs.add("-Xexpect-actual-classes")` + `suppressWarnings.set(true)` to the compile task. |
| `Cannot determine the dependencies of task ... publishWasmWasiPublicationToMavenLocal` configuring `compose/animation/animation` | Configuration-on-Demand kicked in. Add `-Dorg.gradle.configureondemand=false`. |
| `Kotlin/Wasm standard library has the ABI version (2.3.0)` | The ABI-pinning didn't kick in. Verify `configurePinnedKotlinLibraries(WASM_WASI)` is called from `AndroidXMultiplatformExtension.wasmWasi()`. |

## Module → publish task quick reference

| Module path | Task |
|---|---|
| All compatibility stubs | `:X:Y:publishWasmWasiPublicationToMavenLocal` (+ `publishKotlinMultiplatformDecoratedPublicationToMavenLocal` for umbrellas) |
| `runtime-retain` | `publishWasmWasiPublicationToMavenLocal` + `publishKotlinMultiplatformPublicationToMavenLocal` |
| `graphics-shapes` | `publishWasmWasiPublicationToMavenLocal` + `publishKotlinMultiplatformDecoratedPublicationToMavenLocal` |
| Real-source compose modules | `publishWasmWasiPublicationToMavenLocal` |
| Skiko fork | (in `~/skiko/skiko/`) `publishWasmWasiPublicationToMavenLocal` |
