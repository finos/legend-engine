// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration.util;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperMappingBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph.HelperAuthenticationBuilder;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ArrayType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BaseTypeVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BinaryType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BoolType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BsonType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DateType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DecimalType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DoubleType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.IntType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.JavaScriptType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.LongType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MaxKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MinKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.NullType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectIdType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.PropertyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.RegExType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Schema;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.StringType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.TimeStampType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Validator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.JsonSchemaExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBURL;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_ArrayType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_ArrayType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_BaseType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_BoolType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_BoolType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_Collection;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_Collection_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_DateType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_DateType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_DecimalType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_DecimalType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_DoubleType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_DoubleType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_IntType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_IntType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_LongType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_LongType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_NullType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_NullType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_ObjectType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_ObjectType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_PropertyType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_PropertyType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_Schema;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_Schema_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_StringType;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_StringType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_Validator;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_Validator_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_aggregation_JsonSchemaExpression;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_aggregation_JsonSchemaExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_mapping_MongoDBPropertyMapping;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_mapping_MongoDBPropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_EmbeddedMongoDBSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_EmbeddedMongoDBSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_runtime_MongoDBConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_pure_runtime_MongoDBConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_runtime_MongoDBDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_runtime_MongoDBDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_runtime_MongoDBURL;
import org.finos.legend.pure.generated.Root_meta_external_store_mongodb_metamodel_runtime_MongoDBURL_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingClass_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MongoDBCompilerHelper
{
    public static void compileAndAddCollectionstoMongoDatabase(Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase pureMongoDatabase, MongoDatabase mongoDatabase, CompileContext context)
    {
        pureMongoDatabase._collections(FastList.newList(compileCollections(mongoDatabase.collections, pureMongoDatabase, context)).toImmutable());
    }

    private static List<Root_meta_external_store_mongodb_metamodel_Collection> compileCollections(List<Collection> collections, Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase owner, CompileContext context)
    {
        return ListIterate.collect(collections, collection -> compileCollection(collection, owner, context));
    }

    private static Root_meta_external_store_mongodb_metamodel_Collection compileCollection(Collection col, Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase owner, CompileContext context)
    {
        Root_meta_external_store_mongodb_metamodel_Collection pureCollection = new Root_meta_external_store_mongodb_metamodel_Collection_Impl(col.name, null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::Collection"));
        pureCollection._owner(owner);
        pureCollection._name(col.name);
        pureCollection._uuid(col.uuid);
        pureCollection._validator(compileValidator(col.validator, pureCollection, context));
        return pureCollection;
    }

    private static Root_meta_external_store_mongodb_metamodel_Validator compileValidator(Validator validator, Root_meta_external_store_mongodb_metamodel_Collection pureCollection, CompileContext context)
    {
        Root_meta_external_store_mongodb_metamodel_Validator pureValidator = new Root_meta_external_store_mongodb_metamodel_Validator_Impl(pureCollection._name() + "_Validator", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::Validator"));
        if (validator.validationAction != null)
        {
            pureValidator._validationAction(context.resolveEnumValue("meta::external::store::mongodb::metamodel::SchemaValidationAction", validator.validationAction.name()));
        }
        if (validator.validationLevel != null)
        {
            pureValidator._validationLevel(context.resolveEnumValue("meta::external::store::mongodb::metamodel::SchemaValidationLevel", validator.validationLevel.name()));
        }
        if (validator.validatorExpression instanceof JsonSchemaExpression)
        {
            pureValidator._validatorExpression(compileValidatorExpression((JsonSchemaExpression) validator.validatorExpression, pureValidator, context));
        }
        else
        {

            throw new EngineException("JsonSchemaExpression not provided as validator expression", null, EngineErrorType.COMPILATION);
        }
        return pureValidator;
    }

    public static List<PropertyMapping> generatePropertyMappings(Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail bindingDetail, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass, String sourceSetId, List<EmbeddedSetImplementation> embeddedSetImplementations, PropertyMappingsImplementation owner, SourceInformation sourceInformation, Set<Class<?>> processedClasses, CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parent)
    {
        String classFullPath = HelperModelBuilder.getElementFullPath(pureClass, context.pureModel.getExecutionSupport());

        if (processedClasses.contains(pureClass))
        {
            throw new EngineException("Non serializable model mapped with MongoDB Mapping", sourceInformation, EngineErrorType.COMPILATION);
        }
        processedClasses.add(pureClass);

        RichIterable<Property> properties = bindingDetail.mappedPropertiesForClass(pureClass, context.getExecutionSupport());

        RichIterable<Property> primitiveProperties = properties.select(prop -> core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_isPrimitiveValueProperty_AbstractProperty_1__Boolean_1_(prop, context.getExecutionSupport()));
        RichIterable<Property> nonPrimitiveProperties = properties.select(prop -> !core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_isPrimitiveValueProperty_AbstractProperty_1__Boolean_1_(prop, context.getExecutionSupport()));

        List<PropertyMapping> allPropertyMapping = org.eclipse.collections.impl.factory.Lists.mutable.empty();

        List<PropertyMapping> primitivePropertyMappings = primitiveProperties.collect(prop -> buildPrimitivePropertyMapping(prop, sourceSetId, context)).toList();
        List<PropertyMapping> nonPrimitivePropertyMappings = nonPrimitiveProperties.collect(prop -> buildNonPrimitivePropertyMapping(prop, sourceSetId, bindingDetail, embeddedSetImplementations, owner, sourceInformation, new HashSet<>(processedClasses), parent, context, false)).toList();
        List<PropertyMapping> associationPropertyMappings = context.pureModel.getClass(classFullPath)._propertiesFromAssociations().toList().stream().filter(p -> !processedClasses.contains(p._genericType()._rawType())).map(p -> buildNonPrimitivePropertyMapping(p, sourceSetId, bindingDetail, embeddedSetImplementations, owner, sourceInformation, processedClasses, parent, context, true)).collect(Collectors.toList());

        allPropertyMapping.addAll(primitivePropertyMappings);
        allPropertyMapping.addAll(nonPrimitivePropertyMappings);
        allPropertyMapping.addAll(associationPropertyMappings);

        return allPropertyMapping;
    }


    private static PropertyMapping buildPrimitivePropertyMapping(Property property, String sourceSetId, CompileContext context)
    {
        Root_meta_external_store_mongodb_metamodel_mapping_MongoDBPropertyMapping propertyMapping = new Root_meta_external_store_mongodb_metamodel_mapping_MongoDBPropertyMapping_Impl("", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::mapping::MongoDBPropertyMapping"));

        propertyMapping._property(property);
        propertyMapping._sourceSetImplementationId(sourceSetId);
        propertyMapping._targetSetImplementationId("");

        return propertyMapping;
    }

    public static PropertyMapping buildNonPrimitivePropertyMapping(Property property, String sourceSetId, Root_meta_external_format_shared_binding_validation_SuccessfulBindingDetail bindingDetail, List<EmbeddedSetImplementation> embeddedSetImplementations, PropertyMappingsImplementation owner, SourceInformation sourceInformation, Set<Class<?>> processedClasses, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parent, CompileContext context, boolean fromAssociation)
    {
        Root_meta_external_store_mongodb_metamodel_pure_EmbeddedMongoDBSetImplementation propertyMapping = new Root_meta_external_store_mongodb_metamodel_pure_EmbeddedMongoDBSetImplementation_Impl("", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::pure::EmbeddedMongoDBSetImplementation"));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class) property._genericType()._rawType();
        String id = owner._id() + "_" + property._name();

        String newSourceSetId = fromAssociation ? "Association_" + sourceSetId : sourceSetId;

        propertyMapping._class(pureClass);
        propertyMapping._id(id);
        propertyMapping._owner(owner);
        propertyMapping._parent(parent);
        propertyMapping._property(property);
        propertyMapping._root(false);
        propertyMapping._sourceSetImplementationId(newSourceSetId);
        propertyMapping._targetSetImplementationId(id);

        propertyMapping._propertyMappings(FastList.newList(generatePropertyMappings(bindingDetail, pureClass, id, embeddedSetImplementations, propertyMapping, sourceInformation, new HashSet<>(processedClasses), context, parent)).toImmutable());

        embeddedSetImplementations.add(propertyMapping);
        return propertyMapping;
    }

    public static Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation createMongoDBSetImplementation(RootMongoDBClassMapping classMapping, CompileContext context, Mapping parentMapping, ClassMapping cm, Root_meta_external_format_shared_binding_Binding binding)
    {
        String id = HelperMappingBuilder.getClassMappingId(classMapping, context);
        Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation mongoDBSetImplementation = new Root_meta_external_store_mongodb_metamodel_pure_MongoDBSetImplementation_Impl(id, null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::pure::MongoDBSetImplementation"));
        Store mongoDatabase = context.pureModel.getStore(((RootMongoDBClassMapping) cm).storePath, cm.sourceInformation);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass = context.resolveClass(classMapping._class, classMapping.classSourceInformation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass mappingClass = MongoDBCompilerHelper.generateMappingClass(pureClass, id, classMapping, parentMapping, context);
        mongoDBSetImplementation._id(id);
        mongoDBSetImplementation._root(classMapping.root);
        mongoDBSetImplementation._class(pureClass);
        mongoDBSetImplementation._parent(parentMapping);
        mongoDBSetImplementation._mappingClass(mappingClass);
        mongoDBSetImplementation._storesAdd(mongoDatabase);
        mongoDBSetImplementation._mainCollection(((Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase_Impl) mongoDatabase)._collections());
        mongoDBSetImplementation._binding(binding);

        return mongoDBSetImplementation;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass generateMappingClass(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass, String id, RootMongoDBClassMapping mongoDBClassMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parent, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass mappingClass = new Root_meta_pure_mapping_MappingClass_Impl<>("");

        GenericType gType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(context.pureModel.getType("meta::pure::mapping::MappingClass"))
                ._typeArguments(org.eclipse.collections.impl.factory.Lists.mutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(mappingClass)));
        Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                ._specific(mappingClass)
                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(pureClass));

        mappingClass._name(pureClass._name() + "_" + parent._name() + "_" + id);
        mappingClass._classifierGenericType(gType);
        mappingClass._generalizations(org.eclipse.collections.impl.factory.Lists.mutable.with(g));

        return mappingClass;
    }

    private static Root_meta_external_store_mongodb_metamodel_aggregation_JsonSchemaExpression compileValidatorExpression(JsonSchemaExpression validatorExpression, Root_meta_external_store_mongodb_metamodel_Validator pureValidator, CompileContext context)
    {
        Root_meta_external_store_mongodb_metamodel_aggregation_JsonSchemaExpression pureSchemaExpression = new Root_meta_external_store_mongodb_metamodel_aggregation_JsonSchemaExpression_Impl(pureValidator.getName() + "_JsonSchemaExpression", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::aggregation::JsonSchemaExpression"));
        pureSchemaExpression._schemaExpression(compileSchema(validatorExpression.schemaExpression, pureSchemaExpression, context));
        return pureSchemaExpression;
    }

    private static Root_meta_external_store_mongodb_metamodel_Schema compileSchema(Schema schemaExpression, Root_meta_external_store_mongodb_metamodel_aggregation_JsonSchemaExpression pureSchemaExpression, CompileContext context)
    {
        Root_meta_external_store_mongodb_metamodel_Schema pureSchema = new Root_meta_external_store_mongodb_metamodel_Schema_Impl(pureSchemaExpression.getName() + "_Schema", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::Schema"));

        Root_meta_external_store_mongodb_metamodel_ObjectType pureObjectType = compileObjectType(schemaExpression, pureSchema, context);
        pureSchema._description(pureObjectType._description());
        pureSchema._additionalPropertiesAllowed(pureObjectType._additionalPropertiesAllowed());
        pureSchema._maxProperties(pureObjectType._maxProperties());
        pureSchema._minProperties(pureObjectType._minProperties());
        pureSchema._required(pureObjectType._required());
        pureSchema._properties(pureObjectType._properties());
        return pureSchema;
    }

    private static Root_meta_external_store_mongodb_metamodel_ObjectType compileObjectType(ObjectType objType, Root_meta_external_store_mongodb_metamodel_Schema pureSchema, CompileContext context)
    {
        Root_meta_external_store_mongodb_metamodel_ObjectType pureObjectType = new Root_meta_external_store_mongodb_metamodel_ObjectType_Impl(pureSchema.getName() + "_ObjectType", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::ObjectType"));
        pureObjectType._title(objType.title);
        pureObjectType._description(objType.description);
        pureObjectType._additionalPropertiesAllowed(objType.additionalPropertiesAllowed);
        pureObjectType._maxProperties(objType.maxProperties);
        pureObjectType._minProperties(objType.minProperties);
        pureObjectType._required(ListIterate.collect(objType.required, r -> r));
        pureObjectType._properties(compileProperties(objType.properties, pureObjectType, context));
        return pureObjectType;
    }

    private static RichIterable<? extends Root_meta_external_store_mongodb_metamodel_PropertyType> compileProperties(List<PropertyType> properties, Root_meta_external_store_mongodb_metamodel_ObjectType pureObjType, CompileContext context)
    {
        return ListIterate.collect(properties, p ->
        {
            Root_meta_external_store_mongodb_metamodel_PropertyType propType = new Root_meta_external_store_mongodb_metamodel_PropertyType_Impl(pureObjType.getName() + "_Property", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::PropertyType"));
            propType._key(p.key);
            BaseTypeBuilder baseTypeBuilder = new BaseTypeBuilder(context, pureObjType, propType._key());
            propType._value(p.value.accept(baseTypeBuilder));
            return propType;
        }).toList();
    }

    public static Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase getMongoDatabase(String buildPackageString, SourceInformation sourceInformation, CompileContext context)
    {
        try
        {
            Store store = context.pureModel.getStore(buildPackageString, sourceInformation);
            if (store instanceof Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase)
            {
                return (Root_meta_external_store_mongodb_metamodel_pure_MongoDatabase) store;
            }
            throw new RuntimeException("Store found but not a MongoDatabase");
        }
        catch (Exception e)
        {
            throw new EngineException("Can't find MongoDBStore '" + buildPackageString + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static Root_meta_external_store_mongodb_metamodel_pure_runtime_MongoDBConnection buildConnection(MongoDBConnection connectionValue, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(connectionValue.sourceInformation);

        Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification authSpec = HelperAuthenticationBuilder.buildAuthenticationSpecification(connectionValue.authenticationSpecification, context);
        Root_meta_external_store_mongodb_metamodel_runtime_MongoDBDatasourceSpecification dbDatasourceSpecification = new Root_meta_external_store_mongodb_metamodel_runtime_MongoDBDatasourceSpecification_Impl("MongoDBDatasourceSpecification", sourceInformation, context.pureModel.getClass("meta::external::store::mongodb::metamodel::runtime::MongoDBDatasourceSpecification"));
        dbDatasourceSpecification._databaseName(connectionValue.dataSourceSpecification.databaseName);
        dbDatasourceSpecification._serverURLs(compileServerURLs(connectionValue, context, sourceInformation));

        Root_meta_external_store_mongodb_metamodel_pure_runtime_MongoDBConnection conn = new Root_meta_external_store_mongodb_metamodel_pure_runtime_MongoDBConnection_Impl("MongoDBConnection", sourceInformation, context.pureModel.getClass("meta::external::store::mongodb::metamodel::pure::runtime::MongoDBConnection"))
                ._authenticationSpecification(authSpec)
                ._dataSourceSpecification(dbDatasourceSpecification)
                ._type(context.pureModel.getEnumValue("meta::external::store::mongodb::metamodel::runtime::DatabaseType", connectionValue.type.toString()));

        return conn._validate(true, sourceInformation, context.getExecutionSupport());

    }

    public static RichIterable<? extends Root_meta_external_store_mongodb_metamodel_runtime_MongoDBURL> compileServerURLs(MongoDBConnection connectionValue, CompileContext context, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation)
    {
        List<MongoDBURL> serverURLs = connectionValue.dataSourceSpecification.serverURLs;
        return ListIterate.collect(serverURLs, u ->
        {
            Root_meta_external_store_mongodb_metamodel_runtime_MongoDBURL url = new Root_meta_external_store_mongodb_metamodel_runtime_MongoDBURL_Impl("MongoDBURL", sourceInformation,
                    context.pureModel.getClass("meta::external::store::mongodb::metamodel::runtime::MongoDBURL"));
            Assert.assertTrue(u.port > 0, () -> "MongoDB URL is missing port: " + u.baseUrl, connectionValue.sourceInformation, EngineErrorType.COMPILATION);
            url._baseUrl(u.baseUrl);
            url._port(u.port);
            return url;
        });

    }

    private static class BaseTypeBuilder implements BaseTypeVisitor<Root_meta_external_store_mongodb_metamodel_BaseType>
    {
        private final CompileContext context;
        private final Root_meta_external_store_mongodb_metamodel_ObjectType pureObjType;
        private final String propName;

        private BaseTypeBuilder(CompileContext context, Root_meta_external_store_mongodb_metamodel_ObjectType pureObjType, String propName)
        {
            this.context = context;
            this.pureObjType = pureObjType;
            this.propName = propName;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(ArrayType val)
        {
            Root_meta_external_store_mongodb_metamodel_ArrayType arrayType = new Root_meta_external_store_mongodb_metamodel_ArrayType_Impl(pureObjType.getName() + "_" + this.propName + "_Array", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::ArrayType"));
            arrayType._title(val.title);
            arrayType._description(val.description);
            arrayType._maxItems(val.maxItems);
            arrayType._minItems(val.minItems);
            arrayType._uniqueItems(val.uniqueItems);
            arrayType._additionalItemsAllowed(val.additionalItemsAllowed);
            arrayType._items(ListIterate.collect(val.items, item ->
            {
                BaseTypeBuilder baseTypeBuilder = new BaseTypeBuilder(context, pureObjType, propName);
                return item.accept(baseTypeBuilder);
            }));
            return arrayType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(BinaryType val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(BoolType val)
        {
            Root_meta_external_store_mongodb_metamodel_BoolType boolType = new Root_meta_external_store_mongodb_metamodel_BoolType_Impl(pureObjType.getName() + "_" + this.propName + "_Bool", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::BoolType"));
            boolType._description(val.description);
            boolType._title(val.title);
            return boolType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(BsonType val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(DateType val)
        {
            Root_meta_external_store_mongodb_metamodel_DateType dateType = new Root_meta_external_store_mongodb_metamodel_DateType_Impl(pureObjType.getName() + "_" + this.propName + "_Date", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::DateType"));
            dateType._description(val.description);
            dateType._title(val.title);
            return dateType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(DecimalType val)
        {
            Root_meta_external_store_mongodb_metamodel_DecimalType decimalType = new Root_meta_external_store_mongodb_metamodel_DecimalType_Impl(pureObjType.getName() + "_" + this.propName + "_Decimal", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::DecimalType"));
            decimalType._description(val.description);
            decimalType._title(val.title);
            decimalType._maximum(val.maximum);
            decimalType._minimum(val.minimum);
            decimalType._exclusiveMaximum(val.exclusiveMaximum);
            decimalType._exclusiveMinimum(val.exclusiveMinimum);
            return decimalType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(DoubleType val)
        {
            Root_meta_external_store_mongodb_metamodel_DoubleType dblType = new Root_meta_external_store_mongodb_metamodel_DoubleType_Impl(pureObjType.getName() + "_" + this.propName + "_Double", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::DoubleType"));
            dblType._description(val.description);
            dblType._title(val.title);
            dblType._maximum(val.maximum);
            dblType._minimum(val.minimum);
            dblType._exclusiveMaximum(val.exclusiveMaximum);
            dblType._exclusiveMinimum(val.exclusiveMinimum);
            return dblType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(IntType val)
        {
            Root_meta_external_store_mongodb_metamodel_IntType intType = new Root_meta_external_store_mongodb_metamodel_IntType_Impl(pureObjType.getName() + "_" + this.propName + "_Int", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::IntType"));
            intType._description(val.description);
            intType._title(val.title);
            intType._maximum(val.maximum);
            intType._minimum(val.minimum);
            intType._exclusiveMaximum(val.exclusiveMaximum);
            intType._exclusiveMinimum(val.exclusiveMinimum);
            return intType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(JavaScriptType val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(LongType val)
        {
            Root_meta_external_store_mongodb_metamodel_LongType longType = new Root_meta_external_store_mongodb_metamodel_LongType_Impl(pureObjType.getName() + "_" + this.propName + "_Long", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::LongType"));
            longType._description(val.description);
            longType._title(val.title);
            longType._maximum(val.maximum);
            longType._minimum(val.minimum);
            longType._exclusiveMaximum(val.exclusiveMaximum);
            longType._exclusiveMinimum(val.exclusiveMinimum);
            return longType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(MaxKeyType val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(MinKeyType val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(NullType val)
        {
            Root_meta_external_store_mongodb_metamodel_NullType nullType = new Root_meta_external_store_mongodb_metamodel_NullType_Impl(pureObjType.getName() + "_Null", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::NullType"));
            nullType._description(val.description);
            nullType._title(val.title);
            return nullType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(ObjectIdType val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(ObjectType val)
        {
            Root_meta_external_store_mongodb_metamodel_ObjectType pureObjectType = new Root_meta_external_store_mongodb_metamodel_ObjectType_Impl(pureObjType.getName() + "_" + this.propName + "_ObjectType", null, context.pureModel.getClass("meta::external::store::mongodb::metamodel::ObjectType"));
            pureObjectType._title(val.title);
            pureObjectType._description(val.description);
            pureObjectType._additionalPropertiesAllowed(val.additionalPropertiesAllowed);
            pureObjectType._maxProperties(val.maxProperties);
            pureObjectType._minProperties(val.minProperties);
            pureObjectType._required(ListIterate.collect(val.required, r -> r));
            pureObjectType._properties(compileProperties(val.properties, pureObjectType, context));
            return pureObjectType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(RegExType val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(Schema val)
        {
            return null;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(StringType val)
        {
            Root_meta_external_store_mongodb_metamodel_StringType strType = new Root_meta_external_store_mongodb_metamodel_StringType_Impl(pureObjType.getName() + "_" + this.propName + "_String", null, this.context.pureModel.getClass("meta::external::store::mongodb::metamodel::StringType"));
            strType._maxLength(val.maxLength);
            strType._minLength(val.minLength);
            strType._description(val.description);
            strType._title(val.title);
            return strType;
        }

        @Override
        public Root_meta_external_store_mongodb_metamodel_BaseType visit(TimeStampType val)
        {
            return null;
        }
    }
}
