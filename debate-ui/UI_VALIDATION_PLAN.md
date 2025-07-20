# UI Validation Plan - Ant Design Standard

## Objective
Systematically validate that all sections of the application are using Ant Design components and identify any remaining issues.

## Validation Criteria
For each page/section, check:
1. ✅ All UI components are from Ant Design (no @zamaz/ui imports)
2. ✅ Icons are from @ant-design/icons (no lucide-react)
3. ✅ Styling uses Ant Design theme or inline styles (no Tailwind classes)
4. ✅ Forms use Ant Design Form components
5. ✅ Modals/Dialogs use Ant Design Modal
6. ✅ Tables use Ant Design Table
7. ✅ Notifications use Ant Design notification API

## Pages to Validate

### 1. Login Page (/login)
- [ ] Login form
- [ ] Register form
- [ ] Tab switching
- [ ] Form validation
- [ ] Error messages
- [ ] Development mode alert

### 2. Main Layout
- [ ] Sidebar navigation
- [ ] Header with user menu
- [ ] Organization switcher
- [ ] Menu items and icons
- [ ] Responsive behavior

### 3. Debates Page (/)
- [ ] Debate cards grid
- [ ] Create debate button
- [ ] Refresh button
- [ ] Empty state
- [ ] Status badges
- [ ] Action buttons (Start, Pause, Cancel)

### 4. Create Debate Dialog
- [ ] Modal structure
- [ ] Form fields
- [ ] Participant cards
- [ ] Add/Remove participant
- [ ] Provider/Model selects
- [ ] Temperature slider
- [ ] Form validation

### 5. Debate Detail Page (/debates/:id)
- [ ] Debate info header
- [ ] Progress indicator
- [ ] Round cards
- [ ] Response display
- [ ] Action buttons
- [ ] Polling indicator

### 6. Workflow Editor (/workflow)
- [ ] Page structure
- [ ] Components used
- [ ] Any custom elements

### 7. Analytics Page (/analytics)
- [ ] Metric cards
- [ ] Progress indicators
- [ ] Data visualization
- [ ] Statistics display

### 8. Organizations Page (/organizations)
- [ ] Organization cards
- [ ] User management table
- [ ] API keys section
- [ ] Create/Edit modals
- [ ] Tab navigation

### 9. Settings Page (/settings)
- [ ] Settings form
- [ ] Section cards
- [ ] Save functionality
- [ ] Form validation

## Validation Process
1. Use Puppeteer to navigate to each page
2. Take screenshots for visual validation
3. Check console for errors
4. Verify component rendering
5. Test interactive elements
6. Document any issues found

## Expected Issues to Check
- Remaining Tailwind classes that need conversion
- Any missed @zamaz/ui imports
- Styling inconsistencies
- Component functionality issues
- Missing Ant Design theme configuration