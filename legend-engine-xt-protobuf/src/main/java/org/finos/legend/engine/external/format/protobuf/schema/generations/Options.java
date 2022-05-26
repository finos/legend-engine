package org.finos.legend.engine.external.format.protobuf.schema.generations;

import java.util.Collections;
import java.util.List;

public class Options
{
    public String javaPackage;
    public String javaOuterClassname;
    public Boolean javaMultipleFiles;
    public OptimizeMode optimizeFor;
    public List<CustomOption> customOptions = Collections.EMPTY_LIST;
}
