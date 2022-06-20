/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 0;
    private static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineStateProvider"; //$NON-NLS-1$

    /**
     * @param trace
     *            Trace
     * @param id
     *            Identifier
     */
    public GpuRooflineStateProvider(@NonNull ITmfTrace trace, @NonNull String id) {
        super(trace, id);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new GpuRooflineStateProvider(getTrace(), ID);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        // TODO Implement
    }

}
