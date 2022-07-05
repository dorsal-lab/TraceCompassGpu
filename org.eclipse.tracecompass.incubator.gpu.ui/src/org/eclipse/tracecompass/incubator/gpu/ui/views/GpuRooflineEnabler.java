/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.ui.views;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.tracecompass.incubator.gpu.analysis.GpuRooflineAnalysis;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfAnalysisElement;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuRooflineEnabler extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        TmfAnalysisElement analysis = (TmfAnalysisElement) receiver;
        if (analysis != null) {
            return analysis.getAnalysisId().equals(GpuRooflineAnalysis.ID);
        }
        return false;
    }

}
