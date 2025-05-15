package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "migrateAnnotationsToUsePatientSlideId", order = "011", author = "zaid", transactional = false)
public class DatabaseChangelog011 {


    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {


        mongoTemplate.getCollection("cases").updateMany(
                new Document(),
                new Document("$unset", new Document("id", ""))
        );

        List<ObjectId> orphanedAnnotationIds = new ArrayList<>();

        // Step 1: Fetch all annotations
        List<Document> annotationDocuments = mongoTemplate.getCollection("annotations")
                .find().into(new ArrayList<>());

        for (Document annotationDocument : annotationDocuments) {
            String caseId = annotationDocument.getString("case_id");

            // Step 2: Find the corresponding PatientSlide for the Case
            Document query = ObjectId.isValid(caseId)
                    ? new Document("_id", new ObjectId(caseId))
                    : new Document("name", caseId);

            Document caseDocument = mongoTemplate.getCollection("cases").find(query).first();

            if (caseDocument == null) {
                System.out.println("No Case found for case_id: " + caseId);
                // Remove the annotation
                if (annotationDocument.getObjectId("_id") != null) {
                    orphanedAnnotationIds.add(annotationDocument.getObjectId("_id"));
                }
                continue;
            }

            String caseObjectId = caseDocument.getObjectId("_id").toString();

            // Fetch the corresponding PatientSlide using caseObjectId
            Document patientSlide = mongoTemplate.getCollection("patient_slides")
                    .find(new Document("case_id", caseObjectId))
                    .first();

            if (patientSlide == null) {
                System.out.println("No PatientSlide found for Case ID: " + caseObjectId);
                continue;
            }

            String patientSlideId = patientSlide.getObjectId("_id").toString();

            List<Document> updatedAnnotations = new ArrayList<>();
            // Step 3: Migrate the annotation data
            List<Document> annotations = (ArrayList<Document>) annotationDocument.get("annotations");

            for (Document annotationData : annotations) {
                Document updatedAnnotation = new Document();
                updatedAnnotation.append("_id", new org.bson.types.ObjectId());
                updatedAnnotation.append("patient_slide_id", patientSlideId); // Replace caseId with patientSlideId
                updatedAnnotation.append("annotation_type", convertAnnotationType(annotationDocument.getString("annotation_type")));
                updatedAnnotation.append("name", annotationData.getString("name"));
                updatedAnnotation.append("biological_type", convertBiologicalType(annotationData.getString("type")));
                updatedAnnotation.append("shape", annotationData.getString("shape"));
                updatedAnnotation.append("description", annotationData.getString("description"));
                updatedAnnotation.append("coordinates", annotationData.get("coordinates"));
                updatedAnnotation.append("color", "#FFFFFF"); // Default color (can be customized)

                updatedAnnotations.add(updatedAnnotation);
            }

            // Save the updated document
            if (!updatedAnnotations.isEmpty()) {
                mongoTemplate.getCollection("annotations").insertMany(updatedAnnotations);
            }

            if (!orphanedAnnotationIds.isEmpty()) {
                mongoTemplate.getCollection("annotations").deleteMany(
                        new Document("_id", new Document("$in", orphanedAnnotationIds))
                );
            }

            // Step 4: Remove the old annotation document
            mongoTemplate.getCollection("annotations")
                    .deleteOne(new org.bson.Document("_id", annotationDocument.getObjectId("_id")));
        }
    }

    private String convertAnnotationType(String type) {
        if ("manual".equalsIgnoreCase(type)) {
            return "MANUAL";
        } else if ("AI".equalsIgnoreCase(type)) {
            return "AI";
        }
        throw new IllegalArgumentException("Invalid annotation type: " + type);
    }

    private String convertBiologicalType(String type) {
        switch (type.toLowerCase()) {
            case "mitosis (metaphase)":
                return "MITOSIS_METAPHASE";
            case "mitosis":
                return "MITOSIS_METAPHASE";
            case "blood vessels":
                return "BLOOD_VESSELS";
            case "nerves":
                return "NERVES";
            case "g1":
                return "G1";
            case "g2":
                return "G2";
            case "g3":
                return "G3";
            case "g4":
                return "G4";
            case "gx":
                return "GX";
            case "normal":
                return "NORMAL";
            case "nuclear membrane":
                return "NUCLEAR_MEMBRANE";
            case "nucleoli":
                return "NUCLEOLI";
            case "tubule formation":
                return "TUBULE_FORMATION";
            case "pleomorphism":
                return "PLEOMORPHISM";
            default:
                throw new IllegalArgumentException("Invalid biological type: " + type);
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        throw new UnsupportedOperationException("Rollback not supported for this migration.");
    }
}
