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
 * A Legend Cube saved in the Cube store: its spec, kept as a string, with the
 * owners who may change it. Each save makes a new version.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoredCube
{
    /**
     * Made by the store when the cube is created, and never reused
     */
    public String id;
    /**
     * 1 when created, one more on each change; a change sends the version it read
     */
    public Integer version;
    public String name;
    public List<String> owners;
    public List<String> tags;

    /**
     * The version of the spec's format, which the spec repeats
     */
    public Integer formatVersion;
    /**
     * The spec's JSON, kept as a string so that no key of the user's reaches
     * the database; left out of search results
     */
    public String content;
    /**
     * The sources the cube reads, so cubes can be searched by them
     */
    public List<CubeSourceRef> sourceRefs;

    public StoredCubeAudit audit;
    public Long lastOpenAt;
    /**
     * Set when the cube is deleted softly: it is then no longer read, searched or changed
     */
    public StoredCubeDeletion deleted;
}
