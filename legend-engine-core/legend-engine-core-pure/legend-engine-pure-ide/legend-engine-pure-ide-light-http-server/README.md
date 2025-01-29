#Pure IDE Light

Pure IDE Light is a development environment for Pure, the language underlying the Legend platform.


##Running Pure IDE Light

From the root of legend-engine, run the following to launch the Pure IDE Light server.

```
mvn -pl legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server exec:java -Dexec.mainClass="org.finos.legend.engine.ide.PureIDELight" -Dexec.args="server ./legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json"
```

Then navigate to http://127.0.0.1/ide
