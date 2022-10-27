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

package org.finos.legend.engine.language.pure.dsl.persistence.cloud.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph.IPersistenceCompilerExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph.ValidationContext;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationResult;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationRuleSet;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.cloud.context.AwsGluePersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_context_PersistencePlatform;

import java.util.Collections;
import java.util.List;

public class PersistenceCloudCompilerExtension implements IPersistenceCompilerExtension
{
    public static final ValidationRuleSet<ValidationContext> VALIDATION_RULE_SET = new ValidationRuleSet<>(
            "AWS Glue",
            context -> context.persistenceContextProtocol().platform instanceof AwsGluePersistencePlatform,
            Collections.singletonList(context -> ((AwsGluePersistencePlatform) context.persistenceContextProtocol().platform).dataProcessingUnits < 2
                    ? ValidationResult.failure("Data processing units value must be at least 2")
                    : ValidationResult.success()));

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Function2<PersistencePlatform, CompileContext, Root_meta_pure_persistence_metamodel_context_PersistencePlatform>> getExtraPersistencePlatformProcessors()
    {
        return Collections.singletonList(((persistencePlatform, compileContext) ->
        {
            if (persistencePlatform instanceof AwsGluePersistencePlatform)
            {
                return HelperPersistenceCloudBuilder.buildAwsGluePersistencePlatform((AwsGluePersistencePlatform) persistencePlatform, compileContext);
            }
            return null;
        }));
    }

    @Override
    public ValidationRuleSet<ValidationContext> getExtraValidationRuleset()
    {
        return VALIDATION_RULE_SET;
    }
}
