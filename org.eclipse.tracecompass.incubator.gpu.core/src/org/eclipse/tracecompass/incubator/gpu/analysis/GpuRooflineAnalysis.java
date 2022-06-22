/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuInfo;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipAnalyzerReport;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.*;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.analysis.TmfAnalysisViewOutput;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineAnalysis extends TmfStateSystemAnalysisModule {

    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.core.GpuRooflineAnalysis"; //$NON-NLS-1$
    public static final String ROOFLINE_VIEW_ID = "org.eclipse.tracecompass.incubator.gpu.ui.roofline"; //$NON-NLS-1$

    public static final String HIP_ANALYZER_SUPPLEMENTARY_FILE = "hip_analyzer.json"; //$NON-NLS-1$
    public static final String GPU_INFO_SUPPLEMENTARY_FILE = "gpu_info.json"; //$NON-NLS-1$

    private HipAnalyzerReport report;
    private GpuInfo gpuInfo;

    /**
     * @brief Constructor
     *//*
    public GpuRooflineAnalysis() {
        super();
        registerOutput(new TmfAnalysisViewOutput(ROOFLINE_VIEW_ID));
    }*/

    @SuppressWarnings("null")
    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        Set<@NonNull String> requiredEvents = Set.of(
                "hip_function_name", //$NON-NLS-1$
                "hip_api", //$NON-NLS-1$
                "hip_activity" //$NON-NLS-1$
        );

        TmfAbstractAnalysisRequirement eventsReq = new TmfAnalysisEventRequirement(requiredEvents, PriorityLevel.MANDATORY);
        registerOutput(new TmfAnalysisViewOutput(ROOFLINE_VIEW_ID));

        return Set.of(); // Empty set
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        File hipAnalyzerConf = getConfigFile(HIP_ANALYZER_SUPPLEMENTARY_FILE);
        if (hipAnalyzerConf == null) {
            return false;
        }

        report = HipAnalyzerReport.deserialize(Path.of(hipAnalyzerConf.getAbsolutePath()));

        File gpuInfoPath = getConfigFile(GPU_INFO_SUPPLEMENTARY_FILE);
        if (gpuInfoPath == null) {
            return false;
        }

        gpuInfo = GpuInfo.deserialize(Path.of(gpuInfoPath.getAbsolutePath()));


        return super.executeAnalysis(monitor);
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

    private File getConfigFile(String file) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return null;
        }

        String dir = TmfTraceManager.getSupplementaryFileDir(trace);
        File conf = new File(dir + file);
        if (!conf.exists()) {
            return null;
        }

        return conf;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new GpuRooflineStateProvider(Objects.requireNonNull(getTrace()), getId(), null, report, gpuInfo);
    }

}
