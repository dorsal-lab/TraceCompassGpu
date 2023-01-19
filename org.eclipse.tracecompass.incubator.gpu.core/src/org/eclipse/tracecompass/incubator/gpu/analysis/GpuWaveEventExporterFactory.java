package org.eclipse.tracecompass.incubator.gpu.analysis;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

public class GpuWaveEventExporterFactory implements IDataProviderFactory{

    @SuppressWarnings("nls")
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(GpuBasicBlocksReportDataProvider.ID)
            .setName("GPU Wave event exporter")
            .setDescription("GPU Wave event exporter")
            .setProviderType(ProviderType.TREE_TIME_XY)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        return GpuWaveEventExporter.create(trace);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        GpuWaveStateAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuWaveStateAnalysis.class, GpuWaveStateAnalysis.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.EMPTY_LIST;
    }
}
