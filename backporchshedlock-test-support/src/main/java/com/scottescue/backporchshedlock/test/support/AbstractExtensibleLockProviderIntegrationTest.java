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

import com.scottescue.backporchshedlock.core.SimpleLock;
import com.scottescue.backporchshedlock.Optional;
import org.junit.Test;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public abstract class AbstractExtensibleLockProviderIntegrationTest extends AbstractLockProviderIntegrationTest {

    @Test
    public void shouldBeAbleToExtendLock() {
        Duration originalLockDuration = Duration.ofMillis(1000);
        Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1, originalLockDuration, Duration.ZERO));
        assertThat(lock.isPresent()).isTrue();
        assertLocked(LOCK_NAME1);
        Optional<? extends SimpleLock> newLock = lock.get().extend(Instant.now().plusSeconds(10), Instant.now());
        assertThat(newLock.isPresent()).isTrue();

        // wait for the original lock to be released
        sleepFor(originalLockDuration);
        assertLocked(LOCK_NAME1);

        newLock.get().unlock();
        assertUnlocked(LOCK_NAME1);
    }

    @Test
    public void shouldNotBeAbleToExtendUnlockedLock() {
        Duration originalLockDuration = Duration.ofMillis(1000);
        final Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1, originalLockDuration, Duration.ZERO));
        assertThat(lock.isPresent()).isTrue();
        assertLocked(LOCK_NAME1);
        lock.get().unlock();
        assertUnlocked(LOCK_NAME1);

        assertInvalidLock(new ThrowingCallable() {
            @Override
            public void call() {
                lock.get().extend(Instant.now().plusSeconds(10), Instant.now());
            }
        });
    }

    @Test
    public void shouldNotBeAbleToExtendExpiredLock() {
        Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1, Duration.ofMillis(1), Duration.ZERO));
        sleepFor(Duration.ofMillis(1));
        assertThat(lock.isPresent()).isTrue();

        Optional<? extends SimpleLock> newLock = lock.get().extend(Instant.now().plusSeconds(10), Instant.now());
        assertThat(newLock.isPresent()).isFalse();
        assertUnlocked(LOCK_NAME1);
    }


    @Test
    public void shouldBeAbleToExtendAtLeast() {
        Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1, Duration.ofSeconds(10), Duration.ZERO));
        assertThat(lock.isPresent()).isTrue();

        Optional<? extends SimpleLock> newLock = lock.get().extend(Instant.now().plusSeconds(10), Instant.now().plusSeconds(9));
        assertThat(newLock.isPresent()).isTrue();
        newLock.get().unlock();
        assertLocked(LOCK_NAME1);
    }

    @Test
    public void lockCanNotBeExtendedTwice() {
        final Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1, Duration.ofSeconds(10), Duration.ZERO));
        assertThat(lock.isPresent()).isTrue();
        Optional<? extends SimpleLock> newLock = lock.get().extend(Instant.now().plusSeconds(10), Instant.now().plusSeconds(9));
        assertThat(newLock.isPresent()).isTrue();

        assertInvalidLock(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                lock.get().extend(Instant.now().plusSeconds(10), Instant.now().plusSeconds(9));
            }
        });
    }

    @Test
    public void lockCanNotBeUnlockedAfterExtending() {
        final Optional<? extends SimpleLock> lock = getLockProvider().lock(lockConfig(LOCK_NAME1, Duration.ofSeconds(10), Duration.ZERO));
        assertThat(lock.isPresent()).isTrue();
        Optional<? extends SimpleLock> newLock = lock.get().extend(Instant.now().plusSeconds(10), Instant.now().plusSeconds(9));
        assertThat(newLock.isPresent()).isTrue();

        assertInvalidLock(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                lock.get().unlock();
            }
        });
    }

    void assertInvalidLock(ThrowingCallable operation) {
        try {
            operation.call();
            fail("Expected an IllegalStateException to be thrown");
        } catch (Throwable throwable) {
            assertThat(throwable).isInstanceOf(IllegalStateException.class);
        }
    }

    interface ThrowingCallable {
        public void call() throws Throwable;
    }
}