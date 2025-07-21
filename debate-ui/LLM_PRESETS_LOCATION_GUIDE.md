# 📍 LLM Presets Location Guide

## Where to Find LLM Presets in the Admin Section

### Navigation Path Issues Found

There's a **mismatch** between the navigation menu and actual routes:
- Menu says: `/organizations` 
- Actual route: `/organization-management`

### ✅ **Correct Way to Access LLM Presets**

1. **Direct URL Method** (Recommended):
   ```
   http://localhost:3003/organization-management
   ```

2. **Manual Navigation**:
   - Login with `demo` / `demo123`
   - The sidebar menu shows "Organizations" under Admin
   - However, this may not work due to the route mismatch
   - Use the direct URL above instead

### 📊 **LLM Presets Tab Location**

Once on the Organization Management page, look for these **5 tabs**:

1. 🏢 **Organizations** - First tab
2. 👥 **Users** - Second tab  
3. 🔑 **API Keys** - Third tab
4. ⚡ **LLM Presets** - **Fourth tab** ← **THIS IS WHAT YOU WANT**
5. ⚙️ **Settings** - Fifth tab

### 🖼️ **Visual Structure of LLM Presets Tab**

When you click on the **LLM Presets** tab, you should see:

```
┌─────────────────────────────────────────────────────────────────┐
│ ⚡ LLM Presets                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ LLM Preset Management                                           │
│ Create and manage LLM presets for your organization's debates. │
│ These presets define the AI models, parameters, and prompts    │
│ used by debate participants.                                    │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ ⚡ LLM Presets                                           │   │
│ ├─────────────────────────────────────────────────────────┤   │
│ │                                                         │   │
│ │ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐      │   │
│ │ │Balanced │ │Conserv. │ │Creative │ │   +     │      │   │
│ │ │Debater  │ │ Arguer  │ │Challeng.│ │New Preset│     │   │
│ │ │         │ │         │ │         │ │         │      │   │
│ │ │CLAUDE   │ │OPENAI   │ │GEMINI   │ │         │      │   │
│ │ │Active ✓ │ │Active ✓ │ │Active ✓ │ │         │      │   │
│ │ └─────────┘ └─────────┘ └─────────┘ └─────────┘      │   │
│ │                                                         │   │
│ └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ ⚙️ LLM Configuration                    [Reset] [Save]  │   │
│ ├─────────────────────────────────────────────────────────┤   │
│ │                                                         │   │
│ │ Preset Name: [____________________]                    │   │
│ │ Description: [____________________]                    │   │
│ │                                                         │   │
│ │ Provider:    [▼ Select LLM provider    ]              │   │
│ │              • ⚡ Anthropic Claude                     │   │
│ │              • ⚡ OpenAI GPT                           │   │
│ │              • ⚡ Google Gemini                        │   │
│ │              • ⚡ Meta Llama                           │   │
│ │              • ⚡ Cohere Command                       │   │
│ │              • ⚡ Mistral AI                           │   │
│ │                                                         │   │
│ │ Model:       [▼ Select model           ]              │   │
│ │                                                         │   │
│ │ System Prompt:                                          │   │
│ │ ┌─────────────────────────────────────────────────┐   │   │
│ │ │ Enter the system prompt that will guide the     │   │   │
│ │ │ AI's behavior...                                 │   │   │
│ │ └─────────────────────────────────────────────────┘   │   │
│ │                                                         │   │
│ │ ▼ Advanced Parameters                                   │   │
│ │    Temperature: [────●────] 0.7                        │   │
│ │    Max Tokens:  [1000     ]                           │   │
│ │    Top P:       [────●────] 0.9                        │   │
│ │                                                         │   │
│ └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 🔧 **Fix for Navigation Issue**

The navigation menu needs to be updated. In `src/components/Layout.tsx`, change:
```javascript
{
  key: '/organizations',
  icon: <BankOutlined />,
  label: 'Organizations',
}
```

To:
```javascript
{
  key: '/organization-management',
  icon: <BankOutlined />,
  label: 'Organizations',
}
```

### 📌 **Key Features in LLM Presets Tab**

1. **Preset Cards** - Shows existing presets:
   - Balanced Debater (Claude)
   - Conservative Arguer (OpenAI)
   - Creative Challenger (Gemini)

2. **Configuration Form**:
   - Name & Description
   - Provider selection (6 options)
   - Model selection (provider-specific)
   - System prompt configuration
   - Advanced parameters (temperature, tokens, etc.)

3. **Actions**:
   - Create new presets
   - Edit existing presets
   - Delete presets
   - Copy presets

### 🚨 **Common Issues & Solutions**

1. **Can't see Organizations in menu**:
   - User role must be 'ADMIN'
   - Check `user?.role !== 'ADMIN'` condition

2. **Page is blank**:
   - Use direct URL: `http://localhost:3003/organization-management`
   - Make sure you're logged in first

3. **LLM Presets tab not visible**:
   - It's the 4th tab in Organization Management
   - Look for the ⚡ lightning bolt icon

### ✅ **Verification Steps**

1. Login at `http://localhost:3003/login`
2. Go directly to `http://localhost:3003/organization-management`  
3. Click the 4th tab labeled "LLM Presets"
4. You should see the preset management interface

The LLM Presets functionality is **fully implemented** in the code at:
- Component: `src/components/LLMPresetConfig.tsx`
- Used in: `src/components/OrganizationManagementPage.tsx` (lines 337-365)