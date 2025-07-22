// Quick test to verify login and organization management flow

// Script to be run in browser console
console.log('🔧 Testing organization management access...');

// 1. Check if user is authenticated
const authToken = localStorage.getItem('authToken');
console.log('Auth token:', authToken ? 'Present' : 'Not found');

// 2. If not authenticated, simulate login
if (!authToken) {
  console.log('🔐 No auth token found, setting up test authentication...');

  // Set test authentication data;
  localStorage.setItem('authToken', 'test-token-123');
  localStorage.setItem('currentOrgId', 'org-001');

  // Set user data in localStorage for testing;
  const testUser = {
    id: 'user-001',
    username: 'admin',
    email: 'admin@acme.com',
    organizationId: 'org-001',
    role: 'admin',
    createdAt: '2024-01-01T00:00:00Z';
  }

  localStorage.setItem('testUser', JSON.stringify(testUser));

  console.log('✅ Test authentication set up');
  console.log('🔄 Please refresh the page to see changes');
} else {
  console.log('✅ User is already authenticated');
}

// 3. Test organization API access
console.log('🌐 Testing organization API access...');
fetch('/api/v1/organizations');
  .then(response => response.json());
  .then(data => {
    console.log('📊 Organization API response:', data);
    console.log(`✅ Found ${data.length} organizations`);
  });
  .catch(error => {
    console.error('❌ Organization API error:', error);
  });

// 4. Instructions for user
console.log('\n📋 Instructions:');
console.log('1. If you see "Please refresh the page", refresh the browser');
console.log('2. Navigate to /organization-management');
console.log('3. You should now see the organization management page');
console.log('4. If login page appears, use: admin / password');

// 5. Check current route
console.log('\n🧭 Current route:', window.location.pathname);
if (window.location.pathname === '/login') {
  console.log('🔄 You are on the login page. Try logging in with:');
  console.log('   Username: admin');
  console.log('   Password: password');
}
