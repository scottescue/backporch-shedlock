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
package com.scottescue.backporchshedlock.support;

import com.scottescue.backporchshedlock.core.LockConfiguration;
import com.scottescue.backporchshedlock.TestUtils;
import com.scottescue.backporchshedlock.ThrowingCallable;
import org.junit.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class StorageBasedLockProviderTest {
    private static final LockConfiguration LOCK_CONFIGURATION = new LockConfiguration("name", Instant.now().plus(5, ChronoUnit.MINUTES));
    private static final LockException LOCK_EXCEPTION = new LockException("Test");

    private final StorageAccessor storageAccessor = mock(StorageAccessor.class);

    private final StorageBasedLockProvider lockProvider = new StorageBasedLockProvider(storageAccessor);

    @Test
    public void newRecordShouldOnlyBeInserted() {
        when(storageAccessor.insertRecord(LOCK_CONFIGURATION)).thenReturn(true);
        assertThat(lockProvider.lock(LOCK_CONFIGURATION).isPresent()).isTrue();
        verify(storageAccessor, never()).updateRecord(LOCK_CONFIGURATION);

        // Should update directly without insert
        reset(storageAccessor);
        when(storageAccessor.updateRecord(LOCK_CONFIGURATION)).thenReturn(true);
        assertThat(lockProvider.lock(LOCK_CONFIGURATION).isPresent()).isTrue();
        verify(storageAccessor, never()).insertRecord(LOCK_CONFIGURATION);
        verify(storageAccessor).updateRecord(LOCK_CONFIGURATION);
    }

    @Test
    public void updateOnDuplicateKey() {
        when(storageAccessor.insertRecord(LOCK_CONFIGURATION)).thenReturn(false);
        when(storageAccessor.updateRecord(LOCK_CONFIGURATION)).thenReturn(true);
        assertThat(lockProvider.lock(LOCK_CONFIGURATION).isPresent()).isTrue();
        verify(storageAccessor).updateRecord(LOCK_CONFIGURATION);

        // Should update directly without insert
        reset(storageAccessor);
        when(storageAccessor.updateRecord(LOCK_CONFIGURATION)).thenReturn(true);
        assertThat(lockProvider.lock(LOCK_CONFIGURATION).isPresent()).isTrue();
        verify(storageAccessor, never()).insertRecord(LOCK_CONFIGURATION);
        verify(storageAccessor).updateRecord(LOCK_CONFIGURATION);
    }

    @Test
    public void doNotReturnLockIfUpdatedZeroRows() {
        when(storageAccessor.insertRecord(LOCK_CONFIGURATION)).thenReturn(false);
        when(storageAccessor.updateRecord(LOCK_CONFIGURATION)).thenReturn(false);
        assertThat(lockProvider.lock(LOCK_CONFIGURATION).isPresent()).isFalse();
    }

    @Test
    public void shouldRethrowExceptionFromInsert() {
        when(storageAccessor.insertRecord(LOCK_CONFIGURATION)).thenThrow(LOCK_EXCEPTION);
        Throwable thrown = TestUtils.getThrownBy(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                lockProvider.lock(LOCK_CONFIGURATION);
            }
        });
        assertThat(thrown).isSameAs(LOCK_EXCEPTION);
    }

    @Test
    public void shouldRethrowExceptionFromUpdate() {
        when(storageAccessor.insertRecord(LOCK_CONFIGURATION)).thenReturn(false);
        when(storageAccessor.updateRecord(LOCK_CONFIGURATION)).thenThrow(LOCK_EXCEPTION);
        Throwable thrown = TestUtils.getThrownBy(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                lockProvider.lock(LOCK_CONFIGURATION);
            }
        });
        assertThat(thrown).isSameAs(LOCK_EXCEPTION);
    }
}