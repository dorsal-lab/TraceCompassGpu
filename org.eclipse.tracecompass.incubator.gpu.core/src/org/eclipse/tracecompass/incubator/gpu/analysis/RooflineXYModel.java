package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.GpuInfo;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.SeriesModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

public class RooflineXYModel implements ITmfXyModel {
    private final List<GpuInfo.ComputeRoof> computeRoofs;
    private final List<GpuInfo.MemoryRoof> memoryRoofs;
    private final Collection<Point> executionTimes;
    private String title;

    public static final double ROOFLINE_X_MIN = 0.5;
    public static final double ROOFLINE_X_MAX = 16;
    public static final double ROOFLINE_Y_MIN = 0.5;
    public static final double ROOFLINE_Y_MAX = 128;

    private static final String ROOFLINE_XAXIS_NAME = "Operational Intensity"; //$NON-NLS-1$
    private static final String ROOFLINE_XAXIS_UNIT = "Flops/Byte"; //$NON-NLS-1$
    private static final String ROOFLINE_YAXIS_NAME = "Attainable GFlops/s"; //$NON-NLS-1$
    private static final String ROOFLINE_YAXIS_UNIT = ""; //$NON-NLS-1$

    private static final long FIXED_POINT_MULTIPLIER = 1L << 32L;

    public static class Point {
        public double x, y;
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

        for (GpuInfo.MemoryRoof it : memoryRoofs) {
            // Why are x values long and not double ????

            long[] xValues = { toFixedPoint(ROOFLINE_Y_MIN / it.peak_bandwidth), toFixedPoint(bestCompute.peak_flops_s / it.peak_bandwidth) }; // TODO

            double[] yValues = { ROOFLINE_X_MIN / it.peak_bandwidth, bestCompute.peak_flops_s };

            series.add(new SeriesModel.SeriesModelBuilder(id, it.name, xValues, yValues)
                    .xAxisDescription(new TmfXYAxisDescription(ROOFLINE_XAXIS_NAME, ROOFLINE_XAXIS_UNIT, DataType.BINARY_NUMBER))
                    .yAxisDescription(new TmfXYAxisDescription(ROOFLINE_YAXIS_NAME, ROOFLINE_YAXIS_UNIT))
                    .seriesDisplayType(DisplayType.LINE)
                    .build());
        }

        for (GpuInfo.ComputeRoof it : computeRoofs) {
            // Why are x values long and not double ????

            long[] xValues = { toFixedPoint(it.peak_flops_s / bestMemory.peak_bandwidth), toFixedPoint(ROOFLINE_X_MAX) };
            // TODO figure out a way to properly pass non-integer values ?

            double[] yValues = { it.peak_flops_s, it.peak_flops_s };

            series.add(new SeriesModel.SeriesModelBuilder(id, it.name, xValues, yValues)
                    .xAxisDescription(new TmfXYAxisDescription(ROOFLINE_XAXIS_NAME, ROOFLINE_XAXIS_UNIT, DataType.BINARY_NUMBER))
                    .yAxisDescription(new TmfXYAxisDescription(ROOFLINE_YAXIS_NAME, ROOFLINE_YAXIS_UNIT))
                    .seriesDisplayType(DisplayType.LINE)
                    .build());
        }

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

    public static double fromFixedPoint(long value) {
        return ((double) value) / FIXED_POINT_MULTIPLIER;
    }

    public static long toFixedPoint(double value) {
        return (long) (value * FIXED_POINT_MULTIPLIER);
    }

}
