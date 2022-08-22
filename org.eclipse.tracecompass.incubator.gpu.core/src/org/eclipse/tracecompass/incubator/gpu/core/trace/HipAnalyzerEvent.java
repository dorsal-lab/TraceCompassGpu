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
        protected String name;
        protected List<HipTrace.Event> events;
        protected HipTrace.EventsHeader header;
        protected long rank;
        protected ITmfTrace trace;
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
         * @return Default event name
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
                    new TmfEventField("bb", (int) events.get(0).value, null) //$NON-NLS-1$
            };

            final TmfEventField root = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, eventsFields);

            return new TmfEvent(trace, rank, TmfTimestamp.fromNanos(header.counters.roctracerEnd), new TmfEventType(name(), root), root);
        }
    }

    @SuppressWarnings("nls")
    public static ITmfEvent parse(ITmfTrace trace, long rank, long eventOffset, HipTrace.EventsHeader header, List<HipTrace.Event> events) {
        switch (header.eventName) {
        case "hip::Event":
            if (events.size() != 1) {
                return new BaseEvent(trace, rank, eventOffset, header, events).toEvent();
            }
            return new HipEvent(trace, rank, eventOffset, header, events).toEvent();

        default:
            return new BaseEvent(trace, rank, eventOffset, header, events).toEvent();
        }
    }
}