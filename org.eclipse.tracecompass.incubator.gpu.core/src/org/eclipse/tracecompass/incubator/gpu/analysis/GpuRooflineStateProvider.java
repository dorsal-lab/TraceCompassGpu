/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuInfo;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipAnalyzerReport;
import org.eclipse.tracecompass.incubator.gpu.core.trace.HipTrace;
import org.eclipse.tracecompass.incubator.gpu.core.trace.KernelConfiguration;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
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

    private Map<Long, String> functionMap;

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
        functionMap = new HashMap<>();
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
        switch (event.getType().getName()) {
        case HipTrace.HIPTRACE_NAME:
            handleHipTraceEvent(event);
            break;
        case "hsa_function_name": //$NON-NLS-1$
            handleHsaFunctionNameEvent(event);
            break;
        case "hsa_api": //$NON-NLS-1$
            handleHsaApiEvent(event);
            break;
        default:
            break;
        }

    }

    /**
     * Handles a single Hip Trace event (counters)
     *
     * @param event
     *            The event, assumed to be a valid hiptrace event
     */
    protected void handleHipTraceEvent(@NonNull ITmfEvent event) {
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

        quark = ss.getQuarkAbsoluteAndAdd("floating_ld"); //$NON-NLS-1$
        ss.modifyAttribute(ts, totalFloatLoads, quark);

        quark = ss.getQuarkAbsoluteAndAdd("floating_s"); //$NON-NLS-1$
        ss.modifyAttribute(ts, totalFloatStores, quark);
    }

    /**
     * Handles a name definition event ("hsa_function_name") from the ROCm trace
     *
     * @param event
     *            The event, assumed to be a hsa_fucntion_name event
     */
    protected void handleHsaFunctionNameEvent(@NonNull ITmfEvent event) {
        ITmfEventField root = event.getContent();
        Long cid = (Long) root.getField("correlation_id").getValue();
        String name = (String) root.getField("name").getValue();

        functionMap.put(cid, name);
    }

    protected void handleHsaApiEvent(@NonNull ITmfEvent event) {
        ITmfEventField root = event.getContent();
        Long cid = (Long) root.getField("cid").getValue();
        String func = functionMap.get(cid);

        if(func == null) {
            // Unknown function, do not process
            return;
        }
    }

}
