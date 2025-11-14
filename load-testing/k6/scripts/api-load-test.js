import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp up to 20 users
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '2m', target: 50 },   // Stay at 50 users
    { duration: '30s', target: 0 },   // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% of requests under 500ms
    http_req_failed: ['rate<0.01'],                  // Error rate under 1%
    errors: ['rate<0.1'],
  },
};

const BASE_URL = 'http://localhost:8080/api';

// Test data
const users = [
  { email: 'admin@onboardbuddy.com', password: 'Admin@123' },
  { email: 'hr@onboardbuddy.com', password: 'Hr@123' },
  { email: 'manager@onboardbuddy.com', password: 'Manager@123' },
  { email: 'buddy1@onboardbuddy.com', password: 'Buddy@123' },
  { email: 'employee1@onboardbuddy.com', password: 'Employee@123' },
];

export default function () {
  // Select random user
  const user = users[Math.floor(Math.random() * users.length)];

  // Login
  const loginPayload = JSON.stringify({
    email: user.email,
    password: user.password,
  });

  const loginParams = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, loginParams);
  
  const loginSuccess = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login has token': (r) => r.json('data.token') !== undefined,
  });

  errorRate.add(!loginSuccess);

  if (!loginSuccess) {
    return;
  }

  const authToken = loginRes.json('data.token');
  const authParams = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`,
    },
  };

  sleep(1);

  // Get current user
  const meRes = http.get(`${BASE_URL}/auth/me`, authParams);
  check(meRes, {
    'get me status is 200': (r) => r.status === 200,
  });

  sleep(1);

  // Get users
  const usersRes = http.get(`${BASE_URL}/users`, authParams);
  check(usersRes, {
    'get users status is 200 or 403': (r) => r.status === 200 || r.status === 403,
  });

  sleep(1);

  // Get plans
  const plansRes = http.get(`${BASE_URL}/plans`, authParams);
  check(plansRes, {
    'get plans status is 200': (r) => r.status === 200,
  });

  sleep(2);

  // Logout
  const logoutRes = http.post(`${BASE_URL}/auth/logout`, null, authParams);
  check(logoutRes, {
    'logout status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
