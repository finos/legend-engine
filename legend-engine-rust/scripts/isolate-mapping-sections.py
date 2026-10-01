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

"""Isolation corpus: `###Mapping` sections with the elements whose grammar isn't ported blanked out.

usage: isolate-mapping-sections.py <sections dir> <out dir> <ported element types, comma-separated>
                                   [ported legacy test input types, comma-separated; default object]

A section waits for every grammar it uses, so the golden test can't check
a subphase's elements in sections that still wait for others: this corpus
can. Compare it with `ENGINE_CORPUS=<out dir> cargo test -p
legend-pure-parser-api --test engine_corpus`; every section should match.

Element types are the protocol's: `enumeration` (an enumeration mapping), a class mapping's or
association mapping's `_type`. Blanking replaces text with spaces, newlines kept, so nothing else
moves; the Java result loses the blanked parts. Legacy tests are blanked whole when one of them
has an input other than `object`, and test suites when one of their store data is `serviceStore`.
Sections left with no ported element are skipped.
"""
import glob, json, os, shutil, sys

src_dir, out_dir, ported = sys.argv[1], sys.argv[2], set(sys.argv[3].split(','))
ported_inputs = set(sys.argv[4].split(',')) if len(sys.argv) > 4 else {'object'}
shutil.rmtree(out_dir, ignore_errors=True)
os.makedirs(os.path.join(out_dir, 'sections'))

def offsets(text):
    starts = [0]
    for i, c in enumerate(text):
        if c == '\n':
            starts.append(i + 1)
    return starts

def index(starts, meta, line, col):
    """Character index of a document position (1-based) in the section text."""
    tl = line - meta['lineOffset']
    tc = col - (meta['columnOffset'] if tl == 1 else 0)
    return starts[tl - 1] + tc - 1

def blank(chars, a, b):
    for i in range(a, b + 1):
        if chars[i] != '\n':
            chars[i] = ' '

def span(starts, meta, si):
    return index(starts, meta, si['startLine'], si['startColumn']), index(starts, meta, si['endLine'], si['endColumn'])

kept = 0
for f in sorted(glob.glob(os.path.join(src_dir, '*.meta.json'))):
    meta = json.load(open(f))
    if meta['sectionType'] != 'Mapping':
        continue
    base = f[:-len('.meta.json')]
    java = json.load(open(base + '.java.json'))
    if 'error' in java:
        continue
    text = open(base + '.pure').read()
    starts = offsets(text)
    chars = list(text)
    any_ported = False
    # An element without a position can't be blanked (MongoDB class mappings have none).
    if any('sourceInformation' not in e for m in java['elements'] for key in ('classMappings', 'associationMappings')
           for e in m.get(key, [])):
        continue
    for m in java['elements']:
        if m.get('_type') != 'mapping':
            continue
        keep = lambda t: t in ported
        for key, typ in (('classMappings', lambda e: e['_type']), ('associationMappings', lambda e: e['_type']),
                         ('enumerationMappings', lambda e: 'enumeration')):
            remaining = []
            for e in m.get(key, []):
                if keep(typ(e)):
                    remaining.append(e)
                    any_ported = True
                else:
                    a, b = span(starts, meta, e['sourceInformation'])
                    blank(chars, a, b)
            m[key] = remaining
        tests = m.get('tests', [])
        if any(d['_type'] not in ported_inputs for t in tests for d in t['inputData']):
            a, _ = span(starts, meta, tests[0]['sourceInformation'])
            _, b = span(starts, meta, tests[-1]['sourceInformation'])
            a = text.rindex('MappingTests', 0, a)
            b = text.index(']', b)
            blank(chars, a, b)
            m['tests'] = []
        suites = m.get('testSuites') or []
        if any(d['data'].get('_type') == 'serviceStore' for s in suites for t in s['tests'] for d in t['storeTestData']):
            a, _ = span(starts, meta, suites[0]['sourceInformation'])
            _, b = span(starts, meta, suites[-1]['sourceInformation'])
            a = text.rindex('testSuites', 0, a)
            b = text.index(']', b)
            blank(chars, a, b)
            del m['testSuites']
        if any(i['_type'] != 'mappingIncludeMapping' for i in m.get('includedMappings', [])):
            for i in m['includedMappings']:
                if i['_type'] != 'mappingIncludeMapping':
                    a, b = span(starts, meta, i['sourceInformation'])
                    blank(chars, a, b)
            m['includedMappings'] = [i for i in m['includedMappings'] if i['_type'] == 'mappingIncludeMapping']
    if not any_ported:
        continue
    name = os.path.basename(base)
    out = os.path.join(out_dir, 'sections', name)
    open(out + '.pure', 'w').write(''.join(chars))
    json.dump(meta, open(out + '.meta.json', 'w'))
    json.dump(java, open(out + '.java.json', 'w'))
    kept += 1
print('isolated', kept, 'sections')
