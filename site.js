"use strict";

const screenshots = [
  { file: "1000309496.jpg", title: "Goals dashboard" },
  { file: "1000309498.jpg", title: "Goal progress" },
  { file: "1000309499.jpg", title: "Transaction ledger" },
  { file: "1000309497.jpg", title: "Reminders and backup" },
];
const screenButtons = [...document.querySelectorAll("[data-screen]")];
const galleryImage = document.querySelector("#gallery-image");
const screenshotDialog = document.querySelector("#screenshot-dialog");
let selectedScreen = 0;

screenButtons.forEach((button, index) => {
  button.addEventListener("click", () => {
    selectedScreen = index;
    const screen = screenshots[index];
    galleryImage.src = `Images/${screen.file}`;
    galleryImage.alt = screen.title;
    document.querySelector("#gallery-caption").textContent = `0${index + 1} / ${screen.title}`;
    screenButtons.forEach((choice) => choice.setAttribute("aria-pressed", String(choice === button)));
  });
});

document.querySelector(".gallery-zoom").addEventListener("click", () => {
  const screen = screenshots[selectedScreen];
  const image = document.querySelector("#dialog-image");
  image.src = `Images/${screen.file}`;
  image.alt = screen.title;
  document.querySelector("#screenshot-title").textContent = screen.title;
  screenshotDialog.showModal();
});
screenshotDialog.addEventListener("click", (event) => {
  if (event.target === screenshotDialog) screenshotDialog.close();
});

const demoBody = document.querySelector("#demo-body");
const demoStatus = document.querySelector("#demo-status");
const demoTabs = [...document.querySelectorAll("[data-view]")];
const demoApp = document.querySelector(".demo-app");
const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 });
const target = 1200;
const themes = [
  ["Apple Mint", "#19764f"], ["Cupertino Blue", "#1769ba"], ["Indigo Night", "#5856d6"], ["Rose Pink", "#bd3557"],
  ["Sunset Orange", "#a95416"], ["Pacific Teal", "#1b7584"], ["Royal Purple", "#8c48b1"], ["Midnight Slate", "#526479"],
];
let demo;

function setDemoStatus(message) {
  demoStatus.textContent = message;
}

function resetDemo() {
  demo = {
    view: "goals", theme: 0, day: 3, streak: 3, lastSavedDay: 3,
    ledger: [{ day: 1, amount: 400 }, { day: 2, amount: 150 }, { day: 3, amount: 100 }],
  };
  renderDemo();
  setDemoStatus("Sample data · Changes stay in this tab.");
}

function balance() {
  return demo.ledger.reduce((total, entry) => total + entry.amount, 0);
}

function renderDemo() {
  demoTabs.forEach((tab) => tab.setAttribute("aria-pressed", String(tab.dataset.view === demo.view)));
  demoApp.style.setProperty("--demo-accent", themes[demo.theme][1]);

  if (demo.view === "goals") {
    const saved = balance();
    const percent = Math.min(100, Math.round(saved / target * 100));
    demoBody.innerHTML = `
      <div class="demo-title"><h3>Emergency fund</h3><span class="streak">🔥 ${demo.streak} day streak</span></div>
      <p class="goal-money">${money.format(saved)} <span class="goal-meta">/ ${money.format(target)}</span></p>
      <progress value="${percent}" max="100" aria-label="Goal progress">${percent}%</progress>
      <p class="goal-meta">${percent}% complete · ${money.format(Math.max(0, target - saved))} remaining</p>
      <div class="demo-actions">
        <button type="button" class="button" data-demo="deposit">+ Deposit $100</button>
        <button type="button" class="button secondary" data-demo="withdraw">− Withdraw $50</button>
        <button type="button" class="plain" data-demo="next">Save next day</button>
      </div>`;
  } else if (demo.view === "ledger") {
    const rows = [...demo.ledger].reverse().map(({ day, amount }) => `
      <li><span>Day ${day} · ${amount > 0 ? "Deposit" : "Withdrawal"}</span><strong>${amount > 0 ? "+" : "−"}${money.format(Math.abs(amount))}</strong></li>`);
    demoBody.innerHTML = `<h3>Transaction ledger</h3><ul class="demo-rows">${rows.join("")}</ul>`;
  } else if (demo.view === "archive") {
    demoBody.innerHTML = `<h3>Archived goals</h3><ul class="demo-rows"><li><span>New headphones</span><strong>$180 / $180</strong></li></ul><p class="goal-meta">A completed goal kept for reference.</p>`;
  } else {
    const swatches = themes.map(([name, color], index) => `
      <button type="button" class="swatch" data-theme="${index}" aria-pressed="${index === demo.theme}" aria-label="${name}" title="${name}"><span style="--swatch:${color}"></span></button>`);
    demoBody.innerHTML = `
      <h3>Demo settings</h3><p class="goal-meta">Accent palette</p><div class="swatches">${swatches.join("")}</div>
      <div class="demo-actions"><button type="button" class="plain" data-demo="json">Export JSON</button><button type="button" class="plain" data-demo="csv">Export CSV</button><button type="button" class="plain" data-demo="reminder">Test reminder</button></div>`;
  }
}

function recordTransaction(amount, nextDay = false) {
  if (balance() + amount < 0) {
    setDemoStatus("The demo goal does not have enough saved for that withdrawal.");
    return;
  }
  if (nextDay) demo.day++;
  demo.ledger.push({ day: demo.day, amount });
  if (amount > 0 && demo.lastSavedDay !== demo.day) {
    demo.streak = demo.lastSavedDay === demo.day - 1 ? demo.streak + 1 : 1;
    demo.lastSavedDay = demo.day;
  }
  renderDemo();
  setDemoStatus(`${money.format(Math.abs(amount))} ${amount > 0 ? "deposited" : "withdrawn"} on day ${demo.day}.`);
}

function downloadSample(format) {
  const payload = format === "json"
    ? JSON.stringify({ sample: true, goal: "Emergency fund", target, transactions: demo.ledger }, null, 2)
    : ["Day,Amount USD", ...demo.ledger.map(({ day, amount }) => `${day},${amount}`)].join("\r\n");
  const type = format === "json" ? "application/json" : "text/csv";
  const url = URL.createObjectURL(new Blob([payload], { type }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `savinggoalplus-sample.${format}`;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  setDemoStatus(`Sample ${format.toUpperCase()} download started.`);
}

demoTabs.forEach((tab) => tab.addEventListener("click", () => {
  demo.view = tab.dataset.view;
  renderDemo();
}));
document.querySelector("#demo-reset").addEventListener("click", resetDemo);
demoBody.addEventListener("click", (event) => {
  const button = event.target.closest("button");
  if (!button) return;
  const theme = button.dataset.theme;
  if (theme !== undefined) {
    demo.theme = Number(theme);
    renderDemo();
    demoBody.querySelector(`[data-theme="${theme}"]`).focus({ preventScroll: true });
    return;
  }
  const action = button.dataset.demo;
  if (action === "deposit") recordTransaction(100);
  if (action === "withdraw") recordTransaction(-50);
  if (action === "next") recordTransaction(100, true);
  if (action === "json" || action === "csv") downloadSample(action);
  if (action === "reminder") setDemoStatus("Demo reminder: take a moment to log today's savings.");
  if (!button.isConnected) demoBody.querySelector(`[data-demo="${action}"]`)?.focus({ preventScroll: true });
});
resetDemo();

const releaseRepo = "Minta1234/SavingGoalPlus";
const releaseRoot = `https://github.com/${releaseRepo}/releases/`;
const assetsByKind = {
  apk: (name) => name.endsWith(".apk"),
  ipa: (name) => name.endsWith(".ipa"),
  archive: (name) => name.endsWith(".xcarchive.zip"),
  checksum: (name) => name === "SHA256SUMS.txt",
};

async function loadReleases() {
  const status = document.querySelector("#release-status");
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);
  status.textContent = "Loading release history from GitHub…";
  try {
    const response = await fetch(`https://api.github.com/repos/${releaseRepo}/releases?per_page=10`, { signal: controller.signal });
    if (!response.ok) throw new Error(`GitHub returned HTTP ${response.status}`);
    const data = await response.json();
    if (!Array.isArray(data)) throw new Error("Unexpected GitHub response");
    const releases = data.filter((release) => !release.draft && !release.prerelease && release.html_url?.startsWith(`${releaseRoot}tag/`));
    const latest = releases.find((release) => release.assets?.some((asset) => assetsByKind.apk(asset.name))
      && release.assets.some((asset) => assetsByKind.ipa(asset.name)));
    if (latest) {
      document.querySelector("#release-version").textContent = `Latest combined release: ${latest.tag_name}.`;
      document.querySelectorAll("[data-asset]").forEach((link) => {
        const asset = latest.assets.find((entry) => assetsByKind[link.dataset.asset](entry.name)
          && entry.browser_download_url?.startsWith(`${releaseRoot}download/`));
        link.href = asset?.browser_download_url ?? latest.html_url;
      });
    }
    const list = document.querySelector("#release-list");
    for (const release of releases.slice(0, 5)) {
      const row = document.createElement("li");
      const link = document.createElement("a");
      const title = document.createElement("strong");
      link.href = release.html_url;
      title.textContent = release.name || release.tag_name;
      link.append(title);
      if (release.published_at && !Number.isNaN(Date.parse(release.published_at))) {
        const time = document.createElement("time");
        time.dateTime = release.published_at;
        time.textContent = new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(release.published_at));
        link.append(time);
      }
      row.append(link);
      list.append(row);
    }
    status.textContent = releases.length ? "" : "No published releases found.";
  } catch (error) {
    status.textContent = "Could not load release history. View releases directly on GitHub.";
    console.info("SavingGoalPlus release history unavailable:", error);
  } finally {
    clearTimeout(timeout);
  }
}

loadReleases();
