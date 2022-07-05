/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @class HipAnalyzerReport
 * @brief Loads a Hip Analyzer compilation report (`hip_analyzer.json`)
 *
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class HipAnalyzerReport {

    /**
     * @class BasicBlock
     * @brief Basic block information report. Not meant to be instantiated by
     *        the user
     *
     */
    public static class BasicBlock {
        // Info

        /**
         * @brief Basic block id (contiguous)
         */
        public int id;

        /**
         * @brief Basic block id from the clang CFG
         */
        public int clang_id;

        /**
         * @brief Begin location
         */
        public String begin;

        /**
         * @brief End location
         */
        public String end;

        // Counters

        /**
         * @brief Number of floating point operations
         */
        public int flops;

        /**
         * @brief Total number of loaded bytes
         */
        public int floating_ld;

        /**
         * @brief Total number of stored bytes
         */
        public int floating_st;
    }

    private static Type blockCollectionType = new TypeToken<Collection<BasicBlock>>() {
    }.getType();

    private List<BasicBlock> blocks = new ArrayList<>();

    /**
     * @return List of basic blocks in the kernel
     */
    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    /**
     * @return Serialized object
     */
    public String serialize() {
        Gson serializer = new Gson();
        return serializer.toJson(blocks);
    }

    /**
     * @param file
     *            JSON file path
     * @return Deserialized report
     */
    public static HipAnalyzerReport deserialize(Path file) {
        if (file == null) {
            return null;
        }

        String json;
        try {
            json = Files.readString(file);
        } catch (IOException e) {
            return null;
        }

        return deserialize(json);
    }

    /**
     * @param json
     *            JSON string
     * @return Deserialized report
     */
    public static HipAnalyzerReport deserialize(String json) {
        Gson gson = new Gson();
        List<BasicBlock> list = gson.fromJson(json, blockCollectionType);
        if (list == null) {
            return null;
        }

        HipAnalyzerReport obj = new HipAnalyzerReport();
        obj.blocks = list;

        return obj;
    }

}
