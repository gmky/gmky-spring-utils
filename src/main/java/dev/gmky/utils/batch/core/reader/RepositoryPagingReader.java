package dev.gmky.utils.batch.core.reader;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * Reader for Spring Data JPA repositories with pagination support.
 * <p>
 * Works with any PagingAndSortingRepository.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <T> the type of entity to read
 * @param <ID> the type of the entity's identifier
 */
public class RepositoryPagingReader<T, ID> extends AbstractJpaPagingReader<T> {

    private final PagingAndSortingRepository<T, ID> repository;
    private final Sort sort;

    /**
     * Constructor with sort parameter.
     *
     * @param repository the Spring Data repository
     * @param pageSize the page size
     * @param sort the sort order
     */
    public RepositoryPagingReader(PagingAndSortingRepository<T, ID> repository,
                                 int pageSize,
                                 Sort sort) {
        this.repository = repository;
        this.pageSize = pageSize;
        this.sort = sort != null ? sort : Sort.unsorted();
        setName(ClassUtils.getShortName(getClass()));
    }

    /**
     * Constructor without sort (unsorted).
     *
     * @param repository the Spring Data repository
     * @param pageSize the page size
     */
    public RepositoryPagingReader(PagingAndSortingRepository<T, ID> repository,
                                 int pageSize) {
        this(repository, pageSize, null);
    }

    @Override
    protected List<T> fetchPage(int pageNumber) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, sort);
        Page<T> pageData = repository.findAll(pageRequest);
        return pageData.getContent();
    }
}
