package org.eclipse.tracecompass.incubator.gpu.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * @brief GCN ISA status parser
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GcnAsmParser {

    private static long MEMREALTIME_FREQ = 25000000L;

    /**
     * @param reg
     *            Register value
     * @param low
     *            Lower bound
     * @param high
     *            Higher bound
     * @return Extracted bit range
     */
    public static long extractBits(long reg, int low, int high) {
        return ((1 << (high - low + 1)) - 1 & (reg >> low));
    }

    /**
     * @brief EXEC Register impl
     */
    public static class ExecRegister {
        private long exec;

        /**
         * @param exec
         *            Exec register value
         */
        public ExecRegister(long exec) {
            this.exec = exec;
        }

        @Override
        public String toString() {
            return String.format("<%d active threads>", Long.bitCount(exec)); //$NON-NLS-1$
        }

        /**
         * @return Exec register value
         */
        public long get() {
            return exec;
        }
    }

    /**
     * Represents the HardwareID register value
     */
    public static class HardwareIdRegister {
        private long hwId;

        /**
         * @param hwId
         *            Hardware Id register
         */
        public HardwareIdRegister(long hwId) {
            this.hwId = hwId;
        }

        /**
         * @return Creates the relevant event fields
         */
        public TmfEventField[] toEventFields() {
            long wave_id = extractBits(hwId, 0, 3);
            long simd_id = extractBits(hwId, 4, 5);
            long pipe_id = extractBits(hwId, 6, 7);
            long cu_id = extractBits(hwId, 8, 11);
            long sh_id = extractBits(hwId, 12, 12);
            long se_id = extractBits(hwId, 13, 14);
            long tg_id = extractBits(hwId, 16, 19);
            long vm_id = extractBits(hwId, 20, 23);
            long queue_id = extractBits(hwId, 24, 26);
            long state_id = extractBits(hwId, 27, 29);
            long me_id = extractBits(hwId, 30, 31);

            @SuppressWarnings("nls")
            final TmfEventField[] fields = {
                    new TmfEventField("wave_id", wave_id, null),
                    new TmfEventField("simd_id", simd_id, null),
                    new TmfEventField("pipe_id", pipe_id, null),
                    new TmfEventField("cu_id", cu_id, null),
                    new TmfEventField("sh_id", sh_id, null),
                    new TmfEventField("se_id", se_id, null),
                    new TmfEventField("tg_id", tg_id, null),
                    new TmfEventField("vm_id", vm_id, null),
                    new TmfEventField("queue_id", queue_id, null),
                    new TmfEventField("state_id", state_id, null),
                    new TmfEventField("me_id", me_id, null),
            };

            return fields;
        }
    }

    /**
     * @param sMemRealtime S_MEMREALTIME Register value
     * @param header Event header
     * @return Absolute ITmfTimestamp computed from the offset
     */
    public static @NonNull ITmfTimestamp getStampNanos(long sMemRealtime, HipTrace.EventsHeader header) {
        if(header.hasStamp()) {
             long first = header.getFirstStamp();
             long diffTicks = sMemRealtime - first;

             double diffSeconds = ((double) diffTicks) / ((double) MEMREALTIME_FREQ);
             long diffNanos = (long) (diffSeconds * 1.e9);

             long stamp = header.counters.roctracerEnd + diffNanos;
             return TmfTimestamp.fromNanos(stamp);

        }

        return TmfTimestamp.fromNanos(header.counters.roctracerEnd);
    }
}
