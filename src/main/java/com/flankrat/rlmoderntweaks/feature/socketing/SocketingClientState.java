package com.flankrat.rlmoderntweaks.feature.socketing;

import java.util.Collection;
import java.util.Set;

/**
 * The gems the local player has discovered, as sent by the server. Plain data with no client
 * classes, so the network handler can touch it safely on either side.
 */
public final class SocketingClientState {

    private static volatile Set<String> known = Set.of();

    private SocketingClientState() {
    }

    public static void setKnown(Collection<String> gems) {
        known = Set.copyOf(gems);
    }

    public static boolean isKnown(String gemId) {
        return known.contains(gemId);
    }
}
