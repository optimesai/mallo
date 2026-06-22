package com.ssafy.demo_app.domain.ai.service.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRulePromptServiceTest {

    private final BusinessRulePromptService businessRulePromptService = new BusinessRulePromptService();

    @Test
    void getBusinessRules_includesCoreDomainRules() {
        String rules = businessRulePromptService.getBusinessRules();

        assertThat(rules).contains("current_inventory.current_qty");
        assertThat(rules).contains("item_master.safety_stock");
        assertThat(rules).contains("outbound_shipping.status");
        assertThat(rules).contains("defect_qty");
        assertThat(rules).contains("good_qty");
        assertThat(rules).contains("factory_routing.line_name");
    }
}
