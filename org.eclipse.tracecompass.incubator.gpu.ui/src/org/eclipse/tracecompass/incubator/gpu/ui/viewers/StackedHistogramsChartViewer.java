package org.eclipse.tracecompass.incubator.gpu.ui.viewers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.IAxisSet;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.barchart.TmfHistogramTooltipProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;

public class StackedHistogramsChartViewer extends TmfFilteredXYChartViewer {

    /** Analysis ID **/
    private String fId;

    /** Series width **/
    private static final int DEFAULT_SERIES_WIDTH = 1;

    /** Timeout between updates in the updateData thread **/
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    public StackedHistogramsChartViewer(Composite parent, String title, String id) {
        super(parent, new TmfXYChartSettings(null, null, null, 1), id);
        fId = id;

        IAxisSet axisSet = getSwtChart().getAxisSet();
        axisSet.getXAxis(0).getGrid().setStyle(LineStyle.NONE);
        axisSet.getYAxis(0).getGrid().setStyle(LineStyle.NONE);

        getSwtChart().getTitle().setText(title);

        setTooltipProvider(new TmfHistogramTooltipProvider(this));
    }

    @Override
    public @NonNull OutputElementStyle getSeriesStyle(@NonNull Long seriesId) {
        return getPresentationProvider().getSeriesStyle(seriesId, StyleProperties.SeriesType.BAR, DEFAULT_SERIES_WIDTH);
    }
}
