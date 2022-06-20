package org.eclipse.tracecompass.incubator.gpu.core.trace;

import com.google.gson.Gson;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class KernelConfigurationGson {
    public static class Dim3 {
        /**
         * @param _x x
         * @param _y y
         * @param _z z
         */
        public Dim3 (int _x, int _y, int _z) {
            x = _x;
            y = _y;
            z = _z;
        }
        public int x, y, z;
    }
    public String kernelName;
    public int bblocks;
    public Dim3 threads;
    public Dim3 blocks;

    public String serialize() {
        Gson serializer = new Gson();
        return serializer.toJson(this);
    }
}
