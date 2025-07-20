import React, { useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import MapViewer, { MapMarker } from './MapViewer';
import { MAP_CONFIG, getMapStyle } from '../config/mapConfig';

export interface SpatialQuery {
  id: string;
  type: 'radius' | 'polygon' | 'rectangle' | 'corridor';
  name: string;
  geometry: any;
  filters?: {
    deviceTypes?: string[];
    valueRange?: { min: number; max: number };
    timeRange?: { start: Date; end: Date };
  };
}

interface SpatialQueryBuilderProps {
  onQueryCreate: (query: SpatialQuery) => void;
  onQueryUpdate?: (query: SpatialQuery) => void;
  onQueryDelete?: (queryId: string) => void;
  existingQueries?: SpatialQuery[];
  center?: [number, number];
  zoom?: number;
}

const SpatialQueryBuilder: React.FC<SpatialQueryBuilderProps> = ({
  onQueryCreate,
  onQueryUpdate,
  onQueryDelete,
  existingQueries = [],
  center,
  zoom
}) => {
  const [activeQuery, setActiveQuery] = useState<Partial<SpatialQuery> | null>(null);
  const [drawMode, setDrawMode] = useState<'radius' | 'polygon' | 'rectangle' | 'corridor' | null>(null);
  const [queries, setQueries] = useState<SpatialQuery[]>(existingQueries);
  const [selectedQueryId, setSelectedQueryId] = useState<string | null>(null);
  
  // Drawing state
  const [drawingPoints, setDrawingPoints] = useState<[number, number][]>([]);
  const [radius, setRadius] = useState<number>(1000); // meters
  const [corridorWidth, setCorridorWidth] = useState<number>(500); // meters

  const startDrawing = (mode: typeof drawMode) => {
    setDrawMode(mode);
    setDrawingPoints([]);
    setActiveQuery({
      id: `query-${Date.now()}`,
      type: mode!,
      name: `${mode} Query`,
      geometry: null
    });
  };

  const handleMapClick = useCallback((lng: number, lat: number) => {
    if (!drawMode || !activeQuery) return;

    const newPoint: [number, number] = [lng, lat];

    switch (drawMode) {
      case 'radius':
        // Only need center point for radius
        setActiveQuery({
          ...activeQuery,
          geometry: {
            type: 'circle',
            center: newPoint,
            radius: radius
          }
        });
        finishDrawing();
        break;

      case 'rectangle':
        if (drawingPoints.length === 0) {
          setDrawingPoints([newPoint]);
        } else {
          // Second click completes rectangle
          const [first] = drawingPoints;
          setActiveQuery({
            ...activeQuery,
            geometry: {
              type: 'rectangle',
              bounds: [
                [Math.min(first[0], newPoint[0]), Math.min(first[1], newPoint[1])],
                [Math.max(first[0], newPoint[0]), Math.max(first[1], newPoint[1])]
              ]
            }
          });
          finishDrawing();
        }
        break;

      case 'polygon':
      case 'corridor':
        setDrawingPoints([...drawingPoints, newPoint]);
        break;
    }
  }, [drawMode, activeQuery, drawingPoints, radius]);

  const finishDrawing = () => {
    if (!activeQuery || !drawMode) return;

    let finalGeometry;

    switch (drawMode) {
      case 'polygon':
        if (drawingPoints.length >= 3) {
          finalGeometry = {
            type: 'polygon',
            coordinates: [...drawingPoints, drawingPoints[0]] // Close polygon
          };
        }
        break;

      case 'corridor':
        if (drawingPoints.length >= 2) {
          finalGeometry = {
            type: 'corridor',
            path: drawingPoints,
            width: corridorWidth
          };
        }
        break;
    }

    if (finalGeometry || activeQuery.geometry) {
      const completeQuery: SpatialQuery = {
        id: activeQuery.id!,
        type: activeQuery.type!,
        name: activeQuery.name!,
        geometry: finalGeometry || activeQuery.geometry,
        filters: activeQuery.filters
      };

      setQueries([...queries, completeQuery]);
      onQueryCreate(completeQuery);
    }

    // Reset drawing state
    setDrawMode(null);
    setActiveQuery(null);
    setDrawingPoints([]);
  };

  const cancelDrawing = () => {
    setDrawMode(null);
    setActiveQuery(null);
    setDrawingPoints([]);
  };

  const deleteQuery = (queryId: string) => {
    setQueries(queries.filter(q => q.id !== queryId));
    if (onQueryDelete) {
      onQueryDelete(queryId);
    }
    if (selectedQueryId === queryId) {
      setSelectedQueryId(null);
    }
  };

  const updateQueryFilters = (queryId: string, filters: SpatialQuery['filters']) => {
    const updatedQueries = queries.map(q => 
      q.id === queryId ? { ...q, filters } : q
    );
    setQueries(updatedQueries);
    
    const updatedQuery = updatedQueries.find(q => q.id === queryId);
    if (updatedQuery && onQueryUpdate) {
      onQueryUpdate(updatedQuery);
    }
  };

  // Convert queries to map markers/shapes for visualization
  const getQueryMarkers = (): MapMarker[] => {
    return queries
      .filter(q => q.geometry.type === 'circle')
      .map(q => ({
        id: q.id,
        lng: q.geometry.center[0],
        lat: q.geometry.center[1],
        color: '#FF9800',
        icon: '🎯',
        properties: {
          label: q.name,
          description: `Radius: ${q.geometry.radius}m`
        }
      }));
  };

  return (
    <div className="spatial-query-builder">
      <div className="query-controls">
        <h3>Spatial Query Builder</h3>
        
        <div className="drawing-tools">
          <button
            className={`tool-button ${drawMode === 'radius' ? 'active' : ''}`}
            onClick={() => startDrawing('radius')}
            disabled={drawMode !== null}
          >
            <span>⭕</span> Radius
          </button>
          <button
            className={`tool-button ${drawMode === 'rectangle' ? 'active' : ''}`}
            onClick={() => startDrawing('rectangle')}
            disabled={drawMode !== null}
          >
            <span>▭</span> Rectangle
          </button>
          <button
            className={`tool-button ${drawMode === 'polygon' ? 'active' : ''}`}
            onClick={() => startDrawing('polygon')}
            disabled={drawMode !== null}
          >
            <span>⬟</span> Polygon
          </button>
          <button
            className={`tool-button ${drawMode === 'corridor' ? 'active' : ''}`}
            onClick={() => startDrawing('corridor')}
            disabled={drawMode !== null}
          >
            <span>〰️</span> Corridor
          </button>
        </div>

        {drawMode && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="drawing-controls"
          >
            <div className="drawing-info">
              <p>Drawing: {drawMode}</p>
              {drawMode === 'radius' && (
                <div className="parameter-control">
                  <label>Radius (m):</label>
                  <input
                    type="number"
                    value={radius}
                    onChange={(e) => setRadius(Number(e.target.value))}
                    min="100"
                    max="10000"
                    step="100"
                  />
                </div>
              )}
              {drawMode === 'corridor' && (
                <div className="parameter-control">
                  <label>Width (m):</label>
                  <input
                    type="number"
                    value={corridorWidth}
                    onChange={(e) => setCorridorWidth(Number(e.target.value))}
                    min="100"
                    max="5000"
                    step="100"
                  />
                </div>
              )}
              {(drawMode === 'polygon' || drawMode === 'corridor') && drawingPoints.length > 0 && (
                <p>Points: {drawingPoints.length}</p>
              )}
            </div>
            <div className="drawing-actions">
              {(drawMode === 'polygon' || drawMode === 'corridor') && drawingPoints.length >= 2 && (
                <button className="finish-button" onClick={finishDrawing}>
                  ✓ Finish
                </button>
              )}
              <button className="cancel-button" onClick={cancelDrawing}>
                ✕ Cancel
              </button>
            </div>
          </motion.div>
        )}
      </div>

      <div className="query-map-container">
        <MapViewer
          center={center || MAP_CONFIG.bounds.stamfordCT.center as [number, number]}
          zoom={zoom || 12}
          markers={getQueryMarkers()}
          height="400px"
          onMapClick={handleMapClick}
          style={getMapStyle()}
        />
      </div>

      <div className="query-list">
        <h4>Active Queries ({queries.length})</h4>
        {queries.map(query => (
          <motion.div
            key={query.id}
            className={`query-item ${selectedQueryId === query.id ? 'selected' : ''}`}
            onClick={() => setSelectedQueryId(query.id)}
            whileHover={{ scale: 1.02 }}
          >
            <div className="query-header">
              <span className="query-type-icon">
                {query.type === 'radius' && '⭕'}
                {query.type === 'rectangle' && '▭'}
                {query.type === 'polygon' && '⬟'}
                {query.type === 'corridor' && '〰️'}
              </span>
              <span className="query-name">{query.name}</span>
              <button
                className="delete-button"
                onClick={(e) => {
                  e.stopPropagation();
                  deleteQuery(query.id);
                }}
              >
                🗑️
              </button>
            </div>
            
            {selectedQueryId === query.id && (
              <motion.div
                initial={{ height: 0 }}
                animate={{ height: 'auto' }}
                className="query-details"
              >
                <div className="filter-section">
                  <h5>Filters</h5>
                  <div className="filter-control">
                    <label>Device Types:</label>
                    <select
                      multiple
                      value={query.filters?.deviceTypes || []}
                      onChange={(e) => {
                        const selected = Array.from(e.target.selectedOptions, option => option.value);
                        updateQueryFilters(query.id, {
                          ...query.filters,
                          deviceTypes: selected
                        });
                      }}
                    >
                      <option value="temperature">Temperature</option>
                      <option value="pressure">Pressure</option>
                      <option value="humidity">Humidity</option>
                      <option value="speed">Speed</option>
                    </select>
                  </div>
                  
                  <div className="filter-control">
                    <label>Value Range:</label>
                    <div className="range-inputs">
                      <input
                        type="number"
                        placeholder="Min"
                        value={query.filters?.valueRange?.min || ''}
                        onChange={(e) => updateQueryFilters(query.id, {
                          ...query.filters,
                          valueRange: {
                            min: Number(e.target.value),
                            max: query.filters?.valueRange?.max || 100
                          }
                        })}
                      />
                      <span>-</span>
                      <input
                        type="number"
                        placeholder="Max"
                        value={query.filters?.valueRange?.max || ''}
                        onChange={(e) => updateQueryFilters(query.id, {
                          ...query.filters,
                          valueRange: {
                            min: query.filters?.valueRange?.min || 0,
                            max: Number(e.target.value)
                          }
                        })}
                      />
                    </div>
                  </div>
                </div>
              </motion.div>
            )}
          </motion.div>
        ))}
      </div>

      <style>{`
        .spatial-query-builder {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .query-controls {
          margin-bottom: 20px;
        }

        .query-controls h3 {
          margin: 0 0 15px 0;
          color: #333;
        }

        .drawing-tools {
          display: flex;
          gap: 10px;
          margin-bottom: 15px;
        }

        .tool-button {
          padding: 10px 15px;
          border: 2px solid #ddd;
          background: white;
          border-radius: 4px;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;
          transition: all 0.2s;
        }

        .tool-button:hover:not(:disabled) {
          border-color: #2196F3;
          background: #f5f5f5;
        }

        .tool-button.active {
          border-color: #2196F3;
          background: #e3f2fd;
          color: #1976D2;
        }

        .tool-button:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .tool-button span {
          font-size: 18px;
        }

        .drawing-controls {
          background: #f5f5f5;
          padding: 15px;
          border-radius: 4px;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .drawing-info p {
          margin: 0 0 10px 0;
          font-size: 14px;
          color: #666;
        }

        .parameter-control {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-top: 10px;
        }

        .parameter-control label {
          font-size: 14px;
          color: #333;
        }

        .parameter-control input {
          width: 100px;
          padding: 5px 10px;
          border: 1px solid #ddd;
          border-radius: 4px;
        }

        .drawing-actions {
          display: flex;
          gap: 10px;
        }

        .finish-button, .cancel-button {
          padding: 8px 16px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: background 0.2s;
        }

        .finish-button {
          background: #4CAF50;
          color: white;
        }

        .finish-button:hover {
          background: #45a049;
        }

        .cancel-button {
          background: #f44336;
          color: white;
        }

        .cancel-button:hover {
          background: #da190b;
        }

        .query-map-container {
          margin-bottom: 20px;
        }

        .query-list {
          border-top: 1px solid #e0e0e0;
          padding-top: 20px;
        }

        .query-list h4 {
          margin: 0 0 15px 0;
          color: #333;
        }

        .query-item {
          background: #f9f9f9;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          padding: 12px;
          margin-bottom: 10px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .query-item:hover {
          border-color: #2196F3;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .query-item.selected {
          border-color: #2196F3;
          background: #e3f2fd;
        }

        .query-header {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .query-type-icon {
          font-size: 20px;
        }

        .query-name {
          flex: 1;
          font-weight: 500;
          color: #333;
        }

        .delete-button {
          background: none;
          border: none;
          cursor: pointer;
          font-size: 16px;
          opacity: 0.6;
          transition: opacity 0.2s;
        }

        .delete-button:hover {
          opacity: 1;
        }

        .query-details {
          margin-top: 15px;
          padding-top: 15px;
          border-top: 1px solid #ddd;
          overflow: hidden;
        }

        .filter-section h5 {
          margin: 0 0 10px 0;
          font-size: 14px;
          color: #666;
        }

        .filter-control {
          margin-bottom: 15px;
        }

        .filter-control label {
          display: block;
          margin-bottom: 5px;
          font-size: 13px;
          color: #333;
        }

        .filter-control select {
          width: 100%;
          padding: 5px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 13px;
        }

        .range-inputs {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .range-inputs input {
          flex: 1;
          padding: 5px 10px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 13px;
        }
      `}</style>
    </div>
  );
};

export default SpatialQueryBuilder;