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
"""Tests for the PostToolUse sync hook's path extraction.

The hook is a bin/ script rather than a package module, so it had no tests at all and is excluded
from Sonar - which is how a compound-command bug survived: `rm a.pure && echo x > b.pure` took the
delete path for the WHOLE command and silently dropped b.pure, leaving the warm session stale on a
file that had just changed. Loaded by path here so it is covered like anything else.
"""
import importlib.machinery
import importlib.util
import os

import pytest

HOOK_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                         "bin", "pure-lsp-sync-hook")


@pytest.fixture(scope="module")
def hook():
    loader = importlib.machinery.SourceFileLoader("pure_lsp_sync_hook", HOOK_PATH)
    spec = importlib.util.spec_from_loader("pure_lsp_sync_hook", loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


@pytest.fixture
def existing(tmp_path):
    p = tmp_path / "Present.pure"
    p.write_text("// content\n")
    return str(p)


ABSENT = "/tmp/pure-dev-tests-definitely-absent.pure"


def test_write_tool_marks_file_modified(hook, existing):
    assert hook._paths_from_tool("Write", {"file_path": existing}) == ([existing], [])


def test_write_tool_on_a_vanished_file_marks_it_deleted(hook, tmp_path):
    gone = str(tmp_path / "Gone.pure")
    assert hook._paths_from_tool("Edit", {"file_path": gone}) == ([], [gone])


def test_non_pure_file_is_ignored(hook, tmp_path):
    other = tmp_path / "notes.md"
    other.write_text("x")
    assert hook._paths_from_tool("Write", {"file_path": str(other)}) == ([], [])


def test_bash_write_marks_modified(hook, existing):
    assert hook._paths_from_tool("Bash", {"command": "echo x > %s" % existing}) == ([existing], [])


def test_bash_rm_marks_deleted(hook):
    assert hook._paths_from_tool("Bash", {"command": "rm %s" % ABSENT}) == ([], [ABSENT])


def test_compound_rm_and_write_handles_both_halves(hook, existing):
    """The regression: one `rm` anywhere used to suppress every write in the whole command."""
    cmd = "rm %s && echo x > %s" % (ABSENT, existing)
    modified, deleted = hook._paths_from_tool("Bash", {"command": cmd})
    assert modified == [existing]
    assert deleted == [ABSENT]


@pytest.mark.parametrize("sep", ["&&", "||", ";", "|", "\n"])
def test_all_shell_separators_split_segments(hook, existing, sep):
    cmd = "rm %s %s echo x > %s" % (ABSENT, sep, existing)
    modified, deleted = hook._paths_from_tool("Bash", {"command": cmd})
    assert modified == [existing]
    assert deleted == [ABSENT]


def test_rm_flag_elsewhere_does_not_delete_a_live_file(hook, existing):
    """`docker run --rm` trips the \\brm\\b word match; the exists() check keeps it a resync."""
    cmd = "docker run --rm -v %s:/x img" % existing
    assert hook._paths_from_tool("Bash", {"command": cmd}) == ([existing], [])


def test_relative_paths_are_ignored(hook):
    assert hook._paths_from_tool("Bash", {"command": "echo x > relative.pure"}) == ([], [])


def test_a_path_is_never_both_modified_and_deleted(hook, existing):
    cmd = "rm %s && touch %s" % (existing, existing)
    modified, deleted = hook._paths_from_tool("Bash", {"command": cmd})
    assert existing in modified
    assert existing not in deleted


def test_unwatched_tool_yields_nothing(hook, existing):
    assert hook._paths_from_tool("Read", {"file_path": existing}) == ([], [])


def test_hook_resolves_a_port_rather_than_hardcoding_8992(hook, monkeypatch):
    """The whole point of the fix: no literal port in the hook."""
    monkeypatch.setenv("PURE_LSP_PORT", "8991")
    assert hook.resolve_port() == 8991
