/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeriesSet;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.DoubleArraySeriesModel;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysisDataProvider;
import org.eclipse.tracecompass.incubator.gpu.analysis.RooflineXYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineView extends AbstractDataProviderClientView {

    private Chart fSwtChart;

    private static final String VIEW = "GPU Roofline"; //$NON-NLS-1$

    /** The color scheme for the chart */
    private @NonNull TimeGraphColorScheme fColorScheme = new TimeGraphColorScheme();

    public GpuRooflineView() {
        super(VIEW, GpuRooflineAnalysisDataProvider.ID);
    }

    @Override
    protected void updateDisplay(ITmfXyModel model, IProgressMonitor monitor) {
        final ITmfXyModel seriesValues = model;
        Display.getDefault().asyncExec(() -> {
            TmfXYAxisDescription xAxisDescription = null;
            TmfXYAxisDescription yAxisDescription = null;

            if (fSwtChart.isDisposed()) {
                return;
            }
            if (monitor != null && monitor.isCanceled()) {
                return;
            }
            ISeriesSet seriesSet = fSwtChart.getSeriesSet();

            for (ISeriesModel entry : seriesValues.getSeriesData()) {

                ILineSeries<Integer> series = (ILineSeries<Integer>) seriesSet.createSeries(SeriesType.LINE, entry.getName());

                series.setDataModel(new DoubleArraySeriesModel(
                        RooflineXYModel.fromFixedPointArray(entry.getXAxis()),
                        entry.getData()));

                if (entry.getDisplayType() == DisplayType.SCATTER) {
                    series.setLineStyle(LineStyle.NONE);
                    series.setSymbolType(PlotSymbolType.SQUARE);
                    series.setSymbolSize(8);
                }

                // Get the x and y data types
                if (xAxisDescription == null) {
                    xAxisDescription = entry.getXAxisDescription();
                }
                if (yAxisDescription == null) {
                    yAxisDescription = entry.getYAxisDescription();
                }
            }

            IAxis xAxis = fSwtChart.getAxisSet().getXAxis(0);
            xAxis.enableLogScale(true);
            xAxis.setRange(new Range(RooflineXYModel.ROOFLINE_X_MIN, RooflineXYModel.ROOFLINE_X_MAX));

            IAxis yAxis = fSwtChart.getAxisSet().getYAxis(0);
            yAxis.enableLogScale(true);
            yAxis.setRange(new Range(RooflineXYModel.ROOFLINE_Y_MIN, RooflineXYModel.ROOFLINE_Y_MAX));

            fSwtChart.redraw();
        });

    }

    @Override
    protected Control createViewContent(Composite parent) {
        fSwtChart = new Chart(parent, SWT.NONE);
        fSwtChart.getPlotArea().addCustomPaintListener(new ICustomPaintListener() {

            @Override
            public void paintControl(PaintEvent e) {
                drawGridLines(e.gc);
            }

            @Override
            public boolean drawBehindSeries() {
                return true;
            }
        });

        Color backgroundColor = fColorScheme.getColor(TimeGraphColorScheme.TOOL_BACKGROUND);
        fSwtChart.setBackground(backgroundColor);
        backgroundColor = fColorScheme.getColor(TimeGraphColorScheme.BACKGROUND);
        fSwtChart.getPlotArea().setBackground(backgroundColor);
        fSwtChart.setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));

        fSwtChart.getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));

        IAxis xAxis = fSwtChart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        xAxis.getTick().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        xAxis.getTitle().setText(RooflineXYModel.ROOFLINE_XAXIS_NAME);
        xAxis.enableLogScale(true);

        IAxis yAxis = fSwtChart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        yAxis.getTick().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        yAxis.getTitle().setText(RooflineXYModel.ROOFLINE_YAXIS_NAME);
        yAxis.enableLogScale(true);

        fSwtChart.getTitle().setText(VIEW);

        return fSwtChart;
    }

    private void drawGridLines(GC gc) {
        // Point size = fSwtChart.getPlotArea().getSize();
        Color foreground = fSwtChart.getAxisSet().getXAxis(0).getGrid().getForeground();
        gc.setForeground(foreground);
        gc.setAlpha(foreground.getAlpha());
        gc.setLineStyle(SWT.LINE_DOT);

        gc.setAlpha(255);
    }

    @Override
    protected Control createViewNavigator(Composite parent) {
        return new Composite(parent, SWT.NONE);
    }

    @Override
    protected void zoomOut() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void zoomIn() {
        // TODO Auto-generated method stub

    }

}
