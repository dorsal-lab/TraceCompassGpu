/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineView extends TmfView {

    private static final String CHART_NAME = "Roofline"; //$NON-NLS-1$
    private static final String Y_AXIS = "FLOP / s"; //$NON-NLS-1$
    private static final String X_AXIS = "Arithmetic Intensity (FLOP / Byte)"; //$NON-NLS-1$

    private static final String VIEW_ID = "org.eclipse.tracecompass.incubator.gpu.ui.roofline"; //$NON-NLS-1$

    private Chart chart;

    private ITmfTrace currentTrace;

    /**
     * @brief Defaut constructor
     */
    public GpuRooflineView() {
        super(VIEW_ID);
    }

    /**
     * @param viewName View name
     */
    public GpuRooflineView(String viewName) {
        super(viewName);
    }

    @Override
    public void setFocus() {
        chart.setFocus();
    }

    @Override
    public void createPartControl(Composite parent) {
        chart = new Chart(parent, SWT.BORDER);
        chart.getTitle().setText(CHART_NAME);

        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setText(X_AXIS);
        xAxis.enableLogScale(true);


        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setText(Y_AXIS);
        yAxis.enableLogScale(true);

        chart.getLegend().setVisible(false);
    }

}
