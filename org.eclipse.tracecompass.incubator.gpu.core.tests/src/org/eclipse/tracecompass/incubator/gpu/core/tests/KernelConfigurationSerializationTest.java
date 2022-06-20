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

    private static final String EXPECTED_JSON = "{\"name\":\"kernel\",\"bblocks\":2,\"geometry\":{\"threads\":{\"x\":1,\"y\":1,\"z\":1},\"blocks\":{\"x\":1,\"y\":1,\"z\":1}}}";

    @Test
    public void serializeKernelConfiguration() {
        KernelConfiguration conf = new KernelConfiguration();

        conf.name = new String("kernel");
        conf.bblocks = 2;
        conf.geometry = new KernelConfiguration.Geometry(
                new KernelConfiguration.Dim3(1, 1, 1),
                new KernelConfiguration.Dim3(1, 1, 1));

        System.out.println(conf.serialize());
        assertEquals("Json output ", conf.serialize(), EXPECTED_JSON);
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

        assertEquals("Kernel name ", conf.name, "matrixSquare");
        assertEquals("Basic blocks ", conf.bblocks, 2);
        assertEquals("Threads ", conf.geometry.threads, new KernelConfiguration.Dim3(64, 1, 1));
        assertEquals("Blocks ", conf.geometry.blocks, new KernelConfiguration.Dim3(131072, 1, 1));
    }
}
