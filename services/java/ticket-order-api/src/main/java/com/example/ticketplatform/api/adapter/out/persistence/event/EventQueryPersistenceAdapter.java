package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.application.port.in.PageMetadata;
import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.infrastructure.config.persistence.ReadQueryExecutor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class EventQueryPersistenceAdapter implements EventQueryRepositoryPort {

  private final EventJpaRepository primaryEventRepository;
  private final EventOrderJpaRepository primaryEventOrderRepository;
  private final ReadReplicaEventJpaRepository readReplicaEventRepository;
  private final ReadReplicaEventOrderJpaRepository readReplicaEventOrderRepository;
  private final EventMapper eventMapper;
  private final ReadQueryExecutor readQueryExecutor;

  @Override
  public Optional<Event> findById(UUID id) {
    return readQueryExecutor.execute(
        () -> readReplicaEventRepository.findByIdWithOrders(id).map(eventMapper::toDomain),
        () -> primaryEventRepository.findByIdWithOrders(id).map(eventMapper::toDomain));
  }

  @Override
  public PageResult<Event> findPublished(PageRequest pageRequest) {
    return readQueryExecutor.execute(
        () ->
            toPageResult(
                readReplicaEventRepository.findByStatus(
                    EventStatusEntity.PUBLISHED, toPageable(pageRequest))),
        () ->
            toPageResult(
                primaryEventRepository.findByStatus(
                    EventStatusEntity.PUBLISHED, toPageable(pageRequest))));
  }

  @Override
  public PageResult<Event> findByOwnerId(UUID ownerId, PageRequest pageRequest) {
    return readQueryExecutor.execute(
        () ->
            toPageResult(
                readReplicaEventRepository.findByOwnerId(ownerId, toPageable(pageRequest))),
        () ->
            toPageResult(primaryEventRepository.findByOwnerId(ownerId, toPageable(pageRequest))));
  }

  @Override
  public boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEventOrderRepository.existsByEventIdAndRowNumberAndPlaceNumber(
                eventId, rowNumber, placeNumber),
        () ->
            primaryEventOrderRepository.existsByEventIdAndRowNumberAndPlaceNumber(
                eventId, rowNumber, placeNumber));
  }

  @Override
  public List<EventOrder> findOrdersByIds(Collection<UUID> ids) {
    return readQueryExecutor.execute(
        () -> readReplicaEventOrderRepository.findByIdIn(ids).stream()
            .map(eventMapper::toDomain)
            .toList(),
        () -> primaryEventOrderRepository.findByIdIn(ids).stream()
            .map(eventMapper::toDomain)
            .toList());
  }

  @Override
  public List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId) {
    return readQueryExecutor.execute(
        () -> readReplicaEventOrderRepository.findByIdInAndCustomerId(ids, customerId).stream()
            .map(eventMapper::toDomain)
            .toList(),
        () -> primaryEventOrderRepository.findByIdInAndCustomerId(ids, customerId).stream()
            .map(eventMapper::toDomain)
            .toList());
  }

  @Override
  public PageResult<EventOrder> findOrdersByCustomerId(UUID customerId, PageRequest pageRequest) {
    return readQueryExecutor.execute(
        () ->
            toOrderPageResult(
                readReplicaEventOrderRepository.findByCustomerId(
                    customerId, toPageable(pageRequest))),
        () ->
            toOrderPageResult(
                primaryEventOrderRepository.findByCustomerId(customerId, toPageable(pageRequest))));
  }

  private PageResult<Event> toPageResult(Page<EventEntity> page) {
    return new PageResult<>(
        page.getContent().stream().map(eventMapper::toDomainWithoutOrders).toList(),
        PageMetadata.of(page.getNumber(), page.getSize(), page.getTotalElements()));
  }

  private PageResult<EventOrder> toOrderPageResult(Page<EventOrderEntity> page) {
    return new PageResult<>(
        page.getContent().stream().map(eventMapper::toDomain).toList(),
        PageMetadata.of(page.getNumber(), page.getSize(), page.getTotalElements()));
  }

  private Pageable toPageable(PageRequest pageRequest) {
    return org.springframework.data.domain.PageRequest.of(
        pageRequest.page(), pageRequest.size(), toSort(pageRequest.sort()));
  }

  private Sort toSort(String sort) {
    String[] parts = sort.split(",", 2);
    Sort.Direction direction =
        parts.length == 2 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC;
    return Sort.by(direction, toPersistenceSortField(parts[0])).and(Sort.by(Sort.Direction.ASC, "id"));
  }

  private String toPersistenceSortField(String apiSortField) {
    return switch (apiSortField) {
      case "eventDate" -> "event.date";
      default -> apiSortField;
    };
  }
}
