BackPorch ShedLock :wink:
========
[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)

# BE AWARE
## This library should only be used in projects that are stuck on Java 6 or 7. If you're running Java 8 or newer, you should most definitely be using [ShedLock](https://github.com/lukas-krecan/ShedLock), the library from which this project was back-ported.

This is a back port of the the [ShedLock](https://github.com/lukas-krecan/ShedLock) distributed locking library. 
BackPorch ShedLock (see what I did there :wink:) allows you to use the core goodness of ShedLock if your application 
is stuck running on Java 6 or 7.

This project's code and most of the documentation has been copied from the upstream. Many thanks to Lukas Krecan for creating 
and doing an amazing job supporting and maintaining the upstream ShedLock project.

BackPorch ShedLock makes sure that your scheduled tasks are executed at most once at the same time. 
If a task is being executed on one node, it acquires a lock which prevents execution of the same task from another node (or thread). 
Please note, that **if one task is already being executed on one node, execution on other nodes does not wait, it is simply skipped**.
 
BackPorch ShedLock uses either a JDBC database or Hazelcast external store. 

My bandwidth for back porting providers from ShedLock was extremely limited. I only back ported what I absolutely needed. 
Pull requests that back port other providers are welcome!

#### BackPorch ShedLock is not a distributed scheduler
Please note that BackPorch ShedLock is not and will never be full-fledged scheduler, it's just a lock. If you need a distributed scheduler, please use another project.
BackPorch ShedLock is designed to be used in situations where you have scheduled tasks that are not ready to be executed in parallel, but can be safely
executed repeatedly.

+ [Components](#components)
+ [Usage](#usage)
+ [Lock Providers](#configure-lockprovider)
  - [JdbcTemplate](#jdbctemplate)
  - [Hazelcast](#hazelcast)
+ [Troubleshooting](#troubleshooting)


## Components
BackPorch Shedlock consists of three parts
* Core - The locking mechanism
* Integration - programmatic integration with your application
* Lock provider - provides the lock using an external process like SQL database or Hazelcast  

## Usage
To use BackPorch ShedLock, you do the following
1) Enable and configure task scheduling
2) Wrap the body of your task programmatically
3) Configure a Lock Provider


### Enable and configure Scheduled locking (Spring)
First of all, we have to import the project

```xml
<dependency>
    <groupId>com.scottescue.backporchshedlock</groupId>
    <artifactId>backporchshedlock-core</artifactId>
    <version>4.1.0</version>
</dependency>
```

Now we need to integrate the library with Spring. In order to enable scheduling use the `@EnableScheduling` annotation 
and create a `LockingTaskExecutor` bean that can be used programmatically

```java
@Configuration
@EnableScheduling
class MySpringConfiguration {
    ...

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(LockProvider lockProvider) {
       return new DefaultLockingTaskExecutor(lockProvider);
    }

    ...
}
```

### Code your scheduled tasks
 
 ```java

...

@Autowired
LockingTaskExecutor executor;

@Scheduled(...)
public void scheduledTask() {
    Instant lockAtMostUntil = Instant.now().plusSeconds(600);
    executor.executeWithLock(new Runnable() {
        @Override
        public void run() {
            // To assert that the lock is held (prevents misconfiguration errors)
            LockAssert.assertLocked();
            // do something
        }
    }, new LockConfiguration("lockName", lockAtMostUntil));


}
```
        
You need to wrap your task inside a `Runnable` and pass that into the `LockingTaskExecutor` `executeWithLock` method.
You also have to provide a `LockConfiguration` that specifies the name for the lock and a `lockAtMostUntil` value. 
Only one tasks with the same name can be executed at the same time. The `lockAtMostUntil` parameter specifies how long the 
lock should be kept in case the executing node dies. This is just a fallback, under normal circumstances the lock is 
released as soon the tasks finishes.
**You have to set `lockAtMostUntil` to a value which is much longer than normal execution time.** If the task takes longer than
`lockAtMostUntil` the resulting behavior may be unpredictable (more then one process will effectively hold the lock).

Lastly, you can pass a `lockAtLeastUntil` value which specifies the minimum amount of time for which the lock should be kept. 
Its main purpose is to prevent execution from multiple nodes in case of really short tasks and clock difference between the nodes.


### Configure LockProvider
There are several implementations of LockProvider.  

#### JdbcTemplate
First, create lock table (**please note that `name` has to be primary key**)

```sql
CREATE TABLE shedlock(
    name VARCHAR(64), 
    lock_until TIMESTAMP(3) NULL, 
    locked_at TIMESTAMP(3) NULL, 
    locked_by  VARCHAR(255), 
    PRIMARY KEY (name)
) 
```
script for MS SQL is [here](https://github.com/lukas-krecan/ShedLock/issues/3#issuecomment-275656227) and for Oracle [here](https://github.com/lukas-krecan/ShedLock/issues/81#issue-355599950)

Add dependency

```xml
<dependency>
    <groupId>com.scottescue.backporchshedlock</groupId>
    <artifactId>backporchshedlock-provider-jdbc-template</artifactId>
    <version>1.0.0</version>
</dependency>
```

Configure:

```java
import JdbcTemplateLockProvider;

...

@Bean
public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(dataSource);
}
```

Tested with MySql, Postgres and HSQLDB, should work on all other JDBC compliant databases. 

For more fine-grained configuration use the `Configuration` object

```java
new JdbcTemplateLockProvider(builder()
    .withTableName("shdlck")
    .withColumnNames(new ColumnNames("n", "lck_untl", "lckd_at", "lckd_by"))
    .withJdbcTemplate(new JdbcTemplate(getDatasource()))
    .withLockedByValue("my-value")
    .build())
```

#### Warning
**Do not manually delete lock row or document from DB table.** BackPorch ShedLock has an in-memory cache of existing locks
so the row will NOT be automatically recreated until application restart. If you need to, you can edit the row/document, risking only
that multiple locks will be held. You can clean the cache by calling `clearCache()` on LockProvider.


#### Hazelcast
Import the project

```xml
<dependency>
    <groupId>com.scottescue.backporchshedlock</groupId>
    <artifactId>backporchshedlock-provider-hazelcast</artifactId>
    <version>1.0.0</version>
</dependency>
```

Configure:

```java
import HazelcastLockProvider;

...

@Bean
public HazelcastLockProvider lockProvider(HazelcastInstance hazelcastInstance) {
    return new HazelcastLockProvider(hazelcastInstance);
}
```

## Troubleshooting
Help, BackPorch ShedLock does not do what it's supposed to do!

1. Check the storage. If you are using JDBC, check the ShedLock table. If it's empty, ShedLock is not properly configured. 
If there is more than one record with the same name, you are missing a primary key.
2. Use BackPorch ShedLock debug log. BackPorch ShedLock logs interesting information on DEBUG level with logger name `com.scottescue.backporchshedlock`.
It should help you to see what's going on. 
3. For short-running tasks consider passing `lockAtLeastUntil`. If the tasks are short-running, they can be executed one
after each other, `lockAtLeastUntil` can prevent it. 
 
   

## Requirements and dependencies
* Java 6
* slf4j-api

# Change log
# 1.0.0
* Back ported from ShedLock version 4.2.0
