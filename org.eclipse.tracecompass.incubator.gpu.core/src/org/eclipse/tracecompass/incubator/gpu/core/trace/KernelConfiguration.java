package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class KernelConfiguration {
    // ----- Inner classes ----- //

    public static class Geometry {
        public Dim3 threads;
        public Dim3 blocks;

        public Geometry(Dim3 _threads, Dim3 _blocks) {
            threads = _threads;
            blocks = _blocks;
        }
    }

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

    // ----- Attributes ----- //

    public String name;
    public int bblocks;
    public Geometry geometry;

    // ----- Methods ----- //

    /**
     * @return Serialized object
     */
    public String serialize() {
        Gson serializer = new Gson();
        return serializer.toJson(this);
    }

    /**
     * @param file
     *            JSON file path
     * @return Deserialized configuration
     */
    public static KernelConfiguration deserialize(Path file) {
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
        return gson.fromJson(json, KernelConfiguration.class);
    }
}
