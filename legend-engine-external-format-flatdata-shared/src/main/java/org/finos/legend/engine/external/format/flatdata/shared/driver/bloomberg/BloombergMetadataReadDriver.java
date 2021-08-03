package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;

public class BloombergMetadataReadDriver<T> extends AbstractBloombergReadDriver<T>
{
    public static final String ID = "BloombergMetadata";
    //private final CheckedParsedDataFactory factory;

    BloombergMetadataReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);
        //this.factory = factory();
    }

    @Override
    public void start()
    {
    }

    @Override
    public String getId()
    {
        return BloombergMetadataReadDriver.ID;
    }

    //@Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        return null; //retrieveMetadata(this.factory);
    }
}
