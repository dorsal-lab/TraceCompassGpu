/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuInfo;
import org.eclipse.tracecompass.internal.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
@SuppressWarnings("restriction")
@NonNullByDefault
public class GpuRooflineAnalysisDataProvider extends AbstractTreeDataProvider<GpuRooflineAnalysis, TmfTreeDataModel> implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    /**
     * @brief Data provider ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysisDataProvider"; //$NON-NLS-1$

    /**
     * @param trace
     *            Trace
     * @param analysisModule
     *            Analysis module
     */
    public GpuRooflineAnalysisDataProvider(ITmfTrace trace, GpuRooflineAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    /**
     * @param trace
     *            Trace to be analyzed
     * @return Data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
        GpuRooflineAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuRooflineAnalysis.class, GpuRooflineAnalysis.ID);
        return module != null ? new GpuRooflineAnalysisDataProvider(trace, module) : null;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull ITmfXyModel> fetchXY(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        /*
         * ITmfStateSystem ss = getAnalysisModule().getStateSystem(); if(ss ==
         * null) { return new TmfModelResponse<>(null,
         * ITmfResponse.Status.FAILED,
         * CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED); }
         *
         *
         * List<Long> times = getExecutionTimes(ss);
         *
         * // Allocate a "classic" array because the TmfCommonXAxisModel does
         * not support Lists long[] timesArray = new long[times.size()]; for(int
         * i = 0; i < times.size(); ++i) { timesArray[i] = times.get(i); }
         *
         *
         * List<IYModel> models = new ArrayList<>();
         */
        GpuRooflineAnalysis module = getAnalysisModule();
        GpuInfo gpuInfo = module.getGpuInfo();


        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null,
                    ITmfResponse.Status.FAILED,
                    CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        int flopsQuark, loadsQuark, storesQuark;

        try {
            flopsQuark = ss.getQuarkAbsolute(GpuRooflineStateProvider.FLOPS_ATTRIBUTE_NAME);
            loadsQuark = ss.getQuarkAbsolute(GpuRooflineStateProvider.LOADS_ATTRIBUTE_NAME);
            storesQuark = ss.getQuarkAbsolute(GpuRooflineStateProvider.STORES_ATTRIBUTE_NAME);

        } catch (AttributeNotFoundException e1) {
            return new TmfModelResponse<>(null,
                    ITmfResponse.Status.FAILED,
                    CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        List<RooflineXYModel.Point> rooflinePoints = new ArrayList<>();

        try {
            for (ITmfStateInterval interval : ss.query2D(List.of(flopsQuark), ss.getStartTime(), ss.getCurrentEndTime())) {
                long flops = interval.getValueLong();
                if (flops != 0L) {
                    long duration = interval.getEndTime() - interval.getStartTime();

                    double attainedPerf = flops / toSeconds(duration);

                    // Get bytes
                    long loadedBytes = ss.querySingleState(interval.getStartTime(), loadsQuark).getValueLong();
                    long storedBytes = ss.querySingleState(interval.getStartTime(), storesQuark).getValueLong();

                    long totalAccessed = loadedBytes + storedBytes;

                    double operationalIntensity = (double)flops / (double)totalAccessed;

                    rooflinePoints.add(new RooflineXYModel.Point(operationalIntensity, attainedPerf));
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null,
                    ITmfResponse.Status.FAILED,
                    CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        return new TmfModelResponse<>(new RooflineXYModel("Roofline", gpuInfo, rooflinePoints), Status.COMPLETED, CommonStatusMessage.COMPLETED); //$NON-NLS-1$
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        /*
         * GpuRooflineAnalysis module = getAnalysisModule(); GpuInfo gpuInfo =
         * module.getGpuInfo();
         *
         * final long PARENT_ID = 0; // Give the same parent id to each (?)
         *
         * List<TmfTreeDataModel> entries = Collections.emptyList();
         *
         * entries.add(new TmfTreeDataModel(PARENT_ID, -1, "root"));
         * //$NON-NLS-1$ entries.add(new RooflineXYModel(1, PARENT_ID,
         * "Roofline", gpuInfo)); //$NON-NLS-1$
         */
        // entries.add(new TimeGraphEntryModel()); // TODO : add XY points
        // corresponding to roofline entries

        return new TmfTreeModel<>(Collections.emptyList(), Collections.emptyList());
    }



    private static double toSeconds(long nanosecDuration) {
        return (nanosecDuration) / 1e9d;
    }

}
