// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

import java.util.Collections;
import java.util.List;

public class GenerationTreeNode
{
    public String id;

    /**
     * Set when the node holds a single generation element. Left null for a node holding
     * {@link #generationElements}, so that a reader which does not understand parallel groups
     * fails rather than silently generating one element out of many.
     */
    public String generationElement;

    /**
     * The generation elements of a parallel group. They are generated against the same compiled
     * graph, so they cannot consume each other's output.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> generationElements = Collections.emptyList();

    public SourceInformation sourceInformation;
}
