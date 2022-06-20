/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.tests;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.eclipse.tracecompass.incubator.gpu.core.trace.KernelConfiguration;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class KernelConfigurationSerializationTest {

    private static final String JSON_PATH = "testfiles/hiptrace.json";

    @Test
    public void serializeKernelConfiguration() {
        KernelConfiguration conf = new KernelConfiguration();

        conf.kernelName = new String("kernel");
        conf.bblocks = 2;
        conf.blocks = new KernelConfiguration.Dim3(1, 1, 1);
        conf.threads = new KernelConfiguration.Dim3(1, 1, 1);

        System.out.println(conf.serialize());
    }

    @Test
    public void deserialializeKernelConfiguration() {
        Path jsonPath = (Path) ActivatorTest.getAbsoluteFilePath(JSON_PATH);

        KernelConfiguration conf = KernelConfiguration.deserialize(jsonPath);
        System.out.println(conf.serialize());
    }

    @Test
    public void compareDeserializedKernel() {
        Path jsonPath = (Path) ActivatorTest.getAbsoluteFilePath(JSON_PATH);

        KernelConfiguration conf = KernelConfiguration.deserialize(jsonPath);

        // Based on the content of the file

        assertEquals("Kernel name ", conf.kernelName, "matrixSquare");
        assertEquals("Basic blocks ", conf.bblocks, 2);
        assertEquals("Threads ", conf.threads, new KernelConfiguration.Dim3(64, 1, 1));
        assertEquals("Blocks ", conf.blocks, new KernelConfiguration.Dim3(131072, 1, 1));
    }
}
