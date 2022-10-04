package org.eclipse.tracecompass.incubator.gpu.ui.views;

import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.ISeriesSet;
import org.eclipse.swtchart.model.DoubleArraySeriesModel;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuBasicBlocksReportDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;

public class GpuBasicBlockReportView extends AbstractDataProviderClientView {

    private SashForm fSash;
    private Chart[] currentCharts;

    /** The color scheme for the chart */
    private @NonNull TimeGraphColorScheme fColorScheme = new TimeGraphColorScheme();

    /**
     * @brief View name
     */
    public static final String VIEW = "GPU Basic Block report"; //$NON-NLS-1$

    /**
     * @brief Chart ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.ui.gpubasicblockreportview"; //$NON-NLS-1$

    private static final String XAXIS_NAME = "Active threads"; //$NON-NLS-1$
    private static final String YAXIS_NAME = "Distribution"; //$NON-NLS-1$

    /**
     *
     */
    public GpuBasicBlockReportView() {
        super(VIEW, GpuBasicBlocksReportDataProvider.ID);

    }

    @Override
    protected Control createViewNavigator(Composite parent) {
        return new Composite(parent, SWT.NONE);
    }

    @Override
    protected void updateDisplay(ITmfXyModel model, IProgressMonitor monitor) {
        final ITmfXyModel seriesValues = model;

        if (currentCharts != null) {
            for (Chart c : currentCharts) {
                if (c != null) {
                    c.dispose();
                }
            }
        }

        if (monitor != null && monitor.isCanceled()) {
            return;
        }

        currentCharts = new Chart[seriesValues.getSeriesData().size()];

        getDisplay().asyncExec(() -> {
            TmfXYAxisDescription xAxisDescription = null;
            TmfXYAxisDescription yAxisDescription = null;

            int i = 0;
            for (ISeriesModel entry : seriesValues.getSeriesData()) {
                Chart chart = createChart();
                ISeriesSet seriesSet = chart.getSeriesSet();

                IBarSeries<Integer> series = (IBarSeries<Integer>) seriesSet.createSeries(SeriesType.BAR, entry.getName());
                series.setDataModel(new DoubleArraySeriesModel(Arrays.stream(entry.getXAxis()).mapToDouble((x) -> x).toArray(), entry.getData()));

                if (xAxisDescription == null) {
                    xAxisDescription = entry.getXAxisDescription();
                }
                if (yAxisDescription == null) {
                    yAxisDescription = entry.getYAxisDescription();
                }

                chart.getAxisSet().adjustRange();
                chart.redraw();
                currentCharts[i] = chart;

                ++i;
            }

            fSash.layout();
            //fScrolled.layout();
        });

        /* fScrolled.layout(); */

    }

    @Override
    protected Control createViewContent(Composite parent) {
        fSash = new SashForm(parent, SWT.VERTICAL);
        fSash.setBackground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_BACKGROUND));

        return fSash;

    }

    @Override
    protected void zoomOut() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void zoomIn() {
        // TODO Auto-generated method stub

    }

    private Chart createChart() {
        Chart fSwtChart = new Chart(fSash, SWT.NONE);
        fSwtChart.getPlotArea().addCustomPaintListener(new ICustomPaintListener() {

            @Override
            public void paintControl(PaintEvent e) {
                drawGridLines(e.gc, fSwtChart);
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
        xAxis.getTitle().setText(XAXIS_NAME);

        IAxis yAxis = fSwtChart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        yAxis.getTick().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        yAxis.getTitle().setText(YAXIS_NAME);

        return fSwtChart;

    }

    private static void drawGridLines(GC gc, Chart swtChart) {
        // Point size = fSwtChart.getPlotArea().getSize();
        Color foreground = swtChart.getAxisSet().getXAxis(0).getGrid().getForeground();
        gc.setForeground(foreground);
        gc.setAlpha(foreground.getAlpha());
        gc.setLineStyle(SWT.LINE_DOT);

        gc.setAlpha(255);
    }

}
