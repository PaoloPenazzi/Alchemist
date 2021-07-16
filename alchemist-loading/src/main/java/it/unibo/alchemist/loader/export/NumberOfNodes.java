/*
 * Copyright (C) 2010-2019, Danilo Pianini and contributors listed in the main project's alchemist/build.gradle file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.loader.export;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Reaction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Logs the number of nodes in the scenario.
 */
public final class NumberOfNodes implements Extractor {

    private static final List<String> COLNAME;
    static {
        COLNAME = List.of("nodes");
    }

    @Override
    public <T> double[] extractData(
            final Environment<T, ?> environment,
            final Reaction<T> reaction,
            final it.unibo.alchemist.model.interfaces.Time time,
            final long step
    ) {
        return new double[]{environment.getNodeCount()};
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "the field is immutable")
    public List<String> getNames() {
        return COLNAME;
    }

}
