package org.finos.legend.engine.query.graphQL.api.execute;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLCacheKey;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.Map;

public interface IGraphQLExecuteExtension
{
    public Map<String, ?> computeExtensionsField(String queryClassPath, String mappingPath, String runtimePath, Document document, OperationDefinition query, PureModel pureModel, GraphQLCacheKey graphQLCacheKey, MutableList<CommonProfile> profiles);
}
