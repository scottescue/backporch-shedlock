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
package com.scottescue.backporchshedlock.provider.jdbctemplate;

import com.scottescue.backporchshedlock.core.LockConfiguration;
import com.scottescue.backporchshedlock.support.AbstractStorageAccessor;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;

import static org.threeten.bp.jdk8.Jdk8Methods.requireNonNull;

/**
 * Spring JdbcTemplate based implementation usable in JTA environment
 */
class JdbcTemplateStorageAccessor extends AbstractStorageAccessor {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplateLockProvider.Configuration configuration;

    JdbcTemplateStorageAccessor(@NotNull JdbcTemplateLockProvider.Configuration configuration) {
        this.configuration = requireNonNull(configuration, "configuration can not be null");
        this.jdbcTemplate = configuration.getJdbcTemplate();
        PlatformTransactionManager transactionManager = configuration.getTransactionManager() != null ?
            configuration.getTransactionManager() :
            new DataSourceTransactionManager(jdbcTemplate.getDataSource());

        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean insertRecord(@NotNull final LockConfiguration lockConfiguration) {
        final String sql = "INSERT INTO " + tableName() + "(" + name() + ", " + lockUntil() + ", " + lockedAt() + ", " + lockedBy() + ") VALUES(?, ?, ?, ?)";

        return transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(@NotNull TransactionStatus transactionStatus) {
                try {
                    int insertedRows = jdbcTemplate.update(sql, new PreparedStatementSetter() {
                        @Override
                        public void setValues(@NotNull PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, lockConfiguration.getName());
                            setTimestamp(preparedStatement, 2, lockConfiguration.getLockAtMostUntil());
                            setTimestamp(preparedStatement, 3, Instant.now());
                            preparedStatement.setString(4, lockedByValue());
                        }
                    });
                    return insertedRows > 0;
                } catch (DuplicateKeyException e) {
                    return false;
                } catch (DataIntegrityViolationException e) {
                    logger.warn("Unexpected exception", e);
                    return false;
                }
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean updateRecord(@NotNull final LockConfiguration lockConfiguration) {
        final String sql = "UPDATE " + tableName()
            + " SET " + lockUntil() + " = ?, " + lockedAt() + " = ?, " + lockedBy() + " = ? WHERE " + name() + " = ? AND " + lockUntil() + " <= ?";
        return transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(@NotNull TransactionStatus transactionStatus) {
                int updatedRows = jdbcTemplate.update(sql, new PreparedStatementSetter() {
                    @Override
                    public void setValues(@NotNull PreparedStatement statement) throws SQLException {
                        Instant now = Instant.now();
                        setTimestamp(statement, 1, lockConfiguration.getLockAtMostUntil());
                        setTimestamp(statement, 2, now);
                        statement.setString(3, lockedByValue());
                        statement.setString(4, lockConfiguration.getName());
                        setTimestamp(statement, 5, now);
                    }
                });
                return updatedRows > 0;
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean extend(@NotNull final LockConfiguration lockConfiguration) {
        final String sql = "UPDATE " + tableName()
            + " SET " + lockUntil() + " = ? WHERE " + name() + " = ? AND " + lockedBy() + " = ? AND " + lockUntil() + " > ? ";

        logger.debug("Extending lock={} until={}", lockConfiguration.getName(), lockConfiguration.getLockAtMostUntil());
        return transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(@NotNull TransactionStatus transactionStatus) {
                int updatedRows = jdbcTemplate.update(sql, new PreparedStatementSetter() {
                    @Override
                    public void setValues(@NotNull PreparedStatement statement) throws SQLException {
                        setTimestamp(statement, 1, lockConfiguration.getLockAtMostUntil());
                        statement.setString(2, lockConfiguration.getName());
                        statement.setString(3, lockedByValue());
                        setTimestamp(statement, 4, Instant.now());
                    }
                });
                return updatedRows > 0;
            }
        });
    }

    private void setTimestamp(PreparedStatement preparedStatement, int parameterIndex, Instant time) throws SQLException {
        TimeZone timeZone = configuration.getTimeZone();
        if (timeZone == null) {
            preparedStatement.setTimestamp(parameterIndex, DateTimeUtils.toSqlTimestamp(time));
        } else {
            preparedStatement.setTimestamp(parameterIndex, DateTimeUtils.toSqlTimestamp(time), Calendar.getInstance(timeZone));
        }
    }

    @Override
    public void unlock(@NotNull final LockConfiguration lockConfiguration) {
        final String sql = "UPDATE " + tableName() + " SET " + lockUntil() + " = ? WHERE " + name() + " = ?";
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                jdbcTemplate.update(sql, new PreparedStatementSetter() {
                    @Override
                    public void setValues(@NotNull PreparedStatement statement) throws SQLException {
                        setTimestamp(statement, 1, lockConfiguration.getUnlockTime());
                        statement.setString(2, lockConfiguration.getName());
                    }
                });
            }
        });
    }

    private String name() {
        return configuration.getColumnNames().getName();
    }

    private String lockUntil() {
        return configuration.getColumnNames().getLockUntil();
    }

    private String lockedAt() {
        return configuration.getColumnNames().getLockedAt();
    }

    private String lockedBy() {
        return configuration.getColumnNames().getLockedBy();
    }


    private String lockedByValue() {
        return configuration.getLockedByValue();
    }

    private String tableName() {
        return configuration.getTableName();
    }

}
