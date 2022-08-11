package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
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

    /*
     * private String kernelName; private int instrSize; private long stamp;
     * private long roctracerBegin; private long roctracerEnd; private
     * KernelConfiguration configuration;
     */
    private boolean managed;

    /**
     * @brief The offset map is filled when initializing the trace, by reading
     *        every header and storing the offset of each new event
     */
    private Map<Long, Long> offsetsMap;

    /**
     * @brief Number of expected tokens in header, with the kernel info
     */
    private static final int COUNTER_HEADER_TOKENS = 15;

    /**
     * @brief Number of expected tokens in header, without kernel info
     */
    private static final int COUNTER_HEADER_TOKENS_NO_KI = 7;

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
     * @brief TmfEvent : event root for counters aggregate
     */
    public static final String HIPTRACE_COUNTERS_ROOT = "hiptrace_counters"; //$NON-NLS-1$

    /**
     * @brief TmfEvent : counters event
     */
    public static final String HIPTRACE_COUNTERS_COUNTER = "hiptrace_counter"; //$NON-NLS-1$

    /**
     * @brief Trace buffered read size
     */
    private static final int BUFFER_SIZE = 32768;

    private static class CountersHeader {
        public final String kernelName;
        public final long numCounters;
        public final long stamp;
        public final long roctracerBegin;
        public final long roctracerEnd;
        public final long sizeofCounter;
        public final KernelConfiguration configuration;

        public CountersHeader(String kernelName, long numCounters, long stamp, long roctracerBegin, long roctracerEnd, long sizeofCounter, KernelConfiguration configuration) {
            this.kernelName = kernelName;
            this.numCounters = numCounters;
            this.stamp = stamp;
            this.roctracerBegin = roctracerBegin;
            this.roctracerEnd = roctracerEnd;
            this.sizeofCounter = sizeofCounter;
            this.configuration = configuration;
        }

        public long totalSize() {
            return numCounters * sizeofCounter;
        }
    }

    private static class EventsHeader {
        public final long eventSize;
        public final long totalSize;

        public EventsHeader(long eventSize, long totalSize) {
            this.eventSize = eventSize;
            this.totalSize = totalSize;
        }
    }

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

        if (!parseFileHeader(f)) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not parse trace header"); //$NON-NLS-1$
        }

        /*
         * // As of the new hiptrace_managed version, the size will only be
         * checked on the trace initialization long traceSize; try { traceSize =
         * Files.size(Path.of(path)); } catch (IOException e) { // Should not
         * happen return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
         * "Could not query trace size"); //$NON-NLS-1$ }
         *
         * if ((traceSize - fOffset) != instrSize * sizeofCounter) { return new
         * Status(IStatus.ERROR, Activator.PLUGIN_ID,
         * "Wrong data size, different from header"); //$NON-NLS-1$ }
         */

        // configuration = KernelConfiguration.deserialize(Path.of(path +
        // ".json")); //$NON-NLS-1$

        return new TraceValidationStatus(100, Activator.PLUGIN_ID);
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fFile = new File(path);
        fSize = fFile.length();

        if (!parseFileHeader(fFile)) {
            throw new TmfTraceException("Invalid trace header"); //$NON-NLS-1$
        }

        // configuration = KernelConfiguration.deserialize(Path.of(path +
        // ".json")); //$NON-NLS-1$

        try {
            stream = new FileInputStream(fFile);
            fFileChannel = stream.getChannel();
        } catch (IOException e) {
            throw new TmfTraceException("Could not create reading channel"); //$NON-NLS-1$
        }

        if (!initMap()) {
            throw new TmfTraceException("Could not read trace events offsets"); //$NON-NLS-1$
        }

        fCurrent = new TmfLongLocation(0L);
    }

    @Override
    public synchronized ITmfEvent getNext(ITmfContext context) {
        TmfEvent event = null;
        long rank = context.getRank();

        // Read header
        Long offset = offsetsMap.get(rank);
        if (offset != null) {
            String header = new String();
            try (BufferedReader br = new BufferedReader(new FileReader(fFile));) {
                br.skip(offset);
                header = br.readLine();
            } catch (IOException e) {
                return null;
            }

            if (header == null) {
                return null;
            }

            Object parsedHeader = readHeader(header);

            if (parsedHeader instanceof CountersHeader) {
                long dataOffset = offset + header.length() + 1;
                event = parseCountersEvent((CountersHeader) parsedHeader, dataOffset, rank);
            } else if (parsedHeader instanceof EventsHeader) {
                event = parseEventsHeader((EventsHeader) parsedHeader);
            } else {
                // What to do ?
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

    private TmfEvent parseCountersEvent(CountersHeader header, long offset, long rank) {
        try {
            seek(offset);
        } catch (IOException e) {
            return null;
        }

        final KernelConfiguration configuration = header.configuration;
        List<Long> counters = new ArrayList<>((int) header.numCounters);

        for (long pos = offset; pos < header.totalSize(); pos += header.sizeofCounter) {
            try {
                if (fMappedByteBuffer.position() + header.sizeofCounter > fMappedByteBuffer.limit()) {
                    seek(pos);
                }
            } catch (IOException e) {
                return null;
            }

            long counter = 0;
            for (int i = 0; i < header.sizeofCounter; ++i) {
                byte b = fMappedByteBuffer.get();
                counter += b << (i * 8);
            }

            counters.add(counter);

        }

        final TmfEventField[] countersFields = {
                new TmfEventField("configuration", configuration, null), //$NON-NLS-1$
                new TmfEventField("counters", counters, null), //$NON-NLS-1$
        };

        final TmfEventField root = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, countersFields);

        return new TmfEvent(this, rank, TmfTimestamp.fromNanos(header.roctracerEnd), new TmfEventType(HIPTRACE_COUNTERS_NAME, root), root);
    }

    static private TmfEvent parseEventsHeader(EventsHeader header) {
        return null;
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return fCurrent;
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        TmfLongLocation loc = (TmfLongLocation) location;
        return loc.getLocationInfo().doubleValue() / fSize;
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
        TmfLongLocation loc = new TmfLongLocation((long) ratio * offsetsMap.size());

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

    private boolean parseFileHeader(File f) {
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
        } else if (tokens[0].equals(HIPTRACE_COUNTERS_NAME)) {
            // Single kernel execution, create by hip::Instrumenter::dumpBin()
            managed = false;
            CountersHeader counterHeader = parseCountersHeader(header);

            return counterHeader != null;
        } else {
            return false;
        }

    }

    private @Nullable CountersHeader parseCountersHeader(String header) {
        String kernelName;
        int instrSize;
        long stamp;
        long roctracerBegin;
        long roctracerEnd;
        long sizeofCounter;

        String[] tokens = header.split(",", COUNTER_HEADER_TOKENS); //$NON-NLS-1$
        kernelName = tokens[1];

        try {
            instrSize = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        try {
            stamp = Long.parseLong(tokens[3]);
        } catch (NumberFormatException e) {
            return null;
        }

        try {
            roctracerBegin = Long.parseLong(tokens[4]);
        } catch (NumberFormatException e) {
            return null;
        }

        try {
            roctracerEnd = Long.parseLong(tokens[5]);
        } catch (NumberFormatException e) {
            return null;
        }

        try {
            sizeofCounter = Short.parseShort(tokens[6]);
        } catch (NumberFormatException e) {
            return null;
        }

        KernelConfiguration configuration;
        if (tokens.length == COUNTER_HEADER_TOKENS) {
            // Call configuration embedded in the header
            configuration = KernelConfiguration.deserializeCsv(kernelName, Arrays.copyOfRange(tokens, COUNTER_HEADER_TOKENS_NO_KI, tokens.length));
        } else {
            // The kernel conf file must have the same name as the trace file +
            // .json (legacy version)
            configuration = KernelConfiguration.deserialize(Path.of(fFile.toPath() + ".json")); //$NON-NLS-1$
        }
        return new CountersHeader(kernelName, instrSize, stamp, roctracerBegin, roctracerEnd, sizeofCounter, configuration);

    }

    static private @Nullable EventsHeader parseEventsHeader(String header) {
        // TODO
        return null;
    }

    private @Nullable Object readHeader(String header) {
        String[] tokens = header.split(","); //$NON-NLS-1$

        switch (tokens[0]) {
        case HIPTRACE_COUNTERS_NAME:
            return parseCountersHeader(header);
        case HIPTRACE_EVENTS_NAME:
            return parseEventsHeader(header);
        default:
            return null;
        }
    }

    private boolean initMap() {
        // The offsets of each events (counters or traces) are stored in a
        // hashmap
        offsetsMap = new TreeMap<>();

        if (!managed) {
            offsetsMap.put(0L, 0L);
        } else {
            long i = 0L;
            long offset = fOffset;

            do {
                // Need to read multiple headers
                String header = new String();
                try (BufferedReader br = new BufferedReader(new FileReader(fFile));) {
                    br.skip(offset);
                    header = br.readLine();
                } catch (IOException e) {
                    return false;
                }

                if (header == null) {
                    return false;
                }

                Object parsedHeader = readHeader(header);

                if (parsedHeader == null) {
                    return false;
                }

                if (parsedHeader instanceof CountersHeader) {
                    CountersHeader countersHeader = (CountersHeader) parsedHeader;
                    offset += countersHeader.totalSize() + header.length();
                } else if (parsedHeader instanceof EventsHeader) {
                    EventsHeader eventsHeader = (EventsHeader) parsedHeader;
                    offset += eventsHeader.totalSize + header.length();
                } else {
                    return false;
                }

                offsetsMap.put(i, offset);
                ++i;

            } while (offset < fSize);
        }
        return true;
    }

    /**
     * @brief Loads the file buffer at the given position
     *
     * @param position
     *            expected offset of the mapped file buffer
     * @throws IOException
     */
    private void seek(long position) throws IOException {
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

}
