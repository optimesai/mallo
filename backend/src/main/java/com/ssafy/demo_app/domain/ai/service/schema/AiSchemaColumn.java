package com.ssafy.demo_app.domain.ai.service.schema;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSchemaColumn {

    private String columnName;
    private String columnComment;
    private String dataType;
    private String columnKey;
    private boolean nullable;
}
