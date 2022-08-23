package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class HipAnalyzerEvent {

    /**
     * @brief Base, unspecialized event
     */
    public static class BaseEvent {
        /**
         * Event name (type)
         */
        protected String name;
        /**
         * Parsed events from the binary trace format
         */
        protected List<HipTrace.Event> events;
        /**
         * Corresponding events header
         */
        protected HipTrace.EventsHeader header;
        /**
         * Event rank in the trace
         */
        protected long rank;
        /**
         * Corresponding trace (HipTrace)
         */
        protected ITmfTrace trace;
        /**
         * Event id in the trace
         */
        protected long eventOffset;

        /**
         * @brief Event name
         * @return Default event name
         */
        public static @NonNull String name() {
            return "hip_event"; //$NON-NLS-1$
        }

        /**
         * @param trace
         *            Trace from which the event is coming
         * @param rank
         *            Event rank in the trace
         * @param eventOffset
         *            Id of the event in the events dump
         * @param header
         *            Events header
         * @param events
         *            Payload
         */
        public BaseEvent(ITmfTrace trace, long rank, long eventOffset, HipTrace.EventsHeader header, List<HipTrace.Event> events) {
            this.name = header.eventName;
            this.header = header;
            this.events = events;
            this.rank = rank;
            this.eventOffset = eventOffset;
            this.trace = trace;
        }

        /**
         * @return Converts to TmfEvent, with relevant fields. To be overloaded.
         */
        public ITmfEvent toEvent() {
            final TmfEventField[] eventsFields = {
                    new TmfEventField("type", name, null), //$NON-NLS-1$
                    new TmfEventField("producer_id", null, header.geometryOf(eventOffset)), //$NON-NLS-1$
                    new TmfEventField("data", events, null) //$NON-NLS-1$
            };

            final TmfEventField root = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, eventsFields);

            return new TmfEvent(trace, rank, TmfTimestamp.fromNanos(header.counters.roctracerEnd), new TmfEventType(name(), root), root);
        }
    }

    /**
     * @brief Implements HipAnalyzer's hip::Event
     */
    public static class HipEvent extends BaseEvent {
        /**
         * @brief Event name
         * @return HipEvent name
         */
        public static @NonNull String name() {
            return "hip::Event"; //$NON-NLS-1$
        }

        /**
         * @param trace
         *            Trace from which the event is coming
         * @param rank
         *            Event rank in the trace
         * @param eventOffset
         *            Id of the event in the events dump
         * @param header
         *            Events header
         * @param events
         *            Payload
         */
        public HipEvent(ITmfTrace trace, long rank, long eventOffset, HipTrace.EventsHeader header, List<HipTrace.Event> events) {
            super(trace, rank, eventOffset, header, events);
        }

        @Override
        public ITmfEvent toEvent() {
            final TmfEventField[] eventsFields = {
                    new TmfEventField("type", name(), null), //$NON-NLS-1$
                    new TmfEventField("producer_id", null, header.geometryOf(eventOffset)), //$NON-NLS-1$
                    new TmfEventField("bb", (long) events.get(0).value, null) //$NON-NLS-1$
            };

            final TmfEventField root = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, eventsFields);

            return new TmfEvent(trace, rank, TmfTimestamp.fromNanos(header.counters.roctracerEnd), new TmfEventType(name(), root), root);
        }
    }

    /**
     * @brief Implements HipAnalyzer's hip::TaggedEvent
     */
    public static class HipTaggedEvent extends BaseEvent {
        /**
         * @brief Event name
         * @return Default event name
         */
        public static @NonNull String name() {
            return "hip::TaggedEvent"; //$NON-NLS-1$
        }

        /**
         * @param trace
         *            Trace from which the event is coming
         * @param rank
         *            Event rank in the trace
         * @param eventOffset
         *            Id of the event in the events dump
         * @param header
         *            Events header
         * @param events
         *            Payload
         */
        public HipTaggedEvent(ITmfTrace trace, long rank, long eventOffset, HipTrace.EventsHeader header, List<HipTrace.Event> events) {
            super(trace, rank, eventOffset, header, events);
        }

        @Override
        public ITmfEvent toEvent() {
            final TmfEventField[] eventsFields = {
                    new TmfEventField("type", name(), null), //$NON-NLS-1$
                    new TmfEventField("producer_id", null, header.geometryOf(eventOffset)), //$NON-NLS-1$
                    new TmfEventField("bb", (long) events.get(0).value, null), //$NON-NLS-1$
                    new TmfEventField("stamp", (long) events.get(1).value, null) //$NON-NLS-1$
            };

            final TmfEventField root = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, eventsFields);

            return new TmfEvent(trace, rank, TmfTimestamp.fromNanos(header.counters.roctracerEnd), new TmfEventType(name(), root), root);
        }
    }

    /**
     * @brief Implements HipAnalyzer's hip::WaveState
     */
    public static class HipWaveState extends BaseEvent {
        /**
         * @brief Event name
         * @return Default event name
         */
        public static @NonNull String name() {
            return "hip::WaveState"; //$NON-NLS-1$
        }

        /**
         * @param trace
         *            Trace from which the event is coming
         * @param rank
         *            Event rank in the trace
         * @param eventOffset
         *            Id of the event in the events dump
         * @param header
         *            Events header
         * @param events
         *            Payload
         */
        public HipWaveState(ITmfTrace trace, long rank, long eventOffset, HipTrace.EventsHeader header, List<HipTrace.Event> events) {
            super(trace, rank, eventOffset, header, events);
        }

        @Override
        public ITmfEvent toEvent() {
            long bb = (long) events.get(0).value;
            long stamp = (long) events.get(1).value;
            long exec = (long) events.get(2).value;
            long hw_id = (long) events.get(3).value;

            @SuppressWarnings("nls")
            final TmfEventField[] eventsFields = {
                    new TmfEventField("type", name(), null),
                    new TmfEventField("producer_id", null, header.geometryOf(eventOffset)),
                    new TmfEventField("bb", bb, null),
                    new TmfEventField("exec", new GcnAsmParser.ExecRegister(exec), null),
                    new TmfEventField("stamp", stamp, null),
                    new TmfEventField("hw_id", hw_id, new GcnAsmParser.HardwareIdRegister(hw_id).toEventFields())
            };

            final TmfEventField root = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, eventsFields);

            return new TmfEvent(trace, rank, TmfTimestamp.fromNanos(header.counters.roctracerEnd), new TmfEventType(name(), root), root);
        }
    }

    /**
     * @param trace
     *            Hip trace
     * @param rank
     *            Rank of the event (for TmfEvent creation)
     * @param eventOffset
     *            Event offset in the kernel events dump
     * @param header
     *            EventsHeader for this kernel launch
     * @param events
     *            Parsed values from the binary trace format
     * @return TmfEvent corresponding to the event
     */
    public static ITmfEvent parse(ITmfTrace trace, long rank, long eventOffset, HipTrace.EventsHeader header, List<HipTrace.Event> events) {
        switch (header.eventName) {
        case "hip::Event": //$NON-NLS-1$
            if (events.size() != 1) {
                return new BaseEvent(trace, rank, eventOffset, header, events).toEvent();
            }
            return new HipEvent(trace, rank, eventOffset, header, events).toEvent();
        case "hip::TaggedEvent": //$NON-NLS-1$
            if (events.size() != 2) {
                return new BaseEvent(trace, rank, eventOffset, header, events).toEvent();
            }
            return new HipTaggedEvent(trace, rank, eventOffset, header, events).toEvent();
        case "hip::WaveState": //$NON-NLS-1$
            if (events.size() != 4) {
                return new BaseEvent(trace, rank, eventOffset, header, events).toEvent();
            }
            return new HipWaveState(trace, rank, eventOffset, header, events).toEvent();
        default:
            return new BaseEvent(trace, rank, eventOffset, header, events).toEvent();
        }
    }
}
