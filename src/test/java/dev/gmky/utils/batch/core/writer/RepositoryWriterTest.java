package dev.gmky.utils.batch.core.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.data.repository.CrudRepository;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for RepositoryWriter.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryWriterTest {

    @Mock
    private CrudRepository<String, Long> repository;

    private RepositoryWriter<String, Long> writer;

    @BeforeEach
    void setUp() {
        writer = new RepositoryWriter<>(repository);
    }

    @Test
    void testWriteItems() throws Exception {
        List<String> items = Arrays.asList("Item 1", "Item 2");
        Chunk<String> chunk = new Chunk<>(items);

        writer.write(chunk);

        verify(repository).saveAll(items);
    }
}
