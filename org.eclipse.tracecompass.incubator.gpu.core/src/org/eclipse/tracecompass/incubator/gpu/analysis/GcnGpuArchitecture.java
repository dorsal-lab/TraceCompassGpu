package org.eclipse.tracecompass.incubator.gpu.analysis;

import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public interface GcnGpuArchitecture {

    // ----- Hardware information ----- //

    /**
     * @return Number of Compute units in the GPU
     */
    int numCU();

    int maxWavesPerCU();

    // ----- State altering ------ //

    /**
     * @param hardwareIdRegister
     *            Value of the HW_ID register, as stored in the HipTrace events
     */
    void registerWave(ITmfEventField hardwareIdRegister);

    // ------ Data retrieval & analysis ----- //
    /**
     * @param computeUnit
     *            Id of the compute unit
     * @return Number of active waves in the compute unit
     */
    int activeWavesInCU(int computeUnit);

    /**
     * @param computeUnit
     *            Id of the compute unit
     * @return Occupancy of the compute unit
     */
    default double occupancy(int computeUnit) {
        return ((double) activeWavesInCU(computeUnit)) / ((double) maxWavesPerCU());
    }

    default double totalOccupancy() {
        double sum = 0.;
        for (int i = 0; i < numCU(); ++i) {
            sum += occupancy(i);
        }

        return sum / numCU();
    }
}
