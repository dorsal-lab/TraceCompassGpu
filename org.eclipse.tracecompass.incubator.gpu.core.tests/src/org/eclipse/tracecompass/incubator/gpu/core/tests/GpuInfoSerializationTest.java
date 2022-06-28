package org.eclipse.tracecompass.incubator.gpu.core.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuInfo;
import org.junit.Test;

import com.google.gson.JsonParser;

public class GpuInfoSerializationTest {
    private static final String EXPECTED_JSON = "{\"name\":\"\",\"memory_roofs\":[{\"name\":\"memory\",\"peak_bandwidth\":0}],\"compute_roofs\":[{\"name\":\"multiply\",\"peak_flops_s\":9.07035e+06},{\"name\":\"add\",\"peak_flops_s\":9.26011e+06},{\"name\":\"fma\",\"peak_flops_s\":5401.64}]}\n";

    @Test
    public void deserializeAndBack() {
        GpuInfo report = GpuInfo.deserialize(EXPECTED_JSON);

        System.out.println(report.serialize());


        assertEquals("Serialized json", JsonParser.parseString(EXPECTED_JSON), JsonParser.parseString(report.serialize()));
    }
}
