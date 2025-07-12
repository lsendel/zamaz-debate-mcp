# CLAUDE.md - Debate UI Documentation & Guidelines

This file documents the UI structure, functionality, and user preferences for the Debate UI project.

## UI Sections Overview

### 1. Header Section
**Location:** Top of every page
**Components:**
- **Logo & Title**: "AI Debate System" with brain icon
- **Organization Switcher**: Dropdown for multi-tenant support
- **Test LLM Button**: Quick access to LLM connectivity testing
- **New Debate Button**: Primary CTA with blue-purple gradient

**Key Functionality:**
- Sticky header that remains visible during scroll
- Real-time connection status indicator (planned)
- Organization context switching
- Quick access to core actions

### 2. Main Content Area
**Structure:** Tab-based navigation with three main sections

#### a) Debates Tab
**Purpose:** List and manage all debates
**Features:**
- Debate cards showing:
  - Title and topic
  - Participant avatars
  - Status badges (active/completed/paused)
  - Turn count
  - Last activity timestamp
- Filter and sort options
- Real-time status updates via WebSocket

#### b) Templates Tab  
**Purpose:** Pre-configured debate setups
**Features:**
- Template library
- Create custom templates
- One-click debate creation from template
- Template categories (Educational, Business, Philosophy, etc.)

#### c) Settings Tab
**Purpose:** Configuration and preferences
**Features:**
- Ollama setup and configuration
- Default model preferences
- Notification settings
- API key management

### 3. Dialogs & Modals

#### Create Debate Dialog
**Trigger:** "New Debate" button
**Sections:**
1. **Basic Information**
   - Debate name (required)
   - Topic (required)
   - Description (optional)

2. **Debate Rules**
   - Format selection (round_robin, free_form, oxford, panel)
   - Max rounds (limited to 10)
   - Turn length limits (50-2000 chars)

3. **Participants Configuration**
   - Add/remove participants
   - For each participant:
     - Name
     - Position/stance
     - LLM provider selection
     - Model selection
     - Temperature setting
     - System prompt (optional)

#### LLM Test Dialog
**Trigger:** "Test LLM" button
**Features:**
- Provider/model selection
- Custom test prompt input
- Response display with timing
- Token usage information
- Connection status

### 4. Quick Actions Panel
**Location:** Below header on homepage
**Actions:**
- Create New Debate
- View History
- Manage Organizations
- Browse Templates

### 5. Notification System
**Types:**
- Toast notifications (success/error/info)
- WebSocket real-time updates
- Connection status alerts
- Debate event notifications

## Important UI Functionality

### 1. Real-time Updates
**Implementation:** WebSocket connection to debate service
**Updates Include:**
- New debate created
- Turn added to debate
- Debate status changed
- Participant joined/left
- Connection status changes

### 2. Multi-tenant Support
**Features:**
- Organization switcher in header
- Organization-scoped data
- User can belong to multiple orgs
- Default organization selection

### 3. LLM Integration
**Capabilities:**
- Multiple provider support (Claude, OpenAI, Gemini, Llama)
- Model selection per participant
- Quick connectivity testing
- Real-time token usage tracking
- Error handling and fallbacks

### 4. Debate Management
**Core Actions:**
- Create debate with multiple participants
- View live debate progress
- Pause/resume debates
- Export debate transcripts
- Share debate links

### 5. Responsive Design
**Breakpoints:**
- Mobile: < 640px
- Tablet: 640px - 1024px  
- Desktop: > 1024px

**Mobile Adaptations:**
- Hamburger menu navigation
- Stacked layouts
- Touch-friendly controls
- Swipe gestures for tabs

### 6. Keyboard Shortcuts
**Global:**
- `Ctrl/Cmd + N`: New debate
- `Ctrl/Cmd + K`: Search
- `Ctrl/Cmd + /`: Keyboard shortcuts help

**Debate View:**
- `Space`: Pause/resume
- `Arrow keys`: Navigate turns
- `Esc`: Close dialogs

### 7. Accessibility Features
**Implementation:**
- ARIA labels on all interactive elements
- Keyboard navigation support
- Focus management in modals
- Screen reader announcements
- High contrast mode support

### 8. Performance Features
**Optimizations:**
- Lazy loading for debate list
- Virtual scrolling for long debates
- Debounced search inputs
- Optimistic UI updates
- Progressive enhancement

## UI State Management

### Global State
- Current organization
- User preferences
- Active debates list
- WebSocket connection
- LLM provider status

### Local Component State
- Form inputs
- Loading states
- Error messages
- UI toggles
- Pagination

### Persisted State
- Organization selection
- User preferences
- Draft debates
- View preferences

## UI Design Principles

### Visual Quality Standards
- **CSS Issues Are Critical**: User has explicitly mentioned CSS problems need immediate attention
- **No Broken Layouts**: Elements must not have zero dimensions, overflow issues, or appear outside viewport
- **Consistent Styling**: Maintain visual consistency across all components
- **Professional Appearance**: UI should look polished, not like a mockup or prototype

### User Interface Features

#### Essential UI Components
1. **LLM Test Dialog**: Quick test functionality to verify LLM connectivity
   - Accessible via "Test LLM" button in header
   - Shows response time and token usage
   - Supports all configured providers

2. **Organization Switcher**: Multi-tenant support in UI
   - Visible in header
   - Easy access to organization management

3. **Debate Creation Flow**: 
   - Clear form layout
   - Model selection for each participant
   - Rules configuration with sensible defaults
   - Maximum 10 rounds limit

4. **Real-time Updates**: WebSocket integration for live notifications
   - Debate status changes
   - Turn notifications
   - Connection status indicators

### Navigation & User Flow

#### Expected User Journey
1. **Homepage** → Shows active debates, quick actions
2. **Test LLM** → Verify services are working
3. **Create Debate** → Configure participants and rules
4. **View Debates** → List and manage ongoing debates
5. **Debate Details** → Real-time debate progress

#### Key Navigation Elements
- Prominent "New Debate" button with gradient styling
- "Test LLM" button for service verification
- Tab-based navigation for different views
- Keyboard shortcuts support (Ctrl+N for new debate)

### Visual Design Preferences

#### Styling Guidelines
- **Gradient Buttons**: Primary actions use blue-to-purple gradients
- **Icons**: Use Lucide icons consistently (Brain, Zap, Plus, etc.)
- **Dark Mode**: Should be supported if available
- **Responsive**: Must work on mobile, tablet, and desktop

#### Color Scheme
- Primary: Blue to Purple gradient
- Success: Green/Emerald
- Warning: Amber
- Error: Red
- Info: Blue

### Testing Requirements

#### UI Testing Priorities
1. **Visual Regression**: Screenshots at each major step
2. **Responsive Design**: Test all viewport sizes
3. **Interactive Elements**: All buttons/forms must work
4. **Error States**: Proper validation and error messages
5. **Loading States**: Spinners and progress indicators

#### Specific Test Scenarios
- Create debate with all fields
- Test LLM connectivity with each provider
- Navigate through all tabs
- Check WebSocket connections
- Verify responsive design breakpoints

### Performance Expectations

- **Page Load**: Under 5 seconds
- **LLM Response**: Show loading state, display response time
- **Smooth Animations**: No janky transitions
- **Memory Efficient**: No leaks from repeated actions

### Accessibility Requirements

- **Keyboard Navigation**: Full keyboard support
- **ARIA Labels**: All interactive elements properly labeled
- **Focus Management**: Clear focus indicators
- **Screen Reader**: Compatible with assistive technology

## User Interaction Patterns

### What Users Expect
1. **Immediate Feedback**: Loading states, success/error messages
2. **Clear CTAs**: Obvious next actions
3. **Intuitive Forms**: Smart defaults, clear labels
4. **Real-time Updates**: Live debate progress
5. **Service Health**: Visible status indicators

### Common User Tasks
1. Test LLM connectivity before creating debates
2. Create debates with specific model combinations
3. Monitor debate progress in real-time
4. Switch between organizations
5. View debate history and summaries

## Development Reminders

### Always Remember
- **No Mock UIs**: User wants real, working interfaces
- **Test with Real Services**: All features must work end-to-end
- **Fix CSS Issues First**: Visual problems are high priority
- **Document with Screenshots**: Visual proof of functionality

### Quick Wins for Better UX
1. Add loading skeletons instead of blank screens
2. Show connection status in header
3. Implement toast notifications for actions
4. Add keyboard shortcuts dialog
5. Include help tooltips on complex features

## UI Analytics & Monitoring

### Key Metrics to Track
1. **Page Load Performance**
   - Time to First Byte (TTFB)
   - First Contentful Paint (FCP)
   - Largest Contentful Paint (LCP)
   - Time to Interactive (TTI)

2. **User Interaction Metrics**
   - Click-through rates on primary CTAs
   - Form completion rates
   - Error occurrence frequency
   - WebSocket connection stability

3. **Feature Usage**
   - LLM test frequency
   - Debate creation patterns
   - Model selection preferences
   - Navigation flow paths

### Error Tracking Requirements
- Track failed API calls with context
- Log WebSocket disconnection reasons
- Monitor form validation failures
- Record CSS rendering issues

## Component Architecture

### Core UI Components Hierarchy
```
App
├── Header
│   ├── Logo & Title
│   ├── OrganizationSwitcher
│   ├── TestLLMButton
│   └── NewDebateButton
├── MainContent
│   ├── OnboardingWizard
│   ├── QuickActions
│   ├── TabNavigation
│   │   ├── DebatesTab
│   │   ├── TemplatesTab
│   │   └── SettingsTab
│   └── ContentArea
│       ├── DebateList
│       ├── DebateDetail
│       └── DebateCreator
└── Dialogs
    ├── CreateDebateDialog
    ├── LLMTestDialog
    └── KeyboardShortcutsDialog
```

### State Management Patterns
1. **Global State**
   - Current organization
   - User preferences
   - Active debates
   - WebSocket connection status

2. **Local State**
   - Form inputs
   - UI toggles
   - Loading states
   - Error messages

### Data Flow
```
User Action → UI Component → API Call → Service → Response → State Update → UI Update
                                ↓
                          WebSocket → Real-time Updates
```

## CSS Architecture & Troubleshooting

### Common CSS Issues to Check
1. **Layout Problems**
   ```css
   /* Check for: */
   - Missing flexbox/grid properties
   - Incorrect z-index stacking
   - Overflow hidden cutting content
   - Fixed positioning conflicts
   ```

2. **Responsive Breakpoints**
   ```css
   /* Mobile: */ @media (max-width: 640px)
   /* Tablet: */ @media (max-width: 768px)
   /* Desktop: */ @media (min-width: 1024px)
   ```

3. **Dark Mode Considerations**
   - Color contrast ratios
   - Border visibility
   - Shadow adjustments
   - Icon color inversions

### CSS Debugging Checklist
- [ ] Elements visible in viewport
- [ ] No content overflow
- [ ] Proper spacing/padding
- [ ] Consistent typography
- [ ] Interactive states (hover, focus, active)
- [ ] Animation performance
- [ ] Cross-browser compatibility

## Testing Strategy

### Automated UI Tests
1. **Unit Tests**: Individual components
2. **Integration Tests**: Component interactions
3. **E2E Tests**: Full user flows
4. **Visual Regression**: Screenshot comparisons
5. **Performance Tests**: Load time benchmarks

### Manual Testing Checklist
- [ ] Create debate with all provider combinations
- [ ] Test error states (network failure, invalid inputs)
- [ ] Verify real-time updates work
- [ ] Check all keyboard shortcuts
- [ ] Test on different devices/browsers
- [ ] Validate accessibility features

## Future UI Enhancements

### Planned Features
1. **Advanced Debate Views**
   - Split-screen debate comparison
   - Argument threading visualization
   - Sentiment analysis display
   - Export debate transcripts

2. **Enhanced User Experience**
   - Drag-and-drop participant ordering
   - Debate templates library
   - Batch debate creation
   - Advanced filtering/search

3. **Collaboration Features**
   - Share debate links
   - Comment on turns
   - Vote on arguments
   - Collaborative moderation

### Technical Improvements
1. **Performance Optimizations**
   - Virtual scrolling for long debates
   - Lazy loading components
   - Image optimization
   - Code splitting

2. **Developer Experience**
   - Component documentation
   - Storybook integration
   - Visual testing automation
   - Performance monitoring

## Debug Helpers

### Console Commands for Development
```javascript
// Check WebSocket status
window.__checkWebSocket = () => {
  const ws = window.__ws;
  console.log('WebSocket State:', ws?.readyState);
  console.log('Connected:', ws?.readyState === 1);
};

// Debug current app state
window.__debugState = () => {
  console.log('Current Org:', localStorage.getItem('currentOrganizationId'));
  console.log('Active Debates:', window.__activeDebates);
};

// Force refresh data
window.__refreshData = () => {
  window.location.reload();
};
```

### Browser DevTools Tips
1. **Network Tab**: Monitor API calls and WebSocket frames
2. **Performance Tab**: Profile rendering issues
3. **Elements Tab**: Inspect computed styles
4. **Console Tab**: Check for errors and warnings
5. **Application Tab**: View localStorage/sessionStorage

## User Feedback Integration Points

### Where to Add Feedback Mechanisms
1. After debate creation
2. On error occurrences
3. After using new features
4. In empty states
5. During long operations

### Feedback Types to Collect
- Feature requests
- Bug reports
- Performance issues
- UI/UX suggestions
- Model preference feedback