package org.eclipse.tracecompass.incubator.gpu.ui.viewers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeriesSet;
import org.eclipse.swtchart.model.DoubleArraySeriesModel;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.tracecompass.incubator.gpu.analysis.RooflineXYModel;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;

public class StackedHistogramsChartViewer extends TmfXYChartViewer {

    /** Analysis ID **/
    private String fId;

    private ScrolledComposite fScrolled;
    private Composite fContent;
    private Chart[] currentCharts;

    /** The color scheme for the chart */
    private @NonNull TimeGraphColorScheme fColorScheme = new TimeGraphColorScheme();

    /** Timeout between updates in the updateData thread **/
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    public StackedHistogramsChartViewer(Composite parent, String title, String id) {
        super(parent, title, "", "");
        fId = id;

        replaceChart();
    }

    private void replaceChart() {
        // Quick hack to replace the swt chart
        Composite fCommonComposite = getSwtChart().getParent();
        getSwtChart().setVisible(false);
        GridData gridData = (GridData) getSwtChart().getParent().getLayoutData();
        gridData.exclude = true;

        fScrolled = new ScrolledComposite(fCommonComposite, SWT.V_SCROLL | SWT.BORDER);
        fContent = new Composite(fCommonComposite, SWT.NONE);
        fContent.setBackground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_BACKGROUND));

        Label label = new Label(fCommonComposite, SWT.CENTER);
        label.setText("Test???");

        StackLayout layout = new StackLayout();
        fContent.setLayout(layout);

        setTimeAxisVisible(false);

        setSelectionProvider(null);
        setMouseDragZoomProvider(null);
        setMouseWheelZoomProvider(null);
        setTooltipProvider(null);
        setMouseDrageProvider(null);
    }

    private Chart createChart() {
        Chart fSwtChart = new Chart(fContent, SWT.NONE);
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
        xAxis.getTitle().setText(RooflineXYModel.ROOFLINE_XAXIS_NAME);
        xAxis.enableLogScale(true);

        IAxis yAxis = fSwtChart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        yAxis.getTick().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        yAxis.getTitle().setText(RooflineXYModel.ROOFLINE_YAXIS_NAME);
        yAxis.enableLogScale(true);

        return fSwtChart;

    }

    @Override
    public Control getControl() {
        return fContent;
    }

    private static void drawGridLines(GC gc, Chart swtChart) {
        // Point size = fSwtChart.getPlotArea().getSize();
        Color foreground = swtChart.getAxisSet().getXAxis(0).getGrid().getForeground();
        gc.setForeground(foreground);
        gc.setAlpha(foreground.getAlpha());
        gc.setLineStyle(SWT.LINE_DOT);

        gc.setAlpha(255);
    }

    @Override
    protected void updateContent() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }
        ITmfXYDataProvider dataProvider = initializeDataProvider(getTrace());

        if (dataProvider == null) {
            return;
        }

        // TODO : Create a thread for the update instead of freezing the UI,
        // just like CommonXAxisChartViewer
        updateData(dataProvider, new NullProgressMonitor());
    }

    private void updateData(@NonNull ITmfXYDataProvider dataProvider, IProgressMonitor monitor) {
        Map<String, Object> parameters = new HashMap<>();

        boolean isComplete = false;
        do {
            TmfModelResponse<@NonNull ITmfXyModel> response = dataProvider.fetchXY(parameters, monitor);
            ITmfXyModel model = response.getModel();
            if (model != null) {
                updateDisplay(model, monitor);
            }

            ITmfResponse.Status status = response.getStatus();
            if (status == ITmfResponse.Status.COMPLETED) {
                /*
                 * Model is complete, no need to request again the data provider
                 */
                isComplete = true;
            } else if (status == ITmfResponse.Status.FAILED || status == ITmfResponse.Status.CANCELLED) {
                /* Error occurred, log and return */
                isComplete = true;
            } else {
                /**
                 * Status is RUNNING. Sleeping current thread to wait before
                 * request data provider again
                 **/
                try {
                    Thread.sleep(BUILD_UPDATE_TIMEOUT);
                } catch (InterruptedException e) {
                    /**
                     * InterruptedException is throw by Thread.Sleep and we
                     * should retry querying the data provider
                     **/
                    // Thread.currentThread().interrupt();
                }
            }
        } while (!isComplete);
    }

    private void updateDisplay(ITmfXyModel model, IProgressMonitor monitor) {
        final ITmfXyModel seriesValues = model;

        if (currentCharts != null) {
            for (Chart c : currentCharts) {
                if (c != null) {
                    c.dispose();
                }
            }
        }

        Display.getDefault().asyncExec(() -> {
            TmfXYAxisDescription xAxisDescription = null;
            TmfXYAxisDescription yAxisDescription = null;

            if (monitor != null && monitor.isCanceled()) {
                return;
            }

            currentCharts = new Chart[seriesValues.getSeriesData().size()];

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

                chart.redraw();
                currentCharts[i] = chart;

                ++i;
            }

            fContent.layout();
            /* fScrolled.layout(); */
        });
    }

    protected ITmfXYDataProvider initializeDataProvider(ITmfTrace trace) {
        ITmfTreeXYDataProvider<?> dataProvider = DataProviderManager.getInstance().getOrCreateDataProvider(trace, fId, ITmfTreeXYDataProvider.class);
        return dataProvider;
    }

}
