package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuInfo;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.SeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

public class RooflineXYModel implements ITmfXyModel {
    private final List<GpuInfo.ComputeRoof> computeRoofs;
    private final List<GpuInfo.MemoryRoof> memoryRoofs;
    private final Collection<Point> executionTimes;
    private String title;

    public static final double ROOFLINE_X_MIN = 0.083333;
    public static final double ROOFLINE_X_MAX = 16;
    public static final double ROOFLINE_Y_MIN = 0.5;
    public static final double ROOFLINE_Y_MAX = 128;
    private static final double FLOPS_TO_GFLOPS = 1e-9;

    public static final String ROOFLINE_XAXIS_NAME = "Operational Intensity (Flops/Byte)"; //$NON-NLS-1$
    private static final String ROOFLINE_XAXIS_UNIT = ""; //$NON-NLS-1$
    public static final String ROOFLINE_YAXIS_NAME = "Attainable Performance (GFlops/s)"; //$NON-NLS-1$
    private static final String ROOFLINE_YAXIS_UNIT = ""; //$NON-NLS-1$

    private static final long FIXED_POINT_MULTIPLIER = 1L << 32L;

    public static class Point {
        public double x, y;

        public Point(double _x, double _y) {
            x = _x;
            y = _y;
        }
    }

    public RooflineXYModel(String _title, GpuInfo gpuInfo, Collection<Point> _executionTimes) {
        title = _title;
        computeRoofs = gpuInfo.compute_roofs;
        memoryRoofs = gpuInfo.memory_roofs;
        executionTimes = _executionTimes;
    }

    public List<GpuInfo.ComputeRoof> getComputeRoofs() {
        return computeRoofs;
    }

    public List<GpuInfo.MemoryRoof> getMemoryRoofs() {
        return memoryRoofs;
    }

    @Override
    public @Nullable String getTitle() {
        return title;
    }

    @Override
    public @NonNull Collection<@NonNull ISeriesModel> getSeriesData() {
        List<@NonNull ISeriesModel> series = new ArrayList<>();
        // Generate roofline series

        GpuInfo.ComputeRoof bestCompute = getMaxRoof(computeRoofs);
        GpuInfo.MemoryRoof bestMemory = getMaxRoof(memoryRoofs);

        long id = 0;

        // Memory roofs (slanted)

        for (GpuInfo.MemoryRoof it : memoryRoofs) {
            // Why are x values long and not double ????

            long[] xValues = { toFixedPoint(ROOFLINE_X_MIN), toFixedPoint(bestCompute.peak_flops_s / it.peak_bandwidth) }; // TODO

            double[] yValues = { fromFixedPoint(xValues[0]) * it.peak_bandwidth * FLOPS_TO_GFLOPS,
                    bestCompute.peak_flops_s * FLOPS_TO_GFLOPS };

            series.add(new SeriesModel.SeriesModelBuilder(id, it.name, xValues, yValues)
                    .xAxisDescription(new TmfXYAxisDescription(ROOFLINE_XAXIS_NAME, ROOFLINE_XAXIS_UNIT, DataType.BINARY_NUMBER))
                    .yAxisDescription(new TmfXYAxisDescription(ROOFLINE_YAXIS_NAME, ROOFLINE_YAXIS_UNIT))
                    .seriesDisplayType(DisplayType.LINE)
                    .build());
            ++id;
        }

        // Compute roofs (straight)

        for (GpuInfo.ComputeRoof it : computeRoofs) {
            // Why are x values long and not double ????

            long[] xValues = { toFixedPoint(it.peak_flops_s / bestMemory.peak_bandwidth), toFixedPoint(ROOFLINE_X_MAX) };
            // TODO figure out a way to properly pass non-integer values ?

            double[] yValues = { it.peak_flops_s * FLOPS_TO_GFLOPS, it.peak_flops_s * FLOPS_TO_GFLOPS };

            series.add(new SeriesModel.SeriesModelBuilder(id, it.name, xValues, yValues)
                    .xAxisDescription(new TmfXYAxisDescription(ROOFLINE_XAXIS_NAME, ROOFLINE_XAXIS_UNIT, DataType.BINARY_NUMBER))
                    .yAxisDescription(new TmfXYAxisDescription(ROOFLINE_YAXIS_NAME, ROOFLINE_YAXIS_UNIT))
                    .seriesDisplayType(DisplayType.LINE)
                    .build());

            ++id;
        }

        // Add roofline points

        long[] pointsXValues = new long[executionTimes.size()];
        double[] pointsYValues = new double[executionTimes.size()];

        int i = 0;
        for (Point p : executionTimes) {
            pointsXValues[i] = toFixedPoint(p.x);
            pointsYValues[i] = p.y * FLOPS_TO_GFLOPS;
            ++i;
        }

        series.add(new SeriesModel.SeriesModelBuilder(id, "Roofline", pointsXValues, pointsYValues) //$NON-NLS-1$
                .seriesDisplayType(DisplayType.SCATTER)
                .xAxisDescription(new TmfXYAxisDescription(ROOFLINE_XAXIS_NAME, ROOFLINE_XAXIS_UNIT, DataType.BINARY_NUMBER))
                .yAxisDescription(new TmfXYAxisDescription(ROOFLINE_YAXIS_NAME, ROOFLINE_YAXIS_UNIT))
                .build());

        ++id;

        return series;
    }

    private static <T extends GpuInfo.IRoof> T getMaxRoof(List<T> roofs) {
        // Uses generics for type safety

        T max = roofs.get(0);

        for (T it : roofs) {
            if (it.getRoof() > max.getRoof()) {
                max = it;
            }
        }

        return max;
    }

    /**
     * @param value
     *            Value to be converted
     * @return Corresponding double value
     */
    public static double fromFixedPoint(long value) {
        return ((double) value) / FIXED_POINT_MULTIPLIER;
    }

    /**
     * @param value
     *            Value to be converted
     * @return Corresponding fixed point value
     */
    public static long toFixedPoint(double value) {
        return (long) (value * FIXED_POINT_MULTIPLIER);
    }

    /**
     * @brief Converts an array of fixed point values to their double floating
     *        point equivalent
     *
     * @param values
     *            Values to be converted
     * @return Converted array
     */
    public static double[] fromFixedPointArray(long[] values) {
        double[] array = new double[values.length];
        for (int i = 0; i < values.length; ++i) {
            array[i] = fromFixedPoint(values[i]);
        }
        return array;
    }

}
