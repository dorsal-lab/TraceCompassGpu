/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.viewers;

import java.text.Format;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxisSet;
import org.eclipse.swtchart.IAxisTick;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeriesSet;
import org.eclipse.swtchart.ITitle;
import org.eclipse.swtchart.Range;
import org.eclipse.swtchart.model.DoubleArraySeriesModel;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.tracecompass.incubator.gpu.analysis.RooflineXYModel;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataTypeUtils;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.IFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.colors.RGBAUtil;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class RooflineChartViewer extends TmfXYChartViewer {

    /** Analysis ID **/
    private String fId;

    /** The color scheme for the chart */
    private @NonNull TimeGraphColorScheme fColorScheme = new TimeGraphColorScheme();

    /** Timeout between updates in the updateData thread **/
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    public RooflineChartViewer(Composite parent, String title, String id) {
        super(parent, title, "", "");

        // Quick hack to replace the swt chart
        Composite fCommonComposite = getSwtChart().getParent();
        getSwtChart().dispose();

        Chart fSwtChart = new Chart(fCommonComposite, SWT.NONE);
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

        setSwtChart(fSwtChart); // TA-DAAA remove useless listeners
        setTimeAxisVisible(false);

        setSelectionProvider(null);
        setMouseDragZoomProvider(null);
        setMouseWheelZoomProvider(null);
        setTooltipProvider(null);
        setMouseDrageProvider(null);

        fId = id;
    }

    private void drawGridLines(GC gc) {
        Chart fSwtChart = getSwtChart();
        Point size = fSwtChart.getPlotArea().getSize();
        Color foreground = fSwtChart.getAxisSet().getXAxis(0).getGrid().getForeground();
        gc.setForeground(foreground);
        gc.setAlpha(foreground.getAlpha());
        gc.setLineStyle(SWT.LINE_DOT);

        /*
         * for (int x : fTimeScaleCtrl.getTickList()) { gc.drawLine(x, 0, x,
         * size.y); }
         */

        gc.setAlpha(255);
    }

    @Override
    protected void updateContent() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }
        ITmfXYDataProvider dataProvider = initializeDataProvider(getTrace());

        // TODO : Create a thread for the update instead of freezing the UI,
        // just like CommonXAxisChartViewer
        updateData(dataProvider, new NullProgressMonitor());
    }

    private void updateData(@NonNull ITmfXYDataProvider dataProvider, IProgressMonitor monitor) {
        Map<String, Object> parameters = new HashMap<>();

        boolean isComplete = false;
        do {
            TmfModelResponse<ITmfXyModel> response = dataProvider.fetchXY(parameters, monitor);
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
        Display.getDefault().asyncExec(() -> {
            TmfXYAxisDescription xAxisDescription = null;
            TmfXYAxisDescription yAxisDescription = null;

            if (getSwtChart().isDisposed()) {
                return;
            }
            if (monitor != null && monitor.isCanceled()) {
                return;
            }
            ISeriesSet seriesSet = getSwtChart().getSeriesSet();

            for (ISeriesModel entry : seriesValues.getSeriesData()) {

                ISeries<Integer> series = seriesSet.createSeries(SeriesType.LINE, entry.getName());

                //series.setYSeries(entry.getData());
                //series.setXSeries(RooflineXYModel.fromFixedPointArray(entry.getXAxis()));

                series.setDataModel(new DoubleArraySeriesModel(
                        RooflineXYModel.fromFixedPointArray(entry.getXAxis()),
                        entry.getData()));

                // Get the x and y data types
                if (xAxisDescription == null) {
                    xAxisDescription = entry.getXAxisDescription();
                }
                if (yAxisDescription == null) {
                    yAxisDescription = entry.getYAxisDescription();
                }
            }

            Chart fSwtChart = getSwtChart();
            IAxis xAxis = fSwtChart.getAxisSet().getXAxis(0);
            xAxis.enableLogScale(true);
            xAxis.setRange(new Range(RooflineXYModel.ROOFLINE_X_MIN, RooflineXYModel.ROOFLINE_X_MAX));

            IAxis yAxis = fSwtChart.getAxisSet().getYAxis(0);
            yAxis.enableLogScale(true);
            yAxis.setRange(new Range(RooflineXYModel.ROOFLINE_Y_MIN, RooflineXYModel.ROOFLINE_Y_MAX));

            fSwtChart.getAxisSet().adjustRange();

            fSwtChart.redraw();
        });
    }

    protected ITmfXYDataProvider initializeDataProvider(ITmfTrace trace) {
        ITmfTreeXYDataProvider<?> dataProvider = DataProviderManager.getInstance().getOrCreateDataProvider(trace, fId, ITmfTreeXYDataProvider.class);
        return dataProvider;
    }

}
