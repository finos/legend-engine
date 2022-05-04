package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;

/**
 * Providers should implement this interface to handle the interpretation of their flat data.
 * A driver is used to handle the processing of data as a section of a <tt>FlatData</tt>.
 */
public interface FlatDataReadDriver<T> extends FlatDataDriver
{
    /**
     * Called before this driver processes any data.  Any prior sections defined in the <tt>FlatData</tt>
     * will have completed their work before this method is called.
     */
    void start();

    /**
     * Called after this driver has processed all its data (i.e. has returned <tt>true</tt> from
     * {@link #isFinished()}.  No subsequent sections defined in the <tt>FlatData</tt> will start their work
     * until this method has completed.
     */
    void stop();

    /**
     * Called to obtain the next set of data from the driver. Each instance of <tt>IChecked</tt>
     * returned should:
     *
     * * Contain defects describing any issues with the data
     * * A record structure indicating the sources of the data
     * * The parsed data if there are no critical defects otherwise <tt>null</tt>.
     *
     * @return the next set of data
     */
    Collection<IChecked<T>> readCheckedObjects();

    /**
     * Called to check that the driver has completed its work.
     *
     * @return <tt>true</tt> if the driver has no more data to return
     */
    boolean isFinished();
}
