package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.util.List;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class ItaniumABIParser {
    /**
     * @param mangledType
     *            Mangled name of type according to the Itanium ABI
     *            Specification
     * @param sizeof
     *            Size (in bytes) of the type
     * @param bytes
     *            Serialized data
     *
     * @return A deserialized version of the given bytes, or an unchanged list
     *         of bytes if the type is not recognized. Numeric types are returned as Long
     */
    public static Object deserializeVariable(String mangledType, long sizeof, List<Byte> bytes) {
        switch (mangledType) {
        case "v": //$NON-NLS-1$
            // Void
            return bytes;
        case "b": //$NON-NLS-1$
            // bool
            return (bytes.get(0) != 0);
        case "c": //$NON-NLS-1$
            // char
            byte b = bytes.get(0);
            return (char) b;
        case "s": //$NON-NLS-1$
            // short
        case "t": //$NON-NLS-1$
            // unsigned short
        case "i": //$NON-NLS-1$
            // int
        case "j": //$NON-NLS-1$
            // unsigned int
        case "l": //$NON-NLS-1$
            // unsigned long
        case "m": //$NON-NLS-1$
            // unsigned long
        case "x": //$NON-NLS-1$
            // long long
        case "y": //$NON-NLS-1$
            // unsigned long long
            return accumulate(sizeof, bytes);
        case "f": //$NON-NLS-1$
            // float
            int accumulated_float = (int) accumulate(sizeof, bytes);
            return Float.intBitsToFloat(accumulated_float);
        case "d": //$NON-NLS-1$
            // double
            long accumulated_double = accumulate(sizeof, bytes);
            return Double.longBitsToDouble(accumulated_double);
        default:
            return bytes;
        }
    }

    private static long accumulate(long sizeof, List<Byte> bytes) {
        long val = 0l;

        for(int i = 0; i < sizeof; ++i) {
            val += bytes.get(i) << (8 * i);
        }

        return val;
    }
}
