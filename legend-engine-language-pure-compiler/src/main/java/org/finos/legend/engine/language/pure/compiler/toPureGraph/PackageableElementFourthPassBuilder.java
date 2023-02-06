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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PackageableElementFourthPassBuilder implements PackageableElementVisitor<PackageableElement>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final CompileContext context;

    public PackageableElementFourthPassBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public PackageableElement visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        this.context.getExtraProcessorOrThrow(element).processFourthPass(element, this.context);
        return null;
    }

    @Override
    public PackageableElement visit(Profile profile)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Enumeration _enum)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<?> targetEnum = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<?>)this.context.pureModel.getType(this.context.pureModel.buildPackageString(_enum._package, _enum.name), _enum.sourceInformation);

        if (targetEnum._generalizations().isEmpty())
        {
            Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, this.context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                    ._general(this.context.pureModel.getGenericType("meta::pure::metamodel::type::Any"))
                    ._specific(targetEnum);
            targetEnum._generalizationsAdd(g);
        }

        return targetEnum;
    }

    @Override
    public PackageableElement visit(Class srcClass)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> targetClass = this.context.pureModel.getClass(this.context.pureModel.buildPackageString(srcClass._package, srcClass.name), srcClass.sourceInformation);

        ProcessingContext ctx = new ProcessingContext("Class '" + this.context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "' Fourth Pass");
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(this.context, this.context.pureModel.buildPackageString(srcClass._package, srcClass.name));

        ListIterate.collect(srcClass.qualifiedProperties, property ->
        {
            ctx.push("Qualified Property " + property.name);
            ctx.addInferredVariables("this", thisVariable);
            MutableList<ValueSpecification> body;
            try
            {
                property.parameters.forEach(p -> p.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx)));
                body = ListIterate.collect(property.body, expression -> expression.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx)));
            }
            catch (Exception e)
            {
                LOGGER.warn(new LogInfo(null, LoggingEventType.GRAPH_EXPRESSION_ERROR, "Can't build derived property '" + property.name + " of class '" + this.context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "' - stack: " + ctx.getStack()).toString());
                if (e instanceof EngineException)
                {
                    throw e;
                }
                throw new EngineException(e.getMessage(), property.sourceInformation, EngineErrorType.COMPILATION);
            }
            ctx.flushVariable("this");
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?> prop = targetClass._qualifiedProperties().detect(o -> HelperModelBuilder.isCompatibleDerivedProperty(o, property));
            HelperModelBuilder.checkCompatibility(this.context, body.getLast()._genericType()._rawType(), body.getLast()._multiplicity(), prop._genericType()._rawType(), prop._multiplicity(), "Error in derived property '" + srcClass.name + "." + property.name + "'", property.body.get(property.body.size() - 1).sourceInformation);
            ctx.pop();
            return prop._expressionSequence(body);
        });

        HelperModelBuilder.processClassConstraints(srcClass, this.context, targetClass, ctx, thisVariable);

        if (targetClass._generalizations().isEmpty())
        {
            Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, this.context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                                ._general(this.context.pureModel.getGenericType("meta::pure::metamodel::type::Any"))
                                ._specific(targetClass);
            targetClass._generalizationsAdd(g);
        }

        return targetClass;
    }

    @Override
    public PackageableElement visit(Association srcAssociation)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Function function)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Measure measure)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Mapping mapping)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = this.context.pureModel.getMapping(this.context.pureModel.buildPackageString(mapping._package, mapping.name), mapping.sourceInformation);
        if (mapping.associationMappings != null)
        {
            RichIterable<AssociationImplementation> associationImplementations = ListIterate.collect(mapping.associationMappings, cm -> HelperMappingBuilder.processAssociationImplementation(cm, this.context, pureMapping));
            pureMapping._associationMappings(associationImplementations);
        }
        if (mapping.classMappings != null)
        {
            mapping.classMappings.forEach(cm -> cm.accept(new ClassMappingSecondPassBuilder(this.context, pureMapping)));
        }
        if (!mapping.tests.isEmpty())
        {
            mapping.tests.forEach(t -> HelperMappingBuilder.processMappingTest(t, this.context));
        }
        if (mapping.testSuites != null)
        {
            List<String> testSuiteIds = ListIterate.collect(mapping.testSuites, suite -> suite.id);
            List<String> duplicateTestSuiteIds = testSuiteIds.stream().filter(e -> Collections.frequency(testSuiteIds, e) > 1).distinct().collect(Collectors.toList());

            if (!duplicateTestSuiteIds.isEmpty())
            {
                throw new EngineException("Multiple testSuites found with ids : '" + String.join(",", duplicateTestSuiteIds) + "'", mapping.sourceInformation, EngineErrorType.COMPILATION);
            }
            pureMapping._tests(ListIterate.collect(mapping.testSuites, suite -> HelperMappingBuilder.processMappingTestAndTestSuite(suite, pureMapping, this.context)));
        }
        return pureMapping;
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        return null;
    }

    @Override
    public PackageableElement visit(PackageableConnection packageableConnection)
    {
        return null;
    }

    @Override
    public PackageableElement visit(SectionIndex sectionIndex)
    {
        return null;
    }

    @Override
    public PackageableElement visit(DataElement dataElement)
    {
        return null;
    }
}
