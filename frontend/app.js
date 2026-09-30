// Base URL of the backend API. When running via docker-compose, the
// backend is published on the host at localhost:8080 (the browser talks
// to it directly, not through the Docker network).
const API_BASE = window.API_BASE_URL || "/api";

const resultBox = document.getElementById("resultBox");
const statusDot = document.getElementById("statusDot");
const statusText = document.getElementById("statusText");

// ---------- tab switching ----------
document.querySelectorAll(".tab").forEach(tab => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
    document.querySelectorAll(".tab-panel").forEach(p => p.classList.remove("active"));
    tab.classList.add("active");
    document.getElementById("form-" + tab.dataset.tab).classList.add("active");
  });
});

// ---------- API health check ----------
async function checkHealth() {
  try {
    const res = await fetch(`${API_BASE}/health`);
    if (res.ok) {
      statusDot.className = "dot ok";
      statusText.textContent = "API connected";
    } else {
      throw new Error("bad status");
    }
  } catch (e) {
    statusDot.className = "dot down";
    statusText.textContent = "API unreachable";
  }
}
checkHealth();
setInterval(checkHealth, 15000);

// ---------- result rendering ----------
function showLoading() {
  resultBox.className = "result-box";
  resultBox.innerHTML = `<p class="muted">Working…</p>`;
}

function showError(message) {
  resultBox.className = "result-box error";
  resultBox.innerHTML = `<p><strong>Error:</strong> ${escapeHtml(message)}</p>`;
}

function showAccount(acc, title) {
  resultBox.className = "result-box success";
  resultBox.innerHTML = `
    <h3 style="margin-top:0">${escapeHtml(title || "Account")}</h3>
    <div class="kv"><span class="k">Account number</span><span class="v">${escapeHtml(acc.accountNumber)}</span></div>
    <div class="kv"><span class="k">Holder</span><span class="v">${escapeHtml(acc.holderName)}</span></div>
    <div class="kv"><span class="k">Type</span><span class="v">${escapeHtml(acc.type)}</span></div>
    <div class="kv"><span class="k">Balance</span><span class="v">${formatMoney(acc.balance)}</span></div>
  `;
}

function showStatement(data) {
  resultBox.className = "result-box success";
  let rows = "";
  if (data.transactions && data.transactions.length) {
    rows = data.transactions.map(t => `
      <div class="tx-row">
        <span class="tx-type">${escapeHtml(t.type)}</span>
        <span>${formatMoney(t.amount)}</span>
        <span>${formatMoney(t.balanceAfter)}</span>
        <span>${escapeHtml(t.timestamp)}</span>
      </div>
    `).join("");
  } else {
    rows = `<p class="muted">No transactions yet.</p>`;
  }
  resultBox.innerHTML = `
    <h3 style="margin-top:0">Statement — ${escapeHtml(data.accountNumber)}</h3>
    <div class="kv"><span class="k">Holder</span><span class="v">${escapeHtml(data.holderName)}</span></div>
    <div class="kv"><span class="k">Type</span><span class="v">${escapeHtml(data.type)}</span></div>
    <div class="kv"><span class="k">Balance</span><span class="v">${formatMoney(data.balance)}</span></div>
    <div class="tx-list">${rows}</div>
  `;
}

function showMessage(message) {
  resultBox.className = "result-box success";
  resultBox.innerHTML = `<p>${escapeHtml(message)}</p>`;
}

function formatMoney(n) {
  return Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function escapeHtml(s) {
  const div = document.createElement("div");
  div.textContent = String(s);
  return div.innerHTML;
}

// ---------- form submit helper ----------
async function apiRequest(method, path, body) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(`${API_BASE}${path}`, opts);
  let data = {};
  try {
    data = await res.json();
  } catch (e) { /* empty body */ }
  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`);
  }
  return data;
}

function formToObject(form) {
  const obj = {};
  new FormData(form).forEach((v, k) => { obj[k] = v; });
  return obj;
}

// ---------- Open account ----------
document.getElementById("form-open").addEventListener("submit", async (e) => {
  e.preventDefault();
  showLoading();
  const data = formToObject(e.target);
  try {
    const acc = await apiRequest("POST", "/api/accounts", data);
    showAccount(acc, "Account opened");
    e.target.reset();
  } catch (err) {
    showError(err.message);
  }
});

// ---------- Deposit ----------
document.getElementById("form-deposit").addEventListener("submit", async (e) => {
  e.preventDefault();
  showLoading();
  const data = formToObject(e.target);
  try {
    const acc = await apiRequest("POST", `/api/accounts/${encodeURIComponent(data.accountNumber)}/deposit`, {
      pin: data.pin, amount: data.amount
    });
    showAccount(acc, "Deposit successful");
    e.target.reset();
  } catch (err) {
    showError(err.message);
  }
});

// ---------- Withdraw ----------
document.getElementById("form-withdraw").addEventListener("submit", async (e) => {
  e.preventDefault();
  showLoading();
  const data = formToObject(e.target);
  try {
    const acc = await apiRequest("POST", `/api/accounts/${encodeURIComponent(data.accountNumber)}/withdraw`, {
      pin: data.pin, amount: data.amount
    });
    showAccount(acc, "Withdrawal successful");
    e.target.reset();
  } catch (err) {
    showError(err.message);
  }
});

// ---------- Transfer ----------
document.getElementById("form-transfer").addEventListener("submit", async (e) => {
  e.preventDefault();
  showLoading();
  const data = formToObject(e.target);
  try {
    const acc = await apiRequest("POST", "/api/transfer", data);
    showAccount(acc, "Transfer successful (source account shown)");
    e.target.reset();
  } catch (err) {
    showError(err.message);
  }
});

// ---------- Statement ----------
document.getElementById("form-statement").addEventListener("submit", async (e) => {
  e.preventDefault();
  showLoading();
  const data = formToObject(e.target);
  try {
    const result = await apiRequest(
      "GET",
      `/api/accounts/${encodeURIComponent(data.accountNumber)}/statement?pin=${encodeURIComponent(data.pin)}`
    );
    showStatement(result);
  } catch (err) {
    showError(err.message);
  }
});

// ---------- Admin: list accounts ----------
document.getElementById("btnListAccounts").addEventListener("click", async () => {
  const list = document.getElementById("accountsList");
  list.innerHTML = `<p class="muted">Loading…</p>`;
  try {
    const accounts = await apiRequest("GET", "/api/accounts");
    if (!accounts.length) {
      list.innerHTML = `<p class="muted">No accounts exist yet.</p>`;
      return;
    }
    list.innerHTML = accounts.map(a => `
      <div class="account-row">
        <span>${escapeHtml(a.accountNumber)}</span>
        <span class="badge">${escapeHtml(a.type)}</span>
        <span>${escapeHtml(a.holderName)}</span>
        <span>${formatMoney(a.balance)}</span>
      </div>
    `).join("");
  } catch (err) {
    list.innerHTML = `<p class="muted">Error: ${escapeHtml(err.message)}</p>`;
  }
});

// ---------- Admin: month end ----------
document.getElementById("btnMonthEnd").addEventListener("click", async () => {
  showLoading();
  try {
    const result = await apiRequest("POST", "/api/monthend");
    showMessage(`${result.message} — ${result.accountsProcessed} account(s) processed.`);
  } catch (err) {
    showError(err.message);
  }
});
