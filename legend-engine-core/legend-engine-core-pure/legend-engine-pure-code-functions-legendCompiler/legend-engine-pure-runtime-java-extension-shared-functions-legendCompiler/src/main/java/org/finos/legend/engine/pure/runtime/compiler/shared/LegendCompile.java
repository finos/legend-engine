// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.compiler.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ModelElementAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

public class LegendCompile
{
    public static MutableList<PackageableElement> doCompile(String code, Metadata metadata)
    {
        // Parse
        PureModelContextData data = PureGrammarParser.newInstance().parseModel(code);
        // Compile
        PureModel pm = org.finos.legend.engine.language.pure.compiler.Compiler.compile(data, DeploymentMode.PROD, Identity.getAnonymousIdentity().getName(), "", metadata);
        // Extract Compiled created elements
        return extractCreatedElementFromCompiledGraph(data, pm);
    }

    public static ValueSpecification doCompileVS(String code, Metadata metadata)
    {
        // Parse
        PureModelContextData data = PureGrammarParser.newInstance().parseModel("function a::f():Any[*]{" + code + "}");
        // Compile
        PureModel pm = org.finos.legend.engine.language.pure.compiler.Compiler.compile(data, DeploymentMode.PROD, Identity.getAnonymousIdentity().getName(), "", metadata);
        // Extract Compiled created elements
        return ((ConcreteFunctionDefinition<?>) extractCreatedElementFromCompiledGraph(data, pm).getFirst())._expressionSequence().getFirst();
    }

    public static MutableList<PackageableElement> doCompilePMCD(String code, Metadata metadata)
    {
        // Parse
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
        PureModelContextData data = null;
        try
        {
            data = objectMapper.readValue(code, PureModelContextData.class);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }

        // Compile
        PureModel pm = org.finos.legend.engine.language.pure.compiler.Compiler.compile(data, DeploymentMode.PROD, Identity.getAnonymousIdentity().getName(), "", metadata);

        // Extract Compiled created elements
        return extractCreatedElementFromCompiledGraph(data, pm);
    }

    private static MutableList<PackageableElement> extractCreatedElementFromCompiledGraph(PureModelContextData pureModelContextData, PureModel pureModel)
    {
        return ListIterate.collectIf(pureModelContextData.getElements(),
                x -> !(x instanceof SectionIndex) && !(x instanceof Package),
                x ->
                {
                    Package elementGraphPackage = (Package) pureModel.getPackageableElement(x._package);
                    String elementId = x instanceof Function ? HelperModelBuilder.getSignature((Function) x) : x.name;
                    PackageableElement graphElement = elementGraphPackage._children().detect(s -> elementId.equals(s._name()));
                    Assert.assertTrue(graphElement != null, () -> "Element " + elementId + " can't be found in package " + x._package + " children: " + elementGraphPackage._children().collect(ModelElementAccessor::_name).makeString(", "));
                    return graphElement;
                });
    }

    public static ValueSpecification doCompileVS(org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification vs, Metadata metadata)
    {
        return doCompileVS(vs, PureModelContextData.newPureModelContextData(), metadata);
    }

    public static ValueSpecification doCompileVS(org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification vs, PureModelContextData base, Metadata metadata)
    {
        Function f = new Function();
        f._package = "a";
        f.name = "f";
        f.body = FastList.newListWith(vs);
        f.parameters = FastList.newList();
        f.returnGenericType = new GenericType();
        f.returnGenericType.rawType = new PackageableType("Any");
        f.returnMultiplicity = Multiplicity.PURE_ONE;

        // Build
        PureModelContextData data = PureModelContextData.newBuilder().withPureModelContextData(base).withElement(f).build();
        // Compile
        PureModel pm = org.finos.legend.engine.language.pure.compiler.Compiler.compile(data, DeploymentMode.PROD, Identity.getAnonymousIdentity().getName(), "", metadata);

        // Extract Compiled created elements
        return pm.getConcreteFunctionDefinition_safe("a::f__Any_1_")._expressionSequence().getFirst();
    }
}
