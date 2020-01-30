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
package com.scottescue.backporchshedlock.test.support.jdbc;

import com.scottescue.backporchshedlock.core.LockProvider;
import com.scottescue.backporchshedlock.test.support.FuzzTester;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ExecutionException;

public class TransactionalFuzzTester {

    public static void fuzzTestShouldWorkWithTransaction(LockProvider lockProvider, final DataSource dataSource) throws ExecutionException, InterruptedException {
        new FuzzTester(lockProvider) {
            @Override
            protected Void task(final int iterations) {
                TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
                return transactionTemplate.execute(new TransactionCallback<Void>() {
                    @Override
                    public Void doInTransaction(@NotNull TransactionStatus transactionStatus) {
                        return superTask(iterations);
                    }
                });
            }

            @Override
            protected boolean shouldLog() {
                return true;
            }

            private Void superTask(int iterations) {
                return super.task(iterations);
            }
        }.doFuzzTest();
    }
}
