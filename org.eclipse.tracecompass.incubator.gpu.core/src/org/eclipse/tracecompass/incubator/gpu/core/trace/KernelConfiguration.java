/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.File;
import java.io.IOError;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.tracecompass.incubator.internal.gpu.core.Activator;
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
    public KernelConfiguration() {
    }

    /**
     * @param filename
     *            Kernel launch configuration path
     */
    public KernelConfiguration(String filename) {
        IStatus status = parse(filename);
        if (status.getCode() == IStatus.ERROR) {
            throw new IOError(new Exception());
        }
    }

    /**
     * @param filename
     *            Kernel launch configuration path
     * @return Status
     */
    public IStatus parse(String filename) {
        File f = new File(filename);
        if (!f.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File does not exist"); //$NON-NLS-1$
        }

        if (!f.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Path is not a file"); //$NON-NLS-1$
        }

        String json;
        try {
            json = Files.readString(Path.of(filename));
        } catch (Exception e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not read file " + filename); //$NON-NLS-1$
        }

        JsonElement element = JsonParser.parseString(json);
        if (!element.isJsonObject()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid JSON file " + filename); //$NON-NLS-1$
        }

        JsonObject obj = element.getAsJsonObject();

        kernelName = obj.get("name").getAsString(); //$NON-NLS-1$
        bblocks = obj.get("bblocks").getAsInt(); //$NON-NLS-1$

        JsonElement geometry = obj.get("geometry"); //$NON-NLS-1$
        threads = parseDim3(geometry.getAsJsonObject().get("threads")); //$NON-NLS-1$
        blocks = parseDim3(geometry.getAsJsonObject().get("blocks")); //$NON-NLS-1$

        return new Status(IStatus.OK, Activator.PLUGIN_ID, null);
    }

    /**
     * @param el
     * @return
     */
    public int[] parseDim3(JsonElement el) {
        if (!el.isJsonObject()) {
            return new int[] { 0, 0, 0 };
        }

        JsonObject obj = el.getAsJsonObject();

        int x = obj.get("x").getAsInt(); //$NON-NLS-1$
        int y = obj.get("y").getAsInt(); //$NON-NLS-1$
        int z = obj.get("z").getAsInt(); //$NON-NLS-1$

        return new int[] { x, y, z };
    }

    /**
     * @return Kernel Name
     */
    public String getKernelName() {
        return kernelName;
    }

    /**
     * @return Thread dimensions
     */
    public int[] getThreads() {
        return threads;
    }

    /**
     * @return Blocks dimension
     */
    public int[] getBlocks() {
        return blocks;
    }

    /**
     * @return Number of instrumented basic blocks
     */
    public int getBblocks() {
        return bblocks;
    }

    private String kernelName;
    private int bblocks;
    private int[] threads;
    private int[] blocks;
}
