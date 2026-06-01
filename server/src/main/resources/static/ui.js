'use strict';

let token     = localStorage.getItem('hs_token') || null;
let tokenRole = localStorage.getItem('hs_role')  || null;
let tokenUser = localStorage.getItem('hs_user')  || null;

function v(id) { return document.getElementById(id).value.trim(); }

function show(panelId, btn) {
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
  document.getElementById(panelId).classList.add('active');
  btn.classList.add('active');
}

function setStatus(ok, msg) {
  const bar = document.getElementById('statusBar');
  bar.className = ok ? 'ok' : 'err';
  document.getElementById('statusText').textContent = msg;
}

function updateStatusBar() {
  if (token && tokenUser) {
    setStatus(true, 'Logged in as ' + tokenUser + ' (' + (tokenRole || '?') + ')');
  } else {
    setStatus(false, 'Not logged in');
  }
}

function showResponse(id, data, isError) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
  el.className = 'response visible' + (isError ? ' err' : '');
}

async function callApi(method, path, responseId, body) {
  const rId = responseId || 'rGeneral';
  const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  try {
    const res = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    let data;
    const ct = res.headers.get('content-type') || '';
    data = ct.includes('json') ? await res.json() : await res.text();
    showResponse(rId, data, !res.ok);
    return { ok: res.ok, status: res.status, data };
  } catch (e) {
    showResponse(rId, 'Network error: ' + e.message, true);
    return { ok: false, status: 0, data: null };
  }
}

async function doLogin() {
  const result = await callApi('POST', '/api/auth/token', 'rLogin', {
    username: v('loginUser'),
    password: v('loginPass'),
  });
  if (result.ok && result.data && result.data.accessToken) {
    token     = result.data.accessToken;
    tokenRole = result.data.role;
    tokenUser = v('loginUser');
    localStorage.setItem('hs_token', token);
    localStorage.setItem('hs_role', tokenRole);
    localStorage.setItem('hs_user', tokenUser);
    updateStatusBar();
  }
}

async function doLogout() {
  await callApi('POST', '/api/auth/logout', 'rLogout');
  token = null; tokenRole = null; tokenUser = null;
  localStorage.removeItem('hs_token');
  localStorage.removeItem('hs_role');
  localStorage.removeItem('hs_user');
  updateStatusBar();
}

function changePassword() {
  callApi('POST', '/api/auth/users/me/change-password', 'rChangePass', {
    currentPassword: v('cpCurrent'),
    newPassword:     v('cpNew'),
  });
}

function createUser() {
  callApi('POST', '/api/auth/users', 'rCreateUser', {
    username: v('cuUser'),
    password: v('cuPass'),
    role:     v('cuRole'),
    enabled:  document.getElementById('cuEnabled').value === 'true',
  });
}

function updateUser() {
  const body = {};
  const pass = v('uuPass');  if (pass) body.password = pass;
  const role = v('uuRole');  if (role) body.role = role;
  const en   = document.getElementById('uuEnabled').value; if (en) body.enabled = en === 'true';
  callApi('PUT', '/api/auth/users/' + encodeURIComponent(v('uuUser')), 'rUpdateUser', body);
}

function deleteUser() {
  callApi('DELETE', '/api/auth/users/' + encodeURIComponent(v('delUser')), 'rDeleteUser');
}

function revokeTokens() {
  callApi('POST', '/api/auth/users/' + encodeURIComponent(v('rtUser')) + '/revoke-tokens', 'rRevokeTokens');
}

function listEvals() {
  const params = new URLSearchParams();
  const gid = v('fGameId'); if (gid) params.set('gameId', gid);
  const r1  = v('fR1');     if (r1)  params.set('firstRefereeId', r1);
  const r2  = v('fR2');     if (r2)  params.set('secondRefereeId', r2);
  const del = v('fDelegate'); if (del) params.set('delegateId', del);
  const qs  = params.toString();
  callApi('GET', '/api/performance-evaluations' + (qs ? '?' + qs : ''), 'rEvalList');
}

function createEval() {
  let body;
  try { body = JSON.parse(document.getElementById('evalBody').value); }
  catch (e) { showResponse('rCreateEval', 'Invalid JSON: ' + e.message, true); return; }
  callApi('POST', '/api/performance-evaluations', 'rCreateEval', body);
}

document.addEventListener('DOMContentLoaded', () => {
  updateStatusBar();

  document.querySelectorAll('nav button[data-panel]').forEach(btn => {
    btn.addEventListener('click', () => show(btn.dataset.panel, btn));
  });

  document.getElementById('btnGetInfo').addEventListener('click',      () => callApi('GET', '/', 'rGeneral'));
  document.getElementById('btnGetHealth').addEventListener('click',    () => callApi('GET', '/health', 'rHealth'));
  document.getElementById('btnLogin').addEventListener('click',        doLogin);
  document.getElementById('btnLogout').addEventListener('click',       doLogout);
  document.getElementById('btnGetMe').addEventListener('click',        () => callApi('GET', '/api/auth/users/me', 'rMe'));
  document.getElementById('btnChangePass').addEventListener('click',   changePassword);
  document.getElementById('btnUserList').addEventListener('click',     () => callApi('GET', '/api/auth/users', 'rUserList'));
  document.getElementById('btnGetUser').addEventListener('click',      () => callApi('GET', '/api/auth/users/' + encodeURIComponent(v('getUserName')), 'rGetUser'));
  document.getElementById('btnCreateUser').addEventListener('click',   createUser);
  document.getElementById('btnUpdateUser').addEventListener('click',   updateUser);
  document.getElementById('btnDeleteUser').addEventListener('click',   deleteUser);
  document.getElementById('btnRevokeTokens').addEventListener('click', revokeTokens);
  document.getElementById('btnPhases').addEventListener('click',       () => callApi('GET', '/api/phases', 'rPhases'));
  document.getElementById('btnMatches').addEventListener('click',      () => callApi('GET', '/api/phases/' + encodeURIComponent(v('mPhaseId')) + '/matches', 'rMatches'));
  document.getElementById('btnSingleMatch').addEventListener('click',  () => callApi('GET', '/api/phases/' + encodeURIComponent(v('smPhaseId')) + '/matches/' + encodeURIComponent(v('smMatchId')), 'rSingleMatch'));
  document.getElementById('btnEvalList').addEventListener('click',     listEvals);
  document.getElementById('btnGetEval').addEventListener('click',      () => callApi('GET', '/api/performance-evaluations/' + encodeURIComponent(v('evalId')), 'rGetEval'));
  document.getElementById('btnCreateEval').addEventListener('click',   createEval);

  // ── Sportradar diagnostics ──────────────────────────────────────────────────
  document.getElementById('btnSrPhases').addEventListener('click',
    () => callApi('GET', '/api/sportradar/phases', 'rSrPhases'));
  document.getElementById('btnSrPhaseDetail').addEventListener('click',
    () => callApi('GET', '/api/sportradar/phases/' + encodeURIComponent(v('srPhaseDetailId')), 'rSrPhaseDetail'));
  document.getElementById('btnSrMatches').addEventListener('click',
    () => callApi('GET', '/api/sportradar/phases/' + encodeURIComponent(v('srMatchPhaseId')) + '/matches', 'rSrMatches'));
  document.getElementById('btnSrMatch').addEventListener('click',
    () => callApi('GET', '/api/sportradar/phases/' + encodeURIComponent(v('srSinglePhaseId')) + '/matches/' + encodeURIComponent(v('srSingleMatchId')), 'rSrMatch'));

  // ── Sportradar tournaments / seasons ──────────────────────────────────────
  document.getElementById('btnSrTournaments').addEventListener('click',
    () => callApi('GET', '/api/sportradar/tournaments', 'rSrTournaments'));
  document.getElementById('btnSrTournament').addEventListener('click',
    () => callApi('GET', '/api/sportradar/tournaments/' + encodeURIComponent(v('srTournamentId')), 'rSrTournament'));
  document.getElementById('btnSrSeasons').addEventListener('click',
    () => callApi('GET', '/api/sportradar/tournaments/' + encodeURIComponent(v('srSeasonsTournamentId')) + '/seasons', 'rSrSeasons'));
  document.getElementById('btnSrSeason').addEventListener('click',
    () => callApi('GET', '/api/sportradar/tournaments/' + encodeURIComponent(v('srSeasonTournamentId')) + '/seasons/' + encodeURIComponent(v('srSeasonId')), 'rSrSeason'));
  document.getElementById('btnSrRefresh').addEventListener('click',
    () => callApi('POST', '/api/sportradar/refresh', 'rSrRefresh'));
});
