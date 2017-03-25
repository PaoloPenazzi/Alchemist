package it.unibo.alchemist.loader.export;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Reaction;
import it.unibo.alchemist.model.interfaces.Time;

/**
 * An extractor which provides informations about the running time of the simulation.
 *
 */
public class ExecutionTime implements Extractor {

    private static final double NANOS_TO_SEC = 1000000000.0;
    private static final List<String> COLNAME;
    static {
        final List<String> cName = new LinkedList<>();
        cName.add("runningTime");
        COLNAME = Collections.unmodifiableList(cName);
    }
    private boolean firstRun = true;
    private long initial;
    private long lastStep;

    @Override
    public double[] extractData(final Environment<?> env, final Reaction<?> r, final Time time, final long step) {
        if (lastStep > step) {
            firstRun = true;
        }
        if (firstRun) {
            firstRun = false;
            initial = System.nanoTime();
        }
        lastStep = step;
        final double[] returnValue = new double[1];
        returnValue[0] = ((System.nanoTime() - initial) / NANOS_TO_SEC);
        return returnValue;
    }

    @Override
    public List<String> getNames() {
        return COLNAME;
    }
}
