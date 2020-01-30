package com.scottescue.backporchshedlock.core;

import com.scottescue.backporchshedlock.Optional;
import com.scottescue.backporchshedlock.TestUtils;
import com.scottescue.backporchshedlock.ThrowingCallable;
import org.junit.Test;
import org.threeten.bp.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LockAssertTest {

    @Test
    public void assertLockedShouldFailIfLockNotHeld() {
        Throwable thrown = TestUtils.getThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                LockAssert.assertLocked();
            }
        });
        assertThat(thrown).hasMessageStartingWith("The task is not locked");
    }

    @Test
    public void assertLockedShouldNotFailIfLockHeld() {
        LockConfiguration lockConfiguration = new LockConfiguration("test", Instant.now().plusSeconds(10));

        LockProvider lockProvider = mock(LockProvider.class);
        when(lockProvider.lock(lockConfiguration)).thenReturn(Optional.of(mock(SimpleLock.class)));

        new DefaultLockingTaskExecutor(lockProvider).executeWithLock(
                new Runnable() {
                    @Override
                    public void run() {
                        LockAssert.assertLocked();
                    }
                },
                lockConfiguration
        );
    }

}