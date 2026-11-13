// k6 load test for the school attendance endpoints.
//
//   k6 run -e BASE_URL=http://localhost:8080 -e EMAIL=admin@school.example -e PASSWORD=secret \
//          -e SCHOOL_ID=1 loadtest/attendance.js
//
// Optional: -e STUDENTS=200 (roster size created in setup, <= 500)  -e RATE=20 (bulk writes/s)
//           -e READ_RATE=100 (reads/s)  -e DURATION=60s
//
// IMPORTANT - what this does and does not prove:
//  * It measures requests/second and latency on whatever box you point it at (the single free-tier
//    VM, for now). It does NOT demonstrate "billions of requests/day"; see backend/ARCHITECTURE.md
//    section 6 for the honest scale story (stateless API + partitioned table + caching = the
//    design that scales horizontally; real billion-scale needs replicas, a queue and more nodes).
//  * The bulk and import endpoints are rate limited per client IP (RateLimitProperties:
//    attendance-bulk-per-minute=60, student-import-per-minute=10 by default). One k6 machine is one
//    IP, so to load-test raw throughput raise them for the run, e.g.
//      PROJECTX_SECURITY_RATE_LIMIT_ATTENDANCE_BULK_PER_MINUTE=100000
//    429s are tracked in the `rate_limited` counter and excluded from the failure threshold, so you
//    can see the limiter working when you leave the defaults on.
//  * The login user must be a SCHOOL_ADMIN (or PLATFORM_ADMIN) of SCHOOL_ID. The test creates its own
//    classroom + students (setup) and only writes attendance for today, so it is safe to re-run.

import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const SCHOOL_ID = __ENV.SCHOOL_ID;
const STUDENTS = Math.min(parseInt(__ENV.STUDENTS || '200', 10), 500);
const RATE = parseInt(__ENV.RATE || '20', 10);
const READ_RATE = parseInt(__ENV.READ_RATE || '100', 10);
const DURATION = __ENV.DURATION || '60s';

const rateLimited = new Counter('rate_limited');
const bulkDuration = new Trend('bulk_attendance_ms', true);
const summaryDuration = new Trend('summary_ms', true);
const sheetDuration = new Trend('sheet_ms', true);
const studentsDuration = new Trend('students_page_ms', true);

export const options = {
  scenarios: {
    bulk_writers: {
      executor: 'constant-arrival-rate',
      exec: 'bulkWrite',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 20,
      maxVUs: 200,
    },
    readers: {
      executor: 'constant-arrival-rate',
      exec: 'read',
      rate: READ_RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 20,
      maxVUs: 300,
    },
  },
  thresholds: {
    // 429 is the rate limiter doing its job, not a failure.
    'http_req_failed{expected_response:true}': ['rate<0.01'],
    bulk_attendance_ms: ['p(95)<500'],
    summary_ms: ['p(95)<200'],
    sheet_ms: ['p(95)<300'],
    students_page_ms: ['p(95)<300'],
    checks: ['rate>0.99'],
  },
};

function params(token, name) {
  return {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    tags: { name },
    // let k6 treat 429 as an expected response so it does not pollute http_req_failed
    responseCallback: http.expectedStatuses({ min: 200, max: 299 }, 429),
  };
}

export function setup() {
  if (!SCHOOL_ID) fail('Pass -e SCHOOL_ID=<id>');
  const login = http.post(`${BASE}/auth/login`, JSON.stringify({ email: __ENV.EMAIL, password: __ENV.PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
  });
  if (login.status !== 200) fail(`login failed: ${login.status} ${login.body}`);
  const token = login.json('data.accessToken');

  const suffix = `${Date.now()}`;
  const classroom = http.post(`${BASE}/schools/${SCHOOL_ID}/classrooms`, JSON.stringify({ name: `k6-${suffix}`, section: 'L' }), params(token, 'setup'));
  if (classroom.status !== 201) fail(`classroom create failed: ${classroom.status} ${classroom.body}`);
  const classroomId = classroom.json('data.id');

  const rows = [];
  for (let i = 0; i < STUDENTS; i++) {
    rows.push({
      classroomId, rollNo: `${i + 1}`, firstName: `Load${i}`, lastName: `Test${suffix}`,
      guardianName: `Guardian ${i}`, guardianPhone: `98${String(10000000 + i).padStart(8, '0')}`, marketingConsent: false,
    });
  }
  const imported = http.post(`${BASE}/schools/${SCHOOL_ID}/students/import`, JSON.stringify({ rows }), params(token, 'setup'));
  if (imported.status !== 200) fail(`import failed: ${imported.status} ${imported.body}`);

  const sheet = http.get(`${BASE}/schools/${SCHOOL_ID}/attendance?classroomId=${classroomId}`, params(token, 'setup'));
  const studentIds = sheet.json('data').map((r) => r.studentId);
  return { token, classroomId, studentIds };
}

const STATUSES = ['PRESENT', 'PRESENT', 'PRESENT', 'ABSENT', 'LATE'];
function today() {
  return new Date().toISOString().slice(0, 10); // server uses Asia/Kolkata; k6 UTC may lag by a day near midnight IST - future dates are rejected, past ones are fine
}

export function bulkWrite(data) {
  const entries = data.studentIds.map((studentId) => ({
    studentId, status: STATUSES[Math.floor(Math.random() * STATUSES.length)],
  }));
  const res = http.put(
    `${BASE}/schools/${SCHOOL_ID}/attendance/bulk`,
    JSON.stringify({ date: today(), classroomId: data.classroomId, entries }),
    params(data.token, 'attendance_bulk'),
  );
  if (res.status === 429) {
    rateLimited.add(1);
    return;
  }
  bulkDuration.add(res.timings.duration);
  check(res, { 'bulk 200': (r) => r.status === 200 });
}

export function read(data) {
  const pick = Math.random();
  if (pick < 0.5) {
    const res = http.get(`${BASE}/schools/${SCHOOL_ID}/attendance/summary?date=${today()}`, params(data.token, 'attendance_summary'));
    if (res.status === 429) return rateLimited.add(1);
    summaryDuration.add(res.timings.duration);
    check(res, { 'summary 200': (r) => r.status === 200 });
  } else if (pick < 0.8) {
    const res = http.get(`${BASE}/schools/${SCHOOL_ID}/attendance?date=${today()}&classroomId=${data.classroomId}`, params(data.token, 'attendance_sheet'));
    if (res.status === 429) return rateLimited.add(1);
    sheetDuration.add(res.timings.duration);
    check(res, { 'sheet 200': (r) => r.status === 200 });
  } else {
    const res = http.get(`${BASE}/schools/${SCHOOL_ID}/students?classroomId=${data.classroomId}&size=100`, params(data.token, 'students_page'));
    if (res.status === 429) return rateLimited.add(1);
    studentsDuration.add(res.timings.duration);
    check(res, { 'students 200': (r) => r.status === 200 });
  }
}
