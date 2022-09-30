/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class EmptyGpuView extends GenericGpuChartView {

    private static final String VIEW_ID = "Empty view??"; //$NON-NLS-1$

    public EmptyGpuView() {
        super(VIEW_ID);
    }

    @Override
    protected Control createViewContent(Composite parent) {
        Text content = new Text(parent, SWT.SINGLE);
        content.setText("Hello ?");
        return content;
    }

    @Override
    protected Control createViewNavigator(Composite parent) {
        return emptyControl(parent);
    }

    @Override
    protected void loadTrace() {

    }

    @Override
    protected void zoomOut() {
    }

    @Override
    protected void zoomIn() {
    }

}
