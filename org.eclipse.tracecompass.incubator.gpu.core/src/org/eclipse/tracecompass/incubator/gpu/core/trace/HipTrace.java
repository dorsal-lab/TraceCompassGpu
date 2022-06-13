package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.nio.ByteBuffer;
import java.io.File;
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

        String[] header = parseHeader(f);
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

    private String[] parseHeader(File f) {

    }
}
