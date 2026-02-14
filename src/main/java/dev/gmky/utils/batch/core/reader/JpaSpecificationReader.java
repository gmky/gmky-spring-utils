package dev.gmky.utils.batch.core.reader;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * Reader for Spring Data JPA repositories with Specification support.
 * <p>
 * Allows dynamic queries using JPA Criteria API.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <T> the type of entity to read
 */
public class JpaSpecificationReader<T> extends AbstractJpaPagingReader<T> {

    private final JpaSpecificationExecutor<T> repository;
    private final Specification<T> specification;
    private final Sort sort;

    /**
     * Constructor with specification and sort.
     *
     * @param repository the Spring Data JPA repository with specification support
     * @param specification the JPA specification for filtering
     * @param pageSize the page size
     * @param sort the sort order
     */
    public JpaSpecificationReader(JpaSpecificationExecutor<T> repository,
                                 Specification<T> specification,
                                 int pageSize,
                                 Sort sort) {
        this.repository = repository;
        this.specification = specification;
        this.pageSize = pageSize;
        this.sort = sort != null ? sort : Sort.unsorted();
        setName(ClassUtils.getShortName(getClass()));
    }

    /**
     * Constructor without sort.
     *
     * @param repository the Spring Data JPA repository
     * @param specification the JPA specification
     * @param pageSize the page size
     */
    public JpaSpecificationReader(JpaSpecificationExecutor<T> repository,
                                 Specification<T> specification,
                                 int pageSize) {
        this(repository, specification, pageSize, null);
    }

    @Override
    protected List<T> fetchPage(int pageNumber) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, sort);
        Page<T> pageData = repository.findAll(specification, pageRequest);
        return pageData.getContent();
    }
}
