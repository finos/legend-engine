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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.validation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.MappingValidatorContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.DatabaseProcessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.DataType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.platform_dsl_mapping_functions_Mapping.Root_meta_pure_mapping_classMappingById_Mapping_1__String_1__SetImplementation_$0_1$_;

public class RelationalValidator
{
    private static final String TARGET = "target";
    private static final String SOURCE = "source";

    public static void validateRelationalMapping(PureModel pureModel, MappingValidatorContext mappingValidatorContext)
    {
        //validate all the property Mappings
        mappingValidatorContext.getPureMappings().forEach((s, mapping) ->
                {
                    mapping._classMappings().select(cm -> cm instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation).forEach(cm ->
                    {
                        ((RootRelationalInstanceSetImplementation) cm)._propertyMappings().select(p -> p instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping && !(p instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.SemiStructuredRelationalPropertyMapping)).forEach(pm -> validateRelationalPropertyMapping((RootRelationalInstanceSetImplementation) cm, pureModel, (org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping) pm, s, mappingValidatorContext));
                    });
                    mapping._associationMappings().select(a -> a instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalAssociationImplementation).forEach(relAssoc ->
                    {
                        validateAssociationImplementation((RelationalAssociationImplementation) relAssoc, pureModel, s, mappingValidatorContext);
                    });
                }
        );
        //TODO: Inline , embedded binding
    }

    private static void relationalException(PureModel pureModel, String message, SourceInformation sourceInformation, Boolean useWarning)
    {
        if (useWarning)
        {
            pureModel.addWarnings(Lists.mutable.with(new Warning(sourceInformation, message)));
        }
        else
        {
            throw new EngineException(message, sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static void validateRelationalPropertyMapping(RootRelationalInstanceSetImplementation mappingInstance, PureModel pureModel, org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping propertyMapping, String mappingID, MappingValidatorContext mappingValidatorContext)
    {
        TableAlias tableAlias = mappingInstance._mainTableAlias();
        RelationalOperationElement startTable = (tableAlias == null) ? null : tableAlias._relationalElement();
        Mapping pureMapping = mappingValidatorContext.getPureMappings().get(mappingID);
        RelationalOperationElement elem = propertyMapping._relationalOperationElement();
        Property<? extends Object, ? extends Object> property = propertyMapping._property();
        String targetId = propertyMapping._targetSetImplementationId();
        Type targetClass = property._genericType()._rawType();

        if (targetClass instanceof DataType)
        {
            if (elem instanceof RelationalOperationElementWithJoin)
            {
                JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin) elem)._joinTreeNode();
                if (joinTreeNode != null)
                {
                    validateJoinTreeNode(joinTreeNode, startTable, null, propertyMapping, mappingID, pureModel, mappingValidatorContext);
                }
                if (((RelationalOperationElementWithJoin) elem)._relationalOperationElement() == null)
                {
                    SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
                    relationalException(pureModel, "Mapping error on mapping " + mappingID + ". The property '" + property.getName() + "' returns a data type. However it's mapped to a Join.", sourceInfo, true);
                }
            }
        }
        else
        {
            //scope of valid targets are current mapping plus includes
            Mapping targetMapping = mappingValidatorContext.getMappingRootMappingAndClassMappingId().get(pureMapping).get(targetId);

            if (targetMapping != null)
            { //an incorrect setID gets thrown as a warning since the set can be referenced in a global scope
                SetImplementation targetInstanceMapping = Root_meta_pure_mapping_classMappingById_Mapping_1__String_1__SetImplementation_$0_1$_(targetMapping, targetId, pureModel.getExecutionSupport());
                boolean invalidSubtype = targetInstanceMapping != null && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(targetInstanceMapping._class(), targetClass, pureModel.getExecutionSupport().getProcessorSupport());

                if (invalidSubtype)
                {
                    SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
                    relationalException(pureModel, "Mapping Error: on " + mappingID + " The setImplementationId '" + targetId + "' is implementing the class '" + targetInstanceMapping._class()._name() + "' which is not a subType of '" + targetClass._name() + "' return type of the mapped property '" + property.getName() + "'", sourceInfo, true);
                }

                JoinTreeNode joinTreeNode = elem instanceof RelationalOperationElementWithJoin ? ((RelationalOperationElementWithJoin) elem)._joinTreeNode() : null;
                if (!invalidSubtype && joinTreeNode == null)
                {
                    SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
                    relationalException(pureModel, "Mapping Error: on " + mappingID + " The target type:'" + targetClass + "' on property " + property.getName() + " is not a data type and a join is expected", sourceInfo, true);
                }
                if (!invalidSubtype && joinTreeNode != null && ((RelationalOperationElementWithJoin) elem)._relationalOperationElement() != null)
                {
                    SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
                    relationalException(pureModel, "Mapping Error: on " + mappingID + " The property '" + property.getName() + "' doesn't return a data type. However it's mapped to a column or a function.", sourceInfo, true);
                }

                if (!invalidSubtype && joinTreeNode != null && ((RelationalOperationElementWithJoin) elem)._relationalOperationElement() == null)
                {
                    // TODO targetInstanceMapping should never be null (see above TODO)
                    if (targetInstanceMapping == null)
                    {
                        validateJoinTreeNode(joinTreeNode, startTable, null, propertyMapping, mappingID, pureModel, mappingValidatorContext);
                    }
                    else if (!(targetInstanceMapping instanceof RootRelationalInstanceSetImplementation))
                    {
                        // TODO should we throw an exception here?
                    }
                    else
                    {
                        RelationalOperationElement targetMainTable = ((RootRelationalInstanceSetImplementation) targetInstanceMapping)._mainTableAlias()._relationalElement();
                        validateJoinTreeNode(joinTreeNode, startTable, targetMainTable, propertyMapping, mappingID, pureModel, mappingValidatorContext);
                    }
                }
            }
        }
    }

    private static String getRelationalElementIdentifier(RelationalOperationElement element)
    {
        StringBuilder builder = new StringBuilder();
        String tableName = null;
        Schema tableSchema = null;
        if (element instanceof Table || element instanceof View)
        {
            if (element instanceof Table)
            {
                tableName = ((Table) element)._name();
                tableSchema = ((Table) element)._schema();
            }
            else if (element instanceof View)
            {
                tableName = ((View) element)._name();
                tableSchema = ((View) element)._schema();
            }
            if (tableSchema != null)
            {
                String tableSchemaName = tableSchema._name();
                org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database tableDB = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) tableSchema._database();
                if (tableDB != null)
                {
                    builder.append("[" + tableDB._name() + "]");
                }
                if (!DatabaseProcessor.DEFAULT_SCHEMA_NAME.equals(tableSchemaName))
                {
                    builder.append(tableSchemaName);
                    builder.append('.');
                }
            }
            builder.append(tableName);
        }

        return builder.toString();

    }

    private static void validateJoinTreeNode(JoinTreeNode joinTreeNode, RelationalOperationElement sourceTable, RelationalOperationElement targetTable, PropertyMapping propertyMapping, String mappingID, PureModel pureModel, MappingValidatorContext mappingValidatorContext)
    {
        Join join = joinTreeNode._join();
        RelationalOperationElement newSourceTable = followJoin(join, sourceTable);
        if (newSourceTable == null)
        {
            SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
            relationalException(pureModel, "Mapping error: " + mappingID + " the join " + join._name() + " does not contain the source table " + getRelationalElementIdentifier(sourceTable), sourceInfo, true);
        }

        RichIterable<? extends TreeNode> childrenData = joinTreeNode._childrenData();
        if (childrenData.isEmpty() && newSourceTable != null)
        {
            if ((targetTable != null) && (targetTable != newSourceTable))
            {
                SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
                relationalException(pureModel, "Mapping error: " + mappingID + " the join " + join._name() + " does not connect from the source table " + getRelationalElementIdentifier(sourceTable) + " to the target table " + getRelationalElementIdentifier(targetTable) + "; instead it connects to " + getRelationalElementIdentifier(newSourceTable), sourceInfo, true);

            }
        }
        else if (!childrenData.isEmpty() && newSourceTable != null)
        {
            for (TreeNode child : childrenData)
            {
                validateJoinTreeNode((JoinTreeNode) child, newSourceTable, targetTable, propertyMapping, mappingID, pureModel, mappingValidatorContext);
            }
        }
    }

    private static RelationalOperationElement followJoin(Join join, RelationalOperationElement sourceTable)
    {
        for (Pair pair : join._aliases())
        {
            if (sourceTable == ((TableAlias) pair._first())._relationalElement())
            {
                return ((TableAlias) pair._second())._relationalElement();
            }
        }
        return null;
    }


    public static void validateAssociationImplementation(RelationalAssociationImplementation associationMapping, PureModel pureModel, String mappingID, MappingValidatorContext mappingValidatorContext) throws PureCompilationException
    {
        Mapping parentMapping = (Mapping) associationMapping._parent();
        MapIterable<String, SetImplementation> classMappingIndex = getClassMappingsByIdIncludeEmbedded(pureModel, parentMapping, mappingValidatorContext);

        RichIterable<? extends PropertyMapping> propertyMappings = associationMapping._propertyMappings();
        for (PropertyMapping propertyMapping : propertyMappings)
        {
            Optional<SetImplementation> sourceSetImplementation = validateAssociationId(associationMapping, propertyMapping, classMappingIndex, propertyMapping._sourceSetImplementationId(), SOURCE, mappingID, pureModel, mappingValidatorContext);

            Optional<SetImplementation> targetSetImplementation = validateAssociationId(associationMapping, propertyMapping, classMappingIndex, propertyMapping._targetSetImplementationId(), TARGET, mappingID, pureModel, mappingValidatorContext);


            JoinTreeNode joinTreeNode = propertyMapping instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping && ((org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping) propertyMapping)._relationalOperationElement() instanceof RelationalOperationElementWithJoin ? ((RelationalOperationElementWithJoin) ((org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping) propertyMapping)._relationalOperationElement())._joinTreeNode() : null;
            if (joinTreeNode == null)
            {
                SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
                relationalException(pureModel, "Mapping Error! on " + mappingID + " Expected a Join", sourceInfo, true);
            }
            else if (sourceSetImplementation.isPresent() && targetSetImplementation.isPresent())
            {
                RelationalOperationElement sourceTable = findMainTable((RelationalInstanceSetImplementation) sourceSetImplementation.get());
                RelationalOperationElement targetTable = findMainTable((RelationalInstanceSetImplementation) targetSetImplementation.get());
                validateJoinTreeNode(joinTreeNode, sourceTable, targetTable, propertyMapping, mappingID, pureModel, mappingValidatorContext);
            }
        }
    }

    public static Optional<SetImplementation> validateAssociationId(AssociationImplementation associationMapping, PropertyMapping propertyMapping, MapIterable<String, ? extends SetImplementation> classMappingIndex, String setImplementationId, String sourceOrTarget, String mappingID, PureModel pureModel, MappingValidatorContext mappingValidatorContext)
    {

        SetImplementation setImplementation = classMappingIndex.get(setImplementationId);
        if (setImplementation == null)
        {
            SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
            relationalException(pureModel, "Unable to find " + sourceOrTarget + " class mapping (id:" + setImplementationId + ") for property \'"
                    + propertyMapping._property()._name() + "\' in Association mapping \'" + HelperModelBuilder.getElementFullPath(associationMapping._association(), pureModel.getExecutionSupport())
                    + "\'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax \'property[sourceId, targetId]\'.", sourceInfo, true);

        }
        Type targetClass = propertyMapping._property()._genericType()._rawType();

        if (sourceOrTarget.equals(TARGET) && setImplementation != null && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(setImplementation._class(), targetClass, pureModel.getExecutionSupport().getProcessorSupport()))
        {
            SourceInformation sourceInfo = safeGetSourceInfoForProperty(propertyMapping, mappingID, pureModel, mappingValidatorContext);
            relationalException(pureModel, "Mapping Error: on " + mappingID + " The setImplementationId '" + setImplementationId + "' is implementing the class '" + setImplementation._class()._name() + "' which is not a subType of '" + targetClass._name() + "' return type of the mapped property '" + propertyMapping._property().getName() + "'", sourceInfo, true);
        }

        return Optional.ofNullable(setImplementation);
    }

    private static RelationalOperationElement findMainTable(RelationalInstanceSetImplementation setImplementation)
    {
        TableAlias mainTableAlias = findMainTableAlias(setImplementation);
        return mainTableAlias._relationalElement();
    }

    private static TableAlias findMainTableAlias(RelationalInstanceSetImplementation setImplementation)
    {
        if (setImplementation instanceof RootRelationalInstanceSetImplementation)
        {
            return ((RootRelationalInstanceSetImplementation) setImplementation)._mainTableAlias();
        }
        if (setImplementation instanceof EmbeddedRelationalInstanceSetImplementation)
        {
            RootRelationalInstanceSetImplementation owner = ((EmbeddedRelationalInstanceSetImplementation) setImplementation)._setMappingOwner();
            return findMainTableAlias(owner);
        }
        throw new RuntimeException("Unhandled set implementation type: " + PackageableElement.getUserPathForPackageableElement(setImplementation.getClassifier()));
    }


    private static SourceInformation safeGetSourceInfoForProperty(PropertyMapping propertyMapping, String mappingID, PureModel pureModel, MappingValidatorContext mappingValidatorContext)
    {
        try
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping protocolMap = mappingValidatorContext.getProtocolMappingsByNameWithID(mappingID);
            PropertyMappingsImplementation propertyOwner = propertyMapping._owner();
            if (propertyOwner instanceof RootRelationalInstanceSetImplementation)
            {
                String ownerSetID = propertyMapping._owner()._id();
                List<ClassMapping> protocolCM = protocolMap.classMappings.stream().filter(c -> c.id == null ? c._class.equals(HelperModelBuilder.getElementFullPath(((RootRelationalInstanceSetImplementation) propertyMapping._owner())._class(), pureModel.getExecutionSupport())) : c.id == ownerSetID).collect(Collectors.toList());
                if (protocolCM.size() == 1 && protocolCM.get(0) instanceof RootRelationalClassMapping)
                {
                    List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping> protocolProperty = ((RootRelationalClassMapping) protocolCM.get(0)).propertyMappings.stream().filter(pm -> pm.property.property.equals(propertyMapping._property()._name()) && (pm.target == null && propertyMapping._targetSetImplementationId().equals("")) ||
                            (pm.target != null && pm.target.equals(propertyMapping._targetSetImplementationId()))).collect(Collectors.toList());
                    if (protocolProperty.size() == 1)
                    {
                        return protocolProperty.get(0).sourceInformation;
                    }
                    else
                    {
                        return protocolCM.get(0).sourceInformation;
                    }
                }
            }
            else if (propertyOwner instanceof RelationalAssociationImplementation)
            {
                List<AssociationMapping> protocolAM = protocolMap.associationMappings.stream().filter(a -> a.association.equals(HelperModelBuilder.getElementFullPath(((RelationalAssociationImplementation) propertyMapping._owner())._association(), pureModel.getExecutionSupport()))).collect(Collectors.toList());
                if (protocolAM.size() == 1 && protocolAM.get(0) instanceof RelationalAssociationMapping)
                {
                    List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping> protocolProperty = ((RelationalAssociationMapping) protocolAM.get(0)).propertyMappings.stream().filter(pm -> pm.property.property.equals(propertyMapping._property()._name()) && pm.target == null ||
                            (pm.target != null && pm.target.equals(propertyMapping._targetSetImplementationId()))).collect(Collectors.toList());
                    if (protocolProperty.size() == 1)
                    {
                        return protocolProperty.get(0).sourceInformation;
                    }
                    else
                    {
                        return protocolAM.get(0).sourceInformation;
                    }
                }
            }
            else
            {
                return protocolMap.sourceInformation;
            }
            return protocolMap.sourceInformation;

        }
        catch (Exception e)
        {
            return SourceInformation.getUnknownSourceInformation();

        }

    }

    public static MapIterable<String, SetImplementation> getClassMappingsByIdIncludeEmbedded(PureModel pureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, MappingValidatorContext mappingValidatorContext)
    {
        MutableMap<String, SetImplementation> classMappingsById = (MutableMap<String, SetImplementation>) getMappingEntitiesById(pureModel, mapping, "class", M2MappingProperties.classMappings, M3Properties.id, mappingValidatorContext);
        MutableMap<String, SetImplementation> embeddedClassMappingIds = getEmbeddedMappingsByIdForClassMapping(classMappingsById.valuesView());
        embeddedClassMappingIds.putAll(classMappingsById);
        return embeddedClassMappingIds;
    }

    private static MutableMap<String, SetImplementation> getEmbeddedMappingsByIdForClassMapping(RichIterable<SetImplementation> classMappings)
    {
        MutableMap<String, SetImplementation> embeddedMappingsById = Maps.mutable.of();
        for (SetImplementation classMapping : classMappings)
        {
            getEmbeddedMappingsByIdForPropertiesMapping(classMapping, embeddedMappingsById);
        }
        return embeddedMappingsById;
    }

    private static MutableMap<String, SetImplementation> getEmbeddedMappingsByIdForPropertiesMapping(CoreInstance propertyMappingOwner, MutableMap<String, SetImplementation> embeddedMappingsById)
    {
        RichIterable<? extends PropertyMapping> propertyMappings = propertyMappingOwner instanceof PropertyMappingsImplementation ? ((PropertyMappingsImplementation) propertyMappingOwner)._propertyMappings() : Lists.fixedSize.<PropertyMapping>empty();

        for (PropertyMapping propertyMapping : propertyMappings)
        {
            if (propertyMapping instanceof EmbeddedSetImplementation)
            {
                embeddedMappingsById.put(((EmbeddedSetImplementation) propertyMapping)._id(), (EmbeddedSetImplementation) propertyMapping);
                getEmbeddedMappingsByIdForPropertiesMapping(propertyMapping, embeddedMappingsById);
            }
        }
        return embeddedMappingsById;
    }
//    private static CoreInstance getMappingEntityById(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, String id, String entityProperty, String idProperty, MutableSet<CoreInstance> visited)
//    {
//        if (visited.add(mapping))
//        {
//            CoreInstance entity = mapping.getValueInValueForMetaPropertyToManyWithKey(entityProperty, idProperty, id);
//            if (entity != null)
//            {
//                return entity;
//            }
//            for (MappingInclude include : mapping._includes())
//            {
//                org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping includedMapping = include._included();
//                entity = getMappingEntityById(includedMapping, id, entityProperty, idProperty, visited);
//                if (entity != null)
//                {
//                    return entity;
//                }
//            }
//        }
//        return null;
//    }

//    public static MapIterable<String, SetImplementation> getClassMappingsById(PureModel pureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, MappingValidatorContext mappingValidatorContext)
//    {
//        //Missing inline embedded Ids in the classmappings
//        return (MapIterable<String, SetImplementation>) getMappingEntitiesById(pureModel, mapping, "class", M2MappingProperties.classMappings, M3Properties.id, mappingValidatorContext);
//    }

    private static MutableMap<String, ? extends CoreInstance> getMappingEntitiesById(PureModel pureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, String entityType, String entityProperty, String idProperty, MappingValidatorContext mappingValidatorContext)
    {
        MutableMap<String, CoreInstance> map = Maps.mutable.empty();
        collectMappingEntitiesById(pureModel, map, mapping, mapping, entityType, entityProperty, idProperty, Sets.mutable.<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping>empty(), mappingValidatorContext);
        return map;
    }

    private static void collectMappingEntitiesById(PureModel pureModel, MutableMap<String, CoreInstance> map, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping currentMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping rootMapping, String entityType, String entityProperty, String idProperty, MutableSet<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> visited, MappingValidatorContext mappingValidatorContext)
    {
        if (visited.add(currentMapping))
        {
            RichIterable<? extends SetImplementation> currentClassMappings = currentMapping._classMappings().select(c -> !(c instanceof EmbeddedRelationalInstanceSetImplementation));
            for (CoreInstance entity : entityProperty.equals(M2MappingProperties.classMappings) ? currentClassMappings : currentMapping._enumerationMappings())
            {
                String id = entity instanceof SetImplementation ? ((SetImplementation) entity)._id() : ((EnumerationMapping) entity)._name();
                if (id == null)
                {
                    StringBuilder message = new StringBuilder(entityType);
                    message.append(" mapping without an id in mapping ");
                    PackageableElement.writeUserPathForPackageableElement(message, currentMapping);
                    relationalException(pureModel, message.toString(), mappingValidatorContext.getProtocolMappingsByNameWithID(currentMapping._name()).sourceInformation, true);
                }
                CoreInstance old = map.put(id, entity);
                if (old != null)
                {
                    StringBuilder message = new StringBuilder("Multiple ");
                    message.append(entityType);
                    message.append(" mappings with ");
                    message.append(idProperty);
                    message.append(" '");
                    message.append(id);
                    message.append("' for mapping " + currentMapping._name());
                    relationalException(pureModel, message.toString(), mappingValidatorContext.getProtocolMappingsByNameWithID(currentMapping._name()).sourceInformation, true);

                    PackageableElement.writeUserPathForPackageableElement(message, rootMapping);
                }
            }
            for (MappingInclude include : currentMapping._includes())
            {
                collectMappingEntitiesById(pureModel, map, include._included(), rootMapping, entityType, entityProperty, idProperty, visited, mappingValidatorContext);
            }
        }
    }
}