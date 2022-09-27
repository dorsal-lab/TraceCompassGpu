/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Range;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuOccupancyDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuOccupancyView extends TmfChartView {

    /**
     * @brief View name
     */
    public static final String VIEW = "GPU Occupancy view"; //$NON-NLS-1$

    /**
     * @brief Chart ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.ui.gpuoccupancyview"; //$NON-NLS-1$

    /**
     *
     */
    public GpuOccupancyView() {
        super(VIEW);

    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(VIEW, "Time", "Occupancy", 1); //$NON-NLS-1$ //$NON-NLS-2$

        TmfXYChartViewer chart = new TmfFilteredXYChartViewer(parent, settings, GpuOccupancyDataProvider.ID);

        chart.getSwtChart().getAxisSet().getYAxis(0).setRange(new Range(0., 1.));
        return chart;
    }

}
