package app.api.diagnosticruntime.util;

import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class PraidGenerationEventListener extends AbstractMongoEventListener<Patient> {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Patient> event) {
        Patient patient = event.getSource();
        if (patient.getPraidId() == null) {
            // Find the highest existing praidId
            Query query = new Query()
                .with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "praid"));
            Patient highestPraid = mongoTemplate.findOne(query, Patient.class);
            
            long nextPraidId = 1L;
            if (highestPraid != null && highestPraid.getPraidId() != null) {
                nextPraidId = highestPraid.getPraidId() + 1;
            }
            
            patient.setPraidId(nextPraidId);
        }
    }
} 