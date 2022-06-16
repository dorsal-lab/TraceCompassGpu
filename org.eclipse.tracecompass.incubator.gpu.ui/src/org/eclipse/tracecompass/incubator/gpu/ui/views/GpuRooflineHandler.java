/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import java.util.Objects;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysis;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfAnalysisElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfViewsElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineHandler extends AbstractHandler {

    private String hipAnalyzerPath;
    private String gpuInfoPath;

    private @Nullable TmfAnalysisElement fAnalysis;


    /**
     * Default constructor
     */
    public GpuRooflineHandler() {
        super();
        hipAnalyzerPath = new String();
        gpuInfoPath = new String();
    }

    @Override
    public boolean isEnabled() {

        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return false;
        }

        // Get the selection
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart part = page.getActivePart();
        if (part == null) {
            return false;
        }
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return false;
        }
        ISelection selection = selectionProvider.getSelection();

        // Make sure there is only one selection and that it is a trace
        fAnalysis = null;
        if (selection instanceof TreeSelection) {
            TreeSelection sel = (TreeSelection) selection;
            // There should be only one item selected as per the plugin.xml
            Object element = sel.getFirstElement();
            if (element instanceof TmfAnalysisElement) {
                fAnalysis = (TmfAnalysisElement) element;
            }
        }

        if (fAnalysis != null) {
            return Objects.equals(fAnalysis.getAnalysisId(), GpuRooflineAnalysis.ID);
        }
        return false;
    }

    @Override
    public @Nullable Object execute(@Nullable ExecutionEvent event) throws ExecutionException {
        Shell activeShellChecked = HandlerUtil.getActiveShellChecked(event);

        if (fAnalysis != null && activeShellChecked != null) {
            TmfViewsElement parent = fAnalysis.getParent();
            TmfCommonProjectElement traceElement = parent.getParent();

            GpuRooflineAnalysisConfigView dialog = new GpuRooflineAnalysisConfigView(activeShellChecked, traceElement.getTrace());
            dialog.setPreFilledPaths(hipAnalyzerPath, gpuInfoPath);
            dialog.open();

            hipAnalyzerPath = dialog.getHipAnalyzerPath();
            gpuInfoPath = dialog.getGpuInfoPath();

            TmfOpenTraceHelper.openFromElement(traceElement);
        }
        return null;
    }
}
