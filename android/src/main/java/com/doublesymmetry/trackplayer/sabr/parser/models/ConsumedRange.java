package com.doublesymmetry.trackplayer.sabr.parser.models;

public class ConsumedRange {
    public int startSequenceNumber;
    public int endSequenceNumber;
    public long startTimeMs;
    public long durationMs;

    public ConsumedRange(long startTimeMs, long durationMs, int startSequenceNumber, int endSequenceNumber) {
        this.startTimeMs = startTimeMs;
        this.durationMs = durationMs;
        this.startSequenceNumber = startSequenceNumber;
        this.endSequenceNumber = endSequenceNumber;
    }
}
