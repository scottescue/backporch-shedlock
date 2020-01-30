package com.scottescue.backporchshedlock.core;

import com.scottescue.backporchshedlock.Optional;
import org.junit.Test;
import org.threeten.bp.Instant;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultLockingTaskExecutorTest {
    private final LockProvider lockProvider = mock(LockProvider.class);
    private final DefaultLockingTaskExecutor executor = new DefaultLockingTaskExecutor(lockProvider);
    private final LockConfiguration lockConfig = new LockConfiguration("test", Instant.now().plusSeconds(100));

    @Test
    public void lockShouldBeReentrant() {
        when(lockProvider.lock(lockConfig))
            .thenReturn(Optional.of(mock(SimpleLock.class)))
            .thenReturn(Optional.<SimpleLock>empty());

        final AtomicBoolean called = new AtomicBoolean(false);

        executor.executeWithLock(new Runnable() {
            @Override
            public void run() {
                executor.executeWithLock(new Runnable() {
                    @Override
                    public void run() {
                        called.set(true);
                    }
                }, lockConfig);
            }
        }, lockConfig);

        assertThat(called.get()).isTrue();
    }
}