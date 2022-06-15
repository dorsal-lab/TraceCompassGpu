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

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineAnalysis extends TmfAbstractAnalysisModule {

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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void canceling() {
        // TODO Auto-generated method stub

    }

}
