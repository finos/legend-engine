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

from engine_review import modules, scope

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))


def test_double_star_spans_directories_single_star_does_not():
    assert scope.matches("a/b/c-protocol/X.java", ["**/*-protocol/**"])
    assert scope.matches("c-protocol/X.java", ["**/*-protocol/**"])
    assert not scope.matches("a/b/X.java", ["a/*.java"])
    assert scope.matches("a/b/X.java", ["a/**/*.java"])


def test_every_configured_module_exists_in_the_reactor():
    config = scope.load_config()
    reactor = modules.Reactor(REPO)
    configured = [s["module"] for s in config["suites"].values()] + config["framework"]
    assert [m for m in configured if m not in reactor.by_path] == []


def test_relational_core_change_reaches_the_relational_suite():
    reactor = modules.Reactor(REPO)
    changed = reactor.module_of("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/"
                                "legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/"
                                "src/main/resources/core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure")
    assert changed.artifact_id == "legend-engine-xt-relationalStore-core-pure"
    assert "legend-engine-xt-relationalStore-emit" in reactor.downstream([changed.artifact_id])
    assert changed.artifact_id in reactor.upstream(["legend-engine-xt-relationalStore-emit"])
