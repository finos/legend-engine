# Agent Build Guidance

## Maven workflow for Pure modules

Pure Maven modules do not support reliable incremental lifecycle builds. After changing
Pure or module sources, rebuild the directly touched module from the repository root with:

```bash
mvn clean install -DskipTests -pl <module-path>
```

The `clean` is required because stale Pure PAR/generated-repository state can cause errors such
as `The code repository ... already exists`.

When no source files have changed and only a test rerun is needed, bypass the Maven lifecycle and
invoke Surefire directly so Pure source compilation, PAR generation, Java code generation, and test
compilation are not repeated:

```bash
mvn -pl <module-path> org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test
```

Direct Surefire runs assume the module was previously clean-installed and its `target/` outputs are
current. Do not use `-am` or rebuild dependents unless explicitly requested.
