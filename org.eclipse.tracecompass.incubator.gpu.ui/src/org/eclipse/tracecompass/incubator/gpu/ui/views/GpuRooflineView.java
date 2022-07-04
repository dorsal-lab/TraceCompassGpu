/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysis;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysisDataProvider;
import org.eclipse.tracecompass.incubator.gpu.ui.viewers.RooflineChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.*;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineView extends TmfChartView {

    private static final String CHART_NAME = "Roofline"; //$NON-NLS-1$
    private static final String Y_AXIS = "FLOP / s"; //$NON-NLS-1$
    private static final String X_AXIS = "Arithmetic Intensity (FLOP / Byte)"; //$NON-NLS-1$

    private static final String VIEW_ID = GpuRooflineAnalysis.ROOFLINE_VIEW_ID;

    /**
     * @brief Defaut constructor
     */
    public GpuRooflineView() {
        super(VIEW_ID);
    }

    /**
     * @param viewName
     *            View name
     */
    public GpuRooflineView(String viewName) {
        super(viewName);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(null, null, null, 1);
        // TmfFilteredXYChartViewer chartViewer = new
        // TmfFilteredXYChartViewer(parent, settings,
        // GpuRooflineAnalysisDataProvider.ID);
        TmfXYChartViewer chartViewer = new RooflineChartViewer(parent, "", GpuRooflineAnalysisDataProvider.ID); //$NON-NLS-1$

        Chart chart = chartViewer.getSwtChart();

        // Set title

        chart.getTitle().setText(CHART_NAME);

        // Set log-log chart

        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setText(X_AXIS);
        xAxis.enableLogScale(true);

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setText(Y_AXIS);
        yAxis.enableLogScale(true);

        return chartViewer;
    }

    private static final class TreeViewer extends AbstractSelectTreeViewer2 {
        public TreeViewer(Composite parent) {
            super(parent, 1, GpuRooflineAnalysisDataProvider.ID);
        }

        @Override
        protected ITmfTreeColumnDataProvider getColumnDataProvider() {
            return () -> ImmutableList.of(createColumn("Name", Comparator.comparing(TmfTreeViewerEntry::getName)), //$NON-NLS-1$
                    new TmfTreeColumnData("Legend")); //$NON-NLS-1$
        }
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new TreeViewer(Objects.requireNonNull(parent));
    }

}
