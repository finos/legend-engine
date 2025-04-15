#Pure IDE Light

Pure IDE Light is a development environment for Pure, the language underlying the Legend platform.


##Running Pure IDE Light

From the root of legend-engine, run the following to launch the Pure IDE Light server.

```
mvn -pl legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server exec:java -Dexec.mainClass="org.finos.legend.engine.ide.PureIDELight" -Dexec.args="server ./legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json"
```

Then navigate to http://127.0.0.1/ide

# Configuration
## Configuration file
### Port
The port used by IDE Light is specified in the port option under the server->connection section:
```
"server": {
    "type": "simple",
    "applicationContextPath": "/",
    "adminContextPath": "/admin",
    "connector": {
      "maxRequestHeaderSize": "32KiB",
      "type": "http",
      "port": 9200
    },
    "requestLog": {
      "appenders": [
      ]
    }
```

### Location of repositories

The configuration file for Pure IDE Light
allows you to specify directories containing Pure source code which
will be loaded up in an editable state.  This is controlled in the
sourceLocationConfiguration section:

```
"sourceLocationConfiguration": {
    "welcomeFileDirectory": "./",
    "directories" : ["."],
    "pathPatternsToExclude" : "*/**/{archetype-resources,target}"
  },
```

The welcomeFileDirectory specifies the directory where the welcome
file should be loaded from/stored to allowing you to keep multiple
welcome files on your local file system and choose via configuration
which one to use.

The directories folder specifies locations to search for code
repositories. These directories are traversed and all code
repositories are loaded in an editable state.  If no directories are
specified this defaults to the current directory in which the Pure IDE
Light process is executed from.  If you specify a specific extension
folder (for example legend-engine-xts-java) then the code repositories
under this folder will be loaded as mutable *and* all its required
dependencies too.  All other modules will be loaded via the class
loader mechanism (and hence not editable).

The pathPatternsToExclude property specifies a glob pattern of paths to ignore when searching directories for code repositories.
The example config excludes any paths containing archetype-resources and target.

#### Limiting specific repositories to load in the IDE

You can limit the IDE from loading all classpath repositories
dynamically found by the service loader by using the
requiredRepositories configuration.  This will load only the
repositories specified and any required dependencies needed.

```
  "requiredRepositories" : ["core_external_language_java"]
```

This will load the core_external_language_java in mutable state and
all its dependencies. No other repositories will be loaded
(e.g. repositories discovered by the service loader)

#### Specifying repositories to exclude

You can specify a regex pattern to exclude repositories by using the excludeRepositories configuaration.
This is useful for 'badly' structured repositories where multile definition files and repositories live in the same parent directory

```
  "requiredRepositories" : ["blah*"]
```

The above will exclude all repositories starting with the name blah.

### Runtime Pure properties

The platform uses a number of properties to help with debugging of
execution plan generation.  In particular:

1. PlanLocal - perform execution plan generation in the IDE rather
   than performing this on engine server

2. DebugPlatformCodeGen - enable debug output for the platform code
   generation (currently Java code)

3. ShowLocalPlan - print out the execution plan into the console
   (needs PlanLocal to be enabled)

4. ExecPlan - when executing a query use the generatePlan/executePlan
   endpoints on engine server rather than execute. If PlanLocal is true
   then the plan will be generated in the IDE and then sent to
   executePlan for execution, if not then the plan is generated via
   generatePlan before being sent to the executePlan endpoint.

These options can be set at startup via Java VM properties, for
example setting the following when starting up the IDE:

```
-Dpure.option.PlanLocal
```

These options can also be enabled/disabled using REST endpoints that are documented using Swagger: http://127.0.0.1:9200/swagger#/Pure%20Runtime%20Options/

For example to enable the PlanLocal setting you can execute
http://127.0.0.1:9200/pureRuntimeOptions/setPureRuntimeOption/PlanLocal/true

TODO: Add support in UI to set/view these options