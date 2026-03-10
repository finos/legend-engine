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
//

package org.finos.legend.engine.language.deephaven.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;
import org.finos.legend.engine.protocol.pure.dsl.store.valuespecification.constant.classInstance.RelationStoreAccessor;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_FloatType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_IntType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_TimestampType;
import org.finos.legend.pure.generated.core_deephaven_pure_contract_storeContract;
import org.finos.legend.pure.m2.dsl.store.M2StorePaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_DeephavenStore;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Table;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Column;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_store_RelationStoreAccessor_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_Type;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_CharType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_CustomType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_DecimalType;
import org.finos.legend.pure.m3.navigation.M3Paths;

import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;


public class DeephavenCompilerExtension implements CompilerExtension
{

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
    }

    @Override
    public CompilerExtension build()
    {
        return this;
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(Processor.newProcessor(DeephavenStore.class, HelperDeephavenStoreBuilder::buildStoreFirstPass));
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_core_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.fixedSize.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof DeephavenConnection)
                    {
                        return HelperDeephavenStoreBuilder.buildConnection((DeephavenConnection) connectionValue, context);
                    }
                    return null;
                }
        );
    }

    public List<Function4<RelationStoreAccessor, Store, CompileContext, ProcessingContext, ValueSpecification>>  getExtraRelationStoreAccessorProcessors()
    {
        return Lists.mutable.with((accessor, store, context, processingContext) ->
        {
            if (store instanceof Root_meta_external_store_deephaven_metamodel_store_DeephavenStore)
            {
                if (accessor.path.size() != 2)
                {
                    throw new EngineException("Deephaven accessor requires 2 params (Store and Table); the number of params " + accessor.path.size() + " does not match the number expected" + store._name(), accessor.sourceInformation, EngineErrorType.COMPILATION);
                }

                Root_meta_external_store_deephaven_metamodel_store_DeephavenStore ds = (Root_meta_external_store_deephaven_metamodel_store_DeephavenStore) store;

                String tableName = accessor.path.get(1);

                MutableList<? extends Root_meta_external_store_deephaven_metamodel_store_Table> tables = ds._tables().toList();

                Root_meta_external_store_deephaven_metamodel_store_Table table = tables.stream().filter(t -> t._name().equals(tableName)).findFirst().orElse(null);;
                if (table == null)
                {
                    throw new EngineException("The table " + accessor.path.get(1) + " can't be found in the store " + store._name(), accessor.sourceInformation, EngineErrorType.COMPILATION);
                }

                ProcessorSupport processorSupport = context.pureModel.getExecutionSupport().getProcessorSupport();

                org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = null;

                RelationType<?> type = _RelationType.build(table._columns().collect(c ->
                {
                    Root_meta_external_store_deephaven_metamodel_store_Column col = (Root_meta_external_store_deephaven_metamodel_store_Column) c;
                    String name = col._name();
                    if (name.startsWith("\""))
                    {
                        name = name.substring(1, name.length() - 1);
                    }
                    // TODO - right now no columns are nullable - enable nullable in future.
                    return (CoreInstance) _Column.getColumnInstance(name, false, convertTypes(col._type(), processorSupport, context), (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(1, 1, processorSupport), sourceInformation, processorSupport);
                }).toList(), sourceInformation, processorSupport);

                GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                        ._rawType(context.pureModel.getType(M2StorePaths.RelationStoreAccessor))
                        ._typeArguments(FastList.newListWith(
                                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(type)
                                )
                        );

                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                        ._genericType(genericType)
                        ._multiplicity(context.pureModel.getMultiplicity("one"))
                        ._values(
                                FastList.newListWith(
                                        new Root_meta_pure_store_RelationStoreAccessor_Impl<>("", new org.finos.legend.pure.m4.coreinstance.SourceInformation("X", 0, 0, 0, 0), null)
                                                ._store(ds)
                                                ._sourceElement(table)
                                                ._classifierGenericType(genericType)
                                )
                        );


            }
            return null;
        });
    }

    private GenericType convertTypes(Root_meta_external_store_deephaven_metamodel_type_Type c, ProcessorSupport processorSupport, CompileContext compileContext)
    {
        if (c instanceof Root_meta_external_store_deephaven_metamodel_type_CharType)
        {
            return convertTypes("meta::pure::precisePrimitives::Varchar",
                    Lists.mutable.with(new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, compileContext.pureModel.getClass(M3Paths.InstanceValue))
                            ._genericType(compileContext.pureModel.getGenericType("Integer"))
                            ._multiplicity(compileContext.pureModel.getMultiplicity("One"))
                            ._values(Lists.immutable.with(1L))),
                    compileContext
            );
        }
        else if (c instanceof Root_meta_external_store_deephaven_metamodel_type_IntType)
        {
            return convertTypes("meta::pure::precisePrimitives::Int", compileContext);
        }
        else if (c instanceof Root_meta_external_store_deephaven_metamodel_type_FloatType)
        {
            return convertTypes("meta::pure::precisePrimitives::Float4", compileContext);
        }
        else if (c instanceof Root_meta_external_store_deephaven_metamodel_type_TimestampType)
        {
            return convertTypes("meta::pure::precisePrimitives::Timestamp", compileContext);
        }
        else if (c instanceof Root_meta_external_store_deephaven_metamodel_type_DecimalType)
        {
            Root_meta_external_store_deephaven_metamodel_type_DecimalType decimalType = (Root_meta_external_store_deephaven_metamodel_type_DecimalType) c;
            return convertTypes("meta::pure::precisePrimitives::Numeric",
                    Lists.mutable.with(
                            new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, compileContext.pureModel.getClass(M3Paths.InstanceValue))
                                    ._genericType(compileContext.pureModel.getGenericType("Integer"))
                                    ._multiplicity(compileContext.pureModel.getMultiplicity("One"))
                                    ._values(Lists.immutable.with(decimalType._precision())),
                            new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, compileContext.pureModel.getClass(M3Paths.InstanceValue))
                                    ._genericType(compileContext.pureModel.getGenericType("Integer"))
                                    ._multiplicity(compileContext.pureModel.getMultiplicity("One"))
                                    ._values(Lists.immutable.with(decimalType._scale()))),
                    compileContext);
        }
        else if (c instanceof Root_meta_external_store_deephaven_metamodel_type_CustomType)
        {
            throw new EngineException("CustomType is not yet supported in Deephaven store compilation", null, EngineErrorType.COMPILATION);
        }

        return (GenericType) processorSupport.type_wrapGenericType(
                core_deephaven_pure_contract_storeContract.Root_meta_external_store_deephaven_pureToDeephaven_mapDeephavenTypeToPureType_Type_1__DataType_1_(
                        c,
                        compileContext.pureModel.getExecutionSupport()
                )
        );
    }

    private GenericType convertTypes(String type, CompileContext compileContext)
    {
        return convertTypes(type, Lists.mutable.empty(), compileContext);
    }

    private GenericType convertTypes(String type, RichIterable<? extends ValueSpecification> typeVariableValues, CompileContext compileContext)
    {
        return compileContext.newGenericType((Type) compileContext.resolvePackageableElement(type, null))
                ._typeVariableValues(typeVariableValues);
    }

    @Override
    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return Collections.singletonList(registerElementForPathToElement ->
        {
            ImmutableList<String> versions = PureClientVersions.versionsSinceExclusive("v1_32_0");
            versions.forEach(v -> registerElementForPathToElement.value(
                             "meta::protocols::pure::" + v + "::extension::store::deephaven",
                             Collections.singletonList("deephavenStoreExtension_String_1__SerializerExtension_1_")
                    )
            );
        });
    }
}
