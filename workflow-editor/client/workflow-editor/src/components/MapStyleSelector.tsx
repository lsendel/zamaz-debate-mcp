import React from 'react';
import { motion } from 'framer-motion';
import { AVAILABLE_MAP_STYLES } from '../config/mapConfig';

interface MapStyleSelectorProps {
  currentStyle: string;
  onStyleChange: (style: string) => void;
  position?: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
}

const MapStyleSelector: React.FC<MapStyleSelectorProps> = ({
  currentStyle,
  onStyleChange,
  position = 'top-right'
}) => {
  const [isOpen, setIsOpen] = React.useState(false);

  const positionClasses = {
    'top-left': 'top-4 left-4',
    'top-right': 'top-4 right-4',
    'bottom-left': 'bottom-4 left-4',
    'bottom-right': 'bottom-4 right-4'
  };

  return (
    <div className={`map-style-selector ${positionClasses[position]}`}>
      <motion.button
        className="style-toggle-button"
        onClick={() => setIsOpen(!isOpen)}
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
      >
        <span className="style-icon">🗺️</span>
        <span className="style-label">Map Style</span>
      </motion.button>

      <motion.div
        className="style-dropdown"
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: isOpen ? 1 : 0, y: isOpen ? 0 : -10 }}
        style={{ display: isOpen ? 'block' : 'none' }}
      >
        <h4>Select Map Style</h4>
        <div className="style-options">
          {Object.entries(AVAILABLE_MAP_STYLES).map(([key, style]) => (
            <motion.button
              key={key}
              className={`style-option ${currentStyle === style.style ? 'active' : ''}`}
              onClick={() => {
                onStyleChange(style.style);
                setIsOpen(false);
              }}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <span className="style-preview">{style.preview}</span>
              <span className="style-name">{style.name}</span>
            </motion.button>
          ))}
        </div>
        
        <div className="premium-styles">
          <h5>Premium Styles (API Key Required)</h5>
          <div className="premium-info">
            <p>For even more professional styles:</p>
            <ul>
              <li>🗺️ <strong>Mapbox</strong>: Streets, Satellite, Blueprint</li>
              <li>🌍 <strong>MapTiler</strong>: Positron, Dark Matter, Topo</li>
              <li>📍 <strong>Stadia</strong>: Alidade Smooth, Outdoors</li>
            </ul>
            <small>Add API keys to your .env file to enable</small>
          </div>
        </div>
      </motion.div>

      <style>{`
        .map-style-selector {
          position: absolute;
          z-index: 1000;
        }

        .style-toggle-button {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px 16px;
          background: white;
          border: none;
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
          cursor: pointer;
          font-size: 14px;
          font-weight: 500;
          color: #333;
          transition: all 0.2s;
        }

        .style-toggle-button:hover {
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }

        .style-icon {
          font-size: 18px;
        }

        .style-dropdown {
          position: absolute;
          top: 50px;
          right: 0;
          background: white;
          border-radius: 12px;
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
          padding: 16px;
          min-width: 280px;
          max-width: 320px;
        }

        .style-dropdown h4 {
          margin: 0 0 12px 0;
          font-size: 16px;
          color: #333;
          font-weight: 600;
        }

        .style-options {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 8px;
          margin-bottom: 16px;
        }

        .style-option {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px;
          background: #f5f5f5;
          border: 2px solid transparent;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.2s;
          font-size: 14px;
          color: #555;
          width: 100%;
          text-align: left;
        }

        .style-option:hover {
          background: #e8e8e8;
          border-color: #ddd;
        }

        .style-option.active {
          background: #e3f2fd;
          border-color: #2196F3;
          color: #1976D2;
          font-weight: 500;
        }

        .style-preview {
          font-size: 24px;
        }

        .style-name {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .premium-styles {
          border-top: 1px solid #e0e0e0;
          padding-top: 12px;
          margin-top: 12px;
        }

        .premium-styles h5 {
          margin: 0 0 8px 0;
          font-size: 13px;
          color: #666;
          font-weight: 600;
        }

        .premium-info {
          background: #f9f9f9;
          border-radius: 6px;
          padding: 12px;
          font-size: 12px;
          color: #666;
        }

        .premium-info p {
          margin: 0 0 8px 0;
        }

        .premium-info ul {
          margin: 0;
          padding-left: 20px;
        }

        .premium-info li {
          margin: 4px 0;
        }

        .premium-info small {
          display: block;
          margin-top: 8px;
          color: #999;
          font-style: italic;
        }

        @media (max-width: 768px) {
          .style-dropdown {
            right: auto;
            left: 0;
            max-width: 90vw;
          }
          
          .style-options {
            grid-template-columns: 1fr;
          }
        }
      `}</style>
    </div>
  );
};

export default MapStyleSelector;