package org.eclipse.tracecompass.incubator.gpu.ui.views;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuWaveLifetimeDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

import com.google.common.collect.ImmutableList;

public class GpuWaveCumulatedView extends TmfChartView {

    /**
     * @brief View name
     */
    public static final String VIEW = "GPU Cumulated waves view"; //$NON-NLS-1$

    /**
     * @brief Chart ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.ui.gpuwavecumulatedview"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public GpuWaveCumulatedView() {
        super(VIEW);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(VIEW, null, null, 1);
        return new TmfFilteredXYChartViewer(parent, settings, GpuWaveLifetimeDataProvider.ID);
    }

    public static final class TreeXYViewer extends AbstractSelectTreeViewer2 {

        public TreeXYViewer(Composite parent) {
            super(parent, 1, GpuWaveLifetimeDataProvider.ID);
        }

        @SuppressWarnings("nls")
        @Override
        protected ITmfTreeColumnDataProvider getColumnDataProvider() {
            return () -> ImmutableList.of(createColumn("Kernel", Comparator.comparing(TmfTreeViewerEntry::getName)),
                    new TmfTreeColumnData("Legend"));
        }
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new TreeXYViewer(Objects.requireNonNull(parent));
    }

}
