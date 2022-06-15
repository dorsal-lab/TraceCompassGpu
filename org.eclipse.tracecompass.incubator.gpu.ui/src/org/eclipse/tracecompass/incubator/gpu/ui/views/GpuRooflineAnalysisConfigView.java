/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineAnalysisConfigView extends Dialog {

    private String hipAnalyzerPath;
    private String gpuInfoPath;

    private String traceBasePath;

    // ----- Widgets ----- //

    private Text hipAnalyzerPathField;
    private Text gpuInfoPathField;

    /**
     * @param parent
     *            Parent widget
     * @param trace
     *            Experiment
     */
    public GpuRooflineAnalysisConfigView(Shell parent, ITmfTrace trace) {
        super(parent);

        traceBasePath = trace.getPath();
        hipAnalyzerPath = new String();
        gpuInfoPath = new String();
    }

    public void setPreFilledPaths(String hipTrace, String gpuInfo) {
        hipAnalyzerPath = hipTrace;
        gpuInfoPath = gpuInfo;
    }

    @Override
    protected Control createDialogArea(@Nullable Composite parent) {
        if (parent == null) {
            return null;
        }

        Shell shell = getShell();
        shell.setText("Configure GPU Roofline analysis"); //$NON-NLS-1$
        shell.addControlListener(resizeLayouter(shell));
        parent.addControlListener(resizeLayouter(parent));

        Composite localParent = (Composite) super.createDialogArea(parent);
        localParent.addControlListener(resizeLayouter(localParent));
        localParent.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        localParent.setLayoutData(GridDataFactory.fillDefaults().hint(600, 400).grab(true, true).create());

        // ...

        hipAnalyzerPathField = new Text(localParent, SWT.BORDER);
        hipAnalyzerPathField.setText(hipAnalyzerPath);

        gpuInfoPathField = new Text(localParent, SWT.BORDER);
        gpuInfoPathField.setText(gpuInfoPath);

        // Commit

        localParent.pack();
        return localParent;

    }

    private static ControlListener resizeLayouter(Composite composite) {
        return new ControlListener() {

            @Override
            public void controlResized(ControlEvent e) {
                composite.layout();
            }

            @Override
            public void controlMoved(ControlEvent e) {
                composite.layout();
            }
        };
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == Window.OK) {

        }
    }

    public String getHipAnalyzerPath() {
        return hipAnalyzerPathField.getText();
    }

    public String getGpuInfoPath() {
        return gpuInfoPathField.getText();
    }

}
