package com.company.common.lib.http;

import com.company.common.config.eureka.EurekaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnowflakeIdGeneratorTest {
    @Mock
    private EurekaConfig eurekaConfig;
    private static final long EPOCH = 1704067200000L; // company's epoch
    /**
     * IMPORTANT: The total number of ID_COUNT must be completed divided to THREAD_COUNT otherwise the test will not work.
     * Bad Example: THREAD_COUNT=3; ID_COUNT=10 => 10/3 == 3.3333 [Test will fail]
     * Good Example: THREAD_COUNT=10; ID_COUNT=100 => 100/10 == 10 [Test will pass]
     */
    private static final int THREAD_COUNT = 10; // Number of concurrent threads
    private static final int ID_COUNT = 10000;   // Number of IDs to generate
    private static final int WORKER_NODES = 32;
    private static final int PK_DATA_CENTER = 0;
    private static final int ME_DATA_CENTER = 1;
    private static final int US_DATA_CENTER = 2;

    @Test
    void testSimpleIdComponents() {
        when(eurekaConfig.getCurrentInstanceIndex()).thenReturn(0); // Default
        when(eurekaConfig.getCurrentDataCenterIn5Bit()).thenReturn(PK_DATA_CENTER);
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(eurekaConfig);
        long id = snowflakeIdGenerator.generateId();
        // Directly extract and print components without defining constants
        System.out.println("Generated ID: " + id);
        // Direct extraction and computation using numeric literals
        System.out.println("Timestamp (ms since epoch): " + ((id >> (5 + 5 + 12)) + EPOCH));
        System.out.println("Data center ID: " + ((id >> (5 + 12)) & ((1L << 5) - 1)));
        System.out.println("Machine ID: " + ((id >> 12) & ((1L << 5) - 1)));
        System.out.println("Sequence ID: " + (id & ((1L << 12) - 1)));
        assertEquals((id >> (5 + 12)) & ((1L << 5) - 1), PK_DATA_CENTER);
        assertEquals((id >> 12) & ((1L << 5) - 1), 0);
        assertEquals(id & ((1L << 12) - 1), 0);
    }

    @Test
    void testInvalidWorkerIdOrDsId() {
        // Simulate an invalid data center ID (e.g., 50, which is out of bounds)
        int invalidDataCenterId = 50;
        when(eurekaConfig.getCurrentDataCenterIn5Bit()).thenReturn(invalidDataCenterId);
        // Verify that an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> {
            new SnowflakeIdGenerator(eurekaConfig);
        }, "Datacenter ID can't be greater than 31 or less than 0");

        when(eurekaConfig.getCurrentInstanceIndex()).thenReturn(33); // Invalid WorderID
        when(eurekaConfig.getCurrentDataCenterIn5Bit()).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> {
            new SnowflakeIdGenerator(eurekaConfig);
        }, "Machine ID can't be greater than 31 or less than 0");
    }

    @Test
    void testFor100PcConcurrencyAndThreadSafety() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Long> generatedIds = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        // Create a single instance of SnowflakeIdGenerator to be shared across threads
        when(eurekaConfig.getCurrentInstanceIndex()).thenReturn(0); // Default
        when(eurekaConfig.getCurrentDataCenterIn5Bit()).thenReturn(PK_DATA_CENTER);
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(eurekaConfig);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < ID_COUNT / THREAD_COUNT; j++) {
                        long id = snowflakeIdGenerator.generateId();
                        generatedIds.add(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // Wait for all tasks to complete
        executorService.shutdown();
        Set<Long> uniqueIds = new HashSet<>(generatedIds);
        assertEquals(ID_COUNT, generatedIds.size(), "Total Generated IDs are not equal");
        assertEquals(ID_COUNT, uniqueIds.size(), "Not all generated IDs are unique");
    }

    @Test
    void testForDifferentWorkerNodes() throws InterruptedException {
        List<Long> generatedIds = Collections.synchronizedList(new ArrayList<>());
        when(eurekaConfig.getCurrentDataCenterIn5Bit()).thenReturn(PK_DATA_CENTER);
        for (int i = 0; i < WORKER_NODES; i++) {
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            // Create a single instance of SnowflakeIdGenerator to be shared across threads
            when(eurekaConfig.getCurrentInstanceIndex()).thenReturn(i);
            SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(eurekaConfig);
            for (int j = 0; j < THREAD_COUNT; j++) {
                executorService.submit(() -> {
                    try {
                        for (int k = 0; k < ID_COUNT / THREAD_COUNT; k++) {
                            long id = snowflakeIdGenerator.generateId();
                            generatedIds.add(id);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(); // Wait for all tasks to complete
            executorService.shutdown();
        }
        Set<Long> uniqueIds = new HashSet<>(generatedIds);
        assertEquals(ID_COUNT * WORKER_NODES, generatedIds.size(), "Total Generated IDs are not equal");
        assertEquals(ID_COUNT * WORKER_NODES, uniqueIds.size(), "Not all generated IDs are unique");
    }
}

