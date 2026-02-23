package dev.gmky.utils.batch.core.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.data.domain.*;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RepositoryPagingReader.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryPagingReaderTest {

    @Mock
    private PagingAndSortingRepository<TestEntity, Long> repository;

    private RepositoryPagingReader<TestEntity, Long> reader;

    @BeforeEach
    void setUp() {
        // Default setup
    }

    @Test
    void testConstructorWithSort() {
        Sort sort = Sort.by("name");
        reader = new RepositoryPagingReader<>(repository, 10, sort);

        assertEquals(10, reader.getPageSize());
    }

    @Test
    void testConstructorWithoutSort() {
        reader = new RepositoryPagingReader<>(repository, 20);

        assertEquals(20, reader.getPageSize());
    }

    @Test
    void testConstructorWithNullSortUsesUnsorted() {
        reader = new RepositoryPagingReader<>(repository, 10, null);
        Page<TestEntity> page = new PageImpl<>(Collections.emptyList());
        when(repository.findAll(any(Pageable.class))).thenReturn(page);
        
        reader.open(new ExecutionContext());
        try { reader.read(); } catch (Exception e) {}
        
        verify(repository).findAll(PageRequest.of(0, 10, Sort.unsorted()));
    }

    @Test
    void testReadFirstPage() throws Exception {
        List<TestEntity> content = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        Page<TestEntity> page = new PageImpl<>(content);

        reader = new RepositoryPagingReader<>(repository, 10, Sort.by("id"));

        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        reader.open(new ExecutionContext());
        TestEntity first = reader.read();

        assertNotNull(first);
        assertEquals(1L, first.getId());
        verify(repository).findAll(PageRequest.of(0, 10, Sort.by("id")));
    }

    @Test
    void testReadMultiplePages() throws Exception {
        List<TestEntity> page1Content = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        List<TestEntity> page2Content = Arrays.asList(
            new TestEntity(3L, "Entity 3")
        );
        List<TestEntity> page3Content = Collections.emptyList();

        Page<TestEntity> page1 = new PageImpl<>(page1Content);
        Page<TestEntity> page2 = new PageImpl<>(page2Content);
        Page<TestEntity> page3 = new PageImpl<>(page3Content);

        reader = new RepositoryPagingReader<>(repository, 2);

        when(repository.findAll(any(Pageable.class)))
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

        verify(repository, times(3)).findAll(any(Pageable.class));
    }

    @Test
    void testReadWithSorting() throws Exception {
        List<TestEntity> content = Arrays.asList(new TestEntity(1L, "Entity 1"));
        Page<TestEntity> page = new PageImpl<>(content);

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        reader = new RepositoryPagingReader<>(repository, 10, sort);

        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        reader.open(new ExecutionContext());
        reader.read();

        verify(repository).findAll(PageRequest.of(0, 10, sort));
    }

    @Test
    void testReadEmptyRepository() throws Exception {
        Page<TestEntity> emptyPage = new PageImpl<>(Collections.emptyList());

        reader = new RepositoryPagingReader<>(repository, 10);

        when(repository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        reader.open(new ExecutionContext());
        TestEntity result = reader.read();

        assertNull(result);
    }

    @Test
    void testPageSizeConfiguration() {
        reader = new RepositoryPagingReader<>(repository, 50);

        assertEquals(50, reader.getPageSize());
    }

    @Test
    void testReadAllItems() throws Exception {
        // Create 5 items, page size 2 = 3 pages
        List<TestEntity> page1 = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        List<TestEntity> page2 = Arrays.asList(
            new TestEntity(3L, "Entity 3"),
            new TestEntity(4L, "Entity 4")
        );
        List<TestEntity> page3 = Arrays.asList(
            new TestEntity(5L, "Entity 5")
        );
        List<TestEntity> page4 = Collections.emptyList();

        when(repository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(page1))
            .thenReturn(new PageImpl<>(page2))
            .thenReturn(new PageImpl<>(page3))
            .thenReturn(new PageImpl<>(page4));

        reader = new RepositoryPagingReader<>(repository, 2);
        reader.open(new ExecutionContext());

        int count = 0;
        while (reader.read() != null) {
            count++;
        }

        assertEquals(5, count);
        verify(repository, times(4)).findAll(any(Pageable.class));
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
