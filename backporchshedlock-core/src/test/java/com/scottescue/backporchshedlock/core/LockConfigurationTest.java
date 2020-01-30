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

import com.scottescue.backporchshedlock.TestUtils;
import com.scottescue.backporchshedlock.ThrowingCallable;
import org.junit.Test;
import org.threeten.bp.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class LockConfigurationTest {

    @Test
    public void lockAtLeastUnitilShouldBeBeforeOrEqualsToLockAtMostUntil() {
        final Instant time = Instant.now().plusSeconds(5);
        new LockConfiguration("name", time, time);
        new LockConfiguration("name", time.plusMillis(1), time);

        Throwable thrown = TestUtils.getThrownBy(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new LockConfiguration("name", time, time.plusMillis(1));
            }
        });
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void lockAtMostUntilHasToBeInTheFuture() {
        final Instant now = Instant.now();
        Throwable thrown = TestUtils.getThrownBy(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new LockConfiguration("name", now.minusSeconds(1));
            }
        });
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void nameShouldNotBeEmpty() {
        Throwable thrown = TestUtils.getThrownBy(new ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new LockConfiguration("", Instant.now().plusSeconds(5));
            }
        });
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

}