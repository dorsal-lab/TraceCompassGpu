package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;
import java.io.FileInputStream;
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

    private static final long WAVE_SIZE = 64;

    /**
     * @brief Number of expected tokens in header, with the kernel info
     */
    private static final int COUNTER_HEADER_TOKENS = 15;

    /**
     * @brief Number of expected tokens in header, without kernel info
     */
    private static final int COUNTER_HEADER_TOKENS_NO_KI = 7;

    /**
     * @brief Minimum number of expected tokens in events header
     */
    private static final int HIPTRACE_EVENTS_HEADER_MIN_TOKENS = 7;

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
     * @brief Begin event fields description
     */
    public static final String HIPTRACE_EVENTS_FIELDS = "begin_fields"; //$NON-NLS-1$

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

    public static class CountersHeader {
        public final String kernelName;
        public final long numCounters;
        public final long stamp;
        public final long roctracerBegin;
        public final long roctracerEnd;
        public final long sizeofCounter;
        public final KernelConfiguration configuration;
        public final String str;

        public CountersHeader(String kernelName, long numCounters, long stamp, long roctracerBegin, long roctracerEnd, long sizeofCounter, KernelConfiguration configuration, String str) {
            this.kernelName = kernelName;
            this.numCounters = numCounters;
            this.stamp = stamp;
            this.roctracerBegin = roctracerBegin;
            this.roctracerEnd = roctracerEnd;
            this.sizeofCounter = sizeofCounter;
            this.configuration = configuration;
            this.str = str;
        }

        public long totalSize() {
            return numCounters * sizeofCounter;
        }
    }

    public static class Event {
        public String type;
        public Object value;

        public Event(String type, Object value) {
            this.type = type;
            this.value = value;

        }

        @Override
        public String toString() {
            return type + " : " + value.toString(); //$NON-NLS-1$
        }
    }

    public static class EventsHeader {
        public static class Field {
            public String type;
            public long size;

            /**
             * @param type
             *            Type name, following the Itanium ABI naming convention
             * @param size
             *            Type size, in bytes
             */
            public Field(String type, long size) {
                this.type = type;
                this.size = size;
            }
        }

        private static long SIZEOF_SIZE_T = 8L; // 8 bytes for a size_t

        public final long eventSize;
        public final List<Field> fields;
        public final long numOffsets;
        public final String eventName;
        public CountersHeader counters;
        public List<Long> offsets;
        public final long headerPos;

        public EventsHeader(long headerPos, long eventSize, List<Field> fields, long numOffsets, String eventName) {
            this.headerPos = headerPos;
            this.eventSize = eventSize;
            this.numOffsets = numOffsets + 1; // Accounting for the last offset
                                              // which is the end position
            this.fields = fields;
            this.eventName = eventName;
        }

        /**
         * @return Total number of events
         */
        public long numEvents() {
            return offsets.get(offsets.size() - 1);
        }

        public long offsetsSize() {
            return numOffsets * SIZEOF_SIZE_T;
        }

        public long eventsSize() {
            return numEvents() * eventSize;
        }

        /**
         * @return Total size of the hiptrace_events
         */
        public long totalSize() {
            return offsetsSize() + eventsSize();
        }

        /**
         * @param offset
         *            An offset in the map
         * @return Corresponding global thread id or wave id (depending on the
         *         type of queue)
         */
        public long idOf(long offset) {
            long varOffset = offset / eventSize;
            for (int i = 0; i < offsets.size(); ++i) {
                if (offsets.get(i) > varOffset) {
                    return (long) i - 1;
                }

            }
            return -1;
        }

        public boolean isThread() {
            long numThreads = counters.configuration.totalThreads();
            return numThreads == (numOffsets - 1);
        }

        public TmfEventField[] geometryOf(long offset) {
            long id = idOf(offset);

            KernelConfiguration.Geometry geometry = counters.configuration.geometry;

            if (isThread()) {
                // Here id = global thread id
                long block = id / geometry.threads.total();
                long thread = id % geometry.threads.total();

                final TmfEventField[] geom = {
                        new TmfEventField("block", block, null), //$NON-NLS-1$
                        new TmfEventField("thread", thread, null) //$NON-NLS-1$
                };

                return geom;
            }

            // Wave
            // Here id = global wave id
            long blockDim = geometry.threads.total();
            long wavePerBlock = blockDim / WAVE_SIZE;
            if (blockDim % WAVE_SIZE != 0) {
                wavePerBlock += 1;
            }

            long block = id / wavePerBlock;
            long waveInBlock = id % wavePerBlock;

            final TmfEventField[] geom = {
                    new TmfEventField("block", block, null), //$NON-NLS-1$
                    new TmfEventField("wave", waveInBlock, null) //$NON-NLS-1$
            };

            return geom;
        }

        public void parseOffsets(File f) {
            offsets = new ArrayList<>();
            try(FileInputStream stream = new FileInputStream(f);) {

                FileChannel fileChannel = stream.getChannel();

                MappedByteBuffer mappedByteBuffer = fileChannel.map(MapMode.READ_ONLY, headerPos, numOffsets * SIZEOF_SIZE_T);

                for (long i = 0L; i < numOffsets; ++i) {
                    long offset = 0;
                    for (int b = 0; b < SIZEOF_SIZE_T; ++b) {
                        int unsignedValue = mappedByteBuffer.get() & 0xFF;
                        offset += unsignedValue << (b * 8);
                    }

                    offsets.add(offset);
                }

            } catch (IOException e) {

            }
        }
    }

    private static class TraceLocation {
        public long offset;
        public Object header;

        public TraceLocation(long offset, Object header) {
            this.offset = offset;
            this.header = header;
        }
    }

    private static String getLine(FileInputStream stream) throws IOException {
        List<Byte> bytes = new ArrayList<>();
        int read = stream.read();
        while (read != '\n') {
            bytes.add((byte) read);
            read = stream.read();
        }

        byte[] unboxed = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); ++i) {
            unboxed[i] = bytes.get(i);
        }

        return new String(unboxed, StandardCharsets.US_ASCII);
    }

    /**
     * @brief The offset map is filled when initializing the trace, by reading
     *        every header and storing the offset of each new event. The key is
     *        the rank of the event, and the value represents an offset in the
     *        file and a header corresponding to the event
     */
    private Map<Long, TraceLocation> offsetsMap;

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

        // Read header*
        TraceLocation loc = offsetsMap.get(rank);
        if (loc != null) {
            long offset = loc.offset;

            Object parsedHeader = loc.header;

            if (parsedHeader instanceof CountersHeader) {
                CountersHeader countersHeader = (CountersHeader) parsedHeader;
                long dataOffset = offset + countersHeader.str.length() + 1;
                event = parseCountersEvent(countersHeader, dataOffset, rank);
            } else if (parsedHeader instanceof EventsHeader) {
                EventsHeader eventsHeader = (EventsHeader) parsedHeader;
                event = parseEventsEvent(eventsHeader, offset, rank);
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
        List<Long> counters = new ArrayList<Long>((int) header.numCounters) {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return "<counters>"; //$NON-NLS-1$
            }
        };

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

    private TmfEvent parseEventsEvent(EventsHeader header, long offset, long rank) {
        try {
            seek(offset);
        } catch (IOException e) {
            return null;
        }

        List<Event> data = new ArrayList<>();

        long pos = offset;

        for (EventsHeader.Field f : header.fields) {
            try {
                if (fMappedByteBuffer.position() + f.size > fMappedByteBuffer.limit()) {
                    seek(pos);
                }
            } catch (IOException e) {
                return null;
            }

            ArrayList<Byte> bytes = new ArrayList<>();

            for (int index = 0; index < f.size; ++index) {
                bytes.add(fMappedByteBuffer.get());
            }

            // Attempt to convert to the appropriate type
            Object value = ItaniumABIParser.deserializeVariable(f.type, f.size, bytes);

            data.add(new Event(ItaniumABIParser.demangleType(f.type), value));

            pos += f.size;

        }

        long eventOffset = offset - header.headerPos - header.offsetsSize();

        return (TmfEvent) HipAnalyzerEvent.parse(this, rank, eventOffset, header, data);
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
        try (FileInputStream iStream = new FileInputStream(f);) {
            header = getLine(iStream);
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
        return new CountersHeader(kernelName, instrSize, stamp, roctracerBegin, roctracerEnd, sizeofCounter, configuration, header);

    }

    static private @Nullable EventsHeader parseEventsHeader(String header, long offset) {
        long eventSize;
        long totalSize;
        String eventName;
        List<EventsHeader.Field> fields = new ArrayList<>();

        String[] tokens = header.split(","); //$NON-NLS-1$

        if (tokens.length < HIPTRACE_EVENTS_HEADER_MIN_TOKENS) {
            return null;
        }
        try {
            eventSize = Long.parseLong(tokens[1]);
            totalSize = Long.parseLong(tokens[2]);
            eventName = tokens[3];

            if (!tokens[4].equals("begin_fields")) { //$NON-NLS-1$
                return null;
            }

            for (int i = 5; i < tokens.length; i += 2) {
                fields.add(new EventsHeader.Field(tokens[i], Long.parseLong(tokens[i + 1])));
            }

        } catch (NumberFormatException e) {
            return null;
        }

        return new EventsHeader(offset + header.length() + 1, eventSize, fields, totalSize, eventName);
    }

    private @Nullable Object readHeader(String header, long offset) {
        String[] tokens = header.split(","); //$NON-NLS-1$

        switch (tokens[0]) {
        case HIPTRACE_COUNTERS_NAME:
            return parseCountersHeader(header);
        case HIPTRACE_EVENTS_NAME:
            return parseEventsHeader(header, offset);
        default:
            return null;
        }
    }

    private boolean initMap() {
        // The offsets of each events (counters or traces) are stored in a
        // hashmap
        offsetsMap = new TreeMap<>();
        CountersHeader lastCounters = null;

        if (!managed) {
            String header = new String();
            try (FileInputStream iStream = new FileInputStream(fFile);) {
                header = getLine(iStream);
            } catch (IOException e) {
                return false;
            }

            CountersHeader parsedHeader = parseCountersHeader(header);
            if (parsedHeader == null) {
                return false;
            }

            offsetsMap.put(0L, new TraceLocation(0L, parsedHeader));

        } else {
            long i = 0L;
            long offset = fOffset;

            do {
                // Need to read multiple headers
                String header = new String();
                try (FileInputStream iStream = new FileInputStream(fFile);) {
                    iStream.skip(offset);
                    header = getLine(iStream);
                } catch (IOException e) {
                    return false;
                }

                if (header == null) {
                    return false;
                }

                Object parsedHeader = readHeader(header, offset);

                if (parsedHeader == null) {
                    return false;
                }

                long nextOffset;

                if (parsedHeader instanceof CountersHeader) {
                    CountersHeader countersHeader = (CountersHeader) parsedHeader;
                    nextOffset = offset + countersHeader.totalSize() + header.length() + 1;

                    offsetsMap.put(i, new TraceLocation(offset, parsedHeader));
                    lastCounters = countersHeader;
                    ++i;

                } else if (parsedHeader instanceof EventsHeader) {
                    EventsHeader eventsHeader = (EventsHeader) parsedHeader;
                    eventsHeader.counters = lastCounters;

                    offset += header.length() + 1;

                    // Now read the offsets

                    eventsHeader.parseOffsets(fFile);

                    offset += eventsHeader.offsetsSize();

                    nextOffset = offset + eventsHeader.eventsSize();

                    // All events have the same header (event type), but a new
                    // entry is created for each (since a new TmfEvent will be
                    // created for each entry)

                    long numEvents = eventsHeader.numEvents();

                    for (int j = 0; j < numEvents; ++j) {
                        offsetsMap.put(i, new TraceLocation(offset + j * eventsHeader.eventSize, parsedHeader));
                        ++i;
                    }

                } else {
                    return false;
                }

                offset = nextOffset;

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
