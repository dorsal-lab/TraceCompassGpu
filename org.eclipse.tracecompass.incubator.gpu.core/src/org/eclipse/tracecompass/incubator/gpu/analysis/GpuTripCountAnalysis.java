package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;


public class GpuTripCountAnalysis extends TmfStateSystemAnalysisModule {

    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.core.GpuTripCountAnalysis"; //$NON-NLS-1$
    public static final String TRIPCOUNT_VIEW_ID = "org.eclipse.tracecompass.incubator.gpu.ui.wavestate"; //$NON-NLS-1$

    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Set.of();
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public String getHelpText(@NonNull ITmfTrace trace) {
        return "Compute loop trip count"; //$NON-NLS-1$
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new GpuTripCountStateProvider(Objects.requireNonNull(getTrace()), getId());
    }

    @Override
    protected StateSystemBackendType getBackendType() {
        // Using the in-memory state system to ensure no caching (for dev.)
        return StateSystemBackendType.INMEM;
    }

}
