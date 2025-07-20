package com.zamaz.telemetry.domain.repository;

import com.zamaz.telemetry.domain.entity.TelemetryData;
import com.zamaz.telemetry.domain.query.TelemetryQuery;
import com.zamaz.telemetry.domain.service.TelemetryDomainServiceImpl.TimeRange;

import java.util.List;
import java.util.stream.Stream;

public interface TelemetryRepository {
    void saveTimeSeries(TelemetryData data);
    
    void saveSpatialData(TelemetryData data);
    
    Stream<TelemetryData> queryTimeSeries(TimeRange range);
    
    List<TelemetryData> querySpatial(TelemetryQuery.GeoQuery query);
    
    void deleteOldData(TimeRange range);
    
    long count(TimeRange range);
}