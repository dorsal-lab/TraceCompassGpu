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
import org.eclipse.swtchart.IAxisSet;
import org.eclipse.swtchart.IAxisTick;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ITitle;
import org.eclipse.swtchart.Range;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.incubator.gpu.analysis.RooflineXYModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TraceCompassFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.viewers.xychart.BaseXYPresentationProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataTypeUtils;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.IFilterProperty;
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
        fSwtChart.getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        fSwtChart.getAxisSet().getXAxis(0).getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
        fSwtChart.getAxisSet().getYAxis(0).getTitle().setForeground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_FOREGROUND));

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

            for(ISeriesModel entry : seriesValues.getSeriesData()) {
                // Get the x and y data types
                if (xAxisDescription == null) {
                    xAxisDescription = entry.getXAxisDescription();
                }
                if (yAxisDescription == null) {
                    yAxisDescription = entry.getYAxisDescription();
                }
            }


            IAxisSet axisSet = getSwtChart().getAxisSet();
            if (yAxisDescription != null) {
                Format format = axisSet.getYAxis(0).getTick().getFormat();
                if (format == null) {
                    axisSet.getYAxis(0).getTick().setFormat(DataTypeUtils.getFormat(yAxisDescription.getDataType(), yAxisDescription.getUnit()));
                }
                ITitle title = axisSet.getYAxis(0).getTitle();
                // Set the Y title if it was not previously set (ie it is invisible)
                if (!title.isVisible()) {
                    title.setText(yAxisDescription.getLabel());
                    title.setVisible(true);
                }
            }

            if (xAxisDescription != null) {
                Format format = axisSet.getXAxis(0).getTick().getFormat();
                if (format == null) {
                    axisSet.getXAxis(0).getTick().setFormat(DataTypeUtils.getFormat(xAxisDescription.getDataType(), xAxisDescription.getUnit()));
                }
                ITitle title = axisSet.getXAxis(0).getTitle();
                // Set the Y title if it was not previously set (ie it is invisible)
                if (!title.isVisible()) {
                    title.setText(xAxisDescription.getLabel());
                    title.setVisible(true);
                }
            }

            axisSet.getXAxis(0).setRange(new Range(RooflineXYModel.ROOFLINE_X_MIN, RooflineXYModel.ROOFLINE_X_MAX));
            axisSet.getYAxis(0).setRange(new Range(RooflineXYModel.ROOFLINE_Y_MIN, RooflineXYModel.ROOFLINE_Y_MAX));

            getSwtChart().redraw();
        });
    }

    protected ITmfXYDataProvider initializeDataProvider(ITmfTrace trace) {
        ITmfTreeXYDataProvider<?> dataProvider = DataProviderManager.getInstance().getOrCreateDataProvider(trace, fId, ITmfTreeXYDataProvider.class);
        return dataProvider;
    }


}
