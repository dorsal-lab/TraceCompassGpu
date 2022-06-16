/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.core;

import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysis;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuExperiment;
import org.eclipse.tracecompass.incubator.gpu.ui.views.GpuRooflineHandler;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineParameterProvider extends TmfAbstractAnalysisParamProvider {

    private static final String NAME = "Gpu Roofline parameter provider"; //$NON-NLS-1$

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object getParameter(String name) {
        IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        ICommandService service = win.getService(ICommandService.class);
        GpuRooflineHandler handler = (GpuRooflineHandler) service.getCommand(GpuRooflineHandler.COMMAND_ID).getHandler();

        if (name.equals(GpuRooflineAnalysis.PARAM_HIP_ANALYZER)) {
            return handler.getHipAnalyzerPath();
        }

        if (name.equals(GpuRooflineAnalysis.PARAM_GPU_INFO)) {
            return handler.getGpuInfoPath();
        }

        return null;
    }

    @Override
    public boolean appliesToTrace(ITmfTrace trace) {
        return (trace instanceof GpuExperiment);
    }

}
