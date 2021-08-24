package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionPlugin;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;

import java.util.ServiceLoader;

public class RelationalConnectionPluginLoader
{
    public ImmutableMap<DatabaseType, RelationalConnectionPlugin> loadRelationalPlugins()
    {
        ServiceLoader<RelationalConnectionPlugin> loader = ServiceLoader.load(RelationalConnectionPlugin.class);
        MutableMap<DatabaseType, RelationalConnectionPlugin> plugins = Iterate.addToMap(loader, RelationalConnectionPlugin::getDatabaseType, Maps.mutable.<DatabaseType, RelationalConnectionPlugin>empty());
        return plugins.toImmutable();
    }

    public RelationalConnectionPlugin getPlugin(DatabaseType databaseType)
    {
        ImmutableMap<DatabaseType, RelationalConnectionPlugin> relationalConnectionPlugins = this.loadRelationalPlugins();
        return relationalConnectionPlugins.get(databaseType);
    }
}
