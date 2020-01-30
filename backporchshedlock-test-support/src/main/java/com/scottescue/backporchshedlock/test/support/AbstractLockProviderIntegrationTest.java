/**
 * Copyright 2009-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.scottescue.backporchshedlock.test.support;

import com.scottescue.backporchshedlock.core.LockConfiguration;
import com.scottescue.backporchshedlock.core.LockProvider;
import com.scottescue.backporchshedlock.core.SimpleLock;
import com.scottescue.backporchshedlock.Optional;
import org.junit.Test;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.threeten.bp.temporal.ChronoUnit.MINUTES;
import static org.threeten.bp.temporal.ChronoUnit.SECONDS;

public abstract class AbstractLockProviderIntegrationTest {
    protected static final String LOCK_NAME1 = "name";
    public static final Duration LOCK_AT_LEAST_FOR = Duration.of(2, SECONDS);

    protected abstract LockProvider getLockProvider();

    protected abstract void assertUnlocked(String lockName);

    protected abstract void assertLocked(String lockName);


    @Test
    public void shouldCreateLock() {
        Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1));
        assertThat(lock.isPresent()).isTrue();

        assertLocked(LOCK_NAME1);
        lock.get().unlock();
        assertUnlocked(LOCK_NAME1);
    }

    @Test
    public void shouldNotReturnSecondLock() {
        Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1));
        assertThat(lock.isPresent()).isTrue();
        assertThat(getLockProvider().lock(lockConfig(LOCK_NAME1)).isPresent()).isFalse();
        lock.get().unlock();
    }

    @Test
    public void shouldCreateTwoIndependentLocks() {
        Optional<? extends SimpleLock> lock1 = getLockProvider().lock(lockConfig(LOCK_NAME1));
        assertThat(lock1.isPresent()).isTrue();

        Optional<? extends SimpleLock> lock2 = getLockProvider().lock(lockConfig("name2"));
        assertThat(lock2.isPresent()).isTrue();

        lock1.get().unlock();
        lock2.get().unlock();
    }

    @Test
    public void shouldLockTwiceInARow() {
        Optional<? extends SimpleLock> lock1 = getLockProvider().lock(lockConfig(LOCK_NAME1));
        assertThat(lock1.isPresent()).isTrue();
        lock1.get().unlock();

        Optional<? extends SimpleLock> lock2 = getLockProvider().lock(lockConfig(LOCK_NAME1));
        assertThat(lock2.isPresent()).isTrue();
        lock2.get().unlock();
    }

    @Test
    public void shouldTimeout() throws InterruptedException {
        LockConfiguration configWithShortTimeout = lockConfig(LOCK_NAME1, Duration.ofMillis(20), Duration.ZERO);
        Optional<? extends SimpleLock> lock1 = getLockProvider().lock(configWithShortTimeout);
        assertThat(lock1.isPresent()).isTrue();

        sleep(25);
        assertUnlocked(LOCK_NAME1);

        Optional<? extends SimpleLock> lock2 = getLockProvider().lock(lockConfig(LOCK_NAME1, Duration.ofMillis(5), Duration.ZERO));
        assertThat(lock2.isPresent()).isTrue();
        lock2.get().unlock();
    }


    @Test
    public void shouldBeAbleToLockRightAfterUnlock() {
        LockConfiguration lockConfiguration = lockConfig(LOCK_NAME1);
        for (int i = 0; i < 10; i++) {
            Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfiguration);
            assertThat(getLockProvider().lock(lockConfiguration).isPresent()).isFalse();
            assertThat(lock.isPresent()).isTrue();
            lock.get().unlock();
        }
    }

    @Test
    public void fuzzTestShouldPass() throws ExecutionException, InterruptedException {
        new FuzzTester(getLockProvider()).doFuzzTest();
    }

    @Test
    public void shouldLockAtLeastFor() throws InterruptedException {
        // Lock for LOCK_AT_LEAST_FOR - we do not expect the lock to be released before this time
        Optional<? extends SimpleLock> lock1 = getLockProvider().lock(lockConfig(LOCK_NAME1, LOCK_AT_LEAST_FOR.multipliedBy(2), LOCK_AT_LEAST_FOR));
        assertThat(lock1.isPresent()).isTrue();
        lock1.get().unlock();

        // Even though we have unlocked the lock, it will be held for some time
        assertThat(getLockProvider().lock(lockConfig(LOCK_NAME1)).isPresent()).describedAs("Can not acquire lock, grace period did not pass yet").isFalse();

        // Let's wait wor the lock to be automatically released
        sleep(LOCK_AT_LEAST_FOR.toMillis());

        // Should be able to acquire now
        Optional<? extends SimpleLock> lock3 = getLockProvider().lock(lockConfig(LOCK_NAME1));
        assertThat(lock3.isPresent()).describedAs("Can acquire the lock after grace period").isTrue();
        lock3.get().unlock();
    }

    protected void sleepFor(Duration duration) {
        try {
            sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static LockConfiguration lockConfig(String name) {
        return lockConfig(name, Duration.of(5, MINUTES), Duration.ZERO);
    }

    protected static LockConfiguration lockConfig(String name, Duration lockAtMostFor, Duration lockAtLeastFor) {
        Instant now = Instant.now();
        return new LockConfiguration(name, now.plus(lockAtMostFor), now.plus(lockAtLeastFor));
    }

}