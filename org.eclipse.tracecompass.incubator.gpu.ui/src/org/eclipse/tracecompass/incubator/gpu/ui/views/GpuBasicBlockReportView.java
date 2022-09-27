package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tracecompass.incubator.gpu.ui.viewers.StackedHistogramsChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

public class GpuBasicBlockReportView extends TmfChartView {


    /**
     * @brief View name
     */
    public static final String VIEW = "GPU Basic Block report"; //$NON-NLS-1$

    /**
     * @brief Chart ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.ui.gpubasicblockreportview"; //$NON-NLS-1$

    /**
     *
     */
    public GpuBasicBlockReportView() {
        super(VIEW);

    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        StackedHistogramsChartViewer chart = new StackedHistogramsChartViewer(parent, VIEW, ID);

        return chart;
    }

    private class EmptyViewer extends TmfViewer {
        private Composite fComposite;
        public EmptyViewer(Composite parent) {
            super(parent);
            fComposite = new Composite(parent, SWT.NONE);
        }
        @Override
        public void refresh() {
            // Do nothing
        }
        @Override
        public Control getControl() {
            return fComposite;
        }
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new EmptyViewer(parent);
    }
}
