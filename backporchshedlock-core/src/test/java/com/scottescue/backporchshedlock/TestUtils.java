package com.scottescue.backporchshedlock;

import junit.framework.AssertionFailedError;

public class TestUtils {

    public static Throwable getThrownBy(ThrowingCallable callable) {
        try {
            callable.call();
            throw new AssertionFailedError("Expected Throwable to be thrown");
        } catch (Throwable throwable) {
            return throwable;
        }
    }
}
