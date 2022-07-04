/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.viewers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class RooflineChartViewer extends TmfXYChartViewer {

    /** The color scheme for the chart */
    private @NonNull TimeGraphColorScheme fColorScheme = new TimeGraphColorScheme();

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

        setSwtChart(fSwtChart); // TA-DAAA remove useless listeners
        setTimeAxisVisible(false);
    }

    private void drawGridLines(GC gc) {
        Chart fSwtChart = getSwtChart();
        Point size = fSwtChart.getPlotArea().getSize();
        Color foreground = fSwtChart.getAxisSet().getXAxis(0).getGrid().getForeground();
        gc.setForeground(foreground);
        gc.setAlpha(foreground.getAlpha());
        gc.setLineStyle(SWT.LINE_DOT);

        /*
        for (int x : fTimeScaleCtrl.getTickList()) {
            gc.drawLine(x, 0, x,  size.y);
        }
        */

        gc.setAlpha(255);
    }

    @Override
    public Control getControl() {
        return getSwtChart();
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void updateContent() {
        // TODO Auto-generated method stub

    }

}
