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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Increments counter from several threads coordinating using lock provided under test.
 */
public class FuzzTester {

    private static final int THREADS = 8;
    private static final int ITERATIONS = 100;
    public static final int SHORT_ITERATION = 10;

    private final LockProvider lockProvider;

    private int counter;
    private final LockConfiguration config = new LockConfiguration("lock", Instant.now().plus(5, ChronoUnit.MINUTES));

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public FuzzTester(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    public void doFuzzTest() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        final int[] iterations = new int[THREADS];
        Arrays.fill(iterations, ITERATIONS);
        iterations[0] = SHORT_ITERATION; // short task to simulate MySql issues

        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final int index = i;
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    return task(iterations[index]);
                }
            });
        }
        waitForIt(executor.invokeAll(tasks));

        assertThat(counter).isEqualTo((THREADS - 1) * ITERATIONS + SHORT_ITERATION);
    }

    private void waitForIt(List<Future<Void>> futures) throws InterruptedException, ExecutionException {
        for (Future<Void> f : futures) {
            f.get();
        }
    }

    protected Void task(int iterations) {
        try {
            for (int i = 0; i < iterations; ) {
                Optional<? extends SimpleLock> lock = lockProvider.lock(config);
                if (lock.isPresent()) {
                    int n = counter;
                    if (shouldLog()) logger.debug("action=getLock value={} i={}", n, i);
                    sleep();
                    if (shouldLog()) logger.debug("action=setCounter value={} i={}", n + 1, i);
                    counter = n + 1;
                    i++;
                    lock.get().unlock();
                }
            }
            logger.debug("action=finished");
            return null;
        } catch (RuntimeException e) {
            logger.error("Unexpected exception", e);
            throw e;
        }
    }

    protected boolean shouldLog() {
        return false;
    }

    private void sleep() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
