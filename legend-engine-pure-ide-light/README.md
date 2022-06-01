#Pure IDE Light

Pure IDE Light is a development environment for Pure, the language underlying the Legend platform.


##Running Pure IDE Light

From the root of legend-engine, run the following to launch the Pure IDE Light server.

```
mvn -pl legend-engine-pure-ide-light exec:java -Dexec.mainClass="org.finos.legend.engine.ide.PureIDELight" -Dexec.args="server ./legend-engine-pure-ide-light/src/main/resources/ideLightConfig.json"
```

Then navigate to http://127.0.0.1/ide

## Changing the location of core files from legend-pure
Some core code lives in [legend-pure](https://github.com/finos/legend-pure).
By default, Pure IDE Light assumes that legend-pure is checked out as a sibling to legend-engine project in the filesystem.
This can be overriden by editing the ideLightConfig.json file and adding the following to the end of the configuration file

```
 "sourceLocationConfiguration" : {
    "coreFilesLocation" : <location of legend-pure directory>
  }
```

The core files under legend-pure will now be sourced from the location specified.

