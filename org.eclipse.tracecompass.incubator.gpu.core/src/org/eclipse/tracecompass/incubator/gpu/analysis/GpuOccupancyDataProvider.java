/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GcnAsmParser;
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
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuOccupancyDataProvider extends AbstractTreeDataProvider<@NonNull GpuWaveStateAnalysis, @NonNull TmfTreeDataModel> implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    /**
     * @brief Data provider ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuOccupancyDataProvider"; //$NON-NLS-1$

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
    public GpuOccupancyDataProvider(ITmfTrace trace, GpuWaveStateAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    /**
     * @param trace
     *            Trace to be analyzed
     * @return Data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(@NonNull ITmfTrace trace) {
        GpuWaveStateAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuWaveStateAnalysis.class, GpuWaveStateAnalysis.ID);
        return module != null ? new GpuOccupancyDataProvider(trace, module) : null;
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

        List<@NonNull Integer> waves = ss.getSubAttributes(wavesQuark, false);

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

        double[] averageOccupancy = new double[size];

        try {
            int i = 0;
            for (long time : times) {
                List<@NonNull ITmfStateInterval> values = ss.queryFullState(time);

                MI100Gpu gpuState = new MI100Gpu();

                for (int wave : waves) {
                    ITmfStateInterval interval = values.get(wave);
                    Long bb = (Long) interval.getValue();

                    ITmfStateInterval content = values.get(ss.getSubAttributes(wave, false).get(0));

                    if (bb != null && bb != -1) {
                        ITmfEventField event = (ITmfEventField) content.getValue();

                        if (event != null) {
                            gpuState.registerWave(GcnAsmParser.HardwareIdRegister.fromEventFields(event.getField("hw_id"))); //$NON-NLS-1$
                        }

                    }
                }

                // gpuState.dump();

                averageOccupancy[i] = gpuState.totalOccupancy();

                mon.worked(1);
                ++i;
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null,
                    ITmfResponse.Status.FAILED,
                    CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        YModel occupancyAverage = new YModel(2, "Average occupancy", averageOccupancy); //$NON-NLS-1$

        List<@NonNull IYModel> models = List.of(occupancyAverage);

        mon.done();
        return new TmfModelResponse<>(new TmfCommonXAxisModel("GPU Wave Lifetime Analysis", times, models), Status.COMPLETED, CommonStatusMessage.COMPLETED); //$NON-NLS-1$
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Override
    protected @NonNull TmfTreeModel<@NonNull TmfTreeDataModel> getTree(@NonNull ITmfStateSystem ss, @NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        return new TmfTreeModel<>(Collections.emptyList(), Collections.emptyList());
    }

}
