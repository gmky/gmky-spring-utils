package dev.gmky.utils.batch.core.writer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JpaBatchWriter.
 */
@ExtendWith(MockitoExtension.class)
class JpaBatchWriterTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityTransaction transaction;

    private JpaBatchWriter<TestEntity> writer;

    @BeforeEach
    void setUp() {
        // No common stubs to avoid UnnecessaryStubbingException
    }

    @Test
    void testConstructorWithAllParameters() {
        writer = new JpaBatchWriter<>(entityManagerFactory, 100, true);
        assertNotNull(writer);
    }

    @Test
    void testConstructorWithBatchSize() {
        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        assertNotNull(writer);
    }

    @Test
    void testConstructorWithDefaults() {
        writer = new JpaBatchWriter<>(entityManagerFactory);
        assertNotNull(writer);
    }

    @Test
    void testOpenCreatesEntityManager() {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        writer.open(new ExecutionContext());

        verify(entityManagerFactory).createEntityManager();
    }

    @Test
    void testWriteItemsWithPersist() throws Exception {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50, false);
        writer.open(new ExecutionContext());

        List<TestEntity> items = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        Chunk<TestEntity> chunk = new Chunk<>(items);

        writer.write(chunk);

        verify(transaction).begin();
        verify(entityManager, times(2)).persist(any(TestEntity.class));
        verify(transaction).commit();
    }

    @Test
    void testWriteItemsWithMerge() throws Exception {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50, true);
        writer.open(new ExecutionContext());

        List<TestEntity> items = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        Chunk<TestEntity> chunk = new Chunk<>(items);

        writer.write(chunk);

        verify(transaction).begin();
        verify(entityManager, times(2)).merge(any(TestEntity.class));
        verify(transaction).commit();
    }

    @Test
    void testFlushAndClearAfterBatchSize() throws Exception {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);

        int batchSize = 2;
        writer = new JpaBatchWriter<>(entityManagerFactory, batchSize);
        writer.open(new ExecutionContext());

        List<TestEntity> items = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2"),
            new TestEntity(3L, "Entity 3"),
            new TestEntity(4L, "Entity 4"),
            new TestEntity(5L, "Entity 5")
        );
        Chunk<TestEntity> chunk = new Chunk<>(items);

        writer.write(chunk);

        // Should flush and clear after every 2 items + final flush
        // At index 2 and 4, then final
        verify(entityManager, times(3)).flush();
        verify(entityManager, times(3)).clear();
    }

    @Test
    void testTransactionRollbackOnError() throws Exception {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        writer.open(new ExecutionContext());

        doThrow(new RuntimeException("Persist failed")).when(entityManager).persist(any());
        when(transaction.isActive()).thenReturn(true);

        List<TestEntity> items = Arrays.asList(new TestEntity(1L, "Entity 1"));
        Chunk<TestEntity> chunk = new Chunk<>(items);

        assertThrows(RuntimeException.class, () -> writer.write(chunk));

        verify(transaction).begin();
        verify(transaction).rollback();
        verify(transaction, never()).commit();
    }

    @Test
    void testNoRollbackIfTransactionNotActive() throws Exception {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        writer.open(new ExecutionContext());

        doThrow(new RuntimeException("Persist failed")).when(entityManager).persist(any());
        when(transaction.isActive()).thenReturn(false);

        List<TestEntity> items = Arrays.asList(new TestEntity(1L, "Entity 1"));
        Chunk<TestEntity> chunk = new Chunk<>(items);

        assertThrows(RuntimeException.class, () -> writer.write(chunk));

        verify(transaction, never()).rollback();
    }

    @Test
    void testCloseClosesEntityManager() {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        writer.open(new ExecutionContext());

        when(entityManager.isOpen()).thenReturn(true);

        writer.close();

        verify(entityManager).close();
    }

    @Test
    void testCloseDoesNotFailIfEntityManagerNull() {
        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        assertDoesNotThrow(() -> writer.close());
    }

    @Test
    void testCloseDoesNotFailIfEntityManagerClosed() {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        writer.open(new ExecutionContext());

        when(entityManager.isOpen()).thenReturn(false);

        writer.close();

        verify(entityManager, never()).close();
    }

    @Test
    void testWriteLargeChunkWithBatching() throws Exception {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);

        int batchSize = 10;
        writer = new JpaBatchWriter<>(entityManagerFactory, batchSize);
        writer.open(new ExecutionContext());

        List<TestEntity> items = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            items.add(new TestEntity((long) i, "Entity " + i));
        }
        Chunk<TestEntity> chunk = new Chunk<>(items);

        writer.write(chunk);

        // 100 items / 10 batch size = 10 flushes total (every 10th item)
        verify(entityManager, times(10)).flush();
        verify(entityManager, times(10)).clear();
        verify(entityManager, times(100)).persist(any(TestEntity.class));
        verify(transaction).commit();
    }

    @Test
    void testWriteOrderOfOperations() throws Exception {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);

        writer = new JpaBatchWriter<>(entityManagerFactory, 50);
        writer.open(new ExecutionContext());

        List<TestEntity> items = Arrays.asList(new TestEntity(1L, "Entity 1"));
        Chunk<TestEntity> chunk = new Chunk<>(items);

        writer.write(chunk);

        InOrder inOrder = inOrder(transaction, entityManager);
        inOrder.verify(transaction).begin();
        inOrder.verify(entityManager).persist(any(TestEntity.class));
        inOrder.verify(entityManager).flush();
        inOrder.verify(entityManager).clear();
        inOrder.verify(transaction).commit();
    }

    /**
     * Test entity class.
     */
    static class TestEntity {
        private Long id;
        private String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
