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
     *         of bytes if the type is not recognized. Numeric types are
     *         returned as Long
     */
    @SuppressWarnings("nls")
    public static Object deserializeVariable(String mangledType, long sizeof, List<Byte> bytes) {
        switch (mangledType) {
        case "v":
            // Void
            return bytes;
        case "b":
            // bool
            return (bytes.get(0) != 0);
        case "c":
            // char
            byte b = bytes.get(0);
            return (char) b;
        // Numeric values
        case "h":
            // unsigned char, in most cases == uint8_t so numeric value
        case "s":
            // short
        case "t":
            // unsigned short
        case "i":
            // int
        case "j":
            // unsigned int
        case "l":
            // unsigned long
        case "m":
            // unsigned long
        case "x":
            // long long
        case "y":
            // unsigned long long
            return accumulate(sizeof, bytes);
        case "f":
            // float
            int accumulated_float = (int) accumulate(sizeof, bytes);
            return Float.intBitsToFloat(accumulated_float);
        case "d":
            // double
            long accumulated_double = accumulate(sizeof, bytes);
            return Double.longBitsToDouble(accumulated_double);
        default:
            return bytes;
        }
    }

    private static long accumulate(long sizeof, List<Byte> bytes) {
        long val = 0l;

        for (int i = 0; i < sizeof; ++i) {
            long unsignedValue = bytes.get(i) & 0xff;
            val += unsignedValue << (8 * i);
        }

        return val;
    }

    /**
     * @param mangled
     *            Mangled type name
     * @return Demangled type name if supported, unchanged otherwise
     */
    @SuppressWarnings("nls")
    public static String demangleType(String mangled) {
        switch (mangled) {
        case "v":
            return "void";
        case "b":
            return "bool";
        case "c":
            return "char";
        case "h":
            return "unsigned char";
        case "s":
            return "short";
        case "t":
            return "unsigned short";
        case "i":
            return "int";
        case "j":
            return "unsigned int";
        case "l":
            return "unsigned long";
        case "m":
            return "unsigned long";
        case "x":
            return "long long";
        case "y":
            return "unsigned long long";
        case "f":
            return "float";
        case "d":
            return "double";
        default:
            return mangled;
        }
    }
}
