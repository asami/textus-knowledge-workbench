"use strict";
(() => {
  const component = "knowledge-workbench";
  const root = `/rest/v1/${component}/`;
  const resourceType = "information-candidate";
  const byId = id => document.getElementById(id);
  const labels = { id: "参照ID", kind: "種類", revision: "版", snapshotRef: "スナップショット参照",
    retrievalState: "取得状態", retentionUntil: "保持期限", limitation: "制約・未確認事項", decision: "判断",
    actor: "判断者", occurredAt: "日時", reason: "理由", target: "判断対象", content: "内容", context: "文脈",
    evidenceRefs: "根拠", preparedMaterialRefs: "処理済み資料", sourceRunRef: "処理Run", sourceContextRef: "元の文脈",
    groundingRefs: "Grounding", mappingRefs: "Mapping", information_id: "Information ID", candidate_revision: "承認時のCandidate版",
    approval: "元の承認", requested_by: "採用操作の担当者", requested_at: "採用操作の日時",
    status: "採用結果", information_revision: "確認したInformation版" };
  const groups = [["evidenceRefs", "根拠参照"], ["groundingRefs", "Grounding参照"], ["mappingRefs", "Mapping参照"]];
  const referenceKeys = ["id", "kind", "revision", "snapshotRef", "retrievalState", "retentionUntil", "limitation"];
  let busy = false, selectedId, work, revision, baseline, actions, needsRead = true, acknowledgement = "";
  const producer = "workbench-material", intentStorage = "workbench-material-intent";
  let intent, retainedIntents = [], intentReady = false, intentCaller, material, selectedResources = [], previewUrl;
  try {
    const saved = JSON.parse(sessionStorage.getItem(intentStorage));
    retainedIntents = (Array.isArray(saved) ? saved : [saved]).filter(value =>
      value?.producerRef === producer && typeof value.proposalId === "string" && value.proposalId)
      .map(value => ({ producerRef: producer, proposalId: value.proposalId }));
    intent = retainedIntents.at(-1);
  } catch { /* Storage failure never authorizes a replacement command. */ }

  function node(tag, text, className) {
    const element = document.createElement(tag);
    if (text !== undefined) element.textContent = text;
    if (className) element.className = className;
    return element;
  }
  function controls() {
    byId("load-list").disabled = busy;
    byId("token").disabled = busy;
    byId("reload").disabled = busy || !selectedId;
    byId("edit-fields").disabled = busy || !work || needsRead;
    byId("decision-fields").disabled = busy || !work || needsRead;
    byId("admission-fields").disabled = busy || !work || needsRead;
    byId("admit").disabled = busy || needsRead || work?.state !== "Approved";
    byId("confirm-admission").disabled = busy || needsRead || !work?.candidate.admissions?.length;
    byId("context").disabled = !byId("context-present").checked;
    byId("list").querySelectorAll("button").forEach(button => { button.disabled = busy; });
    ["material-search", "creation-fields"].forEach(id => {
      byId(id).querySelectorAll("input,textarea,button").forEach(element => { element.disabled = busy; });
    });
    byId("material-list").querySelectorAll("button").forEach(button => { button.disabled = busy; });
    byId("material-detail").querySelectorAll("button,input").forEach(element => { element.disabled = busy; });
    byId("new-intent").disabled = busy;
    byId("reconcile-intent").disabled = busy || !intent;
    byId("retained-intents").disabled = busy;
    byId("create-candidate").disabled = busy || !intentReady || !material || !selectedResources.length;
    byId("candidate-source").disabled = busy || needsRead || !sourceSelection();
    [...byId("decision").options].forEach(option => {
      option.disabled = ["review", "approve"].includes(option.value) &&
        (!work?.review_eligible || (option.value === "approve" ? work.state !== "Reviewing" : !["Draft", "Held"].includes(work.state)));
    });
    if (byId("decision").selectedOptions[0]?.disabled) byId("decision").value = "hold";
  }
  function status(text) { byId("status").textContent = text; }
  async function run(action) {
    if (busy) return;
    busy = true; controls();
    try { await action(); }
    catch (error) { status(`${acknowledgement ? acknowledgement + "\n" : ""}${error.message}`); }
    finally { busy = false; controls(); }
  }
  async function request(path, { query, body } = {}) {
    const url = new URL(root + path + ".json", location.origin);
    if (query) url.search = new URLSearchParams(query).toString();
    const headers = { Accept: "application/json" };
    if (byId("token").value) headers.Authorization = `Bearer ${byId("token").value}`;
    if (body !== undefined) headers["Content-Type"] = "application/json; charset=UTF-8";
    let response;
    try { response = await fetch(url, { method: body === undefined ? "GET" : "POST", headers,
      credentials: "omit", cache: "no-store", redirect: "error",
      ...(body === undefined ? {} : { body: JSON.stringify(body) }) }); }
    catch { throw new Error("通信結果を確認できません。再送せず、再読み込みして状態を確認してください。"); }
    let value;
    try { value = await response.json(); }
    catch { throw new Error(`HTTP ${response.status}: 応答を確認できません。再読み込みして照合してください。`); }
    if (!response.ok) throw new Error(`HTTP ${response.status}: 処理を完了できませんでした。権限・入力・最新状態を確認してください。`);
    return value.data ?? value;
  }
  function displayValue(value) {
    if (value.type === "missing") return "未指定";
    if (value.type === "boolean") return value.boolean ? "はい" : "いいえ";
    if (value.type === "text") return value.text;
    return value.number ?? value.instant ?? "未指定";
  }
  function structured(value) {
    if (value === null || value === undefined) return node("span", "未指定");
    if (Array.isArray(value)) {
      const result = node("div");
      if (!value.length) result.append(node("span", "参照なし"));
      value.forEach(entry => { const card = node("div", undefined, "reference"); card.append(structured(entry)); result.append(card); });
      return result;
    }
    if (typeof value === "object") {
      const result = node("dl");
      Object.entries(value).forEach(([key, entry]) => { result.append(node("dt", labels[key] ?? key)); const dd = node("dd"); dd.append(structured(entry)); result.append(dd); });
      return result;
    }
    return node("span", String(value));
  }
  function renderDetail(display) {
    const container = byId("detail"); container.replaceChildren();
    container.append(node("p", `ID: ${display.model.resource.entityId} · revision: ${display.model.resource.revision}`));
    const values = node("dl");
    display.model.sections.flatMap(section => section.fields).forEach(field => {
      values.append(node("dt", field.label)); const dd = node("dd");
      if (["evidence", "materials", "run", "source-context", "grounding", "mapping", "history", "target", "admissions"].includes(field.id)) {
        const data = JSON.parse(field.value.text); dd.append(structured(data.items));
      } else dd.textContent = String(displayValue(field.value));
      values.append(dd);
    }); container.append(values);
  }
  function referenceEditor(group, reference = {}) {
    const card = node("div", undefined, "reference"); card.dataset.reference = group;
    const grid = node("div", undefined, "reference-grid");
    referenceKeys.forEach(key => {
      const label = node("label", labels[key]); const input = node("input");
      input.dataset.key = key; input.setAttribute("aria-label", `${group} ${labels[key]}`);
      input.value = reference[key] ?? ""; input.required = key === "id" || key === "kind";
      label.append(input); grid.append(label);
    });
    const remove = node("button", "この参照を外す", "remove"); remove.type = "button";
    remove.addEventListener("click", () => card.remove()); card.append(grid, remove); return card;
  }
  function workResponse(received) {
    // Standard Record responses use native snake_case; Display JSON has its own closed protocol.
    const reference = value => {
      const result = {};
      referenceKeys.forEach(key => {
        const wire = key.replace(/[A-Z]/g, char => "_" + char.toLowerCase());
        if (Object.hasOwn(value, wire)) result[key] = value[wire];
      }); return result;
    };
    const candidate = { ...received.candidate };
    groups.forEach(([key]) => {
      const wire = key.replace(/[A-Z]/g, char => "_" + char.toLowerCase());
      if (!Array.isArray(candidate[wire])) throw new Error("参照情報の応答形式を確認できません。編集せず再読み込みしてください。");
      candidate[key] = candidate[wire].map(reference);
    }); return { ...received, candidate };
  }
  function populate(current) {
    byId("content").value = current.candidate.content ?? "";
    byId("context").value = current.candidate.context ?? "";
    byId("context-present").checked = Object.hasOwn(current.candidate, "context");
    byId("edit-reason").value = ""; byId("decision-reason").value = "";
    byId("references").replaceChildren();
    groups.forEach(([key, title]) => {
      const group = node("div"); group.dataset.group = key;
      group.append(node("h3", title, "reference-title"));
      const rows = node("div"); rows.dataset.rows = key;
      (current.candidate[key] ?? []).forEach(reference => rows.append(referenceEditor(key, reference)));
      const add = node("button", `${title}を追加`); add.type = "button";
      add.addEventListener("click", () => rows.append(referenceEditor(key)));
      group.append(rows, add); byId("references").append(group);
    }); baseline = JSON.stringify(draft());
  }
  function draft() {
    const result = { content: byId("content").value };
    if (byId("context-present").checked) result.context = byId("context").value;
    groups.forEach(([key]) => {
      result[key] = [...document.querySelectorAll(`[data-reference="${key}"]`)].map(card => {
        const reference = {};
        card.querySelectorAll("input").forEach(input => {
          if (input.value !== "" || input.required) reference[input.dataset.key] = input.value;
        }); return reference;
      });
    }); return result;
  }
  function dirty() { return work && JSON.stringify(draft()) !== baseline; }
  function discardAllowed() { return !dirty() || confirm("未保存の編集を破棄して読み込みますか？"); }
  async function loadSelected(id) {
    needsRead = true; controls(); byId("detail").replaceChildren(node("p", "読込み中…"));
    const display = await request("display/read", { query: { resourceType, screenId: "candidate-detail", resourceId: id } });
    const current = workResponse(await request("candidate/getCandidateWork", { query: { id } }));
    const metadata = await request("display/describeBusiness", { query: { resourceType, screenId: "candidate-detail" } });
    if (current.candidate.id !== id || display.model.resource.entityId !== id || !Number.isSafeInteger(current.revision) ||
        current.revision <= 0 || String(current.revision) !== display.model.resource.revision)
      throw new Error("読取りの間に状態が変わりました。編集せず、明示的に再読み込みしてください。");
    actions = metadata.actions;
    renderDetail(display); populate(current); work = current; revision = current.revision; needsRead = false;
    status(`${acknowledgement ? acknowledgement + "\n" : ""}最新の内容を表示しました。`);
  }
  async function loadList() {
    const display = await request("display/query", { query: { resourceType, screenId: "candidate-list" } });
    const container = byId("list"); container.replaceChildren();
    if (!display.model.items.length) container.append(node("p", "Candidateはまだありません。登録した提案がここに表示されます。"));
    display.model.items.forEach(item => {
      const card = node("article", undefined, "candidate");
      item.fields.forEach(field => card.append(node(field.id === "title" ? "h3" : "p", `${field.label}: ${displayValue(field.value)}`)));
      const select = node("button", "選択"); select.type = "button";
      select.addEventListener("click", () => { if (!discardAllowed()) return; selectedId = item.resource.entityId; acknowledgement = ""; run(reconcileSelected); });
      card.append(select); container.append(card);
    }); status("一覧を読み込みました。Candidateを選択してください。");
  }
  function operation(actionId, expected) {
    const action = actions?.find(value => value.id === actionId);
    if (!action || action.operation.service !== "Candidate" || action.operation.operation !== expected ||
        !["KnowledgeWorkbench", "org.simplemodeling.textus.KnowledgeWorkbench"].includes(action.operation.componentId))
      throw new Error("業務操作の対応を確認できません。再読み込みしてください。");
    return `candidate/${expected}`;
  }
  async function reconcileSelected() {
    needsRead = true;
    try {
      await loadSelected(selectedId);
      await loadList();
      status(`${acknowledgement ? acknowledgement + "\n" : ""}最新の内容を表示しました。`);
    } catch (error) { needsRead = true; throw error; }
  }
  async function mutate(path, body, success) {
    needsRead = true; controls(); acknowledgement = "";
    await request(path, { body });
    acknowledgement = success; baseline = JSON.stringify(draft());
    status(`${success}\n最新の表示を読み込みます。`);
    await reconcileSelected();
  }
  function clearPreview() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    previewUrl = undefined; byId("original-preview").replaceChildren();
  }
  function clearMaterial() {
    clearPreview(); material = undefined; selectedResources = [];
    byId("material-detail").replaceChildren(); controls();
  }
  function sourceSelection() {
    try {
      const source = JSON.parse(work?.candidate.original_proposal).materialSelection;
      return typeof source?.materialRef === "string" && Array.isArray(source.resourceIds) &&
        source.resourceIds.every(id => typeof id === "string") ? source : undefined;
    } catch { return undefined; }
  }
  async function inspectMaterial(ref, retained = []) {
    clearMaterial();
    const received = await request("MaterialWork/inspectMaterial", { query: { materialRef: ref } });
    if (received.material_ref !== ref || !Array.isArray(received.resources) ||
        received.resources.some(entry => typeof entry.resource_id !== "string") ||
        new Set(received.resources.map(entry => entry.resource_id)).size !== received.resources.length)
      throw new Error("資料の対応を確認できません。選択をやり直してください。");
    material = received;
    const container = byId("material-detail");
    container.append(node("h3", received.title ?? "資料"), node("p", ref),
      node("p", "現在の資料を確認しています。原本表示は版・スナップショットを証明しません。"));
    selectedResources = retained.filter(id => received.resources.some(entry => entry.resource_id === id));
    received.resources.forEach(entry => {
      const label = node("label", `${entry.role ?? "resource"}: ${entry.resource_id}`);
      const checkbox = node("input"); checkbox.type = "checkbox"; checkbox.checked = selectedResources.includes(entry.resource_id);
      checkbox.addEventListener("change", () => {
        selectedResources = checkbox.checked ? [...selectedResources, entry.resource_id] : selectedResources.filter(id => id !== entry.resource_id);
        clearPreview(); controls();
      });
      const preview = node("button", "原本を確認"); preview.type = "button";
      preview.addEventListener("click", () => run(() => previewOriginal(ref, entry.resource_id)));
      label.prepend(checkbox); container.append(label, preview);
    });
    if (retained.length !== selectedResources.length) container.append(node("p", "最初の選択の一部は現在取得できません。"));
  }
  async function previewOriginal(materialRef, resourceId) {
    clearPreview();
    const url = new URL(root + "MaterialWork/readOriginal", location.origin);
    url.search = new URLSearchParams({ materialRef, resourceId }).toString();
    const headers = {};
    if (byId("token").value) headers.Authorization = `Bearer ${byId("token").value}`;
    const response = await fetch(url, { headers, credentials: "omit", cache: "no-store", redirect: "error" });
    const media = response.headers.get("Content-Type")?.split(";")[0].trim().toLowerCase();
    const safeMedia = ["image/png", "image/jpeg", "image/gif", "image/webp", "audio/mpeg", "audio/wav", "audio/ogg"];
    if (!response.ok || !safeMedia.includes(media) ||
        !/\bprivate\b/i.test(response.headers.get("Cache-Control") ?? "") ||
        !/\bno-store\b/i.test(response.headers.get("Cache-Control") ?? "") ||
        response.headers.get("X-Content-Type-Options")?.toLowerCase() !== "nosniff" ||
        !/default-src\s+'none'/i.test(response.headers.get("Content-Security-Policy") ?? "") ||
        !/(?:^|;)\s*sandbox(?:\s*;|\s*$)/i.test(response.headers.get("Content-Security-Policy") ?? "") ||
        !/^attachment(?:;|$)/i.test(response.headers.get("Content-Disposition") ?? ""))
      throw new Error(`原本を安全に表示できません (HTTP ${response.status})。以前の表示は破棄しました。`);
    previewUrl = URL.createObjectURL(await response.blob());
    const element = node(media.startsWith("image/") ? "img" : "audio");
    if (element.tagName === "AUDIO") element.controls = true;
    else element.alt = "選択した原本";
    element.addEventListener("error", () => { clearPreview(); status("原本の画像・音声を解釈できませんでした。表示を破棄しました。"); }, { once: true });
    element.src = previewUrl;
    const download = node("a", "原本をダウンロード"); download.href = previewUrl; download.download = "original";
    byId("original-preview").append(element, download);
  }
  function creationStatus(text) { byId("creation-status").textContent = text; }
  function renderIntents() {
    byId("retained-intents").replaceChildren();
    retainedIntents.forEach(value => {
      const option = node("option", value.proposalId); option.value = value.proposalId;
      option.selected = value === intent; byId("retained-intents").append(option);
    });
  }
  async function reconcileIntent() {
    intentReady = false;
    const found = await request("candidate/findCandidateByProposal", { query: intent });
    if (!found.candidate?.id) {
      creationStatus("一致するCandidateは確認できません。以前の送信が不存在とは断定できません。新規作成は明示的に開始してください。");
      return;
    }
    if (found.candidate.producer_ref !== intent.producerRef || found.candidate.proposal_id !== intent.proposalId)
      throw new Error("作成意図と返されたCandidateの対応を確認できません。");
    if (!discardAllowed()) return;
    selectedId = found.candidate.id; acknowledgement = "作成意図に対応するCandidate IDを確認しました。";
    await reconcileSelected(); creationStatus(acknowledgement);
  }
  byId("material-search").addEventListener("submit", event => {
    event.preventDefault(); run(async () => {
      clearMaterial(); byId("material-list").replaceChildren();
      const result = await request("MaterialWork/searchMaterials", { query: { text: byId("material-query").value } });
      if (!Array.isArray(result.items)) throw new Error("検索結果の形式を確認できません。");
      if (!result.items.length) byId("material-list").append(node("p", "資料は見つかりませんでした。"));
      result.items.forEach(item => {
        if (typeof item.material_ref !== "string") throw new Error("資料参照を確認できません。");
        const button = node("button", item.title ?? item.material_ref); button.type = "button";
        button.addEventListener("click", () => run(() => inspectMaterial(item.material_ref)));
        byId("material-list").append(button);
      });
    });
  });
  byId("candidate-source").addEventListener("click", () => {
    const source = sourceSelection(); if (source) run(() => inspectMaterial(source.materialRef, source.resourceIds));
  });
  byId("new-intent").addEventListener("click", () => {
    if (intent && !confirm("保持した以前の意図に結果不明の送信がある場合は照合してください。別のCandidate作成を開始しますか？")) return;
    try {
      const next = { producerRef: producer, proposalId: crypto.randomUUID() };
      sessionStorage.setItem(intentStorage, JSON.stringify([...retainedIntents, next]));
      retainedIntents.push(next); intent = next; intentCaller = byId("token").value; intentReady = true; renderIntents();
      creationStatus("新しい作成意図を保持しました。資料と内容を確認して明示的に送信してください。");
    } catch { intentReady = false; creationStatus("作成意図を保持できないため送信できません。"); }
    controls();
  });
  byId("reconcile-intent").addEventListener("click", () => run(reconcileIntent));
  byId("retained-intents").addEventListener("change", () => {
    intent = retainedIntents.find(value => value.proposalId === byId("retained-intents").value);
    intentReady = false; creationStatus("保持した意図の結果は未確認です。作成時の呼出者で照合してください。"); controls();
  });
  byId("create-form").addEventListener("submit", event => {
    event.preventDefault(); if (!intentReady || !material || !selectedResources.length || busy) return;
    run(async () => {
      if (intentCaller !== byId("token").value) throw new Error("呼出者が変わりました。保持した意図を照合してください。");
      if (!discardAllowed()) return;
      const body = { proposalId: intent.proposalId, materialRef: material.material_ref,
        resourceIds: [...selectedResources], name: byId("creation-name").value, content: byId("creation-content").value };
      if (!body.name.trim() || !body.content.trim()) throw new Error("名前と内容を入力してください。");
      if (byId("creation-context-present").checked) body.context = byId("creation-context").value;
      intentReady = false;
      creationStatus("送信結果は未確認です。失敗・再読込み後は保持した意図を照合してください。自動再送しません。");
      const result = await request("MaterialWork/createCandidateFromMaterial", { body });
      if (typeof result.candidate?.id !== "string" || !result.candidate.id || typeof result.created !== "boolean" ||
          result.candidate.producer_ref !== producer || result.candidate.proposal_id !== intent.proposalId)
        throw new Error("Candidate IDを確認できません。保持した意図を照合してください。");
      selectedId = result.candidate.id; acknowledgement = `Candidate IDを確認しました: ${selectedId}`;
      creationStatus(acknowledgement); await reconcileSelected();
    });
  });
  byId("token").addEventListener("input", () => {
    intentReady = false; needsRead = true; clearMaterial(); byId("material-list").replaceChildren();
    byId("detail").replaceChildren(node("p", "呼出者が変わりました。明示的に再読み込みしてください。"));
    creationStatus("呼出者が変わりました。以前の作成意図は元の呼出者で照合してください。新規作成は明示的に開始してください。");
  });
  if (intent) creationStatus("保持した作成意図があります。結果は未確認です。送信せず照合してください。");
  renderIntents();
  byId("load-list").addEventListener("click", () => run(loadList));
  byId("context-present").addEventListener("change", controls);
  byId("reload").addEventListener("click", () => { if (discardAllowed()) run(reconcileSelected); });
  byId("edit-form").addEventListener("submit", event => {
    event.preventDefault(); if (needsRead || !work || busy) return;
    run(async () => { const reason = byId("edit-reason").value; if (!reason.trim()) throw new Error("編集の理由を入力してください。");
      await mutate(operation("edit", "editCandidateWork"), { id: selectedId, expectedRevision: revision, ...draft(), reason }, "編集内容の保存が確認できました。"); });
  });
  byId("decision-form").addEventListener("submit", event => {
    event.preventDefault(); if (needsRead || !work || busy) return;
    run(async () => { if (dirty()) throw new Error("未保存の編集があります。保存してから判断してください。");
      const reason = byId("decision-reason").value; if (!reason.trim()) throw new Error("判断の理由を入力してください。");
      await mutate(operation("decide", "decideCandidateWork"), { id: selectedId, expectedRevision: revision,
        decision: byId("decision").value, reason }, "判断の保存が確認できました。"); });
  });
  [["admit", "admitCandidateWork", "KnowledgeHubへの採用を確認しました。"],
    ["confirm-admission", "confirmCandidateAdmission", "保存済みInformationとの一致を確認しました。"]].forEach(([id, expected, success]) => {
    byId(id).addEventListener("click", () => {
      if (needsRead || !work || busy) return;
      run(async () => {
        if (dirty()) throw new Error("未保存の編集があります。保存してから採用結果を確認してください。");
        await mutate(operation(id, expected), { id: selectedId, expectedRevision: revision }, success);
      });
    });
  });
  window.addEventListener("beforeunload", event => { if (dirty() || byId("creation-content").value || byId("creation-name").value) { event.preventDefault(); event.returnValue = ""; } });
  window.addEventListener("pagehide", clearPreview);
  controls();
})();
