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
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

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

    // Quarks

    private static int DEFAULT_QUARK = -1;
    private int flopsQuark = DEFAULT_QUARK;
    private int loadsQuark = DEFAULT_QUARK;
    private int storesQuark = DEFAULT_QUARK;

    /**
     * State system attribute name for flop counter
     */
    public static String FLOPS_ATTRIBUTE_NAME = "flops"; //$NON-NLS-1$
    /**
     * State system attribute name for loaded bytes
     */
    public static String LOADS_ATTRIBUTE_NAME = "floating_ld"; //$NON-NLS-1$
    /**
     * State system attribute name for stores bytes
     */
    public static String STORES_ATTRIBUTE_NAME = "floating_s"; //$NON-NLS-1$

    private Map<Pair<String, Long>, String> functionMap;

    private ITmfEvent lastKernelCall = null;

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
        if (flopsQuark == DEFAULT_QUARK) {
            initQuarks();
        }

        ITmfTrace trace = event.getTrace();

        if (trace instanceof HipTrace) {
            handleHipTraceEvent(event);
        } else if (trace instanceof CtfTmfTrace) {
            switch (event.getType().getName()) {
            // HSA
            case "hsa_function_name": //$NON-NLS-1$
                handlefunctionNameEvent(event, APIType.HSA_API);
                break;
            case "hsa_api": //$NON-NLS-1$
                handleHsaApiEvent(event);
                break;
            // HIP
            case "hip_function_name": //$NON-NLS-1$
                handlefunctionNameEvent(event, APIType.HIP_API);
                break;
            case "hip_activity": //$NON-NLS-1$
                break;
            case "hip_api": //$NON-NLS-1$
                handleHipApiEvent(event);
                break;
            case "kernel_event": //$NON-NLS-1$
                handleKernelEvent(event);
                break;
            default:
                break;
            }
        } else {
            // Nothing to do
        }

    }

    /**
     * @brief Initializes the quarks
     */
    protected void initQuarks() {
        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        flopsQuark = ss.getQuarkAbsoluteAndAdd(FLOPS_ATTRIBUTE_NAME);
        loadsQuark = ss.getQuarkAbsoluteAndAdd(LOADS_ATTRIBUTE_NAME);
        storesQuark = ss.getQuarkAbsoluteAndAdd(STORES_ATTRIBUTE_NAME);
    }

    /**
     * Handles a single Hip Trace event (counters)
     *
     * @param event
     *            The event, assumed to be a valid hiptrace event
     */
    protected void handleHipTraceEvent(@NonNull ITmfEvent event) {
        long ts = lastKernelCall.getTimestamp().getValue();

        long counter = (long) event.getContent().getField("counter").getValue(); //$NON-NLS-1$
        long blockId = (long) event.getContent().getField("bblock").getValue(); //$NON-NLS-1$

        HipAnalyzerReport.BasicBlock bblock = report.getBlocks().get((int) blockId);

        totalFlops += counter * bblock.flops;
        totalFloatLoads += counter * bblock.floating_ld;
        totalFloatStores += counter * bblock.floating_st;

        // Update state system

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        ss.modifyAttribute(ts, totalFlops, flopsQuark);
        ss.modifyAttribute(ts, totalFloatLoads, loadsQuark);
        ss.modifyAttribute(ts, totalFloatStores, storesQuark);
    }

    /**
     * Handles a name definition event ("\<api\>_function_name") from the ROCm
     * trace
     *
     * @param event
     *            The event, assumed to be a <...>_function_name event
     * @param prefix
     *            API prefix ({@link APIType}
     */
    protected void handlefunctionNameEvent(@NonNull ITmfEvent event, String prefix) {
        ITmfEventField root = event.getContent();
        Long cid = (Long) root.getField("correlation_id").getValue(); //$NON-NLS-1$
        String name = (String) root.getField("name").getValue(); //$NON-NLS-1$

        functionMap.put(new Pair<>(prefix, cid), name);
    }

    private String getCorrelationName(ITmfEvent event, String prefix) {
        Long cid = (Long) event
                .getContent()
                .getField("cid") //$NON-NLS-1$
                .getValue();
        Pair<String, Long> entry = new Pair<>(prefix, cid);
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
        String func = getCorrelationName(event, APIType.HSA_API);

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

        String func = getCorrelationName(event, APIType.HIP_API);

        if (func == null) {
            // Unknown function, do not process
            return;
        }
    }

    /**
     * Handles a kernel event (most frequently a kernel call)
     *
     * @param event
     *            The event, assumed to be a kernel_event
     */
    protected void handleKernelEvent(@NonNull ITmfEvent event) {
        if (lastKernelCall != null) {
            // Update state system

            ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());
            CtfTmfTrace trace = (CtfTmfTrace) lastKernelCall.getTrace();

            long ts = (Long) lastKernelCall.getContent().getField("end").getValue(); //$NON-NLS-1$
            ts += trace.getOffset(); // The "end" attribute does not take into
                                     // account the offset of the trace, add it
                                     // back here

            ss.modifyAttribute(ts, 0L, flopsQuark);
            ss.modifyAttribute(ts, 0L, loadsQuark);
            ss.modifyAttribute(ts, 0L, storesQuark);

        }

        lastKernelCall = event;
        // Reset last total Flops, loads, stores

        totalFlops = 0L;
        totalFloatLoads = 0L;
        totalFloatStores = 0L;
    }

}
