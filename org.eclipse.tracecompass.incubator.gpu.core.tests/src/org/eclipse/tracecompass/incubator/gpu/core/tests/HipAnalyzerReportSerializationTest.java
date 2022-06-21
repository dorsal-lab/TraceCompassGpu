/**
 *
 */
package org.eclipse.tracecompass.incubator.gpu.core.tests;

import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;

import org.eclipse.tracecompass.incubator.gpu.core.trace.HipAnalyzerReport;
import org.junit.Test;

/**
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class HipAnalyzerReportSerializationTest {

    private static final String EXPECTED_JSON = "[{\"id\":0,\"clang_id\":2,\"begin\":\"/home/sebastien/hip/hipcalc/test/mat_square_instr.cpp:27:13\",\"end\":\"/home/sebastien/hip/hipcalc/test/mat_square_instr.cpp:27:39\",\"flops\": 0,\"floating_ld\":0,\"floating_st\":0},{\"id\":1,\"clang_id\":5,\"begin\":\"/home/sebastien/hip/hipcalc/test/mat_square_instr.cpp:18:5\",\"end\":\"/home/sebastien/hip/hipcalc/test/mat_square_instr.cpp:25:25\",\"flops\": 1,\"floating_ld\":0,\"floating_st\":0}]\n";

    @Test
    public void deserializeAndBack() {
        HipAnalyzerReport report = HipAnalyzerReport.deserialize(EXPECTED_JSON);

        System.out.println(report.serialize());


        assertEquals("Serialized json", JsonParser.parseString(EXPECTED_JSON), JsonParser.parseString(report.serialize()));
    }
}
