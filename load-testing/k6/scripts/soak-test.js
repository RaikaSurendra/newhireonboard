import http from 'k6/http';
import { check, sleep } from 'k6';

// Soak test - Extended duration at moderate load
export const options = {
  stages: [
    { duration: '2m', target: 50 },    // Ramp up to 50 users
    { duration: '30m', target: 50 },   // Stay at 50 users for 30 minutes
    { duration: '2m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080/api';

const users = [
  { email: 'admin@onboardbuddy.com', password: 'Admin@123' },
  { email: 'hr@onboardbuddy.com', password: 'Hr@123' },
  { email: 'manager@onboardbuddy.com', password: 'Manager@123' },
  { email: 'buddy1@onboardbuddy.com', password: 'Buddy@123' },
  { email: 'employee1@onboardbuddy.com', password: 'Employee@123' },
];

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];

  // Login
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: user.email, password: user.password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (loginRes.status === 200) {
    const authToken = loginRes.json('data.token');
    const authHeaders = {
      headers: { 'Authorization': `Bearer ${authToken}` },
    };

    // Simulate realistic user behavior
    sleep(2);
    http.get(`${BASE_URL}/auth/me`, authHeaders);
    
    sleep(3);
    http.get(`${BASE_URL}/plans/my-plans`, authHeaders);
    
    sleep(2);
    http.get(`${BASE_URL}/users`, authHeaders);
    
    sleep(4);
    http.get(`${BASE_URL}/plans`, authHeaders);
    
    sleep(2);
    http.post(`${BASE_URL}/auth/logout`, null, authHeaders);
  }

  sleep(5);
}
