/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
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

        if (trace != null) {
            traceBasePath = trace.getPath();
        } else {
            traceBasePath = ""; //$NON-NLS-1$
        }

        hipAnalyzerPath = traceBasePath + "hip_analyzer.json"; //$NON-NLS-1$
        gpuInfoPath = traceBasePath + "gpu_info.json"; //$NON-NLS-1$
    }

    /**
     * @param hipTrace
     *            Hip trace file path
     * @param gpuInfo
     *            GPU info file path
     */
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
        localParent.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).create());
        localParent.setLayoutData(GridDataFactory.fillDefaults().hint(600, 200).grab(true, true).create());

        // ...

        new Label(localParent, SWT.LEFT).setText("Hip Analyzer file"); //$NON-NLS-1$
        hipAnalyzerPathField = new Text(localParent, SWT.BORDER);
        hipAnalyzerPathField.setText(hipAnalyzerPath);
        hipAnalyzerPathField.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());

        Button hipAnalyzerPathButton = new Button(localParent, SWT.PUSH);
        hipAnalyzerPathButton.setText("Open .."); //$NON-NLS-1$
        hipAnalyzerPathButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.OPEN);
                dialog.setFileName(hipAnalyzerPath);
                hipAnalyzerPath = dialog.open();
                hipAnalyzerPathField.setText(hipAnalyzerPath);
            }
        });

        new Label(localParent, SWT.LEFT).setText("GPU info file"); //$NON-NLS-1$
        gpuInfoPathField = new Text(localParent, SWT.BORDER);
        gpuInfoPathField.setText(gpuInfoPath);
        gpuInfoPathField.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());

        Button gpuInfoPathButton = new Button(localParent, SWT.PUSH);
        gpuInfoPathButton.setText("Open .."); //$NON-NLS-1$
        gpuInfoPathButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.OPEN);
                dialog.setFileName(gpuInfoPath);
                gpuInfoPath = dialog.open();
                gpuInfoPathField.setText(gpuInfoPath);
            }
        });

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
            hipAnalyzerPath = hipAnalyzerPathField.getText();
            gpuInfoPath = gpuInfoPathField.getText();
        }
        super.buttonPressed(buttonId);
    }

    /**
     * @return Selected hip_analyzer kernel info
     */
    public String getHipAnalyzerPath() {
        return hipAnalyzerPath;
    }

    /**
     * @return Selected GPU info
     */
    public String getGpuInfoPath() {
        return gpuInfoPath;
    }

}
