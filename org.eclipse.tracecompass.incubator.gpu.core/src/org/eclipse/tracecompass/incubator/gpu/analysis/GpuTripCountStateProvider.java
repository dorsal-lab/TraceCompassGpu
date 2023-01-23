package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipAnalyzerEvent;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class GpuTripCountStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 0;
    private static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuWaveStateProvider"; //$NON-NLS-1$

    /**
     * @brief Default output file
     */
    public static final String DEFAULT_FILE = "/tmp/trace_trip_count.csv"; //$NON-NLS-1$

    private static int DEFAULT_QUARK = -1;
    private int activeWavesQuark = DEFAULT_QUARK;

    public class StoredQuarks {
        private int waveQuark;
        private int valueQuark;

        public StoredQuarks(int id) {
            ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());
            waveQuark = ss.getQuarkRelativeAndAdd(activeWavesQuark, String.valueOf(id));

            // It is much easier to store directly the value than creating
            // sub-attributes. What's more, it reduces stress on the (probably)
            // overloaded state system by using less quarks
            valueQuark = ss.getQuarkRelativeAndAdd(waveQuark, "trip count"); //$NON-NLS-1$
        }

        public int getWaveQuark() {
            return waveQuark;
        }

        public int getValueQuark() {
            return valueQuark;
        }
    }

    private StoredQuarks[] storedQuarks;

    boolean newEvents = false;

    private final long sequence[] = { 4, 6, 8, 9 };
    private int seqIndex = 0;
    private int tripCount = 0;
    private ITmfEvent eventStart;
    FileWriter writer;

    /**
     * @param trace
     *            Trace
     * @param id
     *            Identifier
     */
    public GpuTripCountStateProvider(@NonNull ITmfTrace trace, @NonNull String id) {
        super(trace, id);
        try {
            writer = new FileWriter(DEFAULT_FILE);
            writer.write("gpu_stamp,block,wave,trip_count,end\n"); //$NON-NLS-1$
        } catch (IOException e) {
        }
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
                storedQuarks = new StoredQuarks[(int) header.parallelism()];

                newEvents = false;
            }

            ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

            ITmfEventField content = event.getContent();

            long bb = (Long) content.getField("bb").getValue(); //$NON-NLS-1$
            ITmfEventField pos = content.getField("producer_id"); //$NON-NLS-1$

            int id = (int) HipTrace.producerIdFromGeometry(header, pos);
            if (storedQuarks[id] == null) {
                storedQuarks[id] = new StoredQuarks(id);
            }

            ss.modifyAttribute(stamp.getValue(), bb, storedQuarks[id].getWaveQuark());
            // modifyAttribute(stamp.getValue(),
            // content,storedQuarks[id].getValueQuark());

            if (bb == sequence[seqIndex]) {
                if(bb == sequence[0] && tripCount == 0) {
                    eventStart = event;
                }
                ++seqIndex;
            } else if (seqIndex != 0) {
                if (tripCount != 0) {
                    ITmfEventField contentStart = eventStart.getContent().getField("producer_id");
                    ss.modifyAttribute(eventStart.getTimestamp().getValue(), tripCount, storedQuarks[id].getValueQuark());
                    ss.modifyAttribute(event.getTimestamp().getValue(), null, storedQuarks[id].getValueQuark());

                    try {
                        writer.write(new StringBuilder().append(eventStart.getContent().getField("stamp").getValue()).append(',')
                            .append(contentStart.getField("block").getValue()).append(',')
                            .append(contentStart.getField("wave").getValue()).append(',')
                            .append(tripCount).append(',')
                            .append(content.getField("stamp").getValue()).append('\n')
                            .toString());
                        writer.flush();
                    } catch (IOException e) {

                    }

                    eventStart = null;
                }
                seqIndex = 0;
                tripCount = 0;
            }

            if (seqIndex == sequence.length) {
                ++tripCount;
                seqIndex = 0;
            }

        } else if (event.getType().getName().equals("hiptrace_counters")) { //$NON-NLS-1$
            // The next events are from a different kernel launch, need to
            // update quarks
            newEvents = true;
        }
    }
}
