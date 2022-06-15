/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.*;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineAnalysis extends TmfAbstractAnalysisModule {

    public static final String PARAM_HIP_ANALYZER = "hip_analyzer_path"; //$NON-NLS-1$
    public static final String PARAM_GPU_INFO = "gpu_info_path"; //$NON-NLS-1$

    @SuppressWarnings("null")
    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        Set<@NonNull String> requiredEvents = Set.of(
                "hip_function_name", //$NON-NLS-1$
                "hip_api", //$NON-NLS-1$
                "hip_activity" //$NON-NLS-1$
        );

        TmfAbstractAnalysisRequirement eventsReq = new TmfAnalysisEventRequirement(requiredEvents, PriorityLevel.MANDATORY);

        return Set.of(eventsReq);
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        String hipAnalyzerPath = (String) getParameter(PARAM_HIP_ANALYZER);
        String gpuInfoPath = (String) getParameter(PARAM_GPU_INFO);

        return false;
    }

    @Override
    protected void canceling() {
        return;
    }

    @Override
    @NonNull
    public String getHelpText(@NonNull ITmfTrace trace) {
        return "Compute Roofline model for this GPU experiment"; //$NON-NLS-1$
    }

}
