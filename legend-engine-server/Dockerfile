FROM openjdk:11.0.10
COPY target/legend-engine-server-*.jar /app/bin/
CMD java -XX:+ExitOnOutOfMemoryError -XX:MaxRAMPercentage=60 -Xss4M -cp /app/bin/*-shaded.jar -Dfile.encoding=UTF8 org.finos.legend.engine.server.Server server /config/config.json
