package org.mars_sim.msp.core.sodium;

/**
 * An interface for 3-argument lambda functions.
 */
public interface Lambda3<A,B,C,D> {
    D apply(A a, B b, C c);
}

