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
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

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

    private Map<Pair<String, Long>, String> functionMap;

    /**
     * @brief Supported ROCm traces
     */
    protected static class APIType {
        /**
         * @brief HIP Trace activity ("--hip-trace")
         */
        public static String HIP_API = "hip"; //$NON-NLS-1$
        /**
         * @brief HSA Runtime tracing activity ("--hsa-trace")
         */
        public static String HSA_API = "hsa"; //$NON-NLS-1$
    }

    /**
     * @param trace
     *            Trace
     * @param id
     *            Identifier
     * @param _kernelConf
     *            Kernel call configuration
     * @param _report
     *            HIP Analyzer report
     * @param _gpuInfo
     *            GPU Information
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
        // HSA
        case "hsa_function_name": //$NON-NLS-1$
            handlefunctionNameEvent(event, APIType.HSA_API);
            break;
        case "hsa_api": //$NON-NLS-1$
            handleHsaApiEvent(event);
            break;
        case "hip_function_name": //$NON-NLS-1$
            handlefunctionNameEvent(event, APIType.HIP_API);
            break;
        case "hip_activity": //$NON-NLS-1$
            break;
        case "hip_api": //$NON-NLS-1$
            handleHipApiEvent(event);
            break;
        case "kernel_event": //$NON-NLS-1$
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
     * @param prefix
     *            API prefix ({@link APIType}
     */
    protected void handlefunctionNameEvent(@NonNull ITmfEvent event, String prefix) {
        ITmfEventField root = event.getContent();
        Long cid = (Long) root.getField("correlation_id").getValue(); //$NON-NLS-1$
        String name = (String) root.getField("name").getValue(); //$NON-NLS-1$

        functionMap.put(new Pair<>(prefix, cid), name);
    }

    private String getCorrelationName(Long cid, String prefix) {
        Pair<String, Long> entry = new Pair<>(prefix, cid); // $NON-NLS-1$
                                                            // //$NON-NLS-2$
        return functionMap.get(entry);
    }

    /**
     * Handles a tracepoint as a call to an hsa api function
     *
     * @param event
     *            The event, assumed to be a hsa_api event
     */
    protected void handleHsaApiEvent(@NonNull ITmfEvent event) {
        ITmfEventField root = event.getContent();
        Long cid = (Long) root.getField("cid").getValue(); //$NON-NLS-1$
        String func = getCorrelationName(cid, APIType.HSA_API);

        if (func == null) {
            // Unknown function, do not process
            return;
        }
    }

    /**
     * Handles a tracepoint as a call to an hip api function
     *
     * @param event
     *            The event, assumed to be a hip_api event
     */
    protected void handleHipApiEvent(@NonNull ITmfEvent event) {
        ITmfEventField root = event.getContent();
        Long cid = (Long) root.getField("cid").getValue(); //$NON-NLS-1$
        String func = getCorrelationName(cid, APIType.HIP_API);

        if (func == null) {
            // Unknown function, do not process
            return;
        }
    }

}
