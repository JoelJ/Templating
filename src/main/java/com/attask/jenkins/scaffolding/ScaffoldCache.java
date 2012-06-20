package com.attask.jenkins.scaffolding;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * User: josephbass
 * Date: 6/20/12
 * Time: 11:47 AM
 */
public class ScaffoldCache {
    private Map<String, Scaffold> cache = new WeakHashMap<String, Scaffold>(); //TODO: make this a persistent cache

    public Scaffold get(String scaffoldName) {
        Scaffold scaffold = cache.get(scaffoldName);
        if (scaffold == null) {
            try {
                scaffold = Scaffold.find(scaffoldName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return scaffold;
    }

    public Set<String> keySet() {
        return Scaffold.getAllNames();
    }

    public void put(Scaffold value) {
        if(value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        cache.put(value.getName(), value);
    }

    public void remove(String name) {
        cache.remove(name);
    }
}