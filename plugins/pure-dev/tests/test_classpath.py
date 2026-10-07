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

import pytest

from pure_lsp_bridge import classpath


def _touch(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write("")
    return path


def test_read_classpath_file_missing_file_raises(tmp_path):
    with pytest.raises(FileNotFoundError):
        classpath.read_classpath_file(str(tmp_path / "does-not-exist.txt"))


def test_read_classpath_file_pathsep_joined_single_line(tmp_path):
    a = _touch(str(tmp_path / "a.jar"))
    b = _touch(str(tmp_path / "b.jar"))
    cp_file = tmp_path / "classpath.txt"
    cp_file.write_text(os.pathsep.join([a, b]))

    entries = classpath.read_classpath_file(str(cp_file))
    assert entries == [a, b]


def test_read_classpath_file_one_per_line(tmp_path):
    a = _touch(str(tmp_path / "a.jar"))
    b = _touch(str(tmp_path / "b.jar"))
    cp_file = tmp_path / "classpath.txt"
    cp_file.write_text("%s\n%s\n" % (a, b))

    entries = classpath.read_classpath_file(str(cp_file))
    assert entries == [a, b]


def test_read_classpath_file_dedups_preserving_order(tmp_path):
    a = _touch(str(tmp_path / "a.jar"))
    cp_file = tmp_path / "classpath.txt"
    cp_file.write_text(os.pathsep.join([a, a]))

    entries = classpath.read_classpath_file(str(cp_file))
    assert entries == [a]


def test_read_classpath_file_relative_entries_resolve_against_file_dir(tmp_path):
    sub = tmp_path / "sub"
    sub.mkdir()
    _touch(str(sub / "rel.jar"))
    cp_file = sub / "classpath.txt"
    cp_file.write_text("rel.jar")

    entries = classpath.read_classpath_file(str(cp_file))
    assert entries == [str(sub / "rel.jar")]


def test_read_classpath_file_missing_entry_raises(tmp_path):
    cp_file = tmp_path / "classpath.txt"
    cp_file.write_text(str(tmp_path / "nope.jar"))

    with pytest.raises(FileNotFoundError):
        classpath.read_classpath_file(str(cp_file))


def test_read_classpath_file_wildcard_with_existing_parent_is_ok(tmp_path):
    deps_dir = tmp_path / "deps"
    deps_dir.mkdir()
    cp_file = tmp_path / "classpath.txt"
    cp_file.write_text(str(deps_dir) + os.sep + "*")

    entries = classpath.read_classpath_file(str(cp_file))
    assert entries == [os.path.normpath(str(deps_dir) + os.sep + "*")]


def test_read_classpath_file_wildcard_with_missing_parent_raises(tmp_path):
    cp_file = tmp_path / "classpath.txt"
    cp_file.write_text(str(tmp_path / "no-such-dir") + os.sep + "*")

    with pytest.raises(FileNotFoundError):
        classpath.read_classpath_file(str(cp_file))


def test_read_dependency_entries_accepts_a_plain_directory(tmp_path):
    dep_dir = tmp_path / "target" / "dependency"
    a = _touch(str(dep_dir / "a.jar"))
    b = _touch(str(dep_dir / "b.jar"))
    _touch(str(dep_dir / "not-a-jar.txt"))

    entries = classpath.read_dependency_entries(str(dep_dir))
    assert entries == [a, b]


def test_read_dependency_entries_accepts_a_classpath_file(tmp_path):
    a = _touch(str(tmp_path / "a.jar"))
    cp_file = tmp_path / "classpath.txt"
    cp_file.write_text(a)

    entries = classpath.read_dependency_entries(str(cp_file))
    assert entries == [a]


def test_resolve_server_classpath_no_dep_entries(tmp_path):
    server_jar = str(tmp_path / "server.jar")
    entries = classpath.resolve_server_classpath(server_jar, [], ["host.jar"])
    assert entries == [server_jar, "host.jar"]


def test_resolve_server_classpath_dep_entries_with_no_host_entries_are_all_kept(tmp_path):
    server_jar = str(tmp_path / "server.jar")
    dep_dir = tmp_path / "deps"
    dep_dir.mkdir()
    dep_jar = _touch(str(dep_dir / "a.jar"))
    entries = classpath.resolve_server_classpath(server_jar, [dep_jar], [])
    assert entries == [server_jar, dep_jar]


def test_resolve_server_classpath_drops_only_basename_collisions_with_host(tmp_path):
    server_jar = str(tmp_path / "server.jar")
    dep_dir = tmp_path / "deps"
    dep_dir.mkdir()
    dep_a = _touch(str(dep_dir / "legend-pure-a.jar"))
    dep_b = _touch(str(dep_dir / "legend-pure-b.jar"))
    host_entries = [str(tmp_path / "host" / "legend-pure-a.jar")]  # same basename as a dep-entry jar

    entries = classpath.resolve_server_classpath(server_jar, [dep_a, dep_b], host_entries)

    # legend-pure-a.jar dropped (host already supplies that basename); legend-pure-b.jar kept even
    # though it shares the legend-pure-* naming, because the host doesn't provide that basename.
    assert server_jar in entries
    assert dep_b in entries
    assert dep_a not in entries
    assert host_entries[0] in entries


def test_resolve_server_classpath_is_unique_preserving_order(tmp_path):
    server_jar = str(tmp_path / "server.jar")
    entries = classpath.resolve_server_classpath(server_jar, [],
                                                 ["a.jar", "b.jar", "a.jar"])
    assert entries == [server_jar, "a.jar", "b.jar"]


# --- version comparison -------------------------------------------------------------------
# resolve_server_classpath drops a dep_entries jar when the host supplies a strictly newer
# version of the same artifact. That rule is the reason the Lakehouse grammar parser works
# (server pins commons-lang3 3.5, host resolves 3.16.0, host's commons-text calls Range.of()
# which only exists from 3.13) - but none of the comparison logic behind it was covered.

@pytest.mark.parametrize("jar, artifact", [
    ("legend-pure-m3-core-5.97.3-SNAPSHOT.jar", "legend-pure-m3-core"),
    ("commons-lang3-3.16.0.jar", "commons-lang3"),
    ("commons-lang3-3.5.jar", "commons-lang3"),
    # Unparseable version -> the whole filename is the artifact, so it never matches anything
    # and is never dropped. Deliberately conservative.
    ("guava-33.0-jre.jar", "guava-33.0-jre.jar"),
    ("foo-1.2.3-rc1.jar", "foo-1.2.3-rc1.jar"),
])
def test_artifact_extraction(jar, artifact):
    assert classpath._artifact(jar) == artifact


def test_version_key_orders_numerically_not_lexically():
    assert classpath._version_key("commons-lang3-3.16.0.jar") > classpath._version_key("commons-lang3-3.5.jar")


def test_snapshot_sorts_below_the_release_it_precedes():
    assert classpath._version_key("a-5.97.3-SNAPSHOT.jar") < classpath._version_key("a-5.97.3.jar")


@pytest.mark.parametrize("jar", ["guava-33.0-jre.jar", "foo-1.2.3-rc1.jar", "nonsense.jar"])
def test_unparseable_versions_are_none_so_they_are_never_dropped(jar):
    assert classpath._version_key(jar) is None


def test_host_newer_artifacts_keeps_the_highest_version_seen():
    best = classpath._host_newer_artifacts(["/h/commons-lang3-3.5.jar", "/h/commons-lang3-3.16.0.jar"])
    assert best["commons-lang3"] == classpath._version_key("commons-lang3-3.16.0.jar")


def test_dependency_jar_dropped_when_host_has_a_newer_third_party_version(tmp_path):
    server_jar = str(tmp_path / "server.jar")
    dep_dir = tmp_path / "deps"
    dep_dir.mkdir()
    dep_jar = _touch(str(dep_dir / "commons-lang3-3.5.jar"))
    host = [str(tmp_path / "host" / "commons-lang3-3.16.0.jar")]

    entries = classpath.resolve_server_classpath(server_jar, [dep_jar], host)

    assert dep_jar not in entries
    assert host[0] in entries


def test_legend_pure_jars_are_exempt_from_the_newer_wins_rule(tmp_path):
    """Blanket-dropping legend-pure-* would strip jars the host never pulls in transitively
    (e.g. legend-pure-runtime-java-engine-mixed) and fail at runtime with NoClassDefFoundError."""
    server_jar = str(tmp_path / "server.jar")
    dep_dir = tmp_path / "deps"
    dep_dir.mkdir()
    dep_jar = _touch(str(dep_dir / "legend-pure-m3-core-5.97.3.jar"))
    host = [str(tmp_path / "host" / "legend-pure-m3-core-5.99.0.jar")]

    entries = classpath.resolve_server_classpath(server_jar, [dep_jar], host)

    assert dep_jar in entries


def test_prefer_server_pure_jars_drops_the_host_copy_by_artifact(tmp_path):
    """Exact-filename matching misses a version conflict; --prefer-server-pure-jars resolves it
    by artifact so the platform repos load from exactly one place."""
    server_jar = str(tmp_path / "server.jar")
    dep_dir = tmp_path / "deps"
    dep_dir.mkdir()
    dep_jar = _touch(str(dep_dir / "legend-pure-m3-core-5.97.3.jar"))
    host = [str(tmp_path / "host" / "legend-pure-m3-core-5.99.0.jar"),
            str(tmp_path / "host" / "legend-pure-only-on-host-5.99.0.jar")]

    entries = classpath.resolve_server_classpath(server_jar, [dep_jar], host,
                                                 prefer_server_pure_jars=True)

    assert dep_jar in entries
    assert host[0] not in entries      # host copy of the SAME artifact dropped
    assert host[1] in entries          # artifact dep_entries doesn't supply is kept
