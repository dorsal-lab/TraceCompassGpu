/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/** @class GpuInfo
 * @brief Holds information about the offload device (peak bandwidth, ..)
 *
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuInfo {
    // ----- Inner classes ----- //

    public static class MemoryRoof {
        public String name;
        public double peak_bandwidth;
    }

    public static class ComputeRoof {
        public String name;
        public double peak_flops_s;
    }

    // ----- Attributes ----- //

    // Types

    private static Type memoryRoofCollectionType = new TypeToken<Collection<MemoryRoof>>() {
    }.getType();

    private static Type computeRoofCollectionType = new TypeToken<Collection<ComputeRoof>>() {
    }.getType();

    // Attributes

    public String name;
    public List<MemoryRoof> memory_roofs;
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
    public static GpuInfo deserialize(String json) {
        Gson gson = new Gson();

        GpuInfo obj = gson.fromJson(json, GpuInfo.class);

        return obj;
    }
}
