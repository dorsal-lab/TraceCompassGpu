package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuBasicBlocksReportDataProvider;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuWaveEventExporter;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;

public class GpuWaveEventExportView extends AbstractDataProviderClientView {

    private SashForm fSash;
    private Text text;
    private Button button;

    /** The color scheme for the chart */
    private @NonNull TimeGraphColorScheme fColorScheme = new TimeGraphColorScheme();

    /**
     * @brief View name
     */
    public static final String VIEW = "GPU Wave Event Export"; //$NON-NLS-1$

    /**
     * @brief Chart ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.gpu.ui.GpuWaveEventExportView"; //$NON-NLS-1$

    /**
     *
     */
    public GpuWaveEventExportView() {
        super(VIEW, GpuBasicBlocksReportDataProvider.ID);

    }

    @Override
    protected Control createViewNavigator(Composite parent) {
        return new Composite(parent, SWT.NONE);
    }

    @Override
    protected void updateDisplay(ITmfXyModel model, IProgressMonitor monitor) {
        final ITmfXyModel seriesValues = model;

        if (monitor != null && monitor.isCanceled()) {
            return;
        }

        getDisplay().asyncExec(() -> {
            fSash.layout();
        });

    }

    @Override
    protected Control createViewContent(Composite parent) {
        fSash = new SashForm(parent, SWT.VERTICAL);
        fSash.setBackground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_BACKGROUND));

        text = new Text(fSash, SWT.SINGLE);
        text.setText(GpuWaveEventExporter.DEFAULT_FILE);

        button = new Button(fSash, SWT.PUSH);
        button.setText("Export file");

        button.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateContent();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }});

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
}
