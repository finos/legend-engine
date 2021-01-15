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

package org.finos.legend.engine.language.pure.compiler;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModelProcessParameter;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.pac4j.core.profile.CommonProfile;

public class Compiler
{
    public static PureModel compile(PureModelContextData model, DeploymentMode deploymentMode, Iterable<? extends CommonProfile> pm)
    {
        return compile(model, deploymentMode, pm, null);
    }

    public static PureModel compile(PureModelContextData model, DeploymentMode deploymentMode, Iterable<? extends CommonProfile> pm, String packageOffset)
    {
        PureModelProcessParameter pureModelProcessParameter = new PureModelProcessParameter(packageOffset);
        return new PureModel(model, pm, deploymentMode, pureModelProcessParameter);
    }

    public static String getLambdaReturnType(Lambda lambda, PureModel pureModel)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification valueSpecification = HelperValueSpecificationBuilder.buildLambdaWithContext(lambda.body, lambda.parameters, new CompileContext.Builder(pureModel).build(), new ProcessingContext("Processing return type for lambda"))._expressionSequence().getLast();
        return HelperModelBuilder.getElementFullPath(valueSpecification._genericType()._rawType(), pureModel.getExecutionSupport());
    }
}
