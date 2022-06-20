/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.tests;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.eclipse.tracecompass.common.core.TraceCompassActivator;
import org.eclipse.tracecompass.incubator.gpu.core.trace.KernelConfiguration;
import org.eclipse.tracecompass.incubator.gpu.core.trace.KernelConfigurationGson;
import org.eclipse.tracecompass.incubator.internal.gpu.core.Activator;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class KernelConfigurationSerializationTest {

    private static final String JSON_PATH = "testfiles/hiptrace.json";

    @Test
    public void serializeKernelConfiguration() {
        KernelConfigurationGson conf = new KernelConfigurationGson();

        conf.kernelName = new String("kernel");
        conf.bblocks = 2;
        conf.blocks = new KernelConfigurationGson.Dim3(1, 1, 1);
        conf.threads = new KernelConfigurationGson.Dim3(1, 1, 1);

        System.out.println(conf.serialize());
    }

    @Test
    public void deserialializeKernelConfiguration() {
        Path jsonPath = (Path) ActivatorTest.getAbsoluteFilePath(JSON_PATH);

        KernelConfigurationGson conf = KernelConfigurationGson.deserialize(jsonPath);
        System.out.println(conf.serialize());
    }

    @Test
    public void compareDeserializedKernel() {
        Path jsonPath = (Path) ActivatorTest.getAbsoluteFilePath(JSON_PATH);

        KernelConfigurationGson conf = KernelConfigurationGson.deserialize(jsonPath);

        // Based on the content of the file

        assertEquals("Kernel name ", conf.kernelName, "matrixSquare");
        assertEquals("Basic blocks ", conf.bblocks, 2);
        assertEquals("Threads ", conf.threads, new KernelConfigurationGson.Dim3(64, 1, 1));
        assertEquals("Blocks ", conf.blocks, new KernelConfigurationGson.Dim3(131072, 1, 1));
    }
}
