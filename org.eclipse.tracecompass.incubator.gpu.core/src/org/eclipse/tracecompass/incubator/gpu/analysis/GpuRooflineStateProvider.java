/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuInfo;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipAnalyzerReport;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipTrace;
import org.eclipse.tracecompass.incubator.gpu.core.trace.KernelConfiguration;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 0;
    private static final String ID = "org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineStateProvider"; //$NON-NLS-1$

    private KernelConfiguration kernelConf;
    private HipAnalyzerReport report;
    private GpuInfo gpuInfo;

    private long totalFlops = 0L;
    private long totalFloatLoads = 0L;
    private long totalFloatStores = 0L;

    /**
     * @param trace
     *            Trace
     * @param id
     *            Identifier
     */
    public GpuRooflineStateProvider(@NonNull ITmfTrace trace, @NonNull String id, KernelConfiguration _kernelConf, HipAnalyzerReport _report, GpuInfo _gpuInfo) {
        super(trace, id);
        kernelConf = _kernelConf;
        report = _report;
        gpuInfo = _gpuInfo;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new GpuRooflineStateProvider(getTrace(), ID, kernelConf, report, gpuInfo);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (!event.getType().getName().equals(HipTrace.HIPTRACE_NAME)) {
            return;
        }

        long ts = event.getTimestamp().getValue();

        long counter = (long) event.getContent().getField("counter").getValue(); //$NON-NLS-1$
        long blockId = (long) event.getContent().getField("bblock").getValue(); //$NON-NLS-1$

        HipAnalyzerReport.BasicBlock bblock = report.getBlocks().get((int) blockId);

        totalFlops += counter * bblock.flops;
        totalFloatLoads += counter * bblock.floating_ld;
        totalFloatStores += counter * bblock.floating_st;

        // Update state system

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        int quark = ss.getQuarkAbsoluteAndAdd("flops"); //$NON-NLS-1$
        ss.modifyAttribute(ts, totalFlops, quark);
    }

}
