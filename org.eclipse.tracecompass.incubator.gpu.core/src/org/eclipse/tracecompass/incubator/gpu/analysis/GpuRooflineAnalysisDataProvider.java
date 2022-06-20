/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.TmfCommonXAxisModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
@SuppressWarnings("restriction")
@NonNullByDefault
public class GpuRooflineAnalysisDataProvider extends AbstractTreeDataProvider<GpuRooflineAnalysis, TmfTreeDataModel> implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    private static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysisDataProvider"; //$NON-NLS-1$

    /**
     * @param trace
     *            Trace
     * @param analysisModule
     *            Analysis module
     */
    public GpuRooflineAnalysisDataProvider(ITmfTrace trace, GpuRooflineAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull ITmfXyModel> fetchXY(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        if(ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        List<Long> times = getExecutionTimes(ss);

        // Allocate a "classic" array because the TmfCommonXAxisModel does not support Lists
        long[] timesArray = new long[times.size()];
        for(int i = 0; i < times.size(); ++i) {
            timesArray[i] = times.get(i);
        }


        List<IYModel> models = new ArrayList<>();

        return new TmfModelResponse<>(new TmfCommonXAxisModel("GPU Roofline Analysis", timesArray, models), Status.COMPLETED, CommonStatusMessage.COMPLETED); //$NON-NLS-1$
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        // TODO Auto-generated method stub
        return null;
    }

    private List<Long> getExecutionTimes(ITmfStateSystem key) {
        List<@NonNull Long> times = new ArrayList<>();

        // TODO : Get the different kernel execution times from the traces by navigating the state system

        return times;
    }

}
