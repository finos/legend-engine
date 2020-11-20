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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.validator;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class PureModelContextDataValidator
{
    // TODO: this currently do not support looking for metadata elements - although that might not be a big deal because in SDLC, we prevent people from creating element in meta:: package
    // TODO: Also, the source information being passed into this method is different for Class and other things. For example:
    // in class, we pass source information of the class path after `Class` whereas in Diagram, we pass in source information of the whole diagram element
    public void validate(PureModel pureModel, PureModelContextData pureModelContextData)
    {
        MutableSet<String> allElementPaths = Sets.mutable.empty();
        pureModelContextData.getElements().forEach(el ->
        {
            // NOTE: this validation is not in happening in PURE but in engine, since we use it in tandem with SDLC server which
            // enforces the best practice to put things in package, we should do this check anyway
            // Check for non-empty element package path
            if (el._package == null || el._package.isEmpty())
            {
                throw new EngineException("Element package is required", el.sourceInformation, EngineErrorType.COMPILATION);
            }
            // Check for non-empty element name
            if (el.name == null || el.name.isEmpty())
            {
                throw new EngineException("Element name is required", el.sourceInformation, EngineErrorType.COMPILATION);
            }
            // Duplication check
            // FIXME: handle user-defined functions with same name but different signature
            String elPath = pureModel.buildPackageString(el._package, el.name);
            if (!allElementPaths.add(elPath))
            {
                throw new EngineException("Duplicated element '" + elPath + "'", el.sourceInformation, EngineErrorType.COMPILATION);
            }
        });
    }
}
