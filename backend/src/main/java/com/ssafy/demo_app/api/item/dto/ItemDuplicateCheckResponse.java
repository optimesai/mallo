package com.ssafy.demo_app.api.item.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemDuplicateCheckResponse {

    private boolean hasDuplicates;
    private List<ItemResponse> items;

    public ItemDuplicateCheckResponse(List<ItemResponse> items) {
        this.items = items;
        this.hasDuplicates = !items.isEmpty();
    }
}
