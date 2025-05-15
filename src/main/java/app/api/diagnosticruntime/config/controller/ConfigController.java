package app.api.diagnosticruntime.config.controller;

import app.api.diagnosticruntime.config.model.Config;
import app.api.diagnosticruntime.config.repository.ConfigRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigRepository configRepository;

    public ConfigController(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @GetMapping("/ec2TaskEnabled")
    public boolean getEc2TaskEnabled() {
        return configRepository.findByKey("ec2TaskEnabled")
                .map(config -> Boolean.parseBoolean(config.getValue()))
                .orElse(false);
    }

    @PutMapping("/ec2TaskEnabled")
    public void setEc2TaskEnabled(@RequestParam boolean enabled) {
        Config config = configRepository.findByKey("ec2TaskEnabled")
                .orElse(new Config());
        config.setKey("ec2TaskEnabled");
        config.setValue(String.valueOf(enabled));
        configRepository.save(config);
    }

    @GetMapping("/anonymizer")
    public boolean getAnonymizerEnabled() {
        return configRepository.findByKey("anonymizer")
                .map(config -> Boolean.parseBoolean(config.getValue()))
                .orElse(false);
    }

    @PutMapping("/anonymizer")
    public void setAnonymizerEnabled(@RequestParam boolean enabled) {
        Config config = configRepository.findByKey("anonymizer")
                .orElse(new Config());
        config.setKey("anonymizer");
        config.setValue(String.valueOf(enabled));
        configRepository.save(config);
    }
}
