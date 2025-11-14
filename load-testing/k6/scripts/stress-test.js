import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '1m', target: 100 },   // Ramp up to 100 users
    { duration: '2m', target: 100 },   // Stay at 100 users
    { duration: '1m', target: 200 },   // Ramp up to 200 users
    { duration: '2m', target: 200 },   // Stay at 200 users
    { duration: '1m', target: 300 },   // Ramp up to 300 users
    { duration: '2m', target: 300 },   // Stay at 300 users
    { duration: '1m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed: ['rate<0.05'],
    errors: ['rate<0.2'],
  },
};

const BASE_URL = 'http://localhost:8080/api';

const users = [
  { email: 'admin@onboardbuddy.com', password: 'Admin@123' },
  { email: 'hr@onboardbuddy.com', password: 'Hr@123' },
  { email: 'employee1@onboardbuddy.com', password: 'Employee@123' },
];

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];

  const loginPayload = JSON.stringify({
    email: user.email,
    password: user.password,
  });

  const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const loginSuccess = check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  errorRate.add(!loginSuccess);

  if (loginSuccess) {
    const authToken = loginRes.json('data.token');
    const authParams = {
      headers: {
        'Authorization': `Bearer ${authToken}`,
      },
    };

    // Rapid fire requests
    http.get(`${BASE_URL}/users`, authParams);
    http.get(`${BASE_URL}/plans`, authParams);
    http.get(`${BASE_URL}/auth/me`, authParams);
  }

  sleep(0.5);
}
