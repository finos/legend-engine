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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.slf4j.Logger;

import java.util.Set;

public class ClassCompilerExtension implements CompilerExtension
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ClassCompilerExtension.class);

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Class");
    }

    @Override
    public CompilerExtension build()
    {
        return new ClassCompilerExtension();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Class.class,
                        Lists.fixedSize.with(Measure.class),
                        (Class srcClass, CompileContext context) ->
                        {
                            String fullPath = context.pureModel.buildPackageString(srcClass._package, srcClass.name);
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> targetClass = new Root_meta_pure_metamodel_type_Class_Impl<>(srcClass.name, SourceInformationHelper.toM3SourceInformation(srcClass.sourceInformation), context.pureModel.getClass("meta::pure::metamodel::type::Class"));
                            context.pureModel.typesIndex.put(fullPath, targetClass);
                            GenericType genericType = HelperCoreBuilder.newGenericType(targetClass, context);
                            context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
                            return targetClass._classifierGenericType(HelperCoreBuilder.newGenericType(context.pureModel.getType("meta::pure::metamodel::type::Class"), genericType, context))
                                    ._stereotypes(ListIterate.collect(srcClass.stereotypes, stereotype -> HelperCoreBuilder.resolveStereotype(stereotype, context)))
                                    ._taggedValues(ListIterate.collect(srcClass.taggedValues, taggedValue -> HelperCoreBuilder.newTaggedValue(taggedValue, context)));
                        },
                        (Class srcClass, CompileContext context) ->
                        {
                            String fullPath = context.pureModel.buildPackageString(srcClass._package, srcClass.name);
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class _class = context.pureModel.getClass(fullPath, srcClass.sourceInformation);
                            GenericType _classGenericType = context.resolveGenericType(fullPath, srcClass.sourceInformation);
                            Set<String> uniqueSuperTypes = Sets.mutable.empty();
                            MutableList<Generalization> generalization = ListIterate.collect(srcClass.superTypes, superTypePtr ->
                            {
                                String superType = superTypePtr.path;
                                // validate no duplicated class supertype
                                if (!uniqueSuperTypes.add(superType))
                                {
                                    throw new EngineException("Duplicated super type '" + superType + "' in class '" + context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "'", srcClass.sourceInformation, EngineErrorType.COMPILATION);
                                }
                                Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", SourceInformationHelper.toM3SourceInformation(superTypePtr.sourceInformation), context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))._general(context.resolveGenericType(superType, superTypePtr.sourceInformation))._specific(_class);
                                if (!context.pureModel.isImmutable(superType))
                                {
                                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> superTypeClass;
                                    Type type = context.resolveType(superType, superTypePtr.sourceInformation);
                                    try
                                    {
                                        superTypeClass = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type;
                                    }
                                    catch (ClassCastException e)
                                    {
                                        throw new EngineException("Invalid supertype: '" + srcClass.name + "' cannot extend '" + superType + "' as it is not a class.", srcClass.sourceInformation, EngineErrorType.COMPILATION);
                                    }
                                    synchronized (superTypeClass)
                                    {
                                        superTypeClass._specializationsAdd(g);
                                    }
                                }
                                return g;
                            });

                            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> properties = ListIterate.collect(srcClass.properties, HelperModelBuilder.processProperty(context, _classGenericType, _class));
                            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> restrictedMilestoningProperties = Milestoning.restrictedMilestoningProperties(_class, srcClass, properties, context.pureModel);
                            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>> withMilestoningProperties = properties.select(p -> !restrictedMilestoningProperties.contains(p)).withAll(Milestoning.generateMilestoningProperties(_class, context));

                            ProcessingContext ctx = new ProcessingContext("Class '" + context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "' Second Pass");
                            ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(context, context.pureModel.buildPackageString(srcClass._package, srcClass.name));
                            ctx.addInferredVariables("this", thisVariable);

                            RichIterable<QualifiedProperty<?>> qualifiedProperties = ListIterate.collect(srcClass.qualifiedProperties, HelperModelBuilder.processQualifiedPropertyFirstPass(context, _class, context.pureModel.buildPackageString(srcClass._package, srcClass.name), ctx));
                            _class._originalMilestonedProperties(ListIterate.collect(srcClass.originalMilestonedProperties, HelperModelBuilder.processProperty(context, _classGenericType, _class)))
                                    ._generalizations(generalization)
                                    ._qualifiedProperties(qualifiedProperties)
                                    ._properties(withMilestoningProperties);
                            if (_class._generalizations().isEmpty())
                            {
                                Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                                        ._general(context.pureModel.getGenericType("meta::pure::metamodel::type::Any"))
                                        ._specific(_class);
                                _class._generalizationsAdd(g);
                            }
                            ctx.flushVariable("this");
                        },
                        (Class srcClass, CompileContext context) ->
                        {
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> targetClass = context.pureModel.getClass(context.pureModel.buildPackageString(srcClass._package, srcClass.name), srcClass.sourceInformation);

                            ProcessingContext ctx = new ProcessingContext("Class '" + context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "' Third Pass");
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification thisVariable = HelperModelBuilder.createThisVariableForClass(context, context.pureModel.buildPackageString(srcClass._package, srcClass.name));

                            ListIterate.collect(srcClass.qualifiedProperties, property ->
                            {
                                ctx.push("Qualified Property " + property.name);
                                ctx.addInferredVariables("this", thisVariable);
                                MutableList<ValueSpecification> body;
                                try
                                {
                                    property.parameters.forEach(p -> p.accept(new ValueSpecificationBuilder(context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
                                    body = ListIterate.collect(property.body, expression -> expression.accept(new ValueSpecificationBuilder(context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
                                }
                                catch (Exception e)
                                {
                                    LOGGER.warn(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.GRAPH_EXPRESSION_ERROR, "Can't build derived property '" + property.name + " of class '" + context.pureModel.buildPackageString(srcClass._package, srcClass.name) + "' - stack: " + ctx.getStack()).toString());
                                    if (e instanceof EngineException)
                                    {
                                        throw e;
                                    }
                                    e.printStackTrace();
                                    throw new EngineException(e.getMessage(), property.sourceInformation, EngineErrorType.COMPILATION);
                                }
                                ctx.flushVariable("this");
                                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty<?> prop = targetClass._qualifiedProperties().detect(o -> HelperModelBuilder.isCompatibleDerivedProperty(o, property));
                                HelperModelBuilder.checkCompatibility(context, body.getLast()._genericType()._rawType(), body.getLast()._multiplicity(), prop._genericType()._rawType(), prop._multiplicity(), "Error in derived property '" + srcClass.name + "." + property.name + "'", property.body.get(property.body.size() - 1).sourceInformation);
                                ctx.pop();
                                return prop._expressionSequence(body);
                            });

                            HelperModelBuilder.processClassConstraints(srcClass, context, targetClass, ctx, thisVariable);
                        }
                )
        );
    }
}
