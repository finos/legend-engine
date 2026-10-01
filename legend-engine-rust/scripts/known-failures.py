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

"""Known-failure lines for the sections the Rust parser reports unsupported,
of the section types ported one grammar at a time: `###Mapping` (step 4),
`###Connection` and `###Runtime` (step 5), `###Service` (step 6), and the
other section types (step 7, one subphase each).

usage: known-failures.py <sections dir> <golden test output> [done subphases, comma-separated]

A done step, e.g. `4`, stands for all its subphases. Each line names every
subphase (docs/porting-status.md) the section still waits for, from the Java
result (the grammars its elements use); a section the engine rejects names
the grammar the Rust parser stopped at. To refresh the lines once a subphase
lands:

1. Make a corpus directory whose `sections` and `expressions` link to
   tests/engine-corpus's, and whose `known-failures.txt` is the corpus's
   without the lines this script writes (`not ported yet`, `which no grammar
   reads`).
2. Run the golden test against it, `ENGINE_CORPUS=<dir> cargo test -p
   legend-pure-parser-api --test engine_corpus > <output>`: every failing
   section is listed.
3. Run this script on tests/engine-corpus/sections and that output, naming
   the subphases done, and append its lines to that base.

It fails if a section fails without waiting for any subphase: a bug to fix.
"""
import glob, json, os, re, sys

sections_dir, run = sys.argv[1], sys.argv[2]
done = set(sys.argv[3].split(',')) if len(sys.argv) > 3 else set()

# (kind, protocol _type or marker) -> (subphase, grammar)
SUBPHASES = {
    ('mapping', 'enumeration'): ('4.1', '`EnumerationMapping`'),
    ('mapping', 'pureInstance'): ('4.2', '`Pure`'),
    ('mapping', 'relational'): ('4.3', '`Relational`'),
    ('mapping', 'relation'): ('4.4', '`Relation`'),
    ('mapping', 'xStore'): ('4.5', '`XStore`'),
    ('mapping', 'modelJoin'): ('4.6', '`ModelJoin`'),
    ('mapping', 'operation'): ('4.7', '`Operation`'),
    ('mapping', 'mergeOperation'): ('4.7', '`Operation`'),
    ('mapping', 'aggregationAware'): ('4.8', '`AggregationAware`'),
    ('mapping', 'serviceStore'): ('4.9', '`ServiceStore`'),
    ('mapping', 'MongoDB'): ('4.10', '`MongoDB`'),
    ('mapping', 'mappingIncludeDataSpace'): ('4.11', '`include dataspace`'),
    ('section', 'Connection'): ('5.0', 'the `###Connection` section'),
    ('section', 'Runtime'): ('5.0', 'the `###Runtime` section'),
    ('connection', 'JsonModelConnection'): ('5.0', '`JsonModelConnection`'),
    ('connection', 'XmlModelConnection'): ('5.0', '`XmlModelConnection`'),
    ('connection', 'ModelChainConnection'): ('5.0', '`ModelChainConnection`'),
    ('connection', 'RelationalDatabaseConnection'): ('5.1', '`RelationalDatabaseConnection`'),
    ('relational', 'snowflake'): ('5.2', 'Snowflake'),
    ('relational', 'snowflakePublic'): ('5.2', 'Snowflake'),
    ('connection', 'serviceStore'): ('5.3', '`ServiceStoreConnection`'),
    ('connection', 'MongoDBConnection'): ('5.4', '`MongoDBConnection`'),
    ('connection', 'deephavenConnection'): ('5.5', '`DeephavenConnection`'),
    ('connection', 'elasticsearch7StoreConnection'): ('5.6', '`Elasticsearch7ClusterConnection`'),
    ('section', 'Service'): ('6', 'the `###Service` section'),
    ('section', 'Persistence'): ('7.1', 'the `###Persistence` section'),
    ('section', 'DataSpace'): ('7.2', 'the `###DataSpace` section'),
    ('section', 'DataQualityValidation'): ('7.3', 'the `###DataQualityValidation` section'),
    ('section', 'ServiceStore'): ('7.4', 'the `###ServiceStore` section'),
    ('section', 'ExternalFormat'): ('7.5', 'the `###ExternalFormat` section'),
    ('section', 'Diagram'): ('7.6', 'the `###Diagram` section'),
    ('section', 'Snowflake'): ('7.7', 'the `###Snowflake` section'),
    ('section', 'Elasticsearch'): ('7.8', 'the `###Elasticsearch` section'),
    ('section', 'MongoDB'): ('7.9', 'the `###MongoDB` section'),
    ('section', 'FileGeneration'): ('7.10', 'the `###FileGeneration` section'),
    ('section', 'GenerationSpecification'): ('7.10', 'the `###GenerationSpecification` section'),
    ('section', 'QueryPostProcessor'): ('7.11', 'the `###QueryPostProcessor` section'),
    ('section', 'Deephaven'): ('7.12', 'the `###Deephaven` section'),
    ('section', 'MemSql'): ('7.13', 'the `###MemSql` section'),
    ('section', 'HostedService'): ('7.14', 'the `###HostedService` section'),
    ('section', 'BigQuery'): ('7.15', 'the `###BigQuery` section'),
    ('section', 'Text'): ('7.16', 'the `###Text` section'),
    ('section', 'FunctionJar'): ('7.17', 'the `###FunctionJar` section'),
}
STEP7_TYPES = ['Persistence', 'DataSpace', 'DataQualityValidation', 'ServiceStore', 'ExternalFormat', 'Diagram',
               'Snowflake', 'Elasticsearch', 'MongoDB', 'FileGeneration', 'GenerationSpecification',
               'QueryPostProcessor', 'Deephaven', 'MemSql', 'HostedService', 'BigQuery', 'Text', 'FunctionJar']
# What a Rust "unsupported" message says no grammar reads -> (kind, name -> key), and how to say it.
BY_NAME = {
    'mapping element': ('mapping', {
        'EnumerationMapping': 'enumeration', 'Pure': 'pureInstance', 'Relational': 'relational',
        'Relation': 'relation', 'XStore': 'xStore', 'ModelJoin': 'modelJoin', 'Operation': 'operation',
        'AggregationAware': 'aggregationAware', 'ServiceStore': 'serviceStore', 'MongoDB': 'MongoDB'},
        'a mapping element'),
    'mapping test input': ('mapping', {'Relational': 'relational'}, 'a legacy test input'),
    'mapping include': ('mapping', {'dataspace': 'mappingIncludeDataSpace'}, 'a mapping include'),
    'section': ('section', {f'###{t}': t for t in ['Connection', 'Runtime', 'Service'] + STEP7_TYPES},
                'a section'),
    'connection': ('connection', {
        'JsonModelConnection': 'JsonModelConnection', 'XmlModelConnection': 'XmlModelConnection',
        'ModelChainConnection': 'ModelChainConnection',
        'RelationalDatabaseConnection': 'RelationalDatabaseConnection', 'ServiceStoreConnection': 'serviceStore',
        'MongoDBConnection': 'MongoDBConnection', 'DeephavenConnection': 'deephavenConnection',
        'Elasticsearch7ClusterConnection': 'elasticsearch7StoreConnection'},
        'a connection'),
    'relational datasource specification': ('relational', {'Snowflake': 'snowflake'}, 'a datasource specification'),
    'relational authentication strategy': ('relational', {'SnowflakePublic': 'snowflakePublic'},
                                           'an authentication strategy'),
    'relational post-processor': ('relational', {}, 'a post-processor'),
    'relational query generation config': ('relational', {}, 'a query generation config'),
    'test assertion': ('test', {}, 'a test assertion'),
    'service store security scheme': ('section', {}, 'a service store security scheme'),
    'data quality persistence strategy': ('section', {}, 'a data quality persistence strategy'),
    'embedded data': ('data', {}, 'embedded data'),
    'service execution': ('service', {}, 'a service execution'),
    'trigger': ('persistence', {}, 'a trigger'),
    'persistence platform': ('persistence', {}, 'a persistence platform'),
    'persistence target': ('persistence', {}, 'a persistence target'),
}
STEP_TYPES = {'Mapping': '4', 'Connection': '5', 'Runtime': '5', 'Service': '6', **{t: '7' for t in STEP7_TYPES}}


def is_done(subphase):
    return subphase in done or subphase.split('.')[0] in done


def mapping_types(mapping):
    for cm in mapping.get('classMappings', []):
        yield cm['_type']
        if cm['_type'] == 'aggregationAware':
            yield cm['mainSetImplementation']['_type']
            for a in cm.get('aggregateSetImplementations', []):
                yield a['setImplementation']['_type']
    for am in mapping.get('associationMappings', []):
        yield am['_type']
    for _ in mapping.get('enumerationMappings', []):
        yield 'enumeration'
    for i in mapping.get('includedMappings', []):
        yield i['_type']
    for t in mapping.get('tests', []):
        for d in t['inputData']:
            yield d['_type']
    for s in mapping.get('testSuites') or []:
        for t in s['tests']:
            for d in t['storeTestData']:
                if d['data']['_type'] == 'serviceStore':
                    yield 'serviceStore'


def connection_types(value):
    yield ('connection', value['_type'])
    for field in ('datasourceSpecification', 'authenticationStrategy'):
        if field in value:
            yield ('relational', value[field]['_type'])


def types(section_type, element):
    if section_type == 'Mapping':
        for t in mapping_types(element):
            yield ('mapping', t)
        return
    yield ('section', section_type)
    if section_type not in ('Connection', 'Runtime'):
        return
    if section_type == 'Connection':
        yield from connection_types(element['connectionValue'])
    else:
        for store in element['runtimeValue'].get('connections', []):
            for identified in store['storeConnections']:
                if identified['connection']['_type'] != 'connectionPointer':
                    yield from connection_types(identified['connection'])


failures = {}
for line in open(run):
    m = re.match(r'^sections/([0-9a-f]+)  rust: unsupported: (.*)$', line.rstrip('\n'))
    if m:
        failures[m.group(1)] = m.group(2)

for f in sorted(glob.glob(os.path.join(sections_dir, '*.meta.json'))):
    h = os.path.basename(f)[:-len('.meta.json')]
    section_type = json.load(open(f))['sectionType']
    if h not in failures or section_type not in STEP_TYPES:
        continue
    java = json.load(open(f[:-len('.meta.json')] + '.java.json'))
    if 'error' in java:
        linked = re.search(r"no (.*) grammar is linked for '([^']*)'", failures[h])
        if not linked:
            sys.exit(f'{h}: the engine rejects it and Rust reports it unsupported ({failures[h]}): '
                     'list it by hand, as accepted')
        what, name = linked.groups()
        kind, names, description = BY_NAME[what]
        if name not in names:
            print(f'{h}  accepted: the engine rejects {description} of type `{name}`, which no grammar reads; '
                  'Rust reports it unsupported, as an engine extension may read it, and the engine then parses the section')
            continue
        needed = {(kind, names[name])}
    else:
        needed = {t for e in java['elements'] for t in types(section_type, e) if t in SUBPHASES}
    steps = sorted({SUBPHASES[t] for t in needed if not is_done(SUBPHASES[t][0])},
                   key=lambda pg: [int(x) for x in pg[0].split('.')])
    if not steps:
        sys.exit(f'{h}: unsupported ({failures[h]}) but needs no pending subphase')
    print(f'{h}  unsupported: not ported yet: ' + ', '.join(f'step {p} ({g})' for p, g in steps))
