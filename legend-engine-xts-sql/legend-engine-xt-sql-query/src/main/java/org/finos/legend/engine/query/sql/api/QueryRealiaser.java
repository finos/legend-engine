// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.protocol.sql.metamodel.AliasedRelation;
import org.finos.legend.engine.protocol.sql.metamodel.AllColumns;
import org.finos.legend.engine.protocol.sql.metamodel.Join;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.QualifiedNameReference;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.Relation;
import org.finos.legend.engine.protocol.sql.metamodel.Table;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.engine.protocol.sql.metamodel.TableSubquery;
import org.finos.legend.engine.protocol.sql.metamodel.Union;

import java.util.Map;

/**
 * this re-aliaser does 2 things
 * 1. adds an alias to all non aliased tables/tableFuncs.
 * 2. ensures all aliases are unique throughout the query.
 * we do not re-alias top level aliases as they must be unique anyway and in edge cases the alias can end up
 * forming part of column names, so for clarity best to expose this to the user as such
 */

/**
 * TODO: move into pure code
 */
public class QueryRealiaser extends BaseNodeModifierVisitor
{
    private boolean root = true;
    private AliasedRelation currentAlias;
    private Map<String, String> realiases = UnifiedMap.newMap();
    private int counter = 1;

    private QueryRealiaser()
    {

    }

    public static Query realias(Query query)
    {
        return (Query) query.accept(new QueryRealiaser());
    }

    @Override
    public Node visit(AliasedRelation val)
    {
        //we store the current state and the inner state becomes empty
        currentAlias = val;
        Map<String, String> scopeRealiases = UnifiedMap.newMap(realiases);
        boolean inRoot = root;

        //reset the state
        root = false;
        realiases = UnifiedMap.newMap();
        AliasedRelation result = (AliasedRelation) super.visit(val);

        //we always rename non root aliases.
        String alias = inRoot ? val.alias : val.alias + '_' + counter++;

        //we add/set the new alias/realias
        scopeRealiases.put(val.alias, alias);
        result.alias = alias;

        //we restore the outer state
        root = inRoot;
        realiases = scopeRealiases;
        currentAlias = val;
        return result;
    }

    @Override
    public Node visit(QualifiedNameReference val)
    {
        QualifiedNameReference result = (QualifiedNameReference) super.visit(val);

        if (result.name.parts.size() == 2)
        {
            result.name.parts = FastList.newListWith(realiases.get(result.name.parts.get(0)), result.name.parts.get(1));
        }

        return result;
    }

    @Override
    public Node visit(AllColumns val)
    {
        val.prefix = val.prefix != null ? realiases.get(val.prefix) : null;

        return val;
    }

    @Override
    public Node visit(Join val)
    {
        currentAlias = null;
        return super.visit(val);
    }

    @Override
    public Node visit(QuerySpecification val)
    {
        currentAlias = null;
        return super.visit(val);
    }

    @Override
    public Node visit(Table val)
    {
        return aliasIfRequired(val, StringUtils.join(val.name.parts, "."));
    }

    @Override
    public Node visit(TableFunction val)
    {
        return aliasIfRequired(val, StringUtils.join(val.functionCall.name.parts, "."));
    }

    @Override
    public Node visit(TableSubquery val)
    {
        //store the current state
        boolean inRoot = root;
        Map<String, String> nodes = UnifiedMap.newMap(realiases);

        //reset the state for recurse
        root = false;
        currentAlias = null;
        realiases = UnifiedMap.newMap();
        Node result = super.visit(val);

        //restore the state
        realiases = nodes;
        root = inRoot;

        return result;
    }

    @Override
    public Node visit(Union val)
    {
        currentAlias = null;
        return super.visit(val);
    }


    private Node aliasIfRequired(Relation relation, String name)
    {
        if (currentAlias == null)
        {
            AliasedRelation a = new AliasedRelation();
            a.alias = "t" + counter++;
            a.relation = relation;

            realiases.put(name, a.alias);

            return a;
        }

        return relation;
    }
}
