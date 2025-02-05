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

package org.finos.legend.engine.language.pure.dsl.service.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.service.grammar.from.ServiceParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.DeploymentOwnership;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Ownership;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionEnvironmentInstance;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.UserListOwnership;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class ServiceGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Service");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof Service)
        {
            return renderService((Service) element, context);
        }
        else if (element instanceof ExecutionEnvironmentInstance)
        {
            return renderExecutionEnvironment((ExecutionEnvironmentInstance) element, context);
        }
        return null;
    });

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer(ServiceParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.of((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = ListIterate.select(elements, e -> e instanceof Service || e instanceof ExecutionEnvironmentInstance);
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements
                    .collect(element ->
                    {
                        if (element instanceof Service)
                        {
                            return ServiceGrammarComposerExtension.renderService((Service) element, context);
                        }
                        else
                        {
                            return renderExecutionEnvironment((ExecutionEnvironmentInstance) element, context);
                        }
                    })
                    .makeString("###" + ServiceParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderService(Service service, PureGrammarComposerContext context)
    {
        StringBuilder serviceBuilder = new StringBuilder().append("Service").append(" ").append(HelperDomainGrammarComposer.renderAnnotations(service.stereotypes, service.taggedValues)).append(PureGrammarComposerUtility.convertPath(service.getPath()));
        serviceBuilder.append("\n{\n");
        serviceBuilder.append(getTabString()).append("pattern: ").append(convertString(service.pattern, true)).append(";\n");
        if (!service.owners.isEmpty())
        {
            serviceBuilder.append(getTabString()).append("owners:\n").append(getTabString()).append("[\n").append(LazyIterate.collect(service.owners, o -> getTabString(2) + convertString(o, true)).makeString(",\n")).append("\n").append(getTabString()).append("];\n");
        }
        if (service.ownership != null)
        {
            serviceBuilder.append(getTabString()).append("ownership: ").append(renderOwnership(service.ownership)).append(";\n");

        }
        serviceBuilder.append(getTabString()).append("documentation: ").append(convertString(service.documentation != null ? service.documentation : "", true)).append(";\n");
        serviceBuilder.append(getTabString()).append("autoActivateUpdates: ").append(service.autoActivateUpdates ? "true" : "false").append(";\n");
        Execution execution = service.execution;
        if (execution instanceof PureExecution)
        {
            serviceBuilder.append(getTabString()).append("execution: ").append(HelperServiceGrammarComposer.renderServiceExecution(execution, context));
        }
        else
        {
            serviceBuilder.append(getTabString()).append(unsupported(execution.getClass(), "service execution type"));
        }

        if (service.testSuites != null)
        {
            serviceBuilder.append(getTabString()).append("testSuites:\n");
            serviceBuilder.append(getTabString()).append("[\n");
            serviceBuilder.append(String.join(",\n", ListIterate.collect(service.testSuites, testSuite -> HelperServiceGrammarComposer.renderServiceTestSuite(testSuite, context)))).append("\n");
            serviceBuilder.append(getTabString()).append("]\n");
        }
        if (service.test != null && !HelperServiceGrammarComposer.isServiceTestEmpty(service.test))
        {
            serviceBuilder.append(getTabString()).append("test: ");
            serviceBuilder.append(HelperServiceGrammarComposer.renderServiceTest(service.test, context));
        }
        if (service.postValidations != null && !service.postValidations.isEmpty())
        {
            serviceBuilder.append(getTabString()).append("postValidations:\n");
            serviceBuilder.append(getTabString()).append("[\n");
            serviceBuilder.append(String.join(",\n", ListIterate.collect(service.postValidations, postValidation -> HelperServiceGrammarComposer.renderPostValidation(postValidation, context))));
            serviceBuilder.append(getTabString()).append("]\n");
        }
        return serviceBuilder.append("}").toString();
    }

    private static String renderOwnership(Ownership o)
    {
        if (o instanceof DeploymentOwnership)
        {
            return "DID { identifier: \'" + ((DeploymentOwnership) o).identifier + "\' }";
        }
        if (o instanceof UserListOwnership)
        {
            return "UserList { users: " + Lists.mutable.withAll(((UserListOwnership) o).users).makeString("[\'", "\', \'", "\']") + " }";
        }
        else
        {
            return "/* Can't transform ownership '" + o.getClass().getSimpleName() + "' in this service */";
        }
    }

    private String renderExecutionEnvironment(ExecutionEnvironmentInstance execEnv, PureGrammarComposerContext context)
    {
        StringBuilder execEnvBuilder = new StringBuilder().append("ExecutionEnvironment").append(" ").append(PureGrammarComposerUtility.convertPath(execEnv.getPath()));
        execEnvBuilder.append("\n{\n");
        execEnvBuilder.append(HelperExecutionEnvironmentGrammarComposer.renderExecutionEnvironmentDetails(execEnv.executionParameters, 1, context));
        execEnvBuilder.append("}");
        return execEnvBuilder.toString();
    }
}
