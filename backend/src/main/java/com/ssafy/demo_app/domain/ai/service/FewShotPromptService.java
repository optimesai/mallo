package com.ssafy.demo_app.domain.ai.service;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class FewShotPromptService {

    private static final String FEW_SHOT_RESOURCE = "ai/few-shot-examples.yml";

    public String getFewShotExamples() {
        List<FewShotExample> examples = loadExamples();
        if (examples.isEmpty()) {
            return "No examples.";
        }

        StringBuilder builder = new StringBuilder();
        for (FewShotExample example : examples) {
            builder.append("- Category: ")
                    .append(example.getCategory())
                    .append("\n")
                    .append("  Question: ")
                    .append(example.getQuestion())
                    .append("\n")
                    .append("  SQL:\n")
                    .append(indentSql(example.getSql()))
                    .append("  Notes: ")
                    .append(example.getNotes())
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private List<FewShotExample> loadExamples() {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ClassPathResource(FEW_SHOT_RESOURCE));
        Properties properties = factoryBean.getObject();
        List<FewShotExample> examples = new ArrayList<>();
        if (properties == null) {
            return examples;
        }

        for (int index = 0; ; index++) {
            String prefix = "examples[" + index + "].";
            String question = properties.getProperty(prefix + "question");
            String sql = properties.getProperty(prefix + "sql");
            if (question == null || sql == null) {
                break;
            }

            FewShotExample example = new FewShotExample();
            example.setId(properties.getProperty(prefix + "id", ""));
            example.setCategory(properties.getProperty(prefix + "category", ""));
            example.setQuestion(question);
            example.setSql(sql.trim());
            example.setNotes(properties.getProperty(prefix + "notes", ""));
            examples.add(example);
        }

        return examples;
    }

    private String indentSql(String sql) {
        return "    " + sql.replace("\n", "\n    ") + "\n";
    }
}
