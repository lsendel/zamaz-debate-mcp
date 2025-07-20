package com.zamaz.workflow.api.graphql.type;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MapTile {
    private String url;
    private int x;
    private int y;
    private int z;
}