/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;

/**
 * @class GpuInfo
 * @brief Holds information about the offload device (peak bandwidth, ..)
 *
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuInfo {
    // ----- Inner classes ----- //

    /**
     * @brief Abstract roof with value
     */
    public interface IRoof {
        /**
         * @return Roof value
         */
        public double getRoof();
    }

    /**
     * @brief Memory bottleneck
     *
     */
    public static class MemoryRoof implements IRoof {
        /**
         * @brief Roof name
         */
        public String name;
        /**
         * @brief Maximum attainable bandwidth (bytes / second)
         */
        public double peak_bandwidth;

        @Override
        public double getRoof() {
            return peak_bandwidth;
        }
    }

    /**
     * @brief Compute efficiency bottleneck
     *
     */
    public static class ComputeRoof implements IRoof {
        /**
         * @brief Roof name
         */
        public String name;
        /**
         * @brief Maximum attainable operational intensity (Flop/second)
         */
        public double peak_flops_s;

        @Override
        public double getRoof() {
            return peak_flops_s;
        }
    }

    // ----- Attributes ----- //

    // Types

    /**
     * @brief Device (or model) name
     */
    public String name;
    /**
     * @brief List of memory roofs
     */
    public List<MemoryRoof> memory_roofs;
    /**
     * @brief List of compute roofs
     */
    public List<ComputeRoof> compute_roofs;

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
     * @return Deserialized report
     */
    public static GpuInfo deserialize(Path file) {
        if (file == null) {
            return null;
        }

        String json;
        try {
            json = Files.readString(file);
        } catch (IOException e) {
            return null;
        }

        return deserialize(json);
    }

    /**
     * @param json
     *            JSON string
     * @return Deserialized report
     */
    public static @Nullable GpuInfo deserialize(String json) {
        Gson gson = new Gson();

        @Nullable
        GpuInfo obj = gson.fromJson(json, GpuInfo.class);

        return obj;
    }
}
