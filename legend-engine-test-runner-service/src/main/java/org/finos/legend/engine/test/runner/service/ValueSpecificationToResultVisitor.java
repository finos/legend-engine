package org.finos.legend.engine.test.runner.service;

import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.*;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;

public class ValueSpecificationToResultVisitor implements ValueSpecificationVisitor<ConstantResult>
{
    @Override
    public ConstantResult visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification valueSpecification) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PackageableElementPtr packageableElementPtr) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Whatever whatever) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CString cString) {
        return new ConstantResult(cString.values.get(0));
    }

    @Override
    public ConstantResult visit(CDateTime cDateTime) {
        return new ConstantResult(cDateTime.values.get(0));
    }

    @Override
    public ConstantResult visit(CLatestDate cLatestDate) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CStrictDate cStrictDate) {
        return new ConstantResult(cStrictDate.values.get(0));
    }

    @Override
    public ConstantResult visit(CStrictTime cStrictTime) {
        return new ConstantResult(cStrictTime.values.get(0));
    }

    @Override
    public ConstantResult visit(AggregateValue aggregateValue) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Class aClass) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CBoolean cBoolean) {
        return new ConstantResult(cBoolean.values.get(0));
    }

    @Override
    public ConstantResult visit(UnknownAppliedFunction unknownAppliedFunction) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Enum anEnum) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(EnumValue enumValue) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(RuntimeInstance runtimeInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Path path) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CInteger cInteger) {
        return new ConstantResult(cInteger.values.get(0));
    }

    @Override
    public ConstantResult visit(CDecimal cDecimal) {
        return new ConstantResult(cDecimal.values.get(0));
    }

    @Override
    public ConstantResult visit(Lambda lambda) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(ExecutionContextInstance executionContextInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Pair pair) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PureList pureList) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Variable variable) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CFloat cFloat) {
        return new ConstantResult(cFloat.values.get(0));
    }

    @Override
    public ConstantResult visit(MappingInstance mappingInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(HackedClass hackedClass) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Collection collection) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(AppliedFunction appliedFunction) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(AppliedQualifiedProperty appliedQualifiedProperty) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PropertyGraphFetchTree propertyGraphFetchTree) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(RootGraphFetchTree rootGraphFetchTree) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(SerializationConfig serializationConfig) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(AppliedProperty appliedProperty) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TdsOlapAggregation tdsOlapAggregation) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TDSAggregateValue tdsAggregateValue) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TDSSortInformation tdsSortInformation) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TDSColumnInformation tdsColumnInformation) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TdsOlapRank tdsOlapRank) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(HackedUnit hackedUnit) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(UnitInstance unitInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(UnitType unitType) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(KeyExpression keyExpression) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PrimitiveType primitiveType) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }
}
