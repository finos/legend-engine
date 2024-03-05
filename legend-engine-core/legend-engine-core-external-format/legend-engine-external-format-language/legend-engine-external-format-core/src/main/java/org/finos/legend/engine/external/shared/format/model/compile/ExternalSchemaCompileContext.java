// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.shared.format.model.compile;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;

public interface ExternalSchemaCompileContext
{
    /**
     * Returns the textual content of the schema being compiled
     */
    String getContent();

    /**
     * Returns the textual content of another schema in the compiled set using its location
     */
    String getContent(String location);

    /**
     * Returns the location of the schema being compiled or <tt>null</tt> if no location assigned
     */
    String getLocation();

    /**
     * Returns the PureModel associated with the compilation
     */
    PureModel getPureModel();
}
