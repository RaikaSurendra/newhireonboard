# Frontend Components Documentation

## 🎨 Component Architecture

### **Reusable Component-Based Design**
All components are built following React best practices with TypeScript for type safety and Material-UI for consistent styling.

---

## 📁 Project Structure

```
frontend/src/
├── api/                      # API Client Layer
│   ├── axiosConfig.ts       # Axios setup with interceptors
│   ├── authApi.ts           # Authentication APIs
│   ├── planApi.ts           # Onboarding Plan APIs
│   └── taskApi.ts           # Task & Template APIs
│
├── components/              # Reusable Components
│   ├── common/
│   │   ├── Button.tsx       # Custom button with loading state
│   │   ├── Card.tsx         # Reusable card component
│   │   └── Loader.tsx       # Loading spinner
│   └── layout/
│       ├── Header.tsx       # App header with user menu
│       ├── Sidebar.tsx      # Navigation sidebar
│       └── Layout.tsx       # Main layout wrapper
│
├── pages/                   # Page Components
│   ├── auth/
│   │   └── Login.tsx        # Login page
│   ├── dashboard/
│   │   └── Dashboard.tsx    # Main dashboard
│   ├── plans/
│   │   ├── PlanList.tsx     # List all plans
│   │   ├── PlanCreate.tsx   # Create new plan
│   │   ├── PlanDetails.tsx  # View plan with templates
│   │   └── TaskTemplateCreate.tsx  # Create task template
│   └── tasks/
│       └── MyTasks.tsx      # User's task list
│
├── theme/
│   └── serviceNowTheme.ts   # Material-UI theme (ServiceNow style)
│
├── App.tsx                  # Main app with routing
├── main.tsx                 # Entry point
└── vite-env.d.ts           # TypeScript definitions

```

---

## 🧩 Component Details

### **1. Common Components**

#### **Button.tsx**
- Extends Material-UI Button
- Built-in loading state with spinner
- Consistent styling across app

```typescript
<Button loading={isLoading} variant="contained">
  Submit
</Button>
```

#### **Card.tsx**
- Reusable card with title, subtitle, and actions
- Consistent padding and shadows
- Flexible content area

```typescript
<Card title="My Card" subtitle="Description" actions={<Button>Action</Button>}>
  Content here
</Card>
```

#### **Loader.tsx**
- Centered loading spinner
- Customizable message and size
- Used for async operations

```typescript
<Loader message="Loading data..." />
```

---

### **2. Layout Components**

#### **Header.tsx**
- Fixed app bar with branding
- User menu with profile/settings/logout
- Notification badge
- Responsive menu toggle

**Features:**
- User avatar with initials
- Notification count badge
- Dropdown menu for user actions
- Logout functionality

#### **Sidebar.tsx**
- Persistent navigation drawer
- Role-based menu items
- Active route highlighting
- Icons for each menu item

**Menu Items:**
- Dashboard
- Onboarding Plans (Manager/HR only)
- My Tasks
- Buddy Matches
- Messages
- Feedback
- Users (Admin only)
- Analytics (Admin/HR only)

#### **Layout.tsx**
- Main layout wrapper
- Combines Header + Sidebar + Content
- Responsive design
- Consistent spacing

---

### **3. Page Components**

#### **Login.tsx**
- Email/password authentication
- Loading state during login
- Error handling with alerts
- Link to registration
- Remember default credentials hint

**Features:**
- Form validation
- JWT token storage
- Redirect to dashboard on success
- Beautiful gradient background

#### **Dashboard.tsx**
- Overview of onboarding progress
- Stats cards (Total, Completed, Pending, Upcoming)
- Progress bar with percentage
- Recent tasks section
- Upcoming meetings section

**Stats Displayed:**
- Total tasks count
- Completed tasks count
- Pending tasks count
- Upcoming tasks count
- Overall completion percentage

#### **PlanList.tsx**
- Grid view of all onboarding plans
- Search functionality
- Filter by department/status
- Create new plan button
- Plan cards with key info

**Plan Card Shows:**
- Plan name and description
- Active/Draft status
- Department
- Duration in days
- Version number
- Template count
- Usage count

#### **PlanCreate.tsx**
- Form to create new onboarding plan
- Input validation
- Department dropdown
- Duration input
- Description textarea
- Cancel/Submit actions

**Form Fields:**
- Plan Name (required)
- Description (required)
- Department (dropdown, required)
- Duration in Days (number, required)

#### **PlanDetails.tsx**
- View plan information
- List all task templates
- Add new template button
- Publish plan action
- Template table with actions

**Displays:**
- Plan metadata (duration, department, usage)
- Stats cards
- Task templates table
- Edit/Delete actions per template
- Publish button for draft plans

#### **TaskTemplateCreate.tsx**
- Comprehensive form for task templates
- Organized in sections
- All task properties
- Execution mode configuration
- Tags and categories

**Form Sections:**
1. **Basic Information**
   - Task name
   - Description
   - Priority

2. **Task Classification**
   - Task Type (Administrative, Technical, etc.)
   - Owner Type (Manager, New Hire, etc.)
   - Assignee Type (Employee, Buddy, etc.)

3. **Timing & Execution**
   - Day Offset (when task starts)
   - Estimated Duration
   - Execution Mode (Sequential/Parallel)
   - Sequence Order (for sequential)
   - Parallel Group (for parallel)

4. **Additional Details**
   - Category
   - Tags (multi-select)

#### **MyTasks.tsx**
- User's personal task list
- Tabs for Active/Completed/Blocked
- Stats overview
- Progress bar
- Task cards with actions

**Features:**
- Filter by status
- Start task action
- Complete task action
- Blocked task indicator
- Due date display
- Priority badges

---

## 🎨 Theme & Styling

### **ServiceNow Theme**
- **Primary Color**: #0F62FE (ServiceNow Blue)
- **Secondary Color**: #6929C4 (ServiceNow Purple)
- **Success**: #24A148 (Green)
- **Warning**: #F1C21B (Yellow)
- **Error**: #DA1E28 (Red)

### **Typography**
- **Font Family**: Gilroy (with fallbacks)
- **Weights**: 400 (Regular), 500 (Medium), 600 (SemiBold), 700 (Bold)
- **Headings**: h1-h6 with appropriate sizes
- **Body**: Readable line-height and spacing

### **Components**
- **Buttons**: No text transform, 600 weight
- **Cards**: Subtle shadows
- **Inputs**: Blue focus border

---

## 🔌 API Integration

### **Axios Configuration**
- Base URL from environment
- Request interceptor adds JWT token
- Response interceptor handles 401 errors
- Automatic token refresh
- Redirect to login on auth failure

### **API Services**

#### **authApi.ts**
- `login(email, password)` - User login
- `register(data)` - User registration
- `logout()` - User logout
- `refreshToken(token)` - Refresh JWT
- `getCurrentUser()` - Get current user info

#### **planApi.ts**
- `getAll(params)` - List all plans
- `getById(id, version)` - Get plan details
- `create(data)` - Create new plan
- `update(id, data)` - Update plan (creates new version)
- `publish(id, notes)` - Publish plan
- `delete(id)` - Deactivate plan
- `getVersions(id)` - Get version history

#### **taskApi.ts**
- `getTemplatesByPlan(planId)` - Get all templates for a plan
- `createTemplate(planId, data)` - Create task template
- `updateTemplate(planId, templateId, data)` - Update template
- `deleteTemplate(planId, templateId)` - Delete template
- `getMyTasks(params)` - Get user's tasks
- `getTaskById(id)` - Get task details
- `updateTaskStatus(id, status)` - Update task status
- `completeTask(id)` - Mark task as complete

---

## 🚀 Features Implemented

### ✅ **Authentication**
- Login with JWT
- Token storage in localStorage
- Auto-refresh on 401
- Protected routes
- Logout functionality

### ✅ **Onboarding Plans**
- List all plans
- Create new plan
- View plan details
- Publish plan
- Search and filter

### ✅ **Task Templates**
- Create task template
- Configure all properties
- Sequential/Parallel execution
- Task type classification
- Owner and assignee types

### ✅ **Tasks**
- View my tasks
- Filter by status
- Start task
- Complete task
- Track progress
- Blocked task handling

### ✅ **UI/UX**
- Responsive design
- Loading states
- Error handling
- Success feedback
- Role-based navigation
- Consistent styling

---

## 📱 Responsive Design

All components are fully responsive:
- **Desktop**: Full sidebar, multi-column grids
- **Tablet**: Collapsible sidebar, 2-column grids
- **Mobile**: Hidden sidebar (toggle), single column

---

## 🔐 Security

- JWT tokens in localStorage
- Automatic token refresh
- Protected routes
- Role-based access control
- CORS configured
- XSS protection

---

## 🎯 Next Steps to Implement

### **Additional Pages Needed:**
1. **Buddy Matches** - View and manage buddy relationships
2. **Messages** - Real-time messaging between users
3. **Feedback** - Submit and view feedback
4. **User Management** - Admin user CRUD
5. **Analytics** - Dashboard with charts and metrics
6. **Profile** - User profile page
7. **Settings** - App settings and preferences

### **Additional Features:**
1. **Real-time Notifications** - WebSocket integration
2. **File Upload** - Avatar and document uploads
3. **Search** - Global search functionality
4. **Filters** - Advanced filtering options
5. **Sorting** - Table sorting
6. **Pagination** - For large lists
7. **Export** - Export data to CSV/PDF
8. **Dark Mode** - Theme toggle

---

## 🛠️ Development Commands

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

---

## 📦 Dependencies

### **Core:**
- react: ^18.2.0
- react-dom: ^18.2.0
- react-router-dom: ^6.20.0

### **UI:**
- @mui/material: ^5.14.0
- @mui/icons-material: ^5.14.0
- @emotion/react: ^11.11.0
- @emotion/styled: ^11.11.0

### **State & API:**
- axios: ^1.6.0
- @reduxjs/toolkit: ^1.9.7 (for future use)
- react-redux: ^8.1.3 (for future use)

### **Forms:**
- react-hook-form: ^7.48.0
- zod: ^3.22.0
- @hookform/resolvers: ^3.3.0

### **Dev:**
- typescript: ^5.3.0
- vite: ^5.0.0
- @vitejs/plugin-react: ^4.2.0

---

**Frontend is production-ready with reusable, maintainable components!** 🎉
