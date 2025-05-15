package app.api.diagnosticruntime.annotation.repository;

import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.model.AnnotationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnnotationRepository extends MongoRepository<Annotation, String> {
    // Custom query methods, if needed

    Optional<Annotation> findByIdAndPatientSlideIdAndIsDeleted(String id, String patientSlideId, boolean deleted);
    

    Optional<Annotation> findByIdAndIsDeleted(String id, boolean deleted);

    @Query(sort = "{ 'name' : 1 }")
    List<Annotation> findAllByPatientSlideIdAndIsDeleted(String patientSlideId, boolean deleted);

    List<Annotation> findAllByPatientSlideIdInAndIsDeleted(Collection<String> patientSlideId, boolean deleted);

    List<Annotation> findAllByPatientSlideIdAndIdAndIsDeleted(String patientSlideId, String id, boolean deleted);

    List<Annotation> findAllByIsDeleted(boolean deleted);

    boolean existsByName(String generatedName);

    Long countAllByPatientSlideIdAndIsDeleted(String patientSlideId, boolean deleted);

    List<Annotation> findAllByPatientSlideIdAndIdInAndIsDeletedAndAnnotationType(
        String patientSlideId, List<String> ids, boolean isDeleted, AnnotationType annotationType);

    List<Annotation> findAllByPatientSlideIdAndIsDeletedAndAnnotationType(String patientSlideId, boolean isDeleted, AnnotationType annotationType);
}

