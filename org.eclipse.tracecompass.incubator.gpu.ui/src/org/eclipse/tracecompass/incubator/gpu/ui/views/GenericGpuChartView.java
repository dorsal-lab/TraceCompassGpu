/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.ITmfAllowMultiple;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public abstract class GenericGpuChartView extends TmfView implements ITmfAllowMultiple {

    private static final int[] DEFAULT_WEIGHTS = { 1, 3 };

    /**
     * @brief Content of the view, as a widget.
     */
    protected Control fContent;
    private Composite fContentContainer;

    protected Control fNavigator;

    private SashForm fSashForm;

    /**
     * @param viewName
     *            Name to be displayed
     */
    public GenericGpuChartView(String viewName) {
        super(viewName);
    }

    /**
     * @param parent
     *            Parent composite
     * @return The content of the view
     */
    protected abstract Control createViewContent(Composite parent);

    /**
     * @param parent
     *            Parent composite
     * @return The view navigator (to be shown on the left part)
     */
    protected abstract Control createViewNavigator(Composite parent);

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();

        menuManager.add(new Separator());
        // ClampAction ?

        fSashForm = new SashForm(parent, SWT.NONE);
        fNavigator = createViewNavigator(fSashForm);
        fContentContainer = new Composite(fSashForm, SWT.NONE);
        fContentContainer.setLayout(contentLayout());

        fContent = createViewContent(fContentContainer);

        fSashForm.setWeights(DEFAULT_WEIGHTS);

        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        setupToolbar(toolBarManager);

        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            loadTrace();
        }
    }

    /**
     * @brief Load the trace in the viewer
     */
    protected abstract void loadTrace();

    /**
     * @return View main content layout
     */
    protected Layout contentLayout() {
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;

        return layout;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fContent != null) {
            fContent.dispose();
        }

        if (fNavigator != null) {
            fNavigator.dispose();
        }
    }

    @Override
    public void setFocus() {
        fContent.setFocus();
    }

    /**
     * @param toolBarManager
     *            Toolbar manager
     */
    protected void setupToolbar(IToolBarManager toolBarManager) {
        Action zoomInAction = new Action() {
            @Override
            public void run() {
                zoomIn();
            }
        };
        zoomInAction.setText("Zoom in"); //$NON-NLS-1$
        zoomInAction.setToolTipText("Zoom in"); //$NON-NLS-1$

        Action zoomOutAction = new Action() {
            @Override
            public void run() {
                zoomOut();
            }
        };
        zoomOutAction.setText("Zoom out"); //$NON-NLS-1$
        zoomOutAction.setToolTipText("Zoom out"); //$NON-NLS-1$
        toolBarManager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, zoomInAction);
        toolBarManager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, zoomOutAction);
    }

    protected abstract void zoomOut();

    protected abstract void zoomIn();

    protected IStatusLineManager getStatusLineManager() {
        return getViewSite().getActionBars().getStatusLineManager();
    }

    /**
     * @param parent
     *            Parent control
     * @return A newly created empty control
     */
    static protected Control emptyControl(Composite parent) {
        return new Composite(parent, SWT.NONE);
    }

}
