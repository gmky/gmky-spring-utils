package dev.gmky.utils.batch.core.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JpaSpecificationReader.
 */
@ExtendWith(MockitoExtension.class)
class JpaSpecificationReaderTest {

    @Mock
    private JpaSpecificationExecutor<TestEntity> repository;

    @Mock
    private Specification<TestEntity> specification;

    private JpaSpecificationReader<TestEntity> reader;

    @BeforeEach
    void setUp() {
        // Default setup
    }

    @Test
    void testConstructorWithSort() {
        Sort sort = Sort.by("name");
        reader = new JpaSpecificationReader<>(repository, specification, 10, sort);

        assertEquals(10, reader.getPageSize());
    }

    @Test
    void testConstructorWithoutSort() {
        reader = new JpaSpecificationReader<>(repository, specification, 20);

        assertEquals(20, reader.getPageSize());
    }

    @Test
    void testReadFirstPage() throws Exception {
        List<TestEntity> content = Arrays.asList(
            new TestEntity(1L, "Entity 1"),
            new TestEntity(2L, "Entity 2")
        );
        Page<TestEntity> page = new PageImpl<>(content);

        reader = new JpaSpecificationReader<>(repository, specification, 10, Sort.by("id"));

        when(repository.findAll(eq(specification), any(Pageable.class))).thenReturn(page);

        reader.open(new ExecutionContext());
        TestEntity first = reader.read();

        assertNotNull(first);
        assertEquals(1L, first.getId());
        verify(repository).findAll(eq(specification), eq(PageRequest.of(0, 10, Sort.by("id"))));
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

        reader = new JpaSpecificationReader<>(repository, specification, 2);

        when(repository.findAll(eq(specification), any(Pageable.class)))
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

        verify(repository, times(3)).findAll(eq(specification), any(Pageable.class));
    }

    @Test
    void testReadWithSorting() throws Exception {
        List<TestEntity> content = Arrays.asList(new TestEntity(1L, "Entity 1"));
        Page<TestEntity> page = new PageImpl<>(content);

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        reader = new JpaSpecificationReader<>(repository, specification, 10, sort);

        when(repository.findAll(eq(specification), any(Pageable.class))).thenReturn(page);

        reader.open(new ExecutionContext());
        reader.read();

        verify(repository).findAll(eq(specification), eq(PageRequest.of(0, 10, sort)));
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
    }
}
