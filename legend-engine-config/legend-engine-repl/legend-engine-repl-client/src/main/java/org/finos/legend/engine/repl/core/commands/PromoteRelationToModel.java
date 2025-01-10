// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.repl.core.commands;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.shared.RelationClassMappingGenerator;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.core_pure_corefunctions_stringExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class PromoteRelationToModel implements Command
{
    private final Client client;

    public PromoteRelationToModel(Client client)
    {
        this.client = client;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("promoteRelationToModel"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length < 1 || tokens.length > 3)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            PureModel currentModel = client.getModelState().compile();
            CompiledExecutionSupport es = currentModel.getExecutionSupport();
            String relationName = core_pure_corefunctions_stringExtension.Root_meta_pure_functions_string_makeCamelCase_String_1__Boolean_1__String_1_(tokens[1], true, es);
            String packagePrefix = tokens[2];
            
            String className = packagePrefix + "::domain::" + relationName + "Class";
            String functionName = packagePrefix + "::function::" + relationName + "Fn";
            String mappingName = packagePrefix + "::mapping::" + relationName + "Mapping";
            
            if (currentModel.getMapping_safe(mappingName) != null)
            {
                throw new EngineException("Mapping by name " + mappingName + "already exists in current state!");
            }
            
            String expression = this.client.getLastCommand(1);
            if (expression == null)
            {
                this.client.printError("Failed to retrieve the last command");
                return true;
            }

            try
            {
                // generate initial function with return type Any, then resolve type post compilation
                FunctionDefinition<?> relationFunction = getFunction(RelationClassMappingGenerator.generateFunction(functionName, expression), functionName);
                RelationType<?> relationType = getRelationType(relationFunction, es);
                String returnType = M3Paths.Relation + "<" + _RelationType.print(relationType, es.getProcessorSupport()) + ">";
                String functionBody = RelationClassMappingGenerator.generateFunction(functionName, expression, returnType);
                
                String classBody = RelationClassMappingGenerator.generateClass(className, relationType, es);
                
                String functionDescriptor = FunctionDescriptor.writeFunctionDescriptor(new StringBuilder(), getFunction(functionBody, functionName), false, es.getProcessorSupport()).toString();
                String mappingBody = RelationClassMappingGenerator.generateClassMapping(mappingName, className, functionDescriptor, relationType, es);
                
                client.getModelState().addElement(mappingBody);
                client.getModelState().addElement(classBody);
                client.getModelState().addElement(functionBody);
                // Verify generated code compiles
                client.getModelState().compile();
                return true;
            }
            catch (Exception e)
            {
                this.client.printError("Last command run may not have been an execution of a Pure expression which returns a Relation (command run: '" + expression + "')");
                if (e instanceof EngineException)
                {
                    this.client.printEngineError((EngineException) e, expression);
                }
                else
                {
                    throw e;
                }
            }
        }
        return false;
    }

    private FunctionDefinition<?> getFunction(String functionBody, String functionName)
    {
        PureModel model = this.client.getModelState().compileWithTransient(functionBody);
        return model.getAllPackageableElementsOfType(ConcreteFunctionDefinition.class).detect(f -> PackageableElement.getUserPathForPackageableElement(f).contains(functionName));
    }

    private static RelationType<?> getRelationType(FunctionDefinition<?> relationFunction, CompiledExecutionSupport es)
    {
        GenericType lastExpressionType = relationFunction._expressionSequence().toList().getLast()._genericType();
        if (!es.getProcessorSupport().type_subTypeOf(lastExpressionType._rawType(), es.getProcessorSupport().package_getByUserPath(M3Paths.Relation)))
        {
            throw new EngineException("Last expression returned a " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(lastExpressionType, es.getProcessorSupport()) + ". A Relation is required for promotion to class.", SourceInformationHelper.fromM3SourceInformation(relationFunction.getSourceInformation()), EngineErrorType.COMPILATION);
        }
        return (RelationType<?>) lastExpressionType._typeArguments().toList().getFirst()._rawType();
    }

    @Override
    public String documentation()
    {
        return "promoteRelationToModel <relation name> <package prefix>";
    }

    @Override
    public String description()
    {
        return "generate model class and corresponding relation expression mapping for last run relation expression. Example Usage: promoteRelationToModel AggregatedPerson my::package";
    }

    @Override
    public MutableList<Candidate> complete(String cmd, LineReader lineReader, ParsedLine parsedLine)
    {
        return null;
    }
}
