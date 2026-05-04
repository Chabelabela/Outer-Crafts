package org.chabelabela.outer_crafts.timeloop;

public enum TimeLoopPhase {
    RUNNING,
    SUPERNOVA_WARNING,
    SUPERNOVA,
    RESETTING;

    public boolean isEndGame() {
        return this == SUPERNOVA_WARNING || this == SUPERNOVA;
    }
}
