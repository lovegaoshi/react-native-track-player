package com.doublesymmetry.trackplayer.sabr.parser.exceptions;

import com.doublesymmetry.trackplayer.sabr.protos.misc.FormatId;

public class SabrSegmentMismatchError extends SabrStreamError {
    public final long expectedSequenceNumber;
    public final long receivedSequenceNumber;

    public SabrSegmentMismatchError(FormatId formatId, long expectedSequenceNumber, long receivedSequenceNumber) {
        super(String.format(
                "Segment sequence number mismatch for format %s: expected %s, received %s",
                formatId,
                expectedSequenceNumber,
                receivedSequenceNumber
        ));
        this.receivedSequenceNumber = receivedSequenceNumber;
        this.expectedSequenceNumber = expectedSequenceNumber;
    }
}
