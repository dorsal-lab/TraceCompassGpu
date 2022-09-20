package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipAnalyzerEvent;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @brief Wavefront lifetime analysis state provider
 *
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 */
public class GpuWaveStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 0;
    private static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuWaveStateProvider"; //$NON-NLS-1$

    private static int DEFAULT_QUARK = -1;
    private int activeWavesQuark = DEFAULT_QUARK;

    private int[] wavesQuarks;
    boolean newEvents = false;

    /**
     * @param trace
     *            Trace
     * @param id
     *            Identifier
     */
    public GpuWaveStateProvider(@NonNull ITmfTrace trace, @NonNull String id) {
        super(trace, id);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new GpuWaveStateProvider(getTrace(), ID);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfTrace trace = event.getTrace();

        if (activeWavesQuark == DEFAULT_QUARK) {
            activeWavesQuark = Objects.requireNonNull(getStateSystemBuilder()).getQuarkAbsoluteAndAdd("waves"); //$NON-NLS-1$
        }

        if (trace instanceof HipTrace) {
            handleHipTraceEvent(event);
        }

    }

    /**
     * @param event
     *            Event to process
     */
    protected void handleHipTraceEvent(@NonNull ITmfEvent event) {
        ITmfTimestamp stamp = event.getTimestamp();

        if (event.getType().getName().equals(HipAnalyzerEvent.HipWaveState.name())) {
            HipTrace.EventsHeader header = (HipTrace.EventsHeader) event.getContent().getField("header").getValue(); //$NON-NLS-1$
            if (newEvents) {
                wavesQuarks = new int[(int) header.parallelism()];

                newEvents = false;
            }

            ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

            long bb = (Long) event.getContent().getField("bb").getValue(); //$NON-NLS-1$
            ITmfEventField pos = event.getContent().getField("producer_id"); //$NON-NLS-1$

            int id = (int) HipTrace.producerIdFromGeometry(header, pos);
            if (wavesQuarks[id] == 0) {
                wavesQuarks[id] = ss.getQuarkRelativeAndAdd(activeWavesQuark, String.valueOf(id));
            }

            ss.modifyAttribute(stamp.getValue(), bb, wavesQuarks[id]);

        } else if (event.getType().getName().equals("hiptrace_counters")) { //$NON-NLS-1$
            // The next events are from a different kernel launch, need to
            // update quarks
            newEvents = true;
        }
    }

}
