package app.api.diagnosticruntime.annotation.ai.controller;

import app.api.diagnosticruntime.annotation.ai.service.TaskProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InstanceController {

    private final TaskProcessorService taskProcessorService;

    @PostMapping("/instance/stop")
    public ResponseEntity<Void> hibernateInstance() {
        taskProcessorService.checkAndHibernateEC2();
        return ResponseEntity.ok().build();
    }
}
