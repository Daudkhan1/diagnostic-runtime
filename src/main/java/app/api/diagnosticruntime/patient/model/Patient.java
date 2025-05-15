package app.api.diagnosticruntime.patient.model;

import app.api.diagnosticruntime.util.AESUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Setter
@Getter
@Document(collection = "patients")
public class Patient {
    @Transient
    public static final String SEQUENCE_NAME = "patient_sequence";

    @Id
    private String id;
    
    @Field("praid")
    @Indexed(unique = true)
    private Long praidId;
    
    @Field("gender")
    private String gender;
    
    @Field("age")
    private Integer age;
    
    @Field("mrn")
    @Indexed(unique = true)
    private String mrn;

    public void setMrn(String rawMrn) {
        if (rawMrn != null && !AESUtil.isEncrypted(rawMrn)) {
            this.mrn = AESUtil.encrypt(rawMrn);
        } else {
            this.mrn = rawMrn;
        }
    }

    public String getDecryptedMrn() {
        return this.mrn != null ? AESUtil.decrypt(this.mrn) : null;
    }

    public String getFormattedPraidId() {
        return praidId != null ? "PRAID-" + praidId : null;
    }
}
