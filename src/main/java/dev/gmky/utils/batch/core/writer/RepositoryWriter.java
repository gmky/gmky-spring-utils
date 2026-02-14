package dev.gmky.utils.batch.core.writer;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Writer that uses Spring Data repositories to save entities.
 * <p>
 * Simple wrapper around CrudRepository.saveAll().
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <T> the type of entity to write
 * @param <ID> the type of the entity's identifier
 */
public class RepositoryWriter<T, ID> extends AbstractDynamicWriter<T> {

    private final CrudRepository<T, ID> repository;

    /**
     * Constructor.
     *
     * @param repository the Spring Data repository
     */
    public RepositoryWriter(CrudRepository<T, ID> repository) {
        this.repository = repository;
    }

    @Override
    protected void writeItems(List<? extends T> items) {
        repository.saveAll(items);
    }
}
