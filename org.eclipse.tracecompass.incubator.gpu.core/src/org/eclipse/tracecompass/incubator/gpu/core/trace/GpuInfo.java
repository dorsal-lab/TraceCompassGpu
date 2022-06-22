/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;

/** @class GpuInfo
 * @brief Holds information about the offload device (peak bandwidth, ..)
 *
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuInfo {
    // TODO

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
