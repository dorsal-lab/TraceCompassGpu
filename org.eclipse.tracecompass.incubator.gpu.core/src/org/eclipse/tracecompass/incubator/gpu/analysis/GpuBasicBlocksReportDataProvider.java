package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GcnAsmParser;
import org.eclipse.tracecompass.internal.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
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

public class GpuBasicBlocksReportDataProvider extends AbstractTreeDataProvider<@NonNull GpuWaveStateAnalysis, @NonNull TmfTreeDataModel> implements ITmfTreeXYDataProvider<@NonNull TmfTreeDataModel> {

    /**
     * @brief Data provider ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuBasicBlocksDataProvider"; //$NON-NLS-1$

    /**
     * @param trace
     *            Trace
     * @param analysisModule
     *            Wave state analysis
     */
    public GpuBasicBlocksReportDataProvider(ITmfTrace trace, GpuWaveStateAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    /**
     * @param trace
     *            Trace to be analyzed
     * @return Data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(@NonNull ITmfTrace trace) {
        GpuWaveStateAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuWaveStateAnalysis.class, GpuWaveStateAnalysis.ID);
        return module != null ? new GpuBasicBlocksReportDataProvider(trace, module) : null;
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

        @SuppressWarnings("nls")
        List<@NonNull Integer> eventsQuarks = ss.getQuarks("waves", "*", "content");

        long[] xAxis = new long[65]; // 0 to 64 active waves
        for (int i = 0; i < xAxis.length; ++i) {
            xAxis[i] = i;
        }

        TreeMap<Long, double[]> histograms = new TreeMap<>();

        try {
            Iterable<@NonNull ITmfStateInterval> intervals = ss.query2D(eventsQuarks, ss.getStartTime(), ss.getCurrentEndTime());

            for (ITmfStateInterval interval : intervals) {
                ITmfEventField event = (ITmfEventField) interval.getValue();

                if (event == null) {
                    continue;
                }

                Long bb = (Long) event.getField("bb").getValue(); //$NON-NLS-1$
                if (bb == -1) {
                    continue;
                }

                GcnAsmParser.ExecRegister exec = (GcnAsmParser.ExecRegister) event.getField("exec").getValue(); //$NON-NLS-1$
                Integer active = exec.activeThreads();

                double[] histogram = histograms.get(bb);
                if (histogram == null) {
                    histogram = new double[65];
                    histogram[active] = 1.;
                    histograms.put(bb, histogram);
                } else {
                    histograms.compute(bb, (key, value) -> {
                        value[active] += 1.;
                        return value;
                    });
                }
            }

        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null,
                    ITmfResponse.Status.FAILED,
                    CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        // BBlocks are ordered

        List<@NonNull IYModel> models = new ArrayList<>();
        for (Map.Entry<Long, double[]> entry : histograms.entrySet()) {
            models.add(new YModel(entry.getKey(), "BB" + entry.getKey().toString(), entry.getValue())); //$NON-NLS-1$
        }

        mon.done();

        return new TmfModelResponse<>(new TmfCommonXAxisModel("GPU Wave Lifetime Analysis", xAxis, models), Status.COMPLETED, CommonStatusMessage.COMPLETED); //$NON-NLS-1$
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
