package cn.edu.whut.sept.zuul.state;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class WorldState {

    private final Map<String, Boolean> flags = new HashMap<>();
    private final Map<String, Integer> counters = new HashMap<>();

    public synchronized void reset() {
        flags.clear();
        counters.clear();
    }

    public synchronized boolean getBoolean(String key) {
        return flags.getOrDefault(key, false);
    }

    public synchronized void setBoolean(String key, boolean value) {
        flags.put(key, value);
    }

    public synchronized int incrementInt(String key) {
        int next = counters.getOrDefault(key, 0) + 1;
        counters.put(key, next);
        return next;
    }

    public synchronized Map<String, Integer> counters() {
        return Collections.unmodifiableMap(new HashMap<>(counters));
    }

    public synchronized void replaceFlags(Map<String, Boolean> nextFlags) {
        flags.clear();
        if (nextFlags != null) {
            flags.putAll(nextFlags);
        }
    }

    public synchronized void replaceCounters(Map<String, Integer> nextCounters) {
        counters.clear();
        if (nextCounters != null) {
            counters.putAll(nextCounters);
        }
    }

    public synchronized Map<String, Boolean> flags() {
        return Collections.unmodifiableMap(new HashMap<>(flags));
    }
}
