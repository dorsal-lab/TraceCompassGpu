package org.eclipse.tracecompass.incubator.gpu.analysis;

import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * @brief GCN Architecture implementation for CDNA 1 GPUs (MI100, MI50, ...)
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class MI100Gpu implements GcnGpuArchitecture {

    private int[] computeUnits;

    public MI100Gpu() {
        computeUnits = new int[numCU()];
    }

    @Override
    public int numCU() {
        return 120;
    }

    @Override
    public int maxWavesPerCU() {
        return 40;
    }

    @Override
    public void registerWave(ITmfEventField hardwareIdRegister) {
        // TODO
    }

    @Override
    public int activeWavesInCU(int computeUnit) {
        // TODO Auto-generated method stub
        return 0;
    }

}
