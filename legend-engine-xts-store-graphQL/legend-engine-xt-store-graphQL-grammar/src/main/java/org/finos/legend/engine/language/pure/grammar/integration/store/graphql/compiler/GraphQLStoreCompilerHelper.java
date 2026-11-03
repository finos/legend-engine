// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.integration.store.graphql.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperMappingBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph.HelperAuthenticationBuilder;
import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.FieldDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InputValueDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ListTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.NamedTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.RootOperationTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.SchemaDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeReference;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLConnection;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLStore;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Definition;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_InputValueDefinition;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_InputValueDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ListTypeReference_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_NamedTypeReference_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_RootOperationTypeDefinition;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_RootOperationTypeDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_SchemaDefinition;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_SchemaDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_TypeReference;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_GraphQLConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_GraphQLConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_GraphQLPropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingClass_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphQLStoreCompilerHelper
{
    public static List<PropertyMapping> generatePropertyMappings(Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail bindingDetail, Class<?> pureClass, String sourceSetId, List<EmbeddedSetImplementation> embeddedSetImplementations, PropertyMappingsImplementation owner, Map<Class<?>, String> classToSetImplementationId, CompileContext context, Mapping parent)
    {
        MutableList<PropertyMapping> propertyMappings = Lists.mutable.empty();
        bindingDetail.mappedPropertiesForClass(pureClass, context.getExecutionSupport()).forEach(prop ->
        {
            Property<?, ?> property = (Property<?, ?>) prop;
            boolean isPrimitive = core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_isPrimitiveValueProperty_AbstractProperty_1__Boolean_1_(property, context.getExecutionSupport());
            propertyMappings.add(isPrimitive
                    ? buildPrimitivePropertyMapping(property, sourceSetId, context)
                    : buildEmbeddedPropertyMapping(property, sourceSetId, bindingDetail, embeddedSetImplementations, owner, classToSetImplementationId, parent, context));
        });
        return propertyMappings;
    }

    private static PropertyMapping buildPrimitivePropertyMapping(Property<?, ?> property, String sourceSetId, CompileContext context)
    {
        return buildGraphQLPropertyMapping(property, sourceSetId, "", context);
    }

    private static PropertyMapping buildGraphQLPropertyMapping(Property<?, ?> property, String sourceSetId, String targetSetId, CompileContext context)
    {
        return new Root_meta_external_store_graphQL_metamodel_pure_GraphQLPropertyMapping_Impl("", null, context.pureModel.getClass("meta::external::store::graphQL::metamodel::pure::GraphQLPropertyMapping"))
                ._property(property)
                ._sourceSetImplementationId(sourceSetId)
                ._targetSetImplementationId(targetSetId);
    }

    private static PropertyMapping buildEmbeddedPropertyMapping(Property<?, ?> property, String sourceSetId, Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail bindingDetail, List<EmbeddedSetImplementation> embeddedSetImplementations, PropertyMappingsImplementation owner, Map<Class<?>, String> classToSetImplementationId, Mapping parent, CompileContext context)
    {
        Class<?> pureClass = (Class<?>) property._genericType()._rawType();
        String existingId = classToSetImplementationId.get(pureClass);
        if (existingId != null)
        {
            return buildGraphQLPropertyMapping(property, sourceSetId, existingId, context);
        }

        Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation embedded = new Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation_Impl("", null, context.pureModel.getClass("meta::external::store::graphQL::metamodel::pure::EmbeddedGraphQLSetImplementation"));
        String id = owner._id() + "_" + property._name();

        embedded._class(pureClass);
        embedded._id(id);
        embedded._owner(owner);
        embedded._parent(parent);
        embedded._property(property);
        embedded._root(false);
        embedded._sourceSetImplementationId(sourceSetId);
        embedded._targetSetImplementationId(id);

        Map<Class<?>, String> nestedClassToSetImplementationId = new HashMap<>(classToSetImplementationId);
        nestedClassToSetImplementationId.put(pureClass, id);
        embedded._propertyMappings(Lists.mutable.withAll(generatePropertyMappings(bindingDetail, pureClass, id, embeddedSetImplementations, embedded, nestedClassToSetImplementationId, context, parent)).toImmutable());

        embeddedSetImplementations.add(embedded);
        return embedded;
    }

    // Gives a polymorphic property one PropertyMapping per possible subtype (instead of a single embedded
    // mapping targeting its declared base type)
    public static void fixUpPolymorphicPropertyMappingsForNativeSubType(Mapping parentMapping, CompileContext context)
    {
        org.finos.legend.pure.m3.navigation.ProcessorSupport ps = context.pureModel.getExecutionSupport().getProcessorSupport();

        Map<Class<?>, String> rootIdByClass = new HashMap<>();
        for (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation si : parentMapping._classMappings())
        {
            if (Boolean.TRUE.equals(si._root()) && si instanceof Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation)
            {
                rootIdByClass.put(si._class(), si._id());
            }
        }

        for (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation si : parentMapping._classMappings())
        {
            if (!(si instanceof PropertyMappingsImplementation))
            {
                continue;
            }
            PropertyMappingsImplementation owner = (PropertyMappingsImplementation) si;
            MutableList<PropertyMapping> replaced = Lists.mutable.empty();
            boolean changed = false;
            for (PropertyMapping pm : owner._propertyMappings())
            {
                Property<?, ?> property = pm._property();
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type rawType = property._genericType()._rawType();
                String targetId = pm._targetSetImplementationId();
                String expectedEmbeddedId = owner._id() + "_" + property._name();
                boolean isSelfEmbedding = rawType instanceof Class && targetId != null && targetId.equals(expectedEmbeddedId);

                List<String> subtypeIds = new java.util.ArrayList<>();
                if (isSelfEmbedding)
                {
                    Class<?> declaredType = (Class<?>) rawType;
                    for (Map.Entry<Class<?>, String> entry : rootIdByClass.entrySet())
                    {
                        if (!entry.getKey().equals(declaredType) && org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(entry.getKey(), declaredType, ps))
                        {
                            subtypeIds.add(entry.getValue());
                        }
                    }
                }

                boolean alreadyFannedOut = !subtypeIds.isEmpty() && owner._propertyMappings().anySatisfy(p2 ->
                        p2 != pm && property.equals(p2._property()) && subtypeIds.contains(p2._targetSetImplementationId()));

                if (!subtypeIds.isEmpty() && !alreadyFannedOut)
                {
                    replaced.add(pm);
                    String sourceSetId = pm._sourceSetImplementationId();
                    for (String subtypeId : subtypeIds)
                    {
                        replaced.add(buildGraphQLPropertyMapping(property, sourceSetId, subtypeId, context)._owner(owner));
                    }
                    changed = true;
                }
                else
                {
                    replaced.add(pm);
                }
            }
            if (changed)
            {
                owner._propertyMappings(replaced.toImmutable());
            }
        }
    }

    public static Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation createRootGraphQLInstanceSetImplementation(GraphQLClassMapping classMapping, CompileContext context, Mapping parentMapping, ClassMapping cm, Root_meta_external_format_shared_binding_Binding binding, Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore store)
    {
        String id = HelperMappingBuilder.getClassMappingId(classMapping, context);
        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation setImpl = new Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation_Impl(id, null, context.pureModel.getClass("meta::external::store::graphQL::metamodel::pure::RootGraphQLInstanceSetImplementation"));

        Class<?> pureClass = context.resolveClass(classMapping._class, classMapping.classSourceInformation);
        MappingClass<?> mappingClass = generateMappingClass(pureClass, id, parentMapping, context);

        setImpl._id(id);
        setImpl._root(classMapping.root);
        setImpl._class(pureClass);
        setImpl._parent(parentMapping);
        setImpl._mappingClass(mappingClass);
        setImpl._store(store);
        setImpl._typeName(classMapping.typeName);
        setImpl._binding(binding);
        setImpl._superSetImplementationId(classMapping.extendsClassMappingId);
        if (classMapping.root)
        {
            if (classMapping.typeName == null)
            {
                throw new EngineException("'~typeName' must be declared on a root class mapping", classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
            if (store._document() != null)
            {
                validateTypeNameAndQualifiedPropertiesAgainstTrackedSchema(classMapping, pureClass, context, store);
            }
        }
        if (classMapping.joinAccessorQualifiedProperty != null)
        {
            setImpl._joinAccessor(resolveJoinAccessor(classMapping, pureClass, context));
        }

        return setImpl;
    }

    private static QualifiedProperty<?> resolveJoinAccessor(GraphQLClassMapping classMapping, Class<?> pureClass, CompileContext context)
    {
        MutableList<? extends QualifiedProperty<?>> matches = pureClass._qualifiedProperties().select(qp -> qp._name().equals(classMapping.joinAccessorQualifiedProperty)).toList();
        if (matches.isEmpty())
        {
            throw new EngineException("'~joinAccessor' qualified property '" + classMapping.joinAccessorQualifiedProperty + "' not found on class '"
                    + HelperModelBuilder.getElementFullPath(pureClass, context.pureModel.getExecutionSupport()) + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
        if (matches.size() > 1)
        {
            throw new EngineException("'~joinAccessor' qualified property '" + classMapping.joinAccessorQualifiedProperty + "' is overloaded (" + matches.size() + " signatures) on class '"
                    + HelperModelBuilder.getElementFullPath(pureClass, context.pureModel.getExecutionSupport()) + "' - '~joinAccessor' cannot disambiguate by argument count/type, rename one of the overloads",
                    classMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
        QualifiedProperty<?> joinAccessor = matches.get(0);
        // Enforcing exactly one parameter keeps cross-instance joins to a single key per parent object,
        // which is why planCrossStoreGraphFetchExecutionGraphQL issues one HTTP call per parent (no
        // batching) - relaxing this to accept a batched list of keys is unimplemented future work.
        int parameterCount = declaredParameters(joinAccessor).size();
        if (parameterCount != 1)
        {
            throw new EngineException("'~joinAccessor' qualified property '" + classMapping.joinAccessorQualifiedProperty + "' must declare exactly one parameter (the join-key value), found "
                    + parameterCount, classMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
        if (resolveThisThreadedDelegateProperty(joinAccessor) == null)
        {
            throw new EngineException("'~joinAccessor' qualified property '" + classMapping.joinAccessorQualifiedProperty
                    + "' must have a body of the form '{ $this.<property> }' (a plain delegate to a real stored property) on class '"
                    + HelperModelBuilder.getElementFullPath(pureClass, context.pureModel.getExecutionSupport())
                    + "' - found a different expression shape", classMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
        return joinAccessor;
    }

    private static Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition findNamedType(Root_meta_external_query_graphQL_metamodel_sdl_Document document, String typeName, String storePath, SourceInformation sourceInformation, String kindForErrorMessage)
    {
        List<Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition> matches = document._definitions()
                .select(d -> d instanceof Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition && ((Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition) d)._name().equals(typeName))
                .collect(d -> (Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition) d)
                .toList();
        if (matches.isEmpty())
        {
            throw new EngineException(kindForErrorMessage + " '" + typeName + "' not found in tracked schema for store '" + storePath + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        if (matches.size() > 1)
        {
            throw new EngineException(kindForErrorMessage + " '" + typeName + "' is declared " + matches.size() + " times in the tracked schema for store '" + storePath + "' - type names must be unique", sourceInformation, EngineErrorType.COMPILATION);
        }
        return matches.get(0);
    }

    private static void validateTypeNameAndQualifiedPropertiesAgainstTrackedSchema(GraphQLClassMapping classMapping, Class<?> pureClass, CompileContext context, Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore store)
    {
        String storePath = HelperModelBuilder.getElementFullPath(store, context.pureModel.getExecutionSupport());
        Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition namedType = findNamedType(store._document(), classMapping.typeName, storePath, classMapping.sourceInformation, "Type");

        for (QualifiedProperty<?> qualifiedProperty : pureClass._qualifiedProperties())
        {
            String qualifiedPropertyName = qualifiedProperty._name();
            Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition field = namedType._fields().detect(f -> f._name().equals(qualifiedPropertyName));
            if (field == null)
            {
                throw new EngineException("Qualified property '" + qualifiedPropertyName + "' has no matching field on type '" + classMapping.typeName + "' in the tracked schema for store '" + storePath + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }

            MutableList<String> declaredParameterNames = Lists.mutable.empty();
            for (VariableExpression declaredParameter : declaredParameters(qualifiedProperty))
            {
                String parameterName = declaredParameter._name();
                declaredParameterNames.add(parameterName);
                boolean matchesDeclaredArgument = field._argumentDefinitions().anySatisfy(a -> a._name().equals(parameterName));
                if (!matchesDeclaredArgument)
                {
                    throw new EngineException("Qualified property '" + qualifiedPropertyName + "'s parameter '" + parameterName + "' does not match the name of any argument declared on field '" + qualifiedPropertyName + "' in the tracked schema for store '" + storePath + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
                }
            }

            // A nullable argument may be omitted - there's no Pure equivalent of "pass nothing".
            for (Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_InputValueDefinition argumentDefinition : field._argumentDefinitions())
            {
                if (!argumentDefinition._type()._nullable() && !declaredParameterNames.contains(argumentDefinition._name()))
                {
                    throw new EngineException("Qualified property '" + qualifiedPropertyName + "' is missing a parameter for required argument '" + argumentDefinition._name() + "' declared on field '" + qualifiedPropertyName + "' in the tracked schema for store '" + storePath + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
                }
            }
        }
    }

    public static void validateQualifiedPropertyReturnTypesHaveAClassMapping(GraphQLClassMapping classMapping, Class<?> pureClass, Mapping parentMapping, CompileContext context)
    {
        for (QualifiedProperty<?> qualifiedProperty : pureClass._qualifiedProperties())
        {
            boolean isPrimitive = core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_isPrimitiveValueProperty_AbstractProperty_1__Boolean_1_(qualifiedProperty, context.getExecutionSupport());
            if (isPrimitive)
            {
                continue;
            }
            Class<?> returnType = (Class<?>) qualifiedProperty._genericType()._rawType();
            boolean hasAnyMatch = parentMapping._classMappings().anySatisfy(c -> c._class() == returnType);
            if (!hasAnyMatch)
            {
                throw new EngineException("Qualified property '" + qualifiedProperty._name() + "' returns '" + HelperModelBuilder.getElementFullPath(returnType, context.pureModel.getExecutionSupport())
                        + "', which has no class mapping in this Mapping", classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }

    private static List<? extends VariableExpression> declaredParameters(QualifiedProperty<?> qualifiedProperty)
    {
        FunctionType functionType = (FunctionType) qualifiedProperty._classifierGenericType()._typeArguments().getFirst()._rawType();
        return ListIterate.drop(functionType._parameters().toList(), 1);
    }

    // A qualified property with body exactly "{ $this.someProperty }" (see
    // aliasForThisThreadedQualifiedProperty in graphQLSelectionSetBuilder.pure) uses that property as its
    // GraphQL field alias; if it has no real mapping, routing/execution fails with a cryptic error instead.
    public static void validateThisThreadedQualifiedPropertiesHaveProxyMapping(GraphQLClassMapping classMapping, PropertyMappingsImplementation setImpl, Class<?> pureClass, CompileContext context)
    {
        for (QualifiedProperty<?> qualifiedProperty : pureClass._qualifiedProperties())
        {
            Property<?, ?> delegate = resolveThisThreadedDelegateProperty(qualifiedProperty);
            if (delegate != null && setImpl._propertyMappings().noneSatisfy(pm -> pm._property()._name().equals(delegate._name())))
            {
                throw new EngineException("GraphQL store: qualified property '" + qualifiedProperty._name() + "' on class '"
                        + HelperModelBuilder.getElementFullPath(pureClass, context.pureModel.getExecutionSupport())
                        + "' delegates to '$this." + delegate._name() + "', but '" + delegate._name()
                        + "' has no mapping - check that it is present (and correctly shaped) in the Binding's JSON schema for this class",
                        classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }

    private static Property<?, ?> resolveThisThreadedDelegateProperty(QualifiedProperty<?> qualifiedProperty)
    {
        RichIterable<? extends ValueSpecification> expressions = qualifiedProperty._expressionSequence();
        if (expressions.size() != 1)
        {
            return null;
        }
        ValueSpecification expression = expressions.getFirst();
        if (!(expression instanceof FunctionExpression))
        {
            return null;
        }
        FunctionExpression functionExpression = (FunctionExpression) expression;
        if (!(functionExpression._func() instanceof Property))
        {
            return null;
        }
        List<? extends ValueSpecification> parameterValues = functionExpression._parametersValues().toList();
        if (parameterValues.isEmpty() || !(parameterValues.get(0) instanceof VariableExpression)
                || !"this".equals(((VariableExpression) parameterValues.get(0))._name()))
        {
            return null;
        }
        return (Property<?, ?>) functionExpression._func();
    }

    public static Root_meta_external_query_graphQL_metamodel_sdl_Document compileSchemaDocument(GraphQLStore graphQLStore, CompileContext context)
    {
        return buildSchemaDocument(graphQLStore.schema, context);
    }

    // Only ObjectTypeDefinition/SchemaDefinition are converted - scalar/enum/interface/union/input-object
    // definitions are skipped entirely.
    private static Root_meta_external_query_graphQL_metamodel_sdl_Document buildSchemaDocument(Document document, CompileContext context)
    {
        MutableList<Root_meta_external_query_graphQL_metamodel_sdl_Definition> definitions = Lists.mutable.empty();
        for (Definition definition : document.definitions)
        {
            if (definition instanceof ObjectTypeDefinition)
            {
                definitions.add(buildObjectTypeDefinition((ObjectTypeDefinition) definition, context));
            }
            else if (definition instanceof SchemaDefinition)
            {
                definitions.add(buildSchemaDefinition((SchemaDefinition) definition, context));
            }
        }
        return new Root_meta_external_query_graphQL_metamodel_sdl_Document_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::Document"))
                ._definitions(definitions.toImmutable());
    }

    private static Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition buildObjectTypeDefinition(ObjectTypeDefinition objectTypeDefinition, CompileContext context)
    {
        Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition pureType = new Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::typeSystem::ObjectTypeDefinition"));
        pureType._name(objectTypeDefinition.name);
        if (objectTypeDefinition.fields != null)
        {
            pureType._fields(Lists.mutable.ofAll(objectTypeDefinition.fields).collect(f -> buildFieldDefinition(f, context)).toImmutable());
        }
        return pureType;
    }

    private static Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition buildFieldDefinition(FieldDefinition fieldDefinition, CompileContext context)
    {
        Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition pureField = new Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::typeSystem::FieldDefinition"));
        pureField._name(fieldDefinition.name);
        pureField._type(buildTypeReference(fieldDefinition.type, context));
        if (fieldDefinition.argumentDefinitions != null)
        {
            pureField._argumentDefinitions(Lists.mutable.ofAll(fieldDefinition.argumentDefinitions).collect(a -> buildInputValueDefinition(a, context)).toImmutable());
        }
        return pureField;
    }

    private static Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_InputValueDefinition buildInputValueDefinition(InputValueDefinition inputValueDefinition, CompileContext context)
    {
        return new Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_InputValueDefinition_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::typeSystem::InputValueDefinition"))
                ._name(inputValueDefinition.name)
                ._type(buildTypeReference(inputValueDefinition.type, context));
    }

    private static Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_TypeReference buildTypeReference(TypeReference typeReference, CompileContext context)
    {
        boolean nullable = Boolean.TRUE.equals(typeReference.nullable);
        if (typeReference instanceof ListTypeReference)
        {
            return new Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ListTypeReference_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::typeSystem::ListTypeReference"))
                    ._itemType(buildTypeReference(((ListTypeReference) typeReference).itemType, context))
                    ._nullable(nullable);
        }
        return new Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_NamedTypeReference_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::typeSystem::NamedTypeReference"))
                ._name(((NamedTypeReference) typeReference).name)
                ._nullable(nullable);
    }

    private static Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_SchemaDefinition buildSchemaDefinition(SchemaDefinition schemaDefinition, CompileContext context)
    {
        Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_SchemaDefinition pureSchemaDefinition = new Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_SchemaDefinition_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::typeSystem::SchemaDefinition"));
        if (schemaDefinition.rootOperationTypeDefinitions != null)
        {
            pureSchemaDefinition._rootOperationTypeDefinitions(Lists.mutable.ofAll(schemaDefinition.rootOperationTypeDefinitions).collect(r -> buildRootOperationTypeDefinition(r, context)).toImmutable());
        }
        return pureSchemaDefinition;
    }

    private static Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_RootOperationTypeDefinition buildRootOperationTypeDefinition(RootOperationTypeDefinition rootOperationTypeDefinition, CompileContext context)
    {
        return new Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_RootOperationTypeDefinition_Impl("", null, context.pureModel.getClass("meta::external::query::graphQL::metamodel::sdl::typeSystem::RootOperationTypeDefinition"))
                ._operationType(context.pureModel.getEnumValue("meta::external::query::graphQL::metamodel::sdl::executable::OperationType", rootOperationTypeDefinition.operationType.name()))
                ._type(rootOperationTypeDefinition.type);
    }

    private static MappingClass<?> generateMappingClass(Class<?> pureClass, String id, Mapping parent, CompileContext context)
    {
        MappingClass<?> mappingClass = new Root_meta_pure_mapping_MappingClass_Impl<>("");

        GenericType gType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass(M3Paths.GenericType))
                ._rawType(context.pureModel.getType(M2MappingPaths.MappingClass))
                ._typeArguments(Lists.immutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass(M3Paths.GenericType))._rawType(mappingClass)));
        Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass(M3Paths.Generalization))
                ._specific(mappingClass)
                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass(M3Paths.GenericType))._rawType(pureClass));

        mappingClass._name(pureClass._name() + "_" + parent._name() + "_" + id);
        mappingClass._classifierGenericType(gType);
        mappingClass._generalizations(Lists.immutable.with(g));

        return mappingClass;
    }

    public static Root_meta_external_store_graphQL_metamodel_pure_GraphQLConnection buildConnection(GraphQLConnection connectionValue, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(connectionValue.sourceInformation);

        Root_meta_external_store_graphQL_metamodel_pure_GraphQLConnection conn = new Root_meta_external_store_graphQL_metamodel_pure_GraphQLConnection_Impl("GraphQLConnection", sourceInformation, context.pureModel.getClass("meta::external::store::graphQL::metamodel::pure::GraphQLConnection"));
        conn._url(connectionValue.url);
        if (connectionValue.authenticationSpecification != null)
        {
            Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification authSpec = HelperAuthenticationBuilder.buildAuthenticationSpecification(connectionValue.authenticationSpecification, context);
            conn._authenticationSpecification(authSpec);
        }
        return conn;
    }
}
