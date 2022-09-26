package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.tracecompass.incubator.gpu.core.trace.GcnAsmParser;

/**
 * @brief GCN Architecture implementation for CDNA 1 GPUs (MI100, MI50, ...)
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class MI100Gpu implements GcnGpuArchitecture {

    private TreeMap<Long, Integer> computeUnits;

    public MI100Gpu() {
        computeUnits = new TreeMap<>();
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
    public double occupancy(int computeUnit) {
        return activeWavesInCU(computeUnit) / maxWavesPerCU();
    }

    @Override
    public double totalOccupancy() {
        double sum = 0.;
        for (Map.Entry<Long, Integer> entry : computeUnits.entrySet()) {
            sum += entry.getValue();
        }

        return sum / ((double) numCU() * maxWavesPerCU());
    }

    @Override
    public void registerWave(long hardwareIdRegister) {
        GcnAsmParser.HardwareIdRegister hwId = new GcnAsmParser.HardwareIdRegister(hardwareIdRegister);
        long cu = hwId.cuId() + (hwId.shId() << 3) + (hwId.seId() << 4);

        Integer activeWavesInCu = computeUnits.getOrDefault(cu, 0) + 1;

        computeUnits.put(cu, activeWavesInCu);

    }

    @Override
    public int activeWavesInCU(int computeUnit) {
        return computeUnits.getOrDefault((long) computeUnit, 0);
    }

    /**
     * @brief Dump content to stdout
     */
    public void dump() {
        int[] values = new int[numCU()];

        for (Map.Entry<Long, Integer> entry : computeUnits.entrySet()) {
            long key = entry.getKey();
            values[(int) key] = entry.getValue();
        }

        long activeWaves = 0L;

        for (int i = 0; i < numCU(); ++i) {
            System.out.format("%d : %d\n", i, values[i]); //$NON-NLS-1$
            activeWaves += values[i];
        }

        System.out.format("Total : %d\n", activeWaves);
    }

}
