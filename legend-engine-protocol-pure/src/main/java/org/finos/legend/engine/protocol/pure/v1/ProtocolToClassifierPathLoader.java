package org.finos.legend.engine.protocol.pure.v1;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;

import java.util.HashMap;
import java.util.Map;

public class ProtocolToClassifierPathLoader {

    public static Map<Class<? extends PackageableElement>, String> getProtocolClassToClassifierMap()
    {
        Map<Class<? extends PackageableElement>, String> protocolToClassifierMap = new HashMap<>();
        LazyIterate.flatCollect(PureProtocolExtensionLoader.extensions(), PureProtocolExtension::getExtraProtocolToClassifierPathCollectors)
                .forEach(listFunction0 -> {
                    listFunction0.value().stream().forEach(classStringPair -> {
                        if (protocolToClassifierMap.containsKey(classStringPair.getOne())) {
                            throw new RuntimeException("Conflicting classifier paths for class '" + classStringPair.getOne().getName() + "'");
                        }
                        protocolToClassifierMap.put(classStringPair.getOne(), classStringPair.getTwo());
                    });
                });
        return protocolToClassifierMap;
    }

}
