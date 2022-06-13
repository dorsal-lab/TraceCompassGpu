/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.File;
import java.io.IOError;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class KernelConfiguration {
    public KernelConfiguration() {}

    public KernelConfiguration(String filename) {
        IStatus status = parse(filename);
        if(status.getCode() == IStatus.ERROR) {
            throw new IOError(new Exception());
        }
    }

    public IStatus parse(String filename) {
        File f = new File(filename);
        if(!f.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File does not exist"); //$NON-NLS-1$
        }

        if(!f.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Path is not a file"); //$NON-NLS-1$
        }

        String json;
        try {
            json = Files.readString(Path.of(filename));
        }
        catch (Exception e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not read file " + filename); //$NON-NLS-1$
        }

        JsonElement element = JsonParser.parseString(json);
        if(!element.isJsonObject()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid JSON file " + filename); //$NON-NLS-1$
        }

        JsonObject obj = element.getAsJsonObject();

        kernelName = obj.get("name").getAsString();
        bblocks = obj.get("bblocks").getAsInt();

        JsonElement geometry = obj.get("geometry");
        threads = parseDim3(geometry.getAsJsonObject().get("threads"));
        blocks = parseDim3(geometry.getAsJsonObject().get("blocks"));


        return new Status(IStatus.OK, Activator.PLUGIN_ID, null);
    }

    public int[] parseDim3(JsonElement el) {
        if(!el.isJsonObject()) {
            return new int[] {0, 0, 0};
        }

        JsonObject obj = el.getAsJsonObject();

        int x = obj.get("x").getAsInt();
        int y = obj.get("y").getAsInt();
        int z = obj.get("z").getAsInt();

        return new int[] {x, y, z};
    }

    public String getKernelName() {
        return kernelName;
    }

    public int[] getThreads() {
        return threads;
    }

    public int getBblocks() {
        return bblocks;
    }

    public int[] getBlocks() {
        return blocks;
    }

    private String kernelName;
    private int bblocks;
    private int[] threads;
    private int[] blocks;
}
