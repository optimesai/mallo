package com.ssafy.demo_app.domain.ai.service.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AiSchemaTable {

    private String tableName;
    private String tableComment;
    private List<AiSchemaColumn> columns = new ArrayList<>();
}
