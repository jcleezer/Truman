package com.leezer.truman;

import java.util.List;

public class Sample {

    private final List<RGB> samples;

    public Sample(List<RGB> samples) {
        this.samples = samples;
    }

    public List<RGB> getSamples() {
        return samples;
    }
}
