# 🔧 UI Fix Plan - Comprehensive Solution

## 🚨 **Issue Identified:** 
URI malformed error in Vite development server causing browser accessibility issues.

## 📋 **Root Cause Analysis:**
1. **Vite Server Error**: `URI malformed` error in `viteServeStaticMiddleware`
2. **Port Configuration**: UI running on port 3001 instead of configured 3000
3. **API Proxy Configuration**: Proxy targeting port 8080 but services run on 5000+ ports
4. **Static Asset Serving**: Malformed URI sequence in request URL handling

## 🎯 **Step-by-Step Fix Plan:**

### Phase 1: Fix Vite Configuration Issues
- [ ] Fix port configuration and proxy settings
- [ ] Resolve URI encoding issues in static file serving
- [ ] Update API proxy targets to match actual service ports
- [ ] Clean and rebuild UI dependencies

### Phase 2: Fix API Client Configuration
- [ ] Update base URLs in API clients to match running services
- [ ] Fix organizationClient proxy path issues
- [ ] Ensure proper URL encoding for API endpoints
- [ ] Test API connectivity with running services

### Phase 3: Fix Routing and Navigation
- [ ] Verify React Router configuration
- [ ] Fix any malformed route paths
- [ ] Test navigation between pages
- [ ] Ensure proper URL handling

### Phase 4: Fix Static Asset Issues
- [ ] Clear Vite cache and node_modules
- [ ] Rebuild with clean dependencies
- [ ] Test static asset loading
- [ ] Verify all imports and paths

### Phase 5: Integration Testing
- [ ] Test login and authentication flow
- [ ] Verify all UI components render correctly
- [ ] Test API calls and data loading
- [ ] Ensure responsive design works

## 🔨 **Immediate Actions:**

### Action 1: Fix Vite Configuration
```javascript
// vite.config.js fixes needed:
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3001, // Match actual running port
    proxy: {
      '/api/organization': {
        target: 'http://localhost:5005', // Organization service
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/organization/, '')
      },
      '/api/llm': {
        target: 'http://localhost:5002', // LLM service
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/llm/, '')
      },
      '/api/debate': {
        target: 'http://localhost:5013', // Debate service
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/debate/, '')
      }
    }
  }
})
```

### Action 2: Fix API Client Base URLs
```typescript
// Update API clients to use absolute URLs since services aren't running
class OrganizationClient extends BaseApiClient {
  constructor() {
    super('http://localhost:5005/api'); // Direct service URL
  }
}
```

### Action 3: Clean Build Process
```bash
# Commands to run:
cd debate-ui
rm -rf node_modules package-lock.json
npm install
npm run build
npm run dev
```

## 🎯 **Expected Outcomes:**
1. ✅ UI loads without URI malformed errors
2. ✅ All routes accessible and navigation works
3. ✅ API calls properly configured (even if services are down)
4. ✅ Clean, professional UI display
5. ✅ Responsive design functional

## 🔄 **Rollback Plan:**
If issues persist:
1. Revert to previous working configuration
2. Use mock API responses for development
3. Implement simple static UI for demonstration

## 📊 **Success Metrics:**
- [ ] Browser loads UI without errors
- [ ] All navigation links work
- [ ] No console errors related to URI malformed
- [ ] UI is responsive and visually correct
- [ ] Ready for backend integration when services are built

---

**Next Steps:** Implement fixes in order, test each phase before proceeding to the next.