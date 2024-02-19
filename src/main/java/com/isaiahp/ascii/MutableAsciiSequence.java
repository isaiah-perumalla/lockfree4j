package com.isaiahp.ascii;

import org.agrona.DirectBuffer;

public interface MutableAsciiSequence {
    void copyFrom(DirectBuffer buffer, int offset, int length);

    void setAt(int index, byte ch);

    void setSize(int i);

    int size();
}
