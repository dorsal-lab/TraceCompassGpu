package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tracecompass.incubator.gpu.core.trace.GcnAsmParser;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * @brief GCN Architecture implementation for CDNA 1 GPUs (MI100, MI50, ...)
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class MI100Gpu implements GcnGpuArchitecture {

    private HashMap<Long, Integer> computeUnits;

    public MI100Gpu() {
        computeUnits = new HashMap<>();
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
        // TODO Auto-generated method stub
        return GcnGpuArchitecture.super.occupancy(computeUnit);
    }

    @Override
    public double totalOccupancy() {
        // TODO Auto-generated method stub
        return GcnGpuArchitecture.super.totalOccupancy();
    }

    @Override
    public void registerWave(long hardwareIdRegister) {
        GcnAsmParser.HardwareIdRegister hwId = new GcnAsmParser.HardwareIdRegister(hardwareIdRegister);
        long cu = hwId.cuId() + hwId.shId() << 3 + hwId.seId() << 4;

        computeUnits.put(cu, 0); // TODO
    }

    @Override
    public int activeWavesInCU(int computeUnit) {
        // TODO Auto-generated method stub
        return 0;
    }

}
