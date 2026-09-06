package com.neepo.backend.config;

import com.neepo.care.context.ContextFreshnessPolicy;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.telemetry.AdaptiveTelemetryPolicy;
import com.neepo.care.telemetry.AdaptiveTelemetryPolicyEvaluator;
import com.neepo.care.detection.DisruptionDetectionPolicy;
import com.neepo.care.detection.DisruptionDetector;
import com.neepo.care.orchestration.RecoveryDecisionEngine;
import com.neepo.care.orchestration.RecoveryReplanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CareEngineConfig {
    @Bean
    RecoveryDecisionEngine recoveryDecisionEngine() {
        return RecoveryDecisionEngine.demoDefaults();
    }

    @Bean
    RecoveryReplanner recoveryReplanner(RecoveryDecisionEngine recoveryDecisionEngine) {
        return new RecoveryReplanner(recoveryDecisionEngine);
    }

    @Bean
    DisruptionDetector disruptionDetector() {
        return new DisruptionDetector(
                new ContextFreshnessValidator(ContextFreshnessPolicy.defaults()),
                DisruptionDetectionPolicy.demoDefaults()
        );
    }

    @Bean
    AdaptiveTelemetryPolicyEvaluator adaptiveTelemetryPolicyEvaluator() {
        return new AdaptiveTelemetryPolicyEvaluator(
                new ContextFreshnessValidator(ContextFreshnessPolicy.defaults()),
                AdaptiveTelemetryPolicy.demoDefaults()
        );
    }
}
