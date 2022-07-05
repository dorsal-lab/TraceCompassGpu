package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;

/**
 * @brief Kernel launch configuration, to be deserialized from the
 *        hip_instrumentation runtime report
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class KernelConfiguration {
    // ----- Inner classes ----- //

    /**
     * @brief Kernel launch geometry
     */
    public static class Geometry {
        /**
         * @brief Thread geometry (dim3)
         */
        public Dim3 threads;

        /**
         * @brief Blocks geometry (dim3)
         */
        public Dim3 blocks;

        /**
         * @param _threads
         *            Thread geometry
         * @param _blocks
         *            Blocks geometry
         */
        public Geometry(Dim3 _threads, Dim3 _blocks) {
            threads = _threads;
            blocks = _blocks;
        }
    }

    /**
     * @brief The dim3 type as used by HIP and Cuda
     */
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

    /**
     * @brief Kernel name
     */
    public String name;

    /**
     * @brief Number of basic blocks
     */
    public int bblocks;

    /**
     * @brief Launch geometry
     */
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
    public static @Nullable KernelConfiguration deserialize(Path file) {
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
