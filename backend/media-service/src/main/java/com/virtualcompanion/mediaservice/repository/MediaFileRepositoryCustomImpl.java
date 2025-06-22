package com.virtualcompanion.mediaservice.repository;

public class MediaFileRepositoryCustomImpl implements MediaFileRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<MediaFile> searchMediaFiles(MediaSearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MediaFile> query = cb.createQuery(MediaFile.class);
        Root<MediaFile> root = query.from(MediaFile.class);

        List<Predicate> predicates = new ArrayList<>();

        // Add search criteria
        if (request.getUserId() != null) {
            predicates.add(cb.equal(root.get("userId"), request.getUserId()));
        }

        if (request.getCharacterId() != null) {
            predicates.add(cb.equal(root.get("characterId"), request.getCharacterId()));
        }

        if (request.getConversationId() != null) {
            predicates.add(cb.equal(root.get("conversationId"), request.getConversationId()));
        }

        if (request.getContentTypes() != null && !request.getContentTypes().isEmpty()) {
            List<Predicate> contentTypePredicates = new ArrayList<>();
            for (String contentType : request.getContentTypes()) {
                contentTypePredicates.add(cb.like(root.get("contentType"), contentType + "%"));
            }
            predicates.add(cb.or(contentTypePredicates.toArray(new Predicate[0])));
        }

        if (request.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartDate()));
        }

        if (request.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndDate()));
        }

        if (request.getProcessingStatus() != null) {
            predicates.add(cb.equal(root.get("processingStatus"), request.getProcessingStatus()));
        }

        if (request.getIsPublic() != null) {
            predicates.add(cb.equal(root.get("isPublic"), request.getIsPublic()));
        }

        // Always exclude deleted files
        predicates.add(cb.isNull(root.get("deletedAt")));

        query.where(predicates.toArray(new Predicate[0]));

        // Apply sorting
        if (request.getSortBy() != null) {
            Path<Object> sortPath = root.get(request.getSortBy());
            if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
                query.orderBy(cb.desc(sortPath));
            } else {
                query.orderBy(cb.asc(sortPath));
            }
        } else {
            query.orderBy(cb.desc(root.get("createdAt")));
        }

        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<MediaFile> countRoot = countQuery.from(MediaFile.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        // Apply pagination
        TypedQuery<MediaFile> typedQuery = entityManager.createQuery(query);
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        List<MediaFile> results = typedQuery.getResultList();

        return new PageImpl<>(results, PageRequest.of(page, size), totalElements);
    }
}
