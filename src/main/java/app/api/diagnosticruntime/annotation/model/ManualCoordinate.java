package app.api.diagnosticruntime.annotation.model;

import lombok.Data;

@Data
public class ManualCoordinate {
    private String x;
    private String y;
    private String width;
    private String height;
}
