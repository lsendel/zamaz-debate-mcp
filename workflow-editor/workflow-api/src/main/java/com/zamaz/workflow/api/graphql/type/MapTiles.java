package com.zamaz.workflow.api.graphql.type;

import com.zamaz.workflow.api.graphql.SampleApplicationGraphQLController.GeoBounds;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MapTiles {
    private List<MapTile> tiles;
    private GeoBounds bounds;
    private int zoomLevel;
}