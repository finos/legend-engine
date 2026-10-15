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

from pure_lsp_bridge import projects


def test_resolve_root_prefers_root_env(monkeypatch, tmp_path):
    checkout = tmp_path / "custom-checkout"
    monkeypatch.setenv("SOME_ROOT", str(checkout))
    layer = {"root": "projects/whatever", "rootEnv": "SOME_ROOT"}
    assert projects.resolve_root(layer) == os.path.abspath(str(checkout))


def test_resolve_root_falls_back_to_home_relative_root(monkeypatch):
    monkeypatch.delenv("SOME_ROOT", raising=False)
    layer = {"root": "projects/some-checkout", "rootEnv": "SOME_ROOT"}
    expected = os.path.join(os.path.expanduser("~"), "projects/some-checkout")
    assert projects.resolve_root(layer) == expected


def test_resolve_root_absolute_root_is_used_as_is():
    layer = {"root": "/abs/path/to/repo"}
    assert projects.resolve_root(layer) == "/abs/path/to/repo"


def test_resolve_root_uses_checkout_enclosing_cwd(monkeypatch, tmp_path):
    (tmp_path / "mod" / "sub").mkdir(parents=True)
    monkeypatch.delenv("PD_TEST_ROOT", raising=False)
    monkeypatch.chdir(tmp_path / "mod" / "sub")
    layer = {"root": "projects/elsewhere", "rootEnv": "PD_TEST_ROOT", "module": "mod"}
    assert projects.resolve_root(layer) == str(tmp_path)


def test_resolve_root_env_beats_enclosing_checkout(monkeypatch, tmp_path):
    (tmp_path / "mod").mkdir()
    other = tmp_path / "other"
    other.mkdir()
    monkeypatch.setenv("PD_TEST_ROOT", str(other))
    monkeypatch.chdir(tmp_path)
    layer = {"root": "projects/elsewhere", "rootEnv": "PD_TEST_ROOT", "module": "mod"}
    assert projects.resolve_root(layer) == str(other)


def test_resolve_root_picks_first_existing_candidate(monkeypatch, tmp_path):
    monkeypatch.setenv("HOME", str(tmp_path))
    monkeypatch.chdir(tmp_path)
    monkeypatch.delenv("PD_TEST_ROOT", raising=False)
    (tmp_path / "projects" / "second").mkdir(parents=True)
    layer = {"root": ["projects/first", "projects/second"], "rootEnv": "PD_TEST_ROOT"}
    assert projects.resolve_root(layer) == str(tmp_path / "projects" / "second")


def test_resolve_root_with_no_existing_candidate_returns_first(monkeypatch, tmp_path):
    monkeypatch.setenv("HOME", str(tmp_path))
    monkeypatch.chdir(tmp_path)
    monkeypatch.delenv("PD_TEST_ROOT", raising=False)
    layer = {"root": ["projects/first", "projects/second"], "rootEnv": "PD_TEST_ROOT"}
    assert projects.resolve_root(layer) == str(tmp_path / "projects" / "first")


def test_resolve_root_enclosing_checkout_uses_marker_over_module(monkeypatch, tmp_path):
    (tmp_path / "marker-dir").mkdir()
    monkeypatch.delenv("PD_TEST_ROOT", raising=False)
    monkeypatch.chdir(tmp_path)
    layer = {"root": "projects/elsewhere", "rootEnv": "PD_TEST_ROOT", "marker": "marker-dir", "module": None}
    assert projects.resolve_root(layer) == str(tmp_path)


def test_resolve_root_ignores_unset_root_env(monkeypatch):
    monkeypatch.delenv("UNSET_ROOT_ENV", raising=False)
    layer = {"root": "projects/foo", "rootEnv": "UNSET_ROOT_ENV"}
    expected = os.path.join(os.path.expanduser("~"), "projects/foo")
    assert projects.resolve_root(layer) == expected


def _diamond_registry():
    return {
        "projects": [
            {"name": "base", "root": "base", "dependsOn": [], "module": "base-mod"},
            {"name": "mid-a", "root": "mid-a", "dependsOn": ["base"], "module": None},
            {"name": "mid-b", "root": "mid-b", "dependsOn": ["base"], "module": "mid-b-mod"},
            {"name": "top", "root": "top", "dependsOn": ["mid-a", "mid-b"], "module": "top-mod"},
        ]
    }


def test_load_layers_reads_projects_key(write_registry):
    write_registry(_diamond_registry())
    layers = projects.load_layers()
    assert [l["name"] for l in layers] == ["base", "mid-a", "mid-b", "top"]
    # path is resolved to an absolute, home-relative path for every layer.
    assert all(os.path.isabs(l["path"]) for l in layers)


def test_load_layers_reads_legacy_layers_key(write_registry):
    write_registry({"layers": [{"name": "solo", "root": "solo", "dependsOn": [], "module": None}]})
    layers = projects.load_layers()
    assert [l["name"] for l in layers] == ["solo"]


def test_load_layers_skips_malformed_entries(write_registry):
    write_registry({"projects": [
        {"name": "good", "root": "good"},
        {"root": "no-name"},          # missing 'name' -> skipped
        "not-a-dict",                  # wrong type -> skipped
        None,
    ]})
    layers = projects.load_layers()
    assert [l["name"] for l in layers] == ["good"]


def test_find_layer_returns_none_when_missing(write_registry):
    write_registry(_diamond_registry())
    layers = projects.load_layers()
    assert projects.find_layer(layers, "base")["name"] == "base"
    assert projects.find_layer(layers, "nonexistent") is None


def test_layer_for_path_picks_longest_matching_root(tmp_path):
    outer = {"name": "outer", "path": str(tmp_path)}
    inner = {"name": "inner", "path": str(tmp_path / "outer" / "inner")}
    os.makedirs(inner["path"])
    layers = [outer, inner]
    hit = projects.layer_for_path(layers, os.path.join(inner["path"], "some", "file.pure"))
    assert hit["name"] == "inner"


def test_layer_for_path_matches_root_itself():
    layers = [{"name": "solo", "path": "/repo/solo"}]
    assert projects.layer_for_path(layers, "/repo/solo")["name"] == "solo"


def test_layer_for_path_returns_none_when_no_match():
    layers = [{"name": "solo", "path": "/repo/solo"}]
    assert projects.layer_for_path(layers, "/somewhere/else") is None


def test_deps_in_order_linear_chain(write_registry):
    write_registry(_diamond_registry())
    layers = projects.load_layers()
    order = [l["name"] for l in projects.deps_in_order(layers, "mid-a")]
    assert order == ["base", "mid-a"]


def test_deps_in_order_diamond_dedups_and_orders_bottom_up(write_registry):
    write_registry(_diamond_registry())
    layers = projects.load_layers()
    order = [l["name"] for l in projects.deps_in_order(layers, "top")]
    assert order[0] == "base"
    assert order[-1] == "top"
    assert order.count("base") == 1
    assert set(order) == {"base", "mid-a", "mid-b", "top"}


def test_deps_in_order_siblings_not_entangled(write_registry):
    write_registry(_diamond_registry())
    layers = projects.load_layers()
    # mid-a and mid-b are siblings (both depend on base, not on each other).
    order_a = [l["name"] for l in projects.deps_in_order(layers, "mid-a")]
    assert "mid-b" not in order_a


def test_deps_in_order_unknown_name_raises(write_registry):
    write_registry(_diamond_registry())
    layers = projects.load_layers()
    with pytest.raises(KeyError):
        projects.deps_in_order(layers, "does-not-exist")


def test_resolve_plan_default_source_roots_full_chain(write_registry):
    write_registry(_diamond_registry())
    plan = projects.resolve_plan("top")
    assert {l["name"] for l in plan["source_roots"]} == {"base", "mid-a", "mid-b", "top"}


def test_resolve_plan_explicit_source_roots_always_includes_target(write_registry):
    write_registry(_diamond_registry())
    plan = projects.resolve_plan("top", source_root_names={"mid-a"})
    names = {l["name"] for l in plan["source_roots"]}
    assert names == {"mid-a", "top"}


def test_resolve_plan_classpath_layer_is_highest_needed_with_module(write_registry):
    write_registry(_diamond_registry())
    # top has its own module, so it should be the classpath anchor for itself.
    plan = projects.resolve_plan("top")
    assert plan["classpath_layer"]["name"] == "top"
    # mid-a has module=None; its classpath anchor is the highest *needed* layer with a module,
    # which for mid-a's chain (base, mid-a) is 'base'.
    plan_mid_a = projects.resolve_plan("mid-a")
    assert plan_mid_a["classpath_layer"]["name"] == "base"


def test_resolve_plan_unknown_target_raises(write_registry):
    write_registry(_diamond_registry())
    with pytest.raises(KeyError):
        projects.resolve_plan("does-not-exist")


def test_load_layers_with_no_extra_file_is_unchanged(write_registry, monkeypatch):
    write_registry(_diamond_registry())
    monkeypatch.delenv("PURE_DEV_EXTRA_PROJECTS_JSON", raising=False)
    layers = projects.load_layers()
    assert [l["name"] for l in layers] == ["base", "mid-a", "mid-b", "top"]


def test_load_layers_merges_independent_extra_project(write_registry, write_extra_registries):
    write_registry(_diamond_registry())
    write_extra_registries({"projects": [
        {"name": "side", "root": "side", "dependsOn": [], "module": None},
    ]})
    layers = projects.load_layers()
    assert {l["name"] for l in layers} == {"base", "mid-a", "mid-b", "top", "side"}


def test_load_layers_extra_project_can_depend_on_base_project(write_registry, write_extra_registries):
    write_registry(_diamond_registry())
    write_extra_registries({"projects": [
        {"name": "extension", "root": "extension", "dependsOn": ["top"], "module": "ext-mod"},
    ]})
    layers = projects.load_layers()
    order = [l["name"] for l in projects.deps_in_order(layers, "extension")]
    assert order == ["base", "mid-a", "mid-b", "top", "extension"]


def test_load_layers_multiple_extra_files_merge_in_order(write_registry, write_extra_registries):
    write_registry(_diamond_registry())
    write_extra_registries(
        {"projects": [{"name": "first-extra", "root": "fe", "dependsOn": [], "module": None}]},
        {"projects": [{"name": "second-extra", "root": "se", "dependsOn": ["first-extra"],
                       "module": None}]},
    )
    layers = projects.load_layers()
    order = [l["name"] for l in projects.deps_in_order(layers, "second-extra")]
    assert order == ["first-extra", "second-extra"]


def test_load_layers_name_collision_between_extra_and_base_raises(write_registry, write_extra_registries):
    write_registry(_diamond_registry())
    write_extra_registries({"projects": [
        {"name": "base", "root": "other-base", "dependsOn": [], "module": None},
    ]})
    with pytest.raises(ValueError):
        projects.load_layers()


def test_load_layers_name_collision_between_two_extras_raises(write_registry, write_extra_registries):
    write_registry(_diamond_registry())
    write_extra_registries(
        {"projects": [{"name": "dup", "root": "a", "dependsOn": [], "module": None}]},
        {"projects": [{"name": "dup", "root": "b", "dependsOn": [], "module": None}]},
    )
    with pytest.raises(ValueError):
        projects.load_layers()
