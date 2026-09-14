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
"""Translate async-profiler collapsed stacks from generated Pure-Java frames back to Pure functions.

Plan generation runs as Java generated from Pure source, so a raw profile only shows frames like

    org/finos/legend/pure/generated/core_pure_router_router_main.Root_meta_pure_router_routeFunction_...

which is unreadable unless you already know the mangling. This script rewrites those frames to the
Pure function they came from (meta::pure::router::routeFunction) and reports self and inclusive
sample counts per Pure function, so a hotspot can be cited as a .pure function rather than a
generated class.

Usage:
    asprof collect -d 30 -e wall -i 5ms -t -o collapsed -f profile.collapsed <pid>
    grep '^\\[main' profile.collapsed | sed 's/^\\[main[^]]*\\];//' > main.collapsed
    demangle_pure_frames.py main.collapsed demangled.collapsed report.txt
"""
import re
import sys
from collections import defaultdict

GENERATED_FRAME = re.compile(r'^org[/.]finos[/.]legend[/.]pure[/.]generated[/.]([A-Za-z0-9_$]+)\.(.+)$')


def pure_function_name(method):
    """Root_meta_pure_router_routeFunction_FunctionDefinition_1__... -> meta::pure::router::routeFunction"""
    method = re.sub(r'\$\d+$', '', method)
    if not method.startswith('Root_meta_'):
        return None
    tokens = method.split('_')
    signature_start = None
    for index, token in enumerate(tokens):
        if index < 2:
            continue
        if token and token[0].isupper() and token != 'Root':
            signature_start = index
            break
    name_tokens = tokens[1:signature_start] if signature_start else tokens[1:]
    return '::'.join(name_tokens) if name_tokens else None


def demangle(frame):
    match = GENERATED_FRAME.match(frame)
    if not match:
        return frame, None, None
    generated_class, method = match.group(1), match.group(2)
    function = pure_function_name(method)
    source = generated_class.split('$')[0]
    if function:
        return 'pure::' + function, function, source
    return 'puregen::' + source, None, source


def main():
    if len(sys.argv) != 4:
        print(__doc__)
        return 1
    source_path, collapsed_path, report_path = sys.argv[1], sys.argv[2], sys.argv[3]

    self_samples = defaultdict(int)
    inclusive_samples = defaultdict(int)
    per_source = defaultdict(int)
    stacks = defaultdict(int)
    total = 0

    with open(source_path) as handle:
        for line in handle:
            line = line.rstrip('\n')
            if not line:
                continue
            stack, _, count_text = line.rpartition(' ')
            try:
                count = int(count_text)
            except ValueError:
                continue
            total += count

            rewritten = []
            seen = set()
            deepest_function = None
            deepest_source = None
            for frame in stack.split(';'):
                name, function, source = demangle(frame)
                rewritten.append(name)
                if function:
                    deepest_function, deepest_source = function, source
                    if function not in seen:
                        inclusive_samples[function] += count
                        seen.add(function)
                elif source:
                    deepest_source = source

            leaf = rewritten[-1]
            if leaf.startswith('pure::') or leaf.startswith('puregen::'):
                if deepest_function:
                    self_samples[deepest_function] += count
                if deepest_source:
                    per_source[deepest_source] += count
            else:
                self_samples['[java] ' + leaf] += count
            stacks[';'.join(rewritten)] += count

    with open(collapsed_path, 'w') as handle:
        for stack, count in stacks.items():
            handle.write('%s %d\n' % (stack, count))

    def table(counts, title, limit=50):
        lines = ['== %s (total samples: %d) ==' % (title, total)]
        for key, value in sorted(counts.items(), key=lambda item: -item[1])[:limit]:
            lines.append('%10d  %6.2f%%  %s' % (value, 100.0 * value / total if total else 0.0, key))
        return '\n'.join(lines)

    with open(report_path, 'w') as handle:
        handle.write(table(self_samples, 'SELF time by Pure function (or Java leaf)'))
        handle.write('\n\n')
        handle.write(table(inclusive_samples, 'INCLUSIVE time by Pure function'))
        handle.write('\n\n')
        handle.write(table(per_source, 'SELF time by generated Pure source class'))
        handle.write('\n')

    print('total samples: %d; wrote %s and %s' % (total, collapsed_path, report_path))
    return 0


if __name__ == '__main__':
    sys.exit(main())
