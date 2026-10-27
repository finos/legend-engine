# Copyright 2021 Goldman Sachs
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# -jdk-, not -jre-: Pure's compiled runtime calls ToolProvider.getSystemJavaCompiler(), which
# returns null without a compiler present.
FROM eclipse-temurin:17.0.20_8-jdk-jammy
COPY target/legend-engine-pure-ide-light-*.jar /app/bin/
COPY src/main/resources/ideLightConfig.json /config/config.json
CMD java -cp /app/bin/*-shaded.jar \
-XX:+ExitOnOutOfMemoryError \
-XX:MaxRAMPercentage=60 \
-Xss4M \
-Dfile.encoding=UTF8 \
--add-opens=java.base/java.nio=ALL-UNNAMED \
org.finos.legend.engine.ide.PureIDELight_NoExtension server /config/config.json
