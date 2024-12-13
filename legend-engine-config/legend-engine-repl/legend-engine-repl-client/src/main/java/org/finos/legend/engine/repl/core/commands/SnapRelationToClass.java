// Copyright 2024 Goldman Sachs
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
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.shared.RelationClassMappingGenerator;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class SnapRelationToClass implements Command
{
    private final Client client;

    public SnapRelationToClass(Client client)
    {
        this.client = client;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("snapRelationToClass"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length < 1 || tokens.length > 4)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            String functionName = tokens[1];
            String className = tokens[2];
            String mappingName = tokens[3];
            
            String expression = this.client.getLastCommand(1);
            if (expression == null)
            {
                this.client.printError("Failed to retrieve the last command");
                return true;
            }

            try
            {
                String functionBody = "###Pure\n" +
                                        "function " + functionName + "():Any[1]\n" +
                                        "{" +
                                            expression + ";\n" +
                                        "}\n";
                String functionPath = functionName + "__Any_1_";
                PureModel model = client.getModelState().compileWithTransient(functionBody);
                ExecutionSupport executionSupport = model.getExecutionSupport();
                FunctionDefinition<?> relationFunction = model.getConcreteFunctionDefinition_safe(functionPath);
                
                String classBody = RelationClassMappingGenerator.generateClass(relationFunction, className, executionSupport);
                String mappingBody = RelationClassMappingGenerator.generateClassMapping(relationFunction, className, functionPath, executionSupport);
                
                Mapping mappingFromState = model.getMapping_safe(mappingName);
                String generatedMapping = client.getModelState().getNamedElement(mappingName);
                if (generatedMapping != null)
                {
                    int lastClosingBracketPosition = generatedMapping.lastIndexOf(")");
                    String newMapping = generatedMapping.substring(0, lastClosingBracketPosition) + mappingBody + ")\n";
                    client.getModelState().addNamedElement(mappingName, newMapping);
                }
                else if (mappingFromState != null)
                {
                    throw new RuntimeException("Existing mapping " + mappingName + "can't re-used for this feature. Need to generate a new mapping with the provided name.");
                }
                else
                {
                    String mapping = "###Mapping\n" +
                                     "Mapping " + mappingName + "\n" +
                                     "(\n" +
                                       mappingBody +
                                     ")\n";
                    client.getModelState().addNamedElement(mappingName, mapping);
                }
                client.getModelState().addElement(classBody);
                client.getModelState().addElement(functionBody);
                client.getModelState().parse();
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

    @Override
    public String documentation()
    {
        return "snapRelationToClass <function name> <class name> <mapping name>";
    }

    @Override
    public String description()
    {
        return "generate model class and corresponding relation expression mapping for last run relation expression. Example Usage: snapRelationToClass my::firmFn my::Firm my::testMapping";
    }

    @Override
    public MutableList<Candidate> complete(String cmd, LineReader lineReader, ParsedLine parsedLine)
    {
        return null;
    }
}
