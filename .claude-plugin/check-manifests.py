#!/usr/bin/env python3
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
"""Cross-check the three places a plugin's skill list is written down.

Each file is individually schema-valid even when they disagree with each other, so nothing
caught marketplace.json drifting behind plugin.json - and since bin/generate-marketplace-pages.py
reads the MARKETPLACE list, the published docs site quietly advertised a subset of the skills the
plugin actually ships.

Checks, per plugin:
  * every skill directory on disk is declared in plugin.json
  * plugin.json and marketplace.json declare the same set
  * every declared skill exists on disk and has a SKILL.md
  * SKILL.md frontmatter `name` matches its directory name

Exit 0 clean, 1 on any mismatch (all mismatches printed, not just the first).
"""
import json
import os
import re
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _skill_names(entries):
    return {e.rstrip("/").rsplit("/", 1)[-1] for e in entries}


def _frontmatter_name(skill_md):
    try:
        with open(skill_md) as f:
            text = f.read()
    except OSError:
        return None
    m = re.match(r"^---\s*\n(.*?)\n---", text, re.DOTALL)
    if not m:
        return None
    m2 = re.search(r"^name:\s*[\"']?([^\"'\n]+)[\"']?\s*$", m.group(1), re.MULTILINE)
    return m2.group(1).strip() if m2 else None


def main():
    problems = []
    marketplace_path = os.path.join(REPO_ROOT, ".claude-plugin", "marketplace.json")
    with open(marketplace_path) as f:
        marketplace = json.load(f)

    for entry in marketplace.get("plugins", []):
        name = entry["name"]
        source = os.path.join(REPO_ROOT, entry.get("source", "./plugins/%s" % name).lstrip("./"))
        plugin_json_path = os.path.join(source, ".claude-plugin", "plugin.json")

        if not os.path.isfile(plugin_json_path):
            problems.append("%s: no plugin.json at %s" % (name, plugin_json_path))
            continue
        with open(plugin_json_path) as f:
            plugin = json.load(f)

        declared = _skill_names(plugin.get("skills", []))
        published = _skill_names(entry.get("skills", []))
        skills_dir = os.path.join(source, "skills")
        on_disk = {d for d in os.listdir(skills_dir)
                   if os.path.isdir(os.path.join(skills_dir, d))} if os.path.isdir(skills_dir) else set()

        for missing in sorted(on_disk - declared):
            problems.append("%s: skills/%s exists on disk but is not in plugin.json" % (name, missing))
        for missing in sorted(declared - on_disk):
            problems.append("%s: plugin.json declares '%s' but skills/%s does not exist" % (name, missing, missing))
        for missing in sorted(declared - published):
            problems.append("%s: plugin.json declares '%s' but marketplace.json does not "
                            "(it will be missing from the docs site)" % (name, missing))
        for extra in sorted(published - declared):
            problems.append("%s: marketplace.json declares '%s' but plugin.json does not" % (name, extra))

        for skill in sorted(declared & on_disk):
            skill_md = os.path.join(skills_dir, skill, "SKILL.md")
            if not os.path.isfile(skill_md):
                problems.append("%s: skills/%s has no SKILL.md" % (name, skill))
                continue
            fm_name = _frontmatter_name(skill_md)
            if fm_name != skill:
                problems.append("%s: skills/%s/SKILL.md frontmatter name is %r, expected %r"
                                % (name, skill, fm_name, skill))

    if problems:
        print("manifest check FAILED:", file=sys.stderr)
        for p in problems:
            print("  - %s" % p, file=sys.stderr)
        return 1
    print("manifest check OK: plugin.json, marketplace.json and skills/ agree for every plugin")
    return 0


if __name__ == "__main__":
    sys.exit(main())
