// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.application.query.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A source a cube reads, written by the client, e.g. a table
 * <code>{type: 'table', path: 'my::Database', element: 'SCHEMA.TABLE', project: 'group:artifact'}</code>.
 * In a search, the fields given must all match one of a cube's sources.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CubeSourceRef
{
    public String type;
    public String path;
    public String element;
    public String project;
}
