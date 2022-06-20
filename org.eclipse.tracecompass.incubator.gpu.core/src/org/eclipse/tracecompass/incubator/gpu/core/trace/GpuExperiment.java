/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.gpu.core.trace;

import java.util.Collections;
import java.util.Set;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * @class GpuExperiment
 * @brief Trace
 *
 * @author Sébastien Darche <sebastien.darche@polymtl.ca>
 *
 */
public class GpuExperiment extends TmfExperiment {

    /**
     * @brief Default constructor
     */
    public GpuExperiment() {
        this("", Collections.EMPTY_SET); //$NON-NLS-1$
    }

    /**
     * @param id
     *            Experiment id
     * @param traces
     *            Set of traces part of the experiment
     */
    public GpuExperiment(String id, Set<ITmfTrace> traces) {
        super(ITmfEvent.class, id, traces.toArray(new ITmfTrace[traces.size()]), TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);
    }

}
