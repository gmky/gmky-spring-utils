package dev.gmky.utils.batch.core.reader;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JpaPagingReader.
 */
@ExtendWith(MockitoExtension.class)
class JpaPagingReaderTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<TestEntity> query;

    private JpaPagingReader<TestEntity> reader;

    @BeforeEach
    void setUp() {
        lenient().when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
    }

    @Test
    void testConstructorWithAllParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "ACTIVE");

        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e WHERE e.status = :status",
            TestEntity.class,
            100,
            params
        );

        assertEquals(100, reader.getPageSize());
    }

    @Test
    void testConstructorWithoutParameters() {
        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            50
        );

        assertEquals(50, reader.getPageSize());
    }

    @Test
    void testConstructorWithNullParametersUsesEmptyMap() throws Exception {
        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            100,
            null
        );

        when(entityManager.createQuery(anyString(), eq(TestEntity.class))).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        reader.open(new ExecutionContext());
        reader.read();

        verify(query, never()).setParameter(anyString(), any());
    }

    @Test
    void testReadFirstPage() throws Exception {
        List<TestEntity> page1 = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );

        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            10
        );

        when(entityManager.createQuery(anyString(), eq(TestEntity.class))).thenReturn(query);
        when(query.setFirstResult(0)).thenReturn(query);
        when(query.setMaxResults(10)).thenReturn(query);
        when(query.getResultList()).thenReturn(page1);

        reader.open(new ExecutionContext());
        TestEntity first = reader.read();

        assertNotNull(first);
        assertEquals(1L, first.getId());
        verify(entityManager).createQuery("SELECT e FROM TestEntity e", TestEntity.class);
        verify(query).setFirstResult(0);
        verify(query).setMaxResults(10);
        verify(entityManager).clear(); // Verify context is cleared
    }

    @Test
    void testReadMultiplePages() throws Exception {
        List<TestEntity> page1 = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        List<TestEntity> page2 = Arrays.asList(
            new TestEntity(3L, "Entity 3")
        );
        List<TestEntity> page3 = Collections.emptyList();

        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            2
        );

        when(entityManager.createQuery(anyString(), eq(TestEntity.class))).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList())
            .thenReturn(page1)
            .thenReturn(page2)
            .thenReturn(page3);

        reader.open(new ExecutionContext());

        // Read page 1
        assertNotNull(reader.read());
        assertNotNull(reader.read());

        // Read page 2
        assertNotNull(reader.read());

        // End of data
        assertNull(reader.read());

        verify(query, times(3)).getResultList();
        verify(entityManager, times(3)).clear();
    }

    @Test
    void testReadWithParameters() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "ACTIVE");
        params.put("minValue", 100);

        List<TestEntity> results = Arrays.asList(new TestEntity(1L, "Entity 1"));

        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e WHERE e.status = :status AND e.value > :minValue",
            TestEntity.class,
            10,
            params
        );

        when(entityManager.createQuery(anyString(), eq(TestEntity.class))).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);

        reader.open(new ExecutionContext());
        reader.read();

        verify(query).setParameter("status", "ACTIVE");
        verify(query).setParameter("minValue", 100);
    }

    @Test
    void testOpenCreatesEntityManager() throws Exception {
        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            10
        );

        reader.open(new ExecutionContext());

        verify(entityManagerFactory).createEntityManager();
    }

    @Test
    void testCloseClosesEntityManager() throws Exception {
        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            10
        );

        when(entityManager.isOpen()).thenReturn(true);

        reader.open(new ExecutionContext());
        reader.close();

        verify(entityManager).close();
    }

    @Test
    void testCloseDoesNotFailIfEntityManagerNull() throws Exception {
        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            10
        );

        // Don't call open, entityManager will be null
        assertDoesNotThrow(() -> reader.close());
    }

    @Test
    void testCloseDoesNotFailIfEntityManagerClosed() throws Exception {
        reader = new JpaPagingReader<>(
            entityManagerFactory,
            "SELECT e FROM TestEntity e",
            TestEntity.class,
            10
        );

        when(entityManager.isOpen()).thenReturn(false);

        reader.open(new ExecutionContext());
        reader.close();

        verify(entityManager, never()).close();
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
