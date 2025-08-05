package org.finos.legend.engine.code.core;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.pure.code.core.FeatureLegendPureCoreExtension;

public class SQLLegendPureCoreExtension implements FeatureLegendPureCoreExtension
{
    @Override
    public String functionFile()
    {
        return "core_external_query_sql/binding/fromPure/frameworkExtension.pure";
    }

    @Override
    public String functionSignature()
    {
        return "meta::external::query::sql::transformation::queryToPure::tests::sqlExpressionFeatureExtension__Extension_1_";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Query", "SQL");
    }
}
