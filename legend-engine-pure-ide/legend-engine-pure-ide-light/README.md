#Pure IDE Light

Pure IDE Light is a development environment for Pure, the language underlying the Legend platform.


##Running Pure IDE Light

From the root of legend-engine, run the following to launch the Pure IDE Light server.

```
mvn -pl legend-engine-pure-ide-light exec:java -Dexec.mainClass="org.finos.legend.engine.ide.PureIDELight" -Dexec.args="server ./legend-engine-pure-ide-light/src/main/resources/ideLightConfig.json"
```

Then navigate to http://127.0.0.1/ide
