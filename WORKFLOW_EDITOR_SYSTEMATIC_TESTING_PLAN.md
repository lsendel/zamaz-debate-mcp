# Workflow Editor Systematic Testing & Fix Plan

## 🚨 Current Issues Identified

1. **Telemetry Map**: No map visible
2. **Stamford Sample**: Map not showing in geospatial sample
3. **Debate Tree**: Screen flashing, no tree structure visible
4. **AI Document Analysis**: PDF file upload not working

## 📋 Systematic Testing Plan

### Phase 1: Basic Application Structure Testing

#### 1.1 Application Load Test
```bash
# Test Steps:
1. Start application: make workflow-ui
2. Open http://localhost:3002
3. Verify header loads: "🔄 Kiro Workflow Editor"
4. Verify navigation bar shows 8 items
5. Check browser console for errors
```

**Expected Result**: Clean application load with no console errors
**Current Status**: ❌ Need to verify

#### 1.2 Navigation Test
```bash
# Test each navigation item:
1. Click "🔀 Workflow Editor" → Should show React-Flow canvas
2. Click "📊 Telemetry Dashboard" → Should show charts/metrics
3. Click "🗺️ Telemetry Map" → Should show interactive map
4. Click "🔍 Spatial Query" → Should show query builder
5. Click "🏢 Stamford Sample" → Should show geospatial data with map
6. Click "💬 Debate Tree" → Should show stable tree structure
7. Click "🌳 Decision Tree" → Should show decision workflow
8. Click "📄 AI Document Analysis" → Should show file upload
```

**Expected Result**: All sections load without flashing or errors
**Current Status**: ❌ Multiple sections failing

---

### Phase 2: Individual Section Diagnosis & Fixes

#### 2.1 Telemetry Map Section 🗺️

**Issue**: No map visible

**Diagnosis Steps**:
1. Check if MapLibre GL JS is loading
2. Verify map container exists
3. Check for CSS styling issues
4. Verify map configuration

**Testing Procedure**:
```bash
# Browser Console Checks:
1. Open Developer Tools → Console
2. Navigate to Telemetry Map
3. Look for errors like:
   - "maplibregl is not defined"
   - "Cannot read property 'addTo' of undefined"
   - CSS/styling issues
   - Network errors for map tiles
```

**Expected Fixes Needed**:
- Install missing MapLibre GL JS dependencies
- Fix map container CSS dimensions
- Configure proper map tile sources
- Add fallback for map loading states

#### 2.2 Stamford Geospatial Sample 🏢

**Issue**: Map not showing in geospatial sample

**Diagnosis Steps**:
1. Check if addresses are being generated
2. Verify map component is rendering
3. Check coordinate bounds for Stamford
4. Verify telemetry data generation

**Testing Procedure**:
```bash
# Console Checks:
1. Navigate to Stamford Sample
2. Check if console shows:
   - "Generating Stamford addresses..."
   - Generated coordinate data
   - Map initialization logs
3. Verify DOM elements exist:
   - Map container
   - Address list
   - Control buttons
```

**Expected Fixes Needed**:
- Fix map component integration
- Ensure address generation works
- Add proper error handling
- Display loading states

#### 2.3 Debate Tree Sample 💬

**Issue**: Screen flashing, no tree visible

**Diagnosis Steps**:
1. Check for infinite re-render loops
2. Verify data fetching from mcp-controller
3. Check React useEffect dependencies
4. Verify tree data structure

**Testing Procedure**:
```bash
# React DevTools Checks:
1. Install React DevTools browser extension
2. Navigate to Debate Tree
3. Watch Components tab for:
   - Rapid re-renders (flashing)
   - State changes in loops
   - Failed API calls
4. Check Network tab for:
   - Failed requests to mcp-controller
   - CORS errors
   - Timeout issues
```

**Expected Fixes Needed**:
- Fix useEffect dependency arrays
- Add proper error boundaries
- Mock debate data if service unavailable
- Stabilize component state

#### 2.4 AI Document Analysis 📄

**Issue**: PDF upload not working

**Diagnosis Steps**:
1. Check file input implementation
2. Verify PDF processing libraries
3. Check integration with mcp-llm service
4. Test file handling

**Testing Procedure**:
```bash
# File Upload Test:
1. Navigate to AI Document Analysis
2. Look for file input element
3. Try uploading a PDF file
4. Check console for:
   - File reading errors
   - API call failures
   - Processing errors
```

**Expected Fixes Needed**:
- Implement proper file upload component
- Add PDF processing library
- Connect to backend service
- Add file validation

---

### Phase 3: Step-by-Step Fix Implementation

#### 3.1 Fix Telemetry Map (Priority: HIGH)

**Step 1: Check Dependencies**
```bash
cd workflow-editor/client/workflow-editor
npm list maplibre-gl
npm list @maplibre/maplibre-gl-leaflet
```

**Step 2: Verify MapLibre Installation**
```bash
# If missing:
npm install maplibre-gl --save
```

**Step 3: Fix Map Component**
- Check TelemetryMap.tsx for proper initialization
- Ensure map container has dimensions
- Add error handling for tile loading

**Step 4: Test Map Loading**
```bash
# Test with basic map:
1. Navigate to Telemetry Map
2. Verify map container appears
3. Check for tile loading
4. Test zoom/pan controls
```

#### 3.2 Fix Stamford Sample (Priority: HIGH)

**Step 1: Debug Address Generation**
```typescript
// Add console logging to see if data generates
console.log('Generated addresses:', addresses);
```

**Step 2: Fix Map Integration**
- Ensure same MapLibre fixes apply
- Verify coordinate bounds are correct
- Check marker rendering

**Step 3: Test Data Flow**
```bash
1. Click "Generate Addresses"
2. Verify 10 addresses appear
3. Check if map shows markers
4. Test address selection
```

#### 3.3 Fix Debate Tree (Priority: HIGH)

**Step 1: Stop Infinite Renders**
```typescript
// Fix useEffect dependencies
useEffect(() => {
  // fetch debate data
}, []); // Empty dependency array if no dependencies
```

**Step 2: Add Error Boundaries**
```typescript
// Wrap component in error boundary
// Add loading states
// Handle API failures gracefully
```

**Step 3: Test Stability**
```bash
1. Navigate to Debate Tree
2. Verify no flashing occurs
3. Check for stable tree display
4. Test node interactions
```

#### 3.4 Fix AI Document Analysis (Priority: HIGH)

**Step 1: Implement File Upload**
```typescript
// Add proper file input
<input type="file" accept=".pdf" onChange={handleFileUpload} />
```

**Step 2: Add PDF Processing**
```bash
# Install PDF library if needed
npm install react-pdf --save
```

**Step 3: Test File Handling**
```bash
1. Navigate to AI Document Analysis
2. Try uploading a PDF
3. Verify file is processed
4. Check AI analysis results
```

---

### Phase 4: Comprehensive Testing Script

#### 4.1 Automated Testing Commands

```bash
# From project root:

# 1. Start application
make workflow-ui

# 2. Wait for startup
sleep 10

# 3. Run E2E tests
make workflow-test

# 4. Generate evidence
cd workflow-editor/e2e-tests
npm run test:ui -- --headed
```

#### 4.2 Manual Testing Checklist

**For Each Section:**
```
□ Navigate to section
□ Wait 5 seconds for loading
□ Take screenshot
□ Check for console errors
□ Test primary interaction
□ Verify data displays
□ Test responsive design
□ Document any issues
```

#### 4.3 Evidence Collection

**Screenshots Required:**
1. `01-workflow-editor-working.png`
2. `02-telemetry-dashboard-working.png`
3. `03-telemetry-map-working.png`
4. `04-spatial-query-working.png`
5. `05-stamford-sample-working.png`
6. `06-debate-tree-working.png`
7. `07-decision-tree-working.png`
8. `08-ai-document-working.png`

**Console Logs Required:**
- No JavaScript errors
- Successful component mounting
- Data loading confirmations
- API call results

---

### Phase 5: Performance & Integration Testing

#### 5.1 Real Data Integration Test

**Verify Real Data Sources:**
```bash
# Check if backend services are needed:
1. mcp-controller for debates: http://localhost:5013
2. mcp-llm for AI analysis: http://localhost:5002
3. Telemetry generation service
4. Map tile services
```

#### 5.2 Cross-Browser Testing

**Test Matrix:**
- Chrome (Desktop)
- Firefox (Desktop) 
- Safari (Desktop)
- Chrome (Mobile)
- Safari (Mobile)

#### 5.3 Performance Benchmarks

**Load Time Targets:**
- Initial app load: < 3 seconds
- Section navigation: < 1 second
- Map rendering: < 5 seconds
- Data generation: < 2 seconds

---

### Phase 6: Success Criteria

**Section-by-Section Success Criteria:**

✅ **Workflow Editor**: React-Flow canvas visible with nodes
✅ **Telemetry Dashboard**: Charts/metrics displaying data  
✅ **Telemetry Map**: Interactive map with markers visible
✅ **Spatial Query**: Query builder form functional
✅ **Stamford Sample**: Map with 10 address markers
✅ **Debate Tree**: Stable tree structure, no flashing
✅ **Decision Tree**: Decision workflow visible
✅ **AI Document**: PDF upload and processing working

**Overall Success Criteria:**
- No console errors across all sections
- All navigation smooth and responsive
- Real data displaying (not mocked)
- All interactions functional
- Performance targets met
- Evidence screenshots captured

---

## 🚀 Next Steps

1. **Start with Telemetry Map** (easiest to fix, high impact)
2. **Fix Stamford Sample** (builds on map fix)
3. **Stabilize Debate Tree** (stop flashing issue)
4. **Implement AI Document Upload** (most complex)
5. **Run comprehensive tests**
6. **Generate evidence package**

Would you like me to start implementing these fixes section by section?