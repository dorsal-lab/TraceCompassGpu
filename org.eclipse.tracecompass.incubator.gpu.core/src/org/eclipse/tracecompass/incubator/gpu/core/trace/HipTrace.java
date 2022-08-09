package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.incubator.internal.gpu.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.*;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * @brief Trace generated with the hip-analyzer tool
 *
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class HipTrace extends TmfTrace implements ITmfTraceKnownSize {

    // ----- Trace file location ----- //

    private File fFile;
    private FileInputStream stream;
    private long fSize;
    private FileChannel fFileChannel;
    private TmfLongLocation fCurrent;
    private MappedByteBuffer fMappedByteBuffer;
    private int fOffset;

    private TmfEvent fCurrentEvent;

    // ----- Trace information ----- //

    private String kernelName;
    private int instrSize;
    private long stamp;
    private long roctracerBegin;
    private long roctracerEnd;
    private short sizeofCounter;
    private KernelConfiguration configuration;
    private boolean managed;

    /**
     * @brief Number of expected tokens in header
     */
    private static final int COUNTER_HEADER_TOKENS = 7;

    /**
     * @brief Expected counters header, identifying a single hiptrace counters
     *        dump
     */
    public static final String HIPTRACE_COUNTERS_NAME = "hiptrace_counters"; //$NON-NLS-1$

    /**
     * @brief Expected global file header, identifying a hiptrace counters
     *        cluster
     */
    public static final String HIPTRACE_MANAGED_NAME = "hiptrace_managed"; //$NON-NLS-1$

    /**
     * @brief Expected events header
     */
    public static final String HIPTRACE_EVENTS_NAME = "hiptrace_events"; //$NON-NLS-1$

    /**
     * @brief Trace buffered read size
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * @brief Unary constructor
     */
    public HipTrace() {
        super();
    }

    @Override
    public IStatus validate(IProject project, String path) {
        File f = new File(path);
        if (!f.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File does not exist"); //$NON-NLS-1$
        }

        if (!f.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Path is not a file"); //$NON-NLS-1$
        }

        if (!parseHeader(f)) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not parse trace header"); //$NON-NLS-1$
        }

        long traceSize;
        try {
            traceSize = Files.size(Path.of(path));
        } catch (IOException e) { // Should not happen
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not query trace size"); //$NON-NLS-1$
        }

        if ((traceSize - fOffset) != instrSize * sizeofCounter) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Wrong data size, different from header"); //$NON-NLS-1$
        }

        configuration = KernelConfiguration.deserialize(Path.of(path + ".json")); //$NON-NLS-1$

        return new TraceValidationStatus(100, Activator.PLUGIN_ID);
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fFile = new File(path);
        fSize = fFile.length();

        if (!parseHeader(fFile)) {
            throw new TmfTraceException("Invalid trace header"); //$NON-NLS-1$
        }

        configuration = KernelConfiguration.deserialize(Path.of(path + ".json")); //$NON-NLS-1$

        try {
            stream = new FileInputStream(fFile);
            fFileChannel = stream.getChannel();
        } catch (IOException e) {
            throw new TmfTraceException("Could not create reading channel"); //$NON-NLS-1$
        }

        fCurrent = new TmfLongLocation(0L);
    }

    @Override
    public synchronized ITmfEvent getNext(ITmfContext context) {
        TmfEvent event = null;
        long pos = context.getRank();

        if (pos < instrSize) {
            try {
                if (fMappedByteBuffer.position() + sizeofCounter > fMappedByteBuffer.limit()) {
                    seek(pos);
                }

                long counter = 0;
                for (int i = 0; i < sizeofCounter; ++i) {
                    byte b = fMappedByteBuffer.get();
                    counter += b << (i * 8);
                }

                long bblock = pos % configuration.bblocks;
                long thread = pos % (configuration.bblocks * configuration.geometry.threads.x);
                long block = pos % (configuration.bblocks * configuration.geometry.threads.x * configuration.geometry.blocks.x);

                final TmfEventField[] fields = {
                        new TmfEventField("counter", counter, null), //$NON-NLS-1$
                        new TmfEventField("bblock", bblock, null), //$NON-NLS-1$
                        new TmfEventField("thread", thread, null), //$NON-NLS-1$
                        new TmfEventField("block", block, null) //$NON-NLS-1$
                };

                final TmfEventField content = new TmfEventField(
                        ITmfEventField.ROOT_FIELD_ID, null, fields);

                event = new TmfEvent(this, pos, TmfTimestamp.fromNanos(roctracerEnd), new TmfEventType(HIPTRACE_COUNTERS_NAME, content), content);

            } catch (IOException e) {
            }
        }

        if (event != null) {
            updateAttributes(context, event);
            context.setLocation(getCurrentLocation());
            context.increaseRank();
            fCurrentEvent = event;
        }

        return event;
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return fCurrent;
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        TmfLongLocation loc = (TmfLongLocation) location;
        return loc.getLocationInfo().doubleValue() / instrSize;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        TmfLongLocation newLoc = (TmfLongLocation) location;
        if (location == null) {
            newLoc = new TmfLongLocation(0L);
        }

        try {
            seek(newLoc.getLocationInfo());
        } catch (IOException e) {
        }

        fCurrent = newLoc;

        return new TmfContext(newLoc, newLoc.getLocationInfo());
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        TmfLongLocation loc = new TmfLongLocation((long) ratio * instrSize);

        return seekEvent(loc);
    }

    @Override
    public int size() {
        return (int) fSize;
    }

    @Override
    public int progress() {
        return fCurrent.getLocationInfo().intValue();
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        return fCurrentEvent;
    }

    // ----- GPU Counters specific methods ----- //

    private boolean parseHeader(File f) {
        String header = new String();
        try (BufferedReader br = new BufferedReader(new FileReader(f));) {
            header = br.readLine();
        } catch (IOException e) {
            return false;
        }

        if (header == null) {
            return false;
        }

        fOffset = header.length() + 1;

        String[] tokens = header.split(",", COUNTER_HEADER_TOKENS); //$NON-NLS-1$

        if (tokens.length == 1 && tokens[0].equals(HIPTRACE_MANAGED_NAME)) {
            // Trace created by hip::HipTraceManager
            managed = true;
            return true;
        } else if (tokens.length == COUNTER_HEADER_TOKENS && tokens[0].equals(HIPTRACE_COUNTERS_NAME)) {
            // Single kernel execution, create by hip::Instrumenter::dumpBin()
            managed = false;
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
                roctracerBegin = Long.parseLong(tokens[4]);
            } catch (NumberFormatException e) {
                return false;
            }

            try {
                roctracerEnd = Long.parseLong(tokens[5]);
            } catch (NumberFormatException e) {
                return false;
            }

            try {
                sizeofCounter = Short.parseShort(tokens[6]);
            } catch (NumberFormatException e) {
                return false;
            }

            return true;
        } else {
            return false;
        }

    }

    private void seek(long rank) throws IOException {
        final long position = fOffset + rank * sizeofCounter;
        int size = Math.min((int) (fFileChannel.size() - position), BUFFER_SIZE);
        fMappedByteBuffer = fFileChannel.map(MapMode.READ_ONLY, position, size);
    }

    @Override
    public synchronized void dispose() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    // ----- Getters ----- //

    /**
     * @return Kernel launch configuration
     */
    public KernelConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return Kernel name
     */
    public String getKernelName() {
        return kernelName;
    }

    /**
     * @return Chrono time stamp
     */
    public long getStamp() {
        return stamp;
    }

    /**
     * @return Roctracer timestamp before kernel launch
     */
    public long getRoctracerBegin() {
        return roctracerBegin;
    }

    /**
     * @return Roctracer timestamp after kernel completion
     */
    public long getRoctracerEnd() {
        return roctracerEnd;
    }

}
