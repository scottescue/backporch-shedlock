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

import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AbstractMySqlJdbcLockProviderIntegrationTest extends AbstractJdbcLockProviderIntegrationTest {
    private static final MySqlConfig dbConfig = new MySqlConfig();

    @BeforeClass
    public static void startMySql() {
        dbConfig.startDb();
    }

    @AfterClass
    public static void shutDownMysql() {
        dbConfig.shutdownDb();
    }

    @Override
    protected DbConfig getDbConfig() {
        return dbConfig;
    }
}