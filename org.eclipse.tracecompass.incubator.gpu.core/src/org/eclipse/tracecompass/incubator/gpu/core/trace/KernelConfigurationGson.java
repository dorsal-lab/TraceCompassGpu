package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class KernelConfigurationGson {
    public static class Dim3 {
        /**
         * @param _x
         *            x
         * @param _y
         *            y
         * @param _z
         *            z
         */
        public Dim3(int _x, int _y, int _z) {
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

    /**
     * @param file
     *            JSON file path
     * @return Deserialized configuration
     */
    public static KernelConfigurationGson deserialize(Path file) {
        if (file == null) {
            return null;
        }
        String json;
        try {
            json = Files.readString(file);
        } catch (IOException e) {
            return null;
        }

        Gson gson = new Gson();
        return gson.fromJson(json, KernelConfigurationGson.class);
    }
}
