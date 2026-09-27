# Preeval Java Native — P0 Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the `core_functions_preeval` Pure repo and its shared, compiled and interpreted Java modules. Declare `prevalNative` (an identity implementation for now) and the `preevalImplementation()` switch. Route every `preval` call in compiled-core through the switch. The default stays `PURE`, so no behaviour changes.

**Architecture:** A new module family, `legend-engine-pure-code-functions-preeval`, sits below compiled-core and is modelled on `legend-engine-pure-code-functions-javaCompiler`:
- **Pure module:** Pure declarations, repo `core_functions_preeval`.
- **Shared module:** mode-agnostic Java that depends only on legend-pure APIs.
- **Compiled shell:** `AbstractNativeFunctionGeneric` pointing at a static method.
- **Interpreted shell:** a `NativeFunction`.

Compiled-core's public `preval` overloads E and F dispatch through `prevalWithImplementation`, which runs the Pure code (`prevalInternal`), the Java native, or both compared (`SHADOW`).

**Tech Stack:** Java 11, Maven, legend-pure 5.105.0 (m3/m4, compiled and interpreted runtimes), Pure, JUnit 5 for plain unit tests, JUnit 4 for Pure-harness tests (the legend-pure base class `AbstractPureTestWithCoreCompiled` is JUnit 4).

**Spec:** `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md` (this plan is phase **P0** of §5).

## Global Constraints

- JDK 11. Every Maven command is prefixed with `. /home/aziem/bin/jdk11.sh &&`, which sets `JAVA_HOME` and `MAVEN_OPTS`.
- Every Maven command carries the flags `-T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true`. This checkout is a git worktree; the revision stamp keeps installs out of the shared `4.x-SNAPSHOT` coordinates.
- Lifecycle builds always use `clean`. Build only the touched modules (`-pl`), except the one-off bootstrap in Task 0.
- To re-run tests with no source change, call Surefire directly: `mvn -pl <module> org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test ...`.
- Every new file starts with the Apache 2.0 header, first line `Copyright 2026 Goldman Sachs`, in the file's comment syntax.
- Java uses 4-space indent and braces on their own lines, including single-statement `if`. Pure uses 2-space indent. No tabs. Checkstyle is blocking.
- Comments only where code is genuinely ambiguous. No comments on tests.
- `maven-dependency-plugin` runs with `failOnWarning=true, ignoreNonCompile=true`. Every compile-scope dependency must be used, and every used artifact must be declared at compile scope.
- Commits are authored solely by the user: **no** `Co-Authored-By` or `Claude-Session` trailers. Commit message style is a sentence-case imperative, e.g. `Add preeval native scaffold`.
- Do not touch the untracked `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/interpreted/` directory. It is unrelated work in progress.
- The switch default is `PURE`. Nothing in this plan changes what production code produces.

## Refinements to the spec made by this plan

1. **`PrevalRuntime` is not created in P0.** In P0 the shared code needs nothing mode-specific beyond `ProcessorSupport`, which both modes already provide. The port is introduced in P1 alongside its first callers (reactivation, hook evaluation), each with a test.
2. **The Pure implementation keeps its current name.** It stays as `prevalInternal`. The switch sits in a new public function, `prevalWithImplementation`, which overloads E and F call. That replaces "rename the body to `prevalPure`" with no loss.
3. **The `SHADOW` mismatch message prints the normalised JSON** (the comparison key) and the flags, not Pure grammar.

Task 7 amends the spec to match.

## File Structure

```
legend-engine-core/legend-engine-core-pure/
├── pom.xml                                                          MODIFY: add module
├── legend-engine-pure-code-functions-preeval/
│   ├── pom.xml                                                      CREATE: family pom
│   ├── legend-engine-pure-functions-preeval-pure/
│   │   ├── pom.xml                                                  CREATE
│   │   └── src/main/
│   │       ├── java/org/finos/legend/engine/pure/preeval/CorePreevalCodeRepositoryProvider.java   CREATE
│   │       └── resources/
│   │           ├── META-INF/services/org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider   CREATE
│   │           ├── core_functions_preeval.definition.json          CREATE
│   │           └── core_functions_preeval/preeval.pure             CREATE: PrevalResult, PrevalHooks, 2 natives
│   ├── legend-engine-pure-runtime-java-extension-shared-functions-preeval/
│   │   ├── pom.xml                                                  CREATE
│   │   └── src/
│   │       ├── main/java/org/finos/legend/engine/pure/preeval/
│   │       │   ├── PreevalImplementation.java                       CREATE: PURE|JAVA|SHADOW from system property
│   │       │   ├── PrevalResult.java                                CREATE: immutable result (record-ready)
│   │       │   ├── Preevaluator.java                                CREATE: entry point (identity in P0)
│   │       │   └── PrevalResults.java                               CREATE: PrevalResult -> Pure PrevalResult instance
│   │       └── test/java/org/finos/legend/engine/pure/preeval/
│   │           ├── TestPreevalImplementation.java                   CREATE: JUnit 5
│   │           └── AbstractTestPrevalNative.java                    CREATE: JUnit 4 Pure harness, shipped as test-jar
│   ├── legend-engine-pure-runtime-java-extension-compiled-functions-preeval/
│   │   ├── pom.xml                                                  CREATE
│   │   └── src/
│   │       ├── main/java/org/finos/legend/engine/pure/preeval/compiled/
│   │       │   ├── CompiledPreeval.java                             CREATE: static targets for generated code
│   │       │   ├── PreevalCompiledExtension.java                    CREATE
│   │       │   └── natives/{PrevalNative,PreevalImplementationNative}.java   CREATE
│   │       ├── main/resources/META-INF/services/org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension   CREATE
│   │       └── test/java/org/finos/legend/engine/pure/preeval/compiled/TestPrevalNativeCompiled.java   CREATE
│   └── legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/
│       ├── pom.xml                                                  CREATE
│       └── src/
│           ├── main/java/org/finos/legend/engine/pure/preeval/interpreted/
│           │   ├── PreevalInterpretedExtension.java                 CREATE
│           │   └── natives/{PrevalNative,PreevalImplementationNative}.java   CREATE
│           ├── main/resources/META-INF/services/org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension   CREATE
│           └── test/java/org/finos/legend/engine/pure/preeval/interpreted/TestPrevalNativeInterpreted.java   CREATE
├── legend-engine-pure-code-compiled-core/
│   ├── pom.xml                                                      MODIFY: 3 deps + analyzer ignore
│   ├── src/main/resources/core.definition.json                      MODIFY: + core_functions_preeval
│   ├── src/main/resources/core/pure/router/preeval/preeval.pure     MODIFY: switch, hooks, shadow compare
│   ├── src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure   CREATE
│   └── src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval.java   CREATE: focused runner
└── legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/
    ├── pom.xml                                                      MODIFY: + interpreted module
    └── src/main/java/org/finos/legend/engine/ide/PureIDELight.java  MODIFY: + repo source
pom.xml (root)                                                       MODIFY: dependencyManagement x4
legend-engine-config/legend-engine-extensions-collection-generation/src/test/java/org/finos/legend/engine/extensions/collection/generation/TestExtensions.java   MODIFY
.github/workflows/resources/modulesToTest.json                       MODIFY: core group
docs/superpowers/specs/2026-09-27-preeval-java-native-design.md      MODIFY: refinements
```

Shorthand used below: `PE=legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval` and `CC=legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core`. Paths in commands are written out in full.

---

### Task 0: Bootstrap the stamped build

**Files:** none.

- [ ] **Step 1: Install everything up to compiled-core under the branch revision**

Run from the repo root (15–25 min):
```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core -am
```
Expected: `BUILD SUCCESS`. All later `-pl` builds resolve their engine dependencies from these `preeval-native-SNAPSHOT` installs.

---

### Task 1: The `core_functions_preeval` Pure repo

**Files:**
- Create: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/pom.xml`
- Create: `.../legend-engine-pure-functions-preeval-pure/pom.xml`
- Create: `.../legend-engine-pure-functions-preeval-pure/src/main/resources/core_functions_preeval.definition.json`
- Create: `.../legend-engine-pure-functions-preeval-pure/src/main/resources/core_functions_preeval/preeval.pure`
- Create: `.../legend-engine-pure-functions-preeval-pure/src/main/java/org/finos/legend/engine/pure/preeval/CorePreevalCodeRepositoryProvider.java`
- Create: `.../legend-engine-pure-functions-preeval-pure/src/main/resources/META-INF/services/org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider`
- Modify: `legend-engine-core/legend-engine-core-pure/pom.xml` (modules list, after `legend-engine-pure-code-functions-javaCompiler`)
- Modify: `pom.xml` (root `dependencyManagement`, after the `legend-engine-pure-runtime-java-extension-shared-functions-variant` entry)

**Interfaces:**
- Produces (Pure, repo `core_functions_preeval`, depends on `platform` only):
  - `Class meta::pure::functions::preeval::PrevalResult { value:Any[1]; canPreval:Boolean[1]; openVars:String[*]; modified:Boolean[1]; }`
  - `Class meta::pure::functions::preeval::PrevalHooks { stopPreeval:Function<{Any[*]->Boolean[1]}>[1]; shouldInline:Function<{Function<Any>[1]->Boolean[1]}>[1]; isGeneratedMilestoningProperty:Function<{Function<Any>[1]->Boolean[1]}>[1]; isGetAllFunction:Function<{Function<Any>[1]->Boolean[1]}>[1]; resolveTdsSchema:Function<{ValueSpecification[1], Map<String, List<Any>>[1]->Any[*]}>[1]; }`
  - `native function meta::pure::functions::preeval::prevalNative(item:Any[1], inScopeVars:Map<String, List<Any>>[1], rollingInScopeVars:Map<String, List<Any>>[1], hooks:PrevalHooks[1], debug:meta::pure::tools::DebugContext[1]):PrevalResult[1];` with native id `prevalNative_Any_1__Map_1__Map_1__PrevalHooks_1__DebugContext_1__PrevalResult_1_`
  - `native function meta::pure::functions::preeval::preevalImplementation():String[1];` with native id `preevalImplementation__String_1_`
- Maven artifact: `org.finos.legend.engine:legend-engine-pure-functions-preeval-pure`.

- [ ] **Step 1: Create the family pom**

`legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
 Copyright 2026 Goldman Sachs

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-core-pure</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>legend-engine-pure-code-functions-preeval</artifactId>
    <packaging>pom</packaging>
    <name>Legend Engine - Pure - Code - Core - Functions - Preeval</name>

    <modules>
        <module>legend-engine-pure-functions-preeval-pure</module>
    </modules>
</project>
```
Tasks 2–4 add the other three modules to `<modules>` as they are created.

- [ ] **Step 2: Create the Pure module pom**

`.../legend-engine-pure-functions-preeval-pure/pom.xml` (same XML header comment as Step 1):
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-pure-code-functions-preeval</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>legend-engine-pure-functions-preeval-pure</artifactId>
    <name>Legend Engine - Pure - Functions - Preeval - Pure</name>

    <build>
        <plugins>
            <plugin>
                <groupId>org.finos.legend.pure</groupId>
                <artifactId>legend-pure-maven-compiler</artifactId>
            </plugin>
            <plugin>
                <groupId>org.finos.legend.pure</groupId>
                <artifactId>legend-pure-maven-generation-par</artifactId>
                <configuration>
                    <sourceDirectory>src/main/resources</sourceDirectory>
                    <purePlatformVersion>${legend.pure.version}</purePlatformVersion>
                    <repositories>
                        <repository>platform</repository>
                        <repository>core_functions_preeval</repository>
                    </repositories>
                    <extraRepositories>
                        <extraRepository>${project.basedir}/src/main/resources/core_functions_preeval.definition.json</extraRepository>
                    </extraRepositories>
                </configuration>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>build-pure-jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m3-core</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create the repo definition**

`.../src/main/resources/core_functions_preeval.definition.json`:
```json
{
  "name": "core_functions_preeval",
  "pattern": "meta::pure::functions::preeval(::.*)?",
  "dependencies": [
    "platform"
  ]
}
```

- [ ] **Step 4: Create the Pure declarations**

`.../src/main/resources/core_functions_preeval/preeval.pure`:
```pure
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

Class meta::pure::functions::preeval::PrevalResult
{
  value     : Any[1];
  canPreval : Boolean[1];
  openVars  : String[*];
  modified  : Boolean[1];
}

Class meta::pure::functions::preeval::PrevalHooks
{
  stopPreeval                    : Function<{Any[*]->Boolean[1]}>[1];
  shouldInline                   : Function<{Function<Any>[1]->Boolean[1]}>[1];
  isGeneratedMilestoningProperty : Function<{Function<Any>[1]->Boolean[1]}>[1];
  isGetAllFunction               : Function<{Function<Any>[1]->Boolean[1]}>[1];
  resolveTdsSchema               : Function<{ValueSpecification[1], Map<String, List<Any>>[1]->Any[*]}>[1];
}

native function meta::pure::functions::preeval::prevalNative(item:Any[1], inScopeVars:Map<String, List<Any>>[1], rollingInScopeVars:Map<String, List<Any>>[1], hooks:meta::pure::functions::preeval::PrevalHooks[1], debug:meta::pure::tools::DebugContext[1]):meta::pure::functions::preeval::PrevalResult[1];

native function meta::pure::functions::preeval::preevalImplementation():String[1];
```

- [ ] **Step 5: Create the code repository provider and its service file**

`.../src/main/java/org/finos/legend/engine/pure/preeval/CorePreevalCodeRepositoryProvider.java`:
```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.pure.preeval;

import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

public class CorePreevalCodeRepositoryProvider implements CodeRepositoryProvider
{
    @Override
    public CodeRepository repository()
    {
        return GenericCodeRepository.build("core_functions_preeval.definition.json");
    }
}
```
`.../src/main/resources/META-INF/services/org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider`:
```
org.finos.legend.engine.pure.preeval.CorePreevalCodeRepositoryProvider
```

- [ ] **Step 6: Register the module and manage its versions**

In `legend-engine-core/legend-engine-core-pure/pom.xml`, after `<module>legend-engine-pure-code-functions-javaCompiler</module>`, add:
```xml
        <module>legend-engine-pure-code-functions-preeval</module>
```
In the root `pom.xml` `dependencyManagement`, directly after the `legend-engine-pure-runtime-java-extension-shared-functions-variant` `<dependency>` block, add:
```xml
            <dependency>
                <groupId>org.finos.legend.engine</groupId>
                <artifactId>legend-engine-pure-functions-preeval-pure</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.finos.legend.engine</groupId>
                <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.finos.legend.engine</groupId>
                <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
                <version>${project.version}</version>
                <type>test-jar</type>
            </dependency>
            <dependency>
                <groupId>org.finos.legend.engine</groupId>
                <artifactId>legend-engine-pure-runtime-java-extension-compiled-functions-preeval</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.finos.legend.engine</groupId>
                <artifactId>legend-engine-pure-runtime-java-extension-interpreted-functions-preeval</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 7: Build the Pure module**

The root and `legend-engine-core-pure` poms changed, so reinstall them non-recursively (`-N`) first, then build the new modules:
```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -N -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  && mvn clean install -N -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -f legend-engine-core/pom.xml \
  && mvn clean install -N -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -f legend-engine-core/legend-engine-core-pure/pom.xml \
  && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
     -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-functions-preeval-pure
```
Expected: `BUILD SUCCESS`. The `build-pure-jar` goal compiles `preeval.pure`, so a Pure syntax or type error fails here with file and line.

- [ ] **Step 8: Commit**

```bash
git add pom.xml legend-engine-core/legend-engine-core-pure/pom.xml legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval
git commit -m "Add core_functions_preeval Pure repo declaring the preeval natives"
```

---

### Task 2: Shared Java core (switch parsing, result type, identity evaluator, test harness)

**Files:**
- Create: `.../legend-engine-pure-runtime-java-extension-shared-functions-preeval/pom.xml`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/PreevalImplementation.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/PrevalResult.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/Preevaluator.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/PrevalResults.java`
- Test: `.../src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalImplementation.java`
- Create (test-jar): `.../src/test/java/org/finos/legend/engine/pure/preeval/AbstractTestPrevalNative.java`
- Modify: the family pom `<modules>`: add `<module>legend-engine-pure-runtime-java-extension-shared-functions-preeval</module>`

**Interfaces:**
- Consumes: Pure class path `meta::pure::functions::preeval::PrevalResult` with properties `value`, `canPreval`, `openVars`, `modified` (Task 1).
- Produces:
  - `enum PreevalImplementation { PURE, JAVA, SHADOW; static final String SYSTEM_PROPERTY = "legend.engine.preeval.implementation"; static PreevalImplementation current(); static PreevalImplementation parse(String value); }`
  - `final class PrevalResult { static PrevalResult unmodified(CoreInstance value); CoreInstance getValue(); boolean canPreval(); ImmutableList<String> getOpenVars(); boolean isModified(); }`
  - `final class Preevaluator { static PrevalResult preval(CoreInstance item); }`
  - `final class PrevalResults { static CoreInstance toPure(PrevalResult result, ProcessorSupport processorSupport); }`
  - Test-jar: `abstract class AbstractTestPrevalNative extends AbstractPureTestWithCoreCompiled`, with `protected static MutableRepositoryCodeStorage getCodeStorage()` and the tests `testPrevalNativeReturnsInputUnmodified` and `testPreevalImplementationIsAKnownValue`.

- [ ] **Step 1: Create the shared module pom**

`.../legend-engine-pure-runtime-java-extension-shared-functions-preeval/pom.xml` (standard XML header):
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-pure-code-functions-preeval</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
    <name>Legend Engine - Pure - Runtime - Java Extension - Shared - Functions - Preeval</name>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>test-jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m4</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m3-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m3-core</artifactId>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```
Surefire only runs the JUnit 5 tests in this module. `AbstractTestPrevalNative` is abstract and runs from the compiled and interpreted modules.

Add `<module>legend-engine-pure-runtime-java-extension-shared-functions-preeval</module>` to the family pom `<modules>`.

- [ ] **Step 2: Write the failing switch-parsing test**

`.../src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalImplementation.java` (Java header as in Task 1 Step 5):
```java
package org.finos.legend.engine.pure.preeval;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPreevalImplementation
{
    @Test
    public void testUnsetDefaultsToPure()
    {
        Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.parse(null));
    }

    @Test
    public void testBlankDefaultsToPure()
    {
        Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.parse("  "));
    }

    @Test
    public void testParsesCaseInsensitively()
    {
        Assertions.assertEquals(PreevalImplementation.JAVA, PreevalImplementation.parse("java"));
        Assertions.assertEquals(PreevalImplementation.SHADOW, PreevalImplementation.parse(" Shadow "));
    }

    @Test
    public void testRejectsUnknownValue()
    {
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> PreevalImplementation.parse("FAST"));
        Assertions.assertEquals("Invalid value 'FAST' for system property legend.engine.preeval.implementation; expected one of PURE, JAVA, SHADOW", e.getMessage());
    }
}
```

- [ ] **Step 3: Run it and confirm it fails**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval
```
Expected: `COMPILATION ERROR`, `cannot find symbol ... PreevalImplementation`.

- [ ] **Step 4: Implement the four main classes**

`PreevalImplementation.java`:
```java
package org.finos.legend.engine.pure.preeval;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum PreevalImplementation
{
    PURE,
    JAVA,
    SHADOW;

    public static final String SYSTEM_PROPERTY = "legend.engine.preeval.implementation";

    public static PreevalImplementation current()
    {
        return parse(System.getProperty(SYSTEM_PROPERTY));
    }

    public static PreevalImplementation parse(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return PURE;
        }
        try
        {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            String expected = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid value '" + value + "' for system property " + SYSTEM_PROPERTY + "; expected one of " + expected, e);
        }
    }
}
```
`PrevalResult.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class PrevalResult
{
    private final CoreInstance value;
    private final boolean canPreval;
    private final ImmutableList<String> openVars;
    private final boolean modified;

    public PrevalResult(CoreInstance value, boolean canPreval, ImmutableList<String> openVars, boolean modified)
    {
        this.value = value;
        this.canPreval = canPreval;
        this.openVars = openVars;
        this.modified = modified;
    }

    public static PrevalResult unmodified(CoreInstance value)
    {
        return new PrevalResult(value, true, Lists.immutable.empty(), false);
    }

    public CoreInstance getValue()
    {
        return this.value;
    }

    public boolean canPreval()
    {
        return this.canPreval;
    }

    public ImmutableList<String> getOpenVars()
    {
        return this.openVars;
    }

    public boolean isModified()
    {
        return this.modified;
    }
}
```
`Preevaluator.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class Preevaluator
{
    private Preevaluator()
    {
    }

    public static PrevalResult preval(CoreInstance item)
    {
        return PrevalResult.unmodified(item);
    }
}
```
`PrevalResults.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class PrevalResults
{
    private static final String PREVAL_RESULT = "meta::pure::functions::preeval::PrevalResult";

    private PrevalResults()
    {
    }

    public static CoreInstance toPure(PrevalResult result, ProcessorSupport processorSupport)
    {
        CoreInstance pureResult = processorSupport.newCoreInstance(null, PREVAL_RESULT, null);
        Instance.setValueForProperty(pureResult, "value", result.getValue(), processorSupport);
        Instance.setValueForProperty(pureResult, "canPreval", newBoolean(result.canPreval(), processorSupport), processorSupport);
        Instance.setValuesForProperty(pureResult, "openVars", result.getOpenVars().collect(name -> processorSupport.newCoreInstance(name, M3Paths.String, null)), processorSupport);
        Instance.setValueForProperty(pureResult, "modified", newBoolean(result.isModified(), processorSupport), processorSupport);
        return pureResult;
    }

    private static CoreInstance newBoolean(boolean value, ProcessorSupport processorSupport)
    {
        return processorSupport.newCoreInstance(Boolean.toString(value), M3Paths.Boolean, null);
    }
}
```

- [ ] **Step 5: Add the Pure-harness base class used by both runtime modules**

`.../src/test/java/org/finos/legend/engine/pure/preeval/AbstractTestPrevalNative.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestPrevalNative extends AbstractPureTestWithCoreCompiled
{
    private static final String HOOKS = "^PrevalHooks("
            + "stopPreeval = {a:Any[*] | false}, "
            + "shouldInline = {f:Function<Any>[1] | false}, "
            + "isGeneratedMilestoningProperty = {f:Function<Any>[1] | false}, "
            + "isGetAllFunction = {f:Function<Any>[1] | false}, "
            + "resolveTdsSchema = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | []})";

    @After
    public void deleteTestSource()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testPrevalNativeReturnsInputUnmodified()
    {
        execute(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let r = prevalNative({|1 + 1}, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert(!$r.modified, |'expected unmodified');",
                "assert($r.canPreval, |'expected canPreval');",
                "assert($r.openVars->isEmpty(), |'expected no open variables');",
                "assert($r.value->instanceOf(LambdaFunction), |'expected the input lambda back');");
    }

    @Test
    public void testPreevalImplementationIsAKnownValue()
    {
        execute("assert(preevalImplementation()->in(['PURE', 'JAVA', 'SHADOW']), |'unexpected: ' + preevalImplementation());");
    }

    private void execute(String... lines)
    {
        String code = "import meta::pure::functions::preeval::*;\n\nfunction test():Any[*]\n{\n    " + String.join("\n    ", lines) + "\n}\n";
        compileTestSource("fromString.pure", code);
        CoreInstance function = runtime.getFunction("test():Any[*]");
        try
        {
            functionExecution.start(function, Lists.immutable.empty());
        }
        catch (PureAssertFailException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        CodeRepositorySet repositories = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true))
                .build()
                .subset("core_functions_preeval");
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories.getRepositories()));
    }
}
```

- [ ] **Step 6: Run the tests and confirm they pass**

Same command as Step 3. Expected: `Tests run: 4, Failures: 0` for `TestPreevalImplementation`, then `BUILD SUCCESS`, and the dependency analyzer is clean. If the analyzer reports `Used undeclared` or `Unused declared`, fix the pom so it matches exactly what `src/main` imports. Do not add ignores for this module.

- [ ] **Step 7: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval
git commit -m "Add shared preeval Java core with implementation switch and identity evaluator"
```

---

### Task 3: Compiled-mode natives

**Files:**
- Create: `.../legend-engine-pure-runtime-java-extension-compiled-functions-preeval/pom.xml`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/compiled/CompiledPreeval.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/compiled/natives/PrevalNative.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/compiled/natives/PreevalImplementationNative.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/compiled/PreevalCompiledExtension.java`
- Create: `.../src/main/resources/META-INF/services/org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension`
- Test: `.../src/test/java/org/finos/legend/engine/pure/preeval/compiled/TestPrevalNativeCompiled.java`
- Modify: the family pom `<modules>`: add `<module>legend-engine-pure-runtime-java-extension-compiled-functions-preeval</module>`

**Interfaces:**
- Consumes: `Preevaluator.preval(CoreInstance)`, `PrevalResults.toPure(PrevalResult, ProcessorSupport)`, `PreevalImplementation.current()`, `AbstractTestPrevalNative` (Task 2); native ids from Task 1.
- Produces:
  - Static targets called by generated code, which must keep these exact names:
    - `org.finos.legend.engine.pure.preeval.compiled.CompiledPreeval.preval(Object, PureMap, PureMap, CoreInstance, CoreInstance, ExecutionSupport): CoreInstance`
    - `CompiledPreeval.preevalImplementation(): String`
  - Maven artifact `legend-engine-pure-runtime-java-extension-compiled-functions-preeval`.

- [ ] **Step 1: Create the pom**

Standard XML header:
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-pure-code-functions-preeval</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>legend-engine-pure-runtime-java-extension-compiled-functions-preeval</artifactId>
    <name>Legend Engine - Pure - Runtime - Java Extension - Compiled - Functions - Preeval</name>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>dependency-analyze</id>
                        <configuration>
                            <ignoredUnusedDeclaredDependencies>
                                <dependency>org.finos.legend.engine:legend-engine-pure-functions-preeval-pure</dependency>
                            </ignoredUnusedDeclaredDependencies>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.finos.legend.pure</groupId>
                <artifactId>legend-pure-maven-generation-java</artifactId>
                <executions>
                    <execution>
                        <phase>compile</phase>
                        <goals>
                            <goal>build-pure-compiled-jar</goal>
                        </goals>
                        <configuration>
                            <generateSources>true</generateSources>
                            <preventJavaCompilation>true</preventJavaCompilation>
                            <generationType>modular</generationType>
                            <useSingleDir>true</useSingleDir>
                            <repositories>
                                <repository>core_functions_preeval</repository>
                            </repositories>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-functions-preeval-pure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m4</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m3-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-runtime-java-engine-compiled</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-platform-java</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections</artifactId>
        </dependency>

        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m3-core</artifactId>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```
`legend-engine-pure-platform-java` and `eclipse-collections` are there for the generated sources (`Root_meta_pure_tools_DebugContext`, eclipse impl classes). If the analyzer reports either as unused declared, move it to `<scope>runtime</scope>`. If it reports a used undeclared artifact from the generated code, declare it. Add the module to the family pom `<modules>`.

- [ ] **Step 2: Write the failing compiled test**

`.../src/test/java/org/finos/legend/engine/pure/preeval/compiled/TestPrevalNativeCompiled.java`:
```java
package org.finos.legend.engine.pure.preeval.compiled;

import org.finos.legend.engine.pure.preeval.AbstractTestPrevalNative;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.BeforeClass;

public class TestPrevalNativeCompiled extends AbstractTestPrevalNative
{
    @BeforeClass
    public static void setUp()
    {
        AbstractPureTestWithCoreCompiled.setUpRuntime(new FunctionExecutionCompiledBuilder().build(), getCodeStorage(), null, AbstractPureTestWithCoreCompiled.getOptions(), AbstractPureTestWithCoreCompiled.getExtra());
        AbstractPureTestWithCoreCompiled.runtime.loadAndCompileSystem();
    }
}
```

- [ ] **Step 3: Run it and confirm it fails**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval
```
Expected: the build fails in `build-pure-compiled-jar` or in the test, with an error naming the missing native, `prevalNative_Any_1__Map_1__Map_1__PrevalHooks_1__DebugContext_1__PrevalResult_1_` or `preevalImplementation__String_1_`.

- [ ] **Step 4: Implement the static targets, native shells and extension**

`CompiledPreeval.java`:
```java
package org.finos.legend.engine.pure.preeval.compiled;

import org.finos.legend.engine.pure.preeval.PreevalImplementation;
import org.finos.legend.engine.pure.preeval.Preevaluator;
import org.finos.legend.engine.pure.preeval.PrevalResults;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

public final class CompiledPreeval
{
    private CompiledPreeval()
    {
    }

    public static CoreInstance preval(Object item, PureMap inScopeVars, PureMap rollingInScopeVars, CoreInstance hooks, CoreInstance debug, ExecutionSupport executionSupport)
    {
        return PrevalResults.toPure(Preevaluator.preval((CoreInstance) item), ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
    }

    public static String preevalImplementation()
    {
        return PreevalImplementation.current().name();
    }
}
```
`natives/PrevalNative.java`:
```java
package org.finos.legend.engine.pure.preeval.compiled.natives;

import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class PrevalNative extends AbstractNativeFunctionGeneric
{
    public PrevalNative()
    {
        super("org.finos.legend.engine.pure.preeval.compiled.CompiledPreeval.preval",
                new Object[]{
                        "Object",
                        "org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap",
                        "org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap",
                        "org.finos.legend.pure.m4.coreinstance.CoreInstance",
                        "org.finos.legend.pure.m4.coreinstance.CoreInstance",
                        "ExecutionSupport"},
                false, true, true,
                "prevalNative_Any_1__Map_1__Map_1__PrevalHooks_1__DebugContext_1__PrevalResult_1_");
    }
}
```
`natives/PreevalImplementationNative.java`:
```java
package org.finos.legend.engine.pure.preeval.compiled.natives;

import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class PreevalImplementationNative extends AbstractNativeFunctionGeneric
{
    public PreevalImplementationNative()
    {
        super("org.finos.legend.engine.pure.preeval.compiled.CompiledPreeval.preevalImplementation", new Class[]{}, "preevalImplementation__String_1_");
    }
}
```
`PreevalCompiledExtension.java`:
```java
package org.finos.legend.engine.pure.preeval.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.pure.preeval.compiled.natives.PreevalImplementationNative;
import org.finos.legend.engine.pure.preeval.compiled.natives.PrevalNative;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

import java.util.List;

public class PreevalCompiledExtension implements CompiledExtension
{
    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(new PrevalNative(), new PreevalImplementationNative());
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_functions_preeval";
    }

    public static CompiledExtension extension()
    {
        return new PreevalCompiledExtension();
    }
}
```
`src/main/resources/META-INF/services/org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension`:
```
org.finos.legend.engine.pure.preeval.compiled.PreevalCompiledExtension
```

- [ ] **Step 5: Run the tests and confirm they pass**

Same command as Step 3. Expected: `TestPrevalNativeCompiled` reports `Tests run: 2, Failures: 0, Errors: 0`, then `BUILD SUCCESS`, and the analyzer is clean.

- [ ] **Step 6: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval
git commit -m "Add compiled-mode preeval natives"
```

---

### Task 4: Interpreted-mode natives

**Files:**
- Create: `.../legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/pom.xml`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/interpreted/natives/PrevalNative.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/interpreted/natives/PreevalImplementationNative.java`
- Create: `.../src/main/java/org/finos/legend/engine/pure/preeval/interpreted/PreevalInterpretedExtension.java`
- Create: `.../src/main/resources/META-INF/services/org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension`
- Test: `.../src/test/java/org/finos/legend/engine/pure/preeval/interpreted/TestPrevalNativeInterpreted.java`
- Modify: the family pom `<modules>`: add `<module>legend-engine-pure-runtime-java-extension-interpreted-functions-preeval</module>`

**Interfaces:**
- Consumes: the same as Task 3.
- Produces: Maven artifact `legend-engine-pure-runtime-java-extension-interpreted-functions-preeval`, registering both native ids.

- [ ] **Step 1: Create the pom**

Standard XML header:
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-pure-code-functions-preeval</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>legend-engine-pure-runtime-java-extension-interpreted-functions-preeval</artifactId>
    <name>Legend Engine - Pure - Runtime - Java Extension - Interpreted - Functions - Preeval</name>

    <dependencies>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-functions-preeval-pure</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m4</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m3-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-runtime-java-engine-interpreted</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.collections</groupId>
            <artifactId>eclipse-collections</artifactId>
        </dependency>

        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.pure</groupId>
            <artifactId>legend-pure-m3-core</artifactId>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```
Add the module to the family pom `<modules>`.

- [ ] **Step 2: Write the failing interpreted test**

`TestPrevalNativeInterpreted.java`:
```java
package org.finos.legend.engine.pure.preeval.interpreted;

import org.finos.legend.engine.pure.preeval.AbstractTestPrevalNative;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;

public class TestPrevalNativeInterpreted extends AbstractTestPrevalNative
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(new FunctionExecutionInterpreted(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra());
        runtime.loadAndCompileSystem();
    }
}
```

- [ ] **Step 3: Run it and confirm it fails**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval
```
Expected: both tests error with an interpreted "native function not found / not implemented" message naming `prevalNative_...` or `preevalImplementation__String_1_`.

- [ ] **Step 4: Implement the natives and extension**

`natives/PrevalNative.java`:
```java
package org.finos.legend.engine.pure.preeval.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.engine.pure.preeval.Preevaluator;
import org.finos.legend.engine.pure.preeval.PrevalResults;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class PrevalNative extends NativeFunction
{
    public PrevalNative(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance item = params.get(0).getValueForMetaPropertyToOne(M3Properties.values);
        CoreInstance result = PrevalResults.toPure(Preevaluator.preval(item), processorSupport);
        return ValueSpecificationBootstrap.wrapValueSpecification(result, false, processorSupport);
    }
}
```
`natives/PreevalImplementationNative.java`:
```java
package org.finos.legend.engine.pure.preeval.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.engine.pure.preeval.PreevalImplementation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class PreevalImplementationNative extends NativeFunction
{
    private final ModelRepository repository;

    public PreevalImplementationNative(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        return ValueSpecificationBootstrap.newStringLiteral(this.repository, PreevalImplementation.current().name(), processorSupport);
    }
}
```
`PreevalInterpretedExtension.java`:
```java
package org.finos.legend.engine.pure.preeval.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.pure.preeval.interpreted.natives.PreevalImplementationNative;
import org.finos.legend.engine.pure.preeval.interpreted.natives.PrevalNative;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class PreevalInterpretedExtension extends BaseInterpretedExtension
{
    public PreevalInterpretedExtension()
    {
        super(Lists.fixedSize.with(
                Tuples.pair("prevalNative_Any_1__Map_1__Map_1__PrevalHooks_1__DebugContext_1__PrevalResult_1_", PrevalNative::new),
                Tuples.pair("preevalImplementation__String_1_", PreevalImplementationNative::new)
        ));
    }

    public static InterpretedExtension extension()
    {
        return new PreevalInterpretedExtension();
    }
}
```
`src/main/resources/META-INF/services/org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension`:
```
org.finos.legend.engine.pure.preeval.interpreted.PreevalInterpretedExtension
```

- [ ] **Step 5: Run the tests and confirm they pass**

Same command as Step 3. Expected: `TestPrevalNativeInterpreted` reports `Tests run: 2, Failures: 0, Errors: 0`, then `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval
git commit -m "Add interpreted-mode preeval natives"
```

---

### Task 5: Route compiled-core preval through the implementation switch

**Files:**
- Modify: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/pom.xml`
- Modify: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core.definition.json`
- Modify: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure`, lines 84 and 118, plus a new section
- Create: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure`
- Create: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval.java`

**Interfaces:**
- Consumes: `prevalNative`, `preevalImplementation`, `PrevalHooks`, `PrevalResult` (Task 1), plus both runtime modules (Tasks 3–4).
- Produces (Pure, package `meta::pure::router::preeval`):
  - `prevalWithImplementation(item:Any[1], state:State[1], extensions:Extension[*], implementation:String[1]):PrevalWrapper<Any>[1]`, the single dispatch point used by overloads E and F.
  - `assertSamePrevalResult(pure:PrevalWrapper<Any>[1], java:PrevalWrapper<Any>[1], inScopeVars:Map<String, List<Any>>[1], extensions:Extension[*]):Boolean[1]`, which fails with a message starting `preeval SHADOW mismatch`.
  - `toPrevalHooks(state:State[1], extensions:Extension[*]):meta::pure::functions::preeval::PrevalHooks[1]`, reused by P1+.

- [ ] **Step 1: Wire the dependencies**

In `core.definition.json` `dependencies`, after `"core_functions_relation"`, add `"core_functions_preeval"`. Remember the comma on the preceding line.

In `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/pom.xml`:
- In `<ignoredUnusedDeclaredDependencies>`, after the `legend-engine-pure-functions-unclassified-pure` entry, add:
```xml
                                <dependency>org.finos.legend.engine:legend-engine-pure-functions-preeval-pure</dependency>
```
- After the `legend-engine-pure-runtime-java-extension-compiled-functions-relation` `<dependency>`, add:
```xml
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-functions-preeval-pure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-compiled-functions-preeval</artifactId>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-interpreted-functions-preeval</artifactId>
            <scope>runtime</scope>
        </dependency>
```
The interpreted module goes on compiled-core at runtime scope so that every interpreted-mode consumer of `core` (the Pure IDE, the Python reverse-PCT modules, relationalStore PCT interpreted, scenario-quant) receives it transitively. From Task 5 on, `preval` always calls `preevalImplementation()`.

- [ ] **Step 2: Write the failing Pure tests**

`.../core/pure/router/preeval/testImplementationSwitch.pure`:
```pure
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import meta::pure::router::preeval::*;
import meta::pure::router::preeval::tests::implementationSwitch::*;

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testPureImplementationSimplifies():Boolean[1]
{
  let r = {|1 + 1}->prevalWith('PURE');
  assert($r.modified, |'expected the Pure implementation to fold 1 + 1');
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testJavaImplementationIsReachable():Boolean[1]
{
  let r = {p:Integer[1] | $p}->prevalWith('JAVA');
  assert(!$r.modified, |'expected an unmodified result');
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowAcceptsMatchingResults():Boolean[1]
{
  let r = {p:Integer[1] | $p}->prevalWith('SHADOW');
  assert(!$r.modified, |'expected the Pure result to be returned');
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentFlags():Boolean[1]
{
  let value = {|1};
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = $value, canPreval = true, openVars = [], modified = true),
        ^PrevalWrapper<Any>(value = $value, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentValues():Boolean[1]
{
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = {|1}, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = {|2}, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testUnknownImplementationIsRejected():Boolean[1]
{
  assertError(
    | {|1}->prevalWith('FAST'),
    {message:String[1], source:SourceInformation[0..1] | assert($message->contains('Unknown preeval implementation: FAST'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testConfiguredImplementationIsAKnownValue():Boolean[1]
{
  assert(meta::pure::functions::preeval::preevalImplementation()->in(['PURE', 'JAVA', 'SHADOW']), |meta::pure::functions::preeval::preevalImplementation());
}

function <<access.private>> meta::pure::router::preeval::tests::implementationSwitch::prevalWith(f:FunctionDefinition<Any>[1], implementation:String[1]):PrevalWrapper<Any>[1]
{
  let inScopeVars = $f->openVariableValues();
  let state = getPreevalStateWithAdditionalStopInlineFunc($inScopeVars, [], []);
  prevalWithImplementation($f->evaluateAndDeactivate(), $state, [], $implementation);
}
```
`.../src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval.java` (Java header):
```java
package org.finos.legend.engine.pure.code.core;

import junit.framework.TestSuite;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Pure_Preeval
{
    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        executionSupport.getConsole().disable();
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::router::preeval", executionSupport.getProcessorSupport(), fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport), ci -> PureTestBuilder.satisfiesConditionsModular(ci, executionSupport.getProcessorSupport())), executionSupport));
        return suite;
    }
}
```

- [ ] **Step 3: Build and confirm it fails**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core
```
Expected: the Pure compile fails in `testImplementationSwitch.pure` with `The system can't find a match for the function: prevalWithImplementation(...)`, or the same for `assertSamePrevalResult`.

- [ ] **Step 4: Implement the switch in `preeval.pure`**

At line 84 (overload E), replace
```pure
    let r = prevalInternal($f->evaluateAndDeactivate(), $state, $extensions)->toOne()->cast(@PrevalWrapper<FunctionDefinition<Any>>);
```
with
```pure
    let r = prevalWithImplementation($f->evaluateAndDeactivate(), $state, $extensions, meta::pure::functions::preeval::preevalImplementation())->cast(@PrevalWrapper<FunctionDefinition<Any>>);
```
At line 118 (overload F), replace
```pure
    let r = prevalInternal($fe, $state, $extensions);
```
with
```pure
    let r = prevalWithImplementation($fe, $state, $extensions, meta::pure::functions::preeval::preevalImplementation());
```
Insert a new section just before the `// Processor Functions` banner (around line 136):
```pure
// ====================================================================================================================================================================
// Implementation Switch
// ====================================================================================================================================================================

function meta::pure::router::preeval::prevalWithImplementation(item:Any[1], state:State[1], extensions:Extension[*], implementation:String[1]):PrevalWrapper<Any>[1]
{
  if($implementation == 'PURE',
    | prevalInternal($item, $state, $extensions),
    | if($implementation == 'JAVA',
        | prevalJava($item, $state, $extensions),
        | assert($implementation == 'SHADOW', | 'Unknown preeval implementation: ' + $implementation);
          let pure = prevalInternal($item, $state, $extensions);
          let java = prevalJava($item, $state, $extensions);
          assertSamePrevalResult($pure, $java, $state.inScopeVars, $extensions);
          $pure;
      )
  );
}

function <<access.private>> meta::pure::router::preeval::prevalJava(item:Any[1], state:State[1], extensions:Extension[*]):PrevalWrapper<Any>[1]
{
  let r = meta::pure::functions::preeval::prevalNative($item, $state.inScopeVars, $state.rollingInScopeVars, $state->toPrevalHooks($extensions), $state.debug);
  ^PrevalWrapper<Any>(value = $r.value, canPreval = $r.canPreval, openVars = $r.openVars, modified = $r.modified);
}

function meta::pure::router::preeval::toPrevalHooks(state:State[1], extensions:Extension[*]):meta::pure::functions::preeval::PrevalHooks[1]
{
  ^meta::pure::functions::preeval::PrevalHooks(
    stopPreeval                    = $state.stopPreeval,
    shouldInline                   = $state.shouldInlineFxn,
    isGeneratedMilestoningProperty = {f:Function<Any>[1] | $f->instanceOf(AbstractProperty) && $f->cast(@AbstractProperty<Any>)->meta::pure::milestoning::hasGeneratedMilestoningPropertyStereotype()},
    isGetAllFunction               = {f:Function<Any>[1] | meta::pure::router::routing::isGetAllFunction($f)},
    resolveTdsSchema               = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | meta::pure::tds::schema::resolveSchema($vs, $vars, $extensions)}
  );
}

function meta::pure::router::preeval::assertSamePrevalResult(pure:PrevalWrapper<Any>[1], java:PrevalWrapper<Any>[1], inScopeVars:Map<String, List<Any>>[1], extensions:Extension[*]):Boolean[1]
{
  let pureDescription = $pure->describePrevalResult($inScopeVars, $extensions);
  let javaDescription = $java->describePrevalResult($inScopeVars, $extensions);
  assert($pureDescription == $javaDescription, | 'preeval SHADOW mismatch\nPURE: ' + $pureDescription + '\nJAVA: ' + $javaDescription);
}

function <<access.private>> meta::pure::router::preeval::describePrevalResult(r:PrevalWrapper<Any>[1], inScopeVars:Map<String, List<Any>>[1], extensions:Extension[*]):String[1]
{
  let value = $r.value->match([
    fd:FunctionDefinition<Any>[1] | $fd->meta::protocols::pure::vX_X_X::transformation::fromPureGraph::valueSpecification::transformFunctionBody($extensions)->meta::json::toJSON(50000),
    vs:ValueSpecification[1]      | $vs->meta::protocols::pure::vX_X_X::transformation::fromPureGraph::valueSpecification::transformValueSpecification([], $inScopeVars, $extensions)->meta::json::toJSON(50000),
    a:Any[1]                      | $a->toString()
  ]);
  'canPreval=' + $r.canPreval->toString() + ', modified=' + $r.modified->toString() + ', openVars=' + $r.openVars->sort()->joinStrings('[', ',', ']') + ', value=' + $value;
}
```

- [ ] **Step 5: Build, then run the preeval suite and confirm it passes**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
&& mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
  org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=Test_Pure_Preeval
```
Expected: `Test_Pure_Preeval` reports `Failures: 0, Errors: 0`. That covers the 7 new switch tests and the existing 101 `tests.pure` tests, which now go through `prevalWithImplementation(..., 'PURE')`. The existing `ToFix` tests are not collected (`satisfiesConditionsModular`), as before.

- [ ] **Step 6: Run the full core suite (regression gate for the router hot path)**

Every routed `NormalizeRequiredFunction` now calls `preevalImplementation()`. The full suite proves the default path is unchanged:
```bash
. /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
  org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=Test_Pure_Core
```
Expected: `Failures: 0, Errors: 0`. If anything fails, run the same command on `master`'s stamped build before blaming this change; see the CLAUDE.md cache-collision notes.

- [ ] **Step 7: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/pom.xml \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core.definition.json \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval.java
git commit -m "Route preval through a PURE/JAVA/SHADOW implementation switch"
```

---

### Task 6: Register the repo in the IDE, the extension inventory and CI groups

**Files:**
- Modify: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/java/org/finos/legend/engine/ide/PureIDELight.java:66`, the `MINIMUM` block
- Modify: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/pom.xml`
- Modify: `legend-engine-config/legend-engine-extensions-collection-generation/src/test/java/org/finos/legend/engine/extensions/collection/generation/TestExtensions.java`, `getExpectedCodeRepositories()`
- Modify: `.github/workflows/resources/modulesToTest.json`, the `core` group

**Interfaces:**
- Consumes: the repo name `core_functions_preeval` and the module artifact ids from Tasks 1–4.

- [ ] **Step 1: Make the IDE load the repo's sources and interpreted natives**

In `PureIDELight.java`, after the line for `.../legend-engine-pure-functions-unclassified-pure", "functions_unclassified"))`, add:
```java
                .with(this.buildCore("legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-functions-preeval-pure", "functions_preeval"))
```
In the IDE http-server `pom.xml`, after the `legend-engine-pure-runtime-java-extension-interpreted-functions-unclassified` `<dependency>` (which is `runtime` scope), add:
```xml
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-interpreted-functions-preeval</artifactId>
            <scope>runtime</scope>
        </dependency>
```

- [ ] **Step 2: Add the repo to the expected inventory**

In `TestExtensions.getExpectedCodeRepositories()`, after `.with("core_functions_json")`, add:
```java
                .with("core_functions_preeval")
```

- [ ] **Step 3: Add the runtime modules to the CI `core` group**

In `modulesToTest.json`, in the `"core"` group's `modules`, after `"legend-engine-pure-runtime-java-extension-interpreted-functions-variant"`, add a comma to that line and then:
```json
        "legend-engine-pure-runtime-java-extension-compiled-functions-preeval",
        "legend-engine-pure-runtime-java-extension-interpreted-functions-preeval"
```
Validate the JSON: `python3 -m json.tool .github/workflows/resources/modulesToTest.json > /dev/null`. It should print nothing and exit 0.

- [ ] **Step 4: Build the IDE server and run the inventory test**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server -am
```
Expected: `BUILD SUCCESS`.
```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-config/legend-engine-extensions-collection-generation -am -Dtest=TestExtensions -DfailIfNoTests=false
```
Expected: `TestExtensions` reports `Failures: 0`. This is effectively a full build (25+ min); run it in the background.

- [ ] **Step 5: Smoke-test the IDE in interpreted mode**

Start `org.finos.legend.engine.ide.PureIDELight` with the args from CLAUDE.md. In the IDE console (port from `ideLightConfig.json`), execute:
```pure
{|1 + 1}->meta::pure::router::preeval::preval([])->meta::pure::metamodel::serialization::grammar::printFunctionDefinition(^meta::pure::metamodel::serialization::grammar::GContext(space=''))
```
Expected: the folded lambda printed with body `2`, and no "native function not found" error. This proves the interpreted `preevalImplementation()` is wired. Stop the IDE afterwards.

- [ ] **Step 6: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server \
        legend-engine-config/legend-engine-extensions-collection-generation/src/test/java/org/finos/legend/engine/extensions/collection/generation/TestExtensions.java \
        .github/workflows/resources/modulesToTest.json
git commit -m "Register core_functions_preeval with the Pure IDE, extension inventory and CI"
```

---

### Task 7: Checkstyle, spec refinements, and final verification

**Files:**
- Modify: `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`

- [ ] **Step 1: Run Checkstyle on everything touched**

```bash
. /home/aziem/bin/jdk11.sh && mvn checkstyle:check -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-functions-preeval-pure,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core,legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server
```
Expected: `BUILD SUCCESS` with no violations. Fix any violation in place and re-run.

- [ ] **Step 2: Record the plan's refinements in the spec**

In §3.3 of the spec, replace the sentence `The existing body is renamed \`prevalPure\`.` with:
```markdown
  The existing Pure implementation stays as `prevalInternal`; overloads E and F dispatch through
  `prevalWithImplementation(item, state, extensions, implementation)`.
```
In the `SHADOW` bullet of §3.3, replace `Any difference fails with both results printed as Pure grammar.` with:
```markdown
  Any difference fails with a `preeval SHADOW mismatch` message showing both normalised results.
```
At the end of the §3.4 `PrevalRuntime` table row, append: ` Introduced in P1 with its first callers; P0 needs only \`ProcessorSupport\`.`

- [ ] **Step 3: Confirm the tree is clean apart from the known untracked directory**

Run: `git status --short`
Expected: only `?? legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/interpreted/`.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-09-27-preeval-java-native-design.md
git commit -m "Record P0 refinements in the preeval Java native spec"
```

---

## P0 exit criteria (spec §5)

- **Build green:** Tasks 1–6 all build with the stamped revision, the dependency analyzer is clean, and Checkstyle is clean.
- **Native callable from Pure in compiled and interpreted modes:** `TestPrevalNativeCompiled` and `TestPrevalNativeInterpreted` pass, the IDE smoke test works, and the `JAVA`/`SHADOW` paths are exercised by `testImplementationSwitch.pure`.
- **No behaviour change:** `Test_Pure_Core` passes with the default `PURE`.

## Next plan

P1 (core traversal) starts from this scaffold. It introduces `PrevalRuntime`, with `reactivate` and `evaluate` implemented and tested in both adapters, and replaces the identity `Preevaluator`. Running `Test_Pure_Preeval` with `-Dlegend.engine.preeval.implementation=JAVA` becomes the progress meter: in P0 it fails wherever Pure modifies the input, and it should reach the constant-folding and variable subset by the end of P1.
