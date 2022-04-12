package org.finos.legend.engine.testable.service.helper;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.*;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;

public class PrimitiveValueSpecificationToObjectVisitor implements ValueSpecificationVisitor<Object>
{
    @Override
    public Object visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification valueSpecification) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(PackageableElementPtr packageableElementPtr) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Whatever whatever) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CString cString) {
        return cString.values.get(0);
    }

    @Override
    public Object visit(CDateTime cDateTime) {
        return cDateTime.values.get(0);
    }

    @Override
    public Object visit(CLatestDate cLatestDate) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CStrictDate cStrictDate) {
        return cStrictDate.values.get(0);
    }

    @Override
    public Object visit(CStrictTime cStrictTime) {
        return cStrictTime.values.get(0);
    }

    @Override
    public Object visit(AggregateValue aggregateValue) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Class aClass) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CBoolean cBoolean) {
        return cBoolean.values.get(0);
    }

    @Override
    public Object visit(UnknownAppliedFunction unknownAppliedFunction) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Enum anEnum) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(EnumValue enumValue) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(RuntimeInstance runtimeInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Path path) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CInteger cInteger) {
        return cInteger.values.get(0);
    }

    @Override
    public Object visit(CDecimal cDecimal) {
        return cDecimal.values.get(0);
    }

    @Override
    public Object visit(Lambda lambda) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(ExecutionContextInstance executionContextInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Pair pair) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(PureList pureList) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Variable variable) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CFloat cFloat) {
        return cFloat.values.get(0);
    }

    @Override
    public Object visit(MappingInstance mappingInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(HackedClass hackedClass) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Collection collection) {
        return ListIterate.collect(collection.values, v -> v.accept(this));
    }

    @Override
    public Object visit(AppliedFunction appliedFunction) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(AppliedQualifiedProperty appliedQualifiedProperty) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(PropertyGraphFetchTree propertyGraphFetchTree) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(RootGraphFetchTree rootGraphFetchTree) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(SerializationConfig serializationConfig) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(AppliedProperty appliedProperty) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(TdsOlapAggregation tdsOlapAggregation) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(TDSAggregateValue tdsAggregateValue) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(TDSSortInformation tdsSortInformation) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(TDSColumnInformation tdsColumnInformation) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(TdsOlapRank tdsOlapRank) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(HackedUnit hackedUnit) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(UnitInstance unitInstance) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(UnitType unitType) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(KeyExpression keyExpression) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(PrimitiveType primitiveType) {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }
}
