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
package com.scottescue.backporchshedlock.core;

import com.scottescue.backporchshedlock.Optional;
import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

public abstract class AbstractSimpleLock extends SimpleLock {
    private boolean valid = true;
    protected final LockConfiguration lockConfiguration;

    protected AbstractSimpleLock(LockConfiguration lockConfiguration) {
        this.lockConfiguration = lockConfiguration;
    }

    @Override
    public final void unlock() {
        checkValidity();
        doUnlock();
        valid = false;
    }

    protected abstract void doUnlock();

    /**
     * Extends the lock. If the lock can be extended a new lock is returned. After calling extend, no other operation
     * can be called on current lock.
     * <p>
     * This method is NOT supported by all lock providers.
     *
     * @return a new lock or empty optional if the lock can not be extended
     * @throws IllegalStateException         if the lock has already been unlocked or extended
     * @throws UnsupportedOperationException if the lock extension is not supported by LockProvider.
     */
    @NotNull
    public final Optional<SimpleLock> extend(@NotNull Instant lockAtMostUntil, @NotNull Instant lockAtLeastUntil) {
        checkValidity();
        Optional<SimpleLock> result = doExtend(new LockConfiguration(lockConfiguration.getName(), lockAtMostUntil, lockAtLeastUntil));
        valid = false;
        return result;
    }

    protected Optional<SimpleLock> doExtend(LockConfiguration newConfiguration) {
        throw new UnsupportedOperationException();
    }

    private void checkValidity() {
        if (!valid) {
            throw new IllegalStateException("Lock is not valid, it has already been unlocked or extended");
        }
    }
}
