FROM openjdk:11
COPY target/legend-engine-server-*-shaded.jar /app/bin/
CMD java -Xmx2G -Xms256M -Xss4M -cp /app/bin/*.jar -Dfile.encoding=UTF8 org.finos.legend.engine.server.Server server /config/config.json
