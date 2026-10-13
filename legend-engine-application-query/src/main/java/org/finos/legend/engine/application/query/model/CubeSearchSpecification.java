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

import java.util.List;

/**
 * A search of the Cube store; every field given narrows it
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CubeSearchSpecification
{
    /**
     * Matches a cube's id exactly, or part of its name or of an owner, in any case
     */
    public String searchTerm;
    public String owner;
    public String creator;
    /**
     * Cubes with all of these tags
     */
    public List<String> tags;
    /**
     * Cubes reading a source with these fields
     */
    public CubeSourceRef sourceRef;
    /**
     * At most 100, the default
     */
    public Integer limit;
    /**
     * The latest saved first, by default
     */
    public QuerySearchSortBy sortByOption;
}
