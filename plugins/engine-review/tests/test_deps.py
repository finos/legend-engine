# Copyright 2026 Goldman Sachs
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
import os

from engine_review import deps, modules
from test_portability import dep, write_pom

EFFECTIVE = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <dependencyManagement><dependencies>
    <dependency><groupId>com.internal</groupId><artifactId>platform</artifactId><version>7.106.3</version></dependency>
    <dependency><groupId>org.public</groupId><artifactId>lib</artifactId><version>1.0</version></dependency>
    <dependency><groupId>org.public</groupId><artifactId>unused</artifactId><version>2.0</version></dependency>
    <dependency><groupId>com.example</groupId><artifactId>example-core</artifactId><version>review-SNAPSHOT</version></dependency>
  </dependencies></dependencyManagement>
  <build><pluginManagement><plugins>
    <plugin><artifactId>maven-surefire-plugin</artifactId><version>2.22.2</version></plugin>
  </plugins></pluginManagement></build>
</project>
"""
SETTINGS = """<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <mirrors><mirror><id>m</id><url>https://mirror.example/maven/</url></mirror></mirrors>
  <profiles><profile><repositories>
    <repository><id>r</id><url>https://repo.example/maven</url></repository>
    <repository><id>local</id><url>file:///opt/lib</url></repository>
  </repositories></profile></profiles>
</settings>
"""


def test_managed_versions_and_repository_urls(tmp_path):
    (tmp_path / "effective.xml").write_text(EFFECTIVE)
    (tmp_path / "settings.xml").write_text(SETTINGS)
    versions = deps.managed_versions(str(tmp_path / "effective.xml"))
    assert versions[("com.internal", "platform")] == "7.106.3"
    assert versions[("org.apache.maven.plugins", "maven-surefire-plugin")] == "2.22.2"
    assert deps.repository_urls(str(tmp_path / "settings.xml")) == ["https://mirror.example/maven/", "https://repo.example/maven/"]


def test_only_the_closures_external_pins_are_checked(tmp_path):
    root = str(tmp_path / "repo")
    parent = ("com.example", "example")
    write_pom(root, "", "example", group="com.example")
    write_pom(root, "core", "example-core", parent=parent, deps=[dep("com.internal", "platform"), dep("org.public", "lib")])
    write_pom(root, "emit", "example-emit", parent=parent, deps=[dep("com.example", "example-core")])
    write_pom(root, "other", "example-other", parent=parent, deps=[dep("org.public", "unused")])
    (tmp_path / "effective.xml").write_text(EFFECTIVE)
    reactor = modules.Reactor(root)
    pins = deps.closure_pins(reactor, reactor.upstream(["example-emit"]), deps.managed_versions(str(tmp_path / "effective.xml")))
    assert pins == {("com.internal", "platform", "7.106.3"), ("org.public", "lib", "1.0")}


def test_a_pin_is_unresolvable_only_when_every_source_says_missing(tmp_path):
    local = str(tmp_path / "m2")
    present = os.path.join(local, deps.pom_path("org.public", "lib", "1.0"))
    os.makedirs(os.path.dirname(present))
    open(present, "w").close()
    answers = {"https://a/": deps.MISSING, "https://b/": deps.MISSING}

    probed = []

    def probe(url):
        probed.append(url)
        return next(answer for prefix, answer in answers.items() if url.startswith(prefix))

    coordinates = {("com.internal", "platform", "7.106.3"), ("org.public", "lib", "1.0")}
    assert deps.unresolvable(coordinates, [local], list(answers), probe) == [("com.internal", "platform", "7.106.3")]
    assert sorted(url.split("/")[2] for url in probed) == ["a", "b"]
    answers["https://b/"] = deps.UNKNOWN
    assert deps.unresolvable(coordinates, [local], list(answers), probe) == []
    assert deps.unresolvable(coordinates, [local], [], probe) == []
