/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.TmfCommonXAxisModel;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuWaveLifetimeDataProvider extends AbstractTreeDataProvider<@NonNull GpuWaveStateAnalysis, @NonNull TmfTreeDataModel> implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    /**
     * @brief Data provider ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuWaveLifetimeDataProvider"; //$NON-NLS-1$

    /**
     * @brief Sampling period to avoid computing each interval
     */
    private static final long SAMPLING_NS = 2_500L;

    /**
     * @param trace
     *            Trace
     * @param analysisModule
     *            Wave state analysis
     */
    public GpuWaveLifetimeDataProvider(ITmfTrace trace, GpuWaveStateAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    /**
     * @param trace
     *            Trace to be analyzed
     * @return Data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(@NonNull ITmfTrace trace) {
        GpuWaveStateAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuWaveStateAnalysis.class, GpuWaveStateAnalysis.ID);
        return module != null ? new GpuWaveLifetimeDataProvider(trace, module) : null;
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull ITmfXyModel> fetchXY(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        IProgressMonitor mon = monitor != null ? monitor : new NullProgressMonitor();

        GpuWaveStateAnalysis module = getAnalysisModule();

        ITmfStateSystem ss = module.getStateSystem();

        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        int wavesQuark;
        try {
            wavesQuark = ss.getQuarkAbsolute("waves"); //$NON-NLS-1$
        } catch (AttributeNotFoundException e1) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        // ----- Sampling time ----- //

        long begin = ss.getStartTime();
        long end = ss.getCurrentEndTime();

        int size = (int) ((end - begin) / SAMPLING_NS);
        long[] times = new long[size];

        long t = begin;
        for (int i = 0; i < size; ++i) {
            times[i] = t;
            t += SAMPLING_NS;
        }

        // ----- Create models ----- //

        mon.beginTask("Process states..", size); //$NON-NLS-1$

        // Cumulative waves

        double[] wavesFinished = new double[size];
        double[] wavesActive = new double[size];
        double[] totalFlops = new double[size];

        @SuppressWarnings("nls")
        @NonNull
        List<@NonNull Integer> contentQuarks = ss.getQuarks("waves", "*", "content");

        try {
            List<Long> timesList = Arrays.stream(times).boxed().collect(Collectors.toList());
            Iterable<@NonNull ITmfStateInterval> intervals = ss.query2D(contentQuarks, timesList);

            for (ITmfStateInterval interval : intervals) {
                ITmfEventField event = (ITmfEventField) interval.getValue();

                if (event == null) {
                    continue;
                }
                Long bb = (Long) event.getField("bb").getValue();

                long currentTime = interval.getStartTime();
                int i = (int) ((currentTime - begin) / SAMPLING_NS);

                if (bb == null) {
                    // Nothing to do, uninitialized
                } else if (bb != -1) {
                    // Valid basic block, currently active
                    ++wavesActive[i];

                    // Compute active flops
                    if (bb == 0) { // TEMPORARY, need to query basic block
                                   // db (hip-analyzer report)

                        // stampMemory interval represents how long we
                        // stayed in the basic block
                        double diff = ((double) interval.getEndTime() - interval.getStartTime()) / 1.e6;
                        totalFlops[i] += 1 / diff; // #Flop / t (second)
                    }

                } else {
                    // bb == -1, by convention the wave is finished
                    ++wavesFinished[i];
                }
            }

        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null,
                    ITmfResponse.Status.FAILED,
                    CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        // Prefix sum, could be wrong as intervals may be returned multiple
        // times. The query2D doc is unclear on this
        double cumulatedFinished = 0.;
        for (int i = 0; i < size; ++i) {
            cumulatedFinished += wavesFinished[i];
            wavesFinished[i] = cumulatedFinished;
        }

        YModel cumulativeWaves = new YModel(0, "Cumulative waves", wavesFinished); //$NON-NLS-1$
        YModel activeWaves = new YModel(1, "Active waves", wavesActive); //$NON-NLS-1$
        YModel flopsTotal = new YModel(2, "Kernel throughput (FLOP/s)", totalFlops); //$NON-NLS-1$

        List<@NonNull IYModel> models = List.of(cumulativeWaves, activeWaves, flopsTotal);

        mon.done();
        return new TmfModelResponse<>(new TmfCommonXAxisModel("GPU Wave Lifetime Analysis", times, models), Status.COMPLETED, CommonStatusMessage.COMPLETED); //$NON-NLS-1$
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected @NonNull TmfTreeModel<@NonNull TmfTreeDataModel> getTree(@NonNull ITmfStateSystem ss, @NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        return new TmfTreeModel<>(Collections.emptyList(), Collections.emptyList());
    }

}
