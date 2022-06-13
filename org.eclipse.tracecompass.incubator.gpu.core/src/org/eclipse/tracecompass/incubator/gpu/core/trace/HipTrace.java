package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

public abstract class HipTrace extends TmfTrace implements ITmfPersistentlyIndexable, ITmfPropertiesProvider, ITmfTraceKnownSize {

    @Override
    public IStatus validate(IProject project, String path) {
        File f = new File(path);
        if(!f.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File does not exist"); //$NON-NLS-1$
        }

        if(!f.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Path is not a file"); //$NON-NLS-1$
        }

        if(!parseHeader(f)) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not parse trace header"); //$NON-NLS-1$
        }

        long traceSize;
        try {
            traceSize = Files.size(Path.of(path));
        } catch (IOException e) { // Should not happen
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not query trace size"); //$NON-NLS-1$
        }

        if((traceSize - fOffset) != instrSize * sizeofCounter) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Wrong data size, different from header"); //$NON-NLS-1$
        }

        configuration = new KernelConfiguration(path);


        return new TraceValidationStatus(100, Activator.PLUGIN_ID);
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int progress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ITmfLocation restoreLocation(ByteBuffer bufferIn) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getCheckpointSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        // TODO Auto-generated method stub
        return null;
    }

    // ----- GPU Counters specific methods ----- //

    private boolean parseHeader(File f) {
        String header = new String();
        try (BufferedReader br = new BufferedReader(new FileReader(f));) {
            header = br.readLine();
        } catch (IOException e) {
        }
        fOffset = header.length() + 1;

        String[] tokens = header.split(",", HEADER_TOKENS); //$NON-NLS-1$

        if(tokens.length < HEADER_TOKENS) {
            return false;
        }

        if(tokens[0] != HIPTRACE_NAME) {
            return false;
        }

        kernelName = tokens[1];

        try {
            instrSize = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            return false;
        }

        try {
            stamp = Long.parseLong(tokens[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        try {
            sizeofCounter = Short.parseShort(tokens[4]);
        } catch (NumberFormatException e) {
            return false;
        }


        return true;
    }

    public KernelConfiguration getConfiguration() {
        return configuration;
    }

    private int fOffset;

    private String kernelName;
    private int instrSize;
    private long stamp;
    private short sizeofCounter;
    private KernelConfiguration configuration;

    private static final int HEADER_TOKENS = 5;
    private static final String HIPTRACE_NAME = "hiptrace";
}
