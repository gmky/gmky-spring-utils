package dev.gmky.utils.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Base interface for generic entity-to-DTO mapping.
 * <p>
 * This interface allows MapStruct to generate implementation for basic CRUD mapping operations.
 * </p>
 *
 * @param <D> the DTO type
 * @param <E> the Entity type
 * @author HiepVH
 * @since 1.0.0
 */
public interface EntityMapper<D, E> {
    /**
     * Maps an entity to its corresponding DTO.
     *
     * @param entity the entity to map
     * @return the mapped DTO
     */
    D toDto(E entity);

    /**
     * Maps a DTO to its corresponding entity.
     *
     * @param dto the DTO to map
     * @return the mapped entity
     */
    E toEntity(D dto);

    /**
     * Maps a list of entities to a list of DTOs.
     *
     * @param entities the list of entities
     * @return the list of DTOs
     */
    List<D> toDto(List<E> entities);

    /**
     * Maps a list of DTOs to a list of entities.
     *
     * @param dtoList the list of DTOs
     * @return the list of entities
     */
    List<E> toEntity(List<D> dtoList);

    /**
     * Updates an existing entity from a DTO, ignoring null properties in the DTO.
     *
     * @param dto    the DTO containing update data
     * @param entity the target entity to update
     * @return the updated entity
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    E partialUpdate(D dto, @MappingTarget E entity);
}
