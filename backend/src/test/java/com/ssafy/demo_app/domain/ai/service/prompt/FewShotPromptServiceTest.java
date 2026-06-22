package com.ssafy.demo_app.domain.ai.service.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FewShotPromptServiceTest {

    private final FewShotPromptService fewShotPromptService = new FewShotPromptService();

    @Test
    void getFewShotExamples_loadsYamlExamples() {
        String prompt = fewShotPromptService.getFewShotExamples();

        assertThat(prompt).contains("이번 달 A 라인");
        assertThat(prompt).contains("production_execution");
        assertThat(prompt).contains("current_inventory");
        assertThat(prompt).contains("bom_structure");
    }

    @Test
    void evictCache_keepsExamplesReloadable() {
        String first = fewShotPromptService.getFewShotExamples();

        fewShotPromptService.evictCache();
        String second = fewShotPromptService.getFewShotExamples();

        assertThat(second).isEqualTo(first);
    }
}
