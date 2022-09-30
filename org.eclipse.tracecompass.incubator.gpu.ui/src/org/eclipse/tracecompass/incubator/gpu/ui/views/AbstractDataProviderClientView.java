package org.eclipse.tracecompass.incubator.gpu.ui.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

public abstract class AbstractDataProviderClientView extends GenericGpuChartView {

    private UpdateThread fUpdateThread;
    private ITmfTrace fTrace;

    /** Timeout between updates in the updateData thread **/
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    public AbstractDataProviderClientView(String viewName) {
        super(viewName);
    }

    @Override
    protected void loadTrace() {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            fTrace = trace;
            updateContent();
        }
    }

    public void updateContent() {
        ITmfTrace trace = fTrace;
        if (trace == null) {
            return;
        }

        getDisplay().asyncExec(() -> {
            if (!trace.equals(fTrace)) {
                return;
            }

            UpdateThread oldUpdateThread = fUpdateThread;
            if (oldUpdateThread != null) {
                // ??
            }
            fUpdateThread = new UpdateThread();
            fUpdateThread.start();
        });
    }

    protected class UpdateThread extends Thread {
        public UpdateThread() {
        }

        @Override
        public void run() {
            ITmfXYDataProvider dataProvider = initializeDataProvider(fTrace);

            if (dataProvider == null) {
                return;
            }

            updateData(dataProvider, new NullProgressMonitor());
        }
    }

    /**
     * @param dataProvider
     * @param monitor
     */
    protected void updateData(ITmfXYDataProvider dataProvider, IProgressMonitor monitor) {
        Map<String, Object> parameters = new HashMap<>();

        boolean isComplete = false;
        do {
            TmfModelResponse<@NonNull ITmfXyModel> response = dataProvider.fetchXY(parameters, monitor);
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

    /**
     *
     * @brief Method to be implemented to apply the output of the data provider
     *        to the
     * @param model
     *            Data provider model output
     * @param monitor
     *            Progress monitor
     */
    protected abstract void updateDisplay(ITmfXyModel model, IProgressMonitor monitor);

    protected ITmfXYDataProvider initializeDataProvider(ITmfTrace trace) {
        ITmfTreeXYDataProvider<?> dataProvider = DataProviderManager.getInstance().getOrCreateDataProvider(trace, fId, ITmfTreeXYDataProvider.class);
        return dataProvider;
    }

}
