# TKL 観点の Knowledge Workbench 要求仕様

- Date: 2026-10-02
- Status: Requirements proposal from TKL; TKW contract review and implementation pending
- Requesting application: Textus Knowledge Lake (TKL)
- Receiving application / owner: Textus Knowledge Workbench (TKW)
- Scope: Proposal receipt, candidate lifecycle, provenance, preparation correlation and Information Admission feedback

## 目的と適用範囲

TKL が準備した根拠付きの提案を TKW に引き渡し、TKW が自分の Information Candidate として形成・編集・レビュー・承認できることを要求する。TKL の Google Workspace 連携と TKW の開発を並行して進められるよう、provider-neutral な受け渡し契約と fixture で確認できる受入条件を定義する。

本書は TKL から TKW に対する要求案である。TKW の既存 Phase を変更・完了したとは扱わず、実装済み API、transport、DB schema、CML syntax を宣言しない。具体的な契約は TKW と共同で確定する。

現在の TKL backend/context notes とその review follow-up を入力とする。TKL 側のこれらの補足は本書作成時点では未コミットであり、共同契約を freeze する際に参照版を確定する。

## 責務と用語

| 対象 | 正本と責務 |
| --- | --- |
| 原資料 | Drive 等の provider に本体を保持。必要な snapshot の保持は TKL が管理する |
| Resource / Evidence | TKL が canonical identity、選択版、出典と処理範囲を管理する |
| KnowledgeProcessingContext / ProcessingRun | TKL が継続的文脈と一回の実行を区別して管理する |
| PreparedMaterial | TKL が stable ID、生成版、producing Run と根拠を管理する |
| RawCandidateProposal | TKL が作成する提案。TKW Information Candidate 自体ではない |
| InformationCandidate / Review / Approval | TKW が identity、編集版、ライフサイクルと人の判断を管理する |
| Canonical Information | KnowledgeHub が正本を管理し、TKW が明示的な人の承認に基づく Admission を要求する |
| KnowledgeProjection | TKL が KnowledgeHub Information の投影を管理する |

TKL の「Knowledge Candidate」は受け渡し時には Information Candidate への提案として扱う。別の canonical Knowledge Entity を TKW に新設しない。TKW の domain Context と TKL の処理 Context、および runtime Presentation Context は異なる概念とする。

## 連携経路

```text
Discovery preparation
  TKL Resource / Evidence -> Context / Run -> PreparedMaterial
    -> RawCandidateProposal -> TKW receipt -> InformationCandidate
    -> Edit / Review / Human Approval -> KnowledgeHub Information Admission
    -> Admission result -> TKL Existing Knowledge Context refresh

Candidate preparation（後続経路）
  TKW InformationCandidate + CandidateSource / RawDataIndex
    -> CandidatePreparationRequest -> TKL preparation / Run
    -> PreparedMaterial + provenance -> 同じ TKW Candidate の根拠へ関連付け
```

## 要求一覧と実装段階

初期連携の受入対象は TKL-TKW-01〜07 とする。TKL-TKW-08 はその先の本番 Admission で必須となる契約、09〜10 はその結果を使う feedback、11〜12 は既存候補の準備処理に向けた後続要求である。後続も要求として記録するが、最初の Drive からの提案受領経路の完了条件には加えない。

| ID | 要求 | 段階 |
| --- | --- | --- |
| TKL-TKW-01 | provider-neutral な提案の受領と入力検証 | 初期 |
| TKL-TKW-02 | 再送・同時受領時の重複防止と内容競合の検出 | 初期 |
| TKL-TKW-03 | 永続的な受領結果と Candidate 対応の照会 | 初期 |
| TKL-TKW-04 | 版付き根拠と処理来歴の保持・表示 | 初期 |
| TKL-TKW-05 | TKW 所有の候補編集・レビュー・承認 | 初期 |
| TKL-TKW-06 | 候補の更新競合と承認対象版の保証 | 初期 |
| TKL-TKW-07 | 部分失敗・timeout・再開の区別 | 初期 |
| TKL-TKW-08 | 人の承認に基づく Information Admission と trace | 本番 Admission |
| TKL-TKW-09 | Admission 結果の TKL への公開 | Admission feedback |
| TKL-TKW-10 | 既存 Information の参照と知識基準の追跡 | Admission feedback |
| TKL-TKW-11 | 候補中心の CandidateSource / RawDataIndex | 既存候補準備 |
| TKL-TKW-12 | 準備依頼・返却成果の相関と重複防止 | 既存候補準備 |

## 初期連携の機能要求

### TKL-TKW-01 — 提案の受領

TKW は TKL の RawCandidateProposal を検証し、Information Candidate を作成できること。初期の一提案は一候補へ対応付ける。既存候補への統合や複数候補への分割は人の編集判断として別の履歴を持ち、受領時に暗黙に行わない。

概念上の最低限の入力項目を以下とする。必須性と実際の field 名は共同 schema で確定する。

| 項目 | 意味 |
| --- | --- |
| schemaVersion | 受け渡し schema の版。Entity の編集版とは分ける |
| producerRef / proposalId | 送信元 TKL とその提案の stable ID |
| candidateKind / suggestedContent | Information の候補として提案する種別と内容 |
| preparedMaterialRefs | PreparedMaterial の ID・生成版と取得用参照 |
| evidenceRefs / provenanceRef | 根拠の ID・選択版・snapshot と処理来歴への参照 |
| sourceContextId / sourceRunId | 提案に関係する TKL の文脈と処理実行 |
| existingInformationRefs | 関連する既存 Information の ID・版（該当する場合） |
| createdAt / correlationRef | 作成時刻と連携・処理の相関情報 |

取得先の URL / Drive ID と canonical TKL ID を区別する。TKW の受領は Google Workspace API、OAuth、Drive Project / Notebook の操作を前提にしない。入力 schema や必須参照が不正な場合は理由を返し、正常な Candidate を作成したと報告しない。

### TKL-TKW-02 — 冪等性と内容競合

producerRef と proposalId を組にしたキーを受領単位とする。TKW は同じキー・同じ提案内容の再送を同じ受領結果と Candidate に解決し、同時に届いた場合も Candidate を重複作成しないこと。

同じキーで異なる提案内容を受けた場合は競合として返し、保存済み提案や人が編集した候補を上書きしない。新しい提案には TKL が新しい proposalId を発行する。提案内容の同一性は共同 schema の対象項目と正規化規則で定義し、transport の再送時刻等を内容の差分と混同しない。

内容ハッシュは整合性の補助に利用できるが、proposalId や Candidate identity の代わりにしない。後の候補編集・統合・保留・却下・削除によって受領キーを忘れ、古い再送から別の Candidate を作ることがないよう、キー保持期間と廃止後の扱いを定義する。

### TKL-TKW-03 — 永続的な受領結果と照会

TKW は受領キー、提案への適用結果、receipt ID、対応する Candidate ID を永続化し、送信元から照会できること。成功を返す前に、重複判定・候補作成・受領対応が一貫した形で確定している必要がある。transaction 等の具体的な保証方式は TKW の永続化契約で決める。

結果は少なくとも未登録、処理中、受領確定、入力拒否、内容競合、結果未確認を区別できること。これは候補の Review / Approved / Admitted 状態とは別の受領状態である。状態名・エラーコードは未確定とする。

元の receipt / Candidate 対応と、候補の最新状態・最新 revision を区別して返す。提案の受領成功は人の承認や Information Admission を意味しない。

### TKL-TKW-04 — 根拠と処理来歴

TKW は Candidate から提案、PreparedMaterial の生成版、ProcessingRun、Evidence の選択版・snapshot、Resource の版まで辿れる参照を保存すること。TKL Context の現在の資料集合や provider の最新版へ差し替えて、過去の根拠を更新しない。

人が編集した Candidate の内容と元の提案内容を区別し、変更履歴、レビュー判断と根拠の関連を残す。TKW の利用者は根拠の概要、出典、準備処理と参照可能性を確認できること。原資料を開くときは既存の参照解決・権限の範囲を利用し、TKW が資料の本体や provider 連携を複製しない。

TKL が保持した snapshot の参照と、内容未保持の provider revision 参照を区別する。参照先の取得不能、版不明、内容未保持、外部 processor の未確認入力は表示・判断上の不確実性として保持する。必要な根拠が取得できない場合に、要件を満たしたレビューと黙って扱わない。候補受領とレビュー可能性の判断は別に記録する。

### TKL-TKW-05 — 候補ライフサイクルと人の判断

TKW は自分の Candidate ID と revision を持ち、提案受領後の編集、既存 Information との比較、Semantic Grounding / Concept Mapping、Review、Approve / Hold / Reject を管理すること。TKL は提案内容と根拠を提供し、候補の承認状態を直接変更しない。

人の判断には対象 Candidate revision、判断者、時刻、理由を対応付ける。TKW の既存の Proposed → Draft → Editing → Review → Approved → Admission Requested → Admitted という方針に沿って具体的な状態遷移を定義する。AI の提案、資料の送信許可、mobile Capture Confirm を Information Admission の承認と同一視しない。

### TKL-TKW-06 — 更新競合と承認対象版

候補の更新は、読み取った revision が有効であることの確認と書き込みを一体として保証し、同時編集で他者の変更を失わないこと。単なる JSON revision の加算を排他制御とみなさない。

承認は特定の候補内容・版に対して有効とする。承認後に内容または判断に用いる根拠が変わる場合、以前の承認を新しい版へ暗黙に流用しない。再レビュー・再承認と Admission 対象版の規則を定義する。後から届いた TKL の成果を根拠として関連付ける操作と、編集内容を適用する操作を区別する。

### TKL-TKW-07 — 部分失敗と再開

TKW は受領・候補作成・receipt 保存が途中で失敗した場合、同じ受領キーによる照会・再送で既存結果へ解決するか、未完了部分を再開できること。成功を確認できない状態を正常完了として報告しない。

送信側の timeout は、TKW が処理しなかった証拠とは扱わない。TKL は同じキーを使用して確認・再送する。再送の結果だけで、人が編集済みの Candidate やそのレビュー状態を初期化しない。

## Admission と knowledge feedback の要求

### TKL-TKW-08 — Canonical Information への Admission

本番経路では TKW が承認対象版を明示して KnowledgeHub の Information Admission を要求し、Candidate / review / approval と admitted Information ID・版の対応を保持すること。Admission 要求、KnowledgeHub の受理、実際の成立、失敗・結果未確認を区別し、同じ対象の再試行が重複 Admission を起こさない共同契約を定義する。

KnowledgeHub の契約と接続は独立した依存である。fixture による Admission 結果の検証を実 KnowledgeHub への登録完了と主張しない。本書の初期連携受入はこの実接続の完了を要求しないが、TKW Phase 1 の本来の完了条件を緩和するものでもない。

### TKL-TKW-09 — Admission 結果の公開

TKW は TKL に、元の producerRef / proposalId、Candidate ID・対象 revision、Admission 結果、Information ID・版、根拠・処理来歴への対応を照会または通知できること。通知する場合は、再配信・順不同・欠落時の再照会と結果の重複適用防止を共同契約で定める。

TKL はこの結果を使い KnowledgeHub の正本から KnowledgeProjection を取得・更新する。通知だけを canonical Information と扱わない。TKW は TKL の投影保存、context rebuild、Drive Project / Notebook 同期を所有しない。

### TKL-TKW-10 — 既存 Information との関係

TKW は提案の関連 Information ID・版と、TKL Run が利用した既存知識の snapshot 参照を保持し、候補の比較・形成の根拠として利用できること。TKW が現在の Information と比較する場合は、その比較対象版も別に記録する。

TKL が準備時に知っていた知識と TKW が形成時に参照する最新版を混同しない。成立後の ID・版を返すことで TKL が新規、更新、支持、矛盾、関係等の次の提案を作れるようにする。これらの提案分類の詳細 schema は共同設計対象とする。

## 既存候補の準備処理に向けた後続要求

### TKL-TKW-11 — 候補中心の資料索引

TKW は CandidateSource / RawDataIndex に、Candidate ID、source の identity、TKL Resource ID・版付き参照、media type、origin / capture / submission 情報、準備要求・成果への対応を保存すること。写真・音声・文書本体の永続化や TKL Resource 管理を TKW DB に複製しない。

同じ資料が複数の候補を支える場合、候補ごとの関連を表現できること。source-storage 状態、preparation 状態、review readiness と Admission 状態を区別する。既存 mobile capture の送信許可と人の Admission 承認の区別を維持する。

### TKL-TKW-12 — 準備依頼と返却結果の相関

TKW は既存候補に対して、requestId、依頼元、tkwCandidateRef、要求時の Candidate revision、resourceRefs、目的・要求処理を持つ CandidatePreparationRequest を作成できること。TKL からの結果には requestId、Candidate 参照、Run と PreparedMaterial の生成版・provenance を対応付ける。

TKW は重複する結果を一度だけ関連付け、別の Candidate への誤適用を防ぐ。要求後に候補が編集・保留・却下された場合は、返却成果を保存しても候補内容を無条件で上書き・再承認しない。内容への適用には対象版の確認と明示的な判断を用いる。

準備失敗、部分成果、処理中、結果未確認を成功と分ける。同じ依頼の再送・再開と新しい準備要求を区別し、TKL の要求受領側の冪等性も共同契約に含める。この経路は最初の discovery proposal 受領経路の後に実装する。

## 初期経路の受入例

以下は executable specification に落とすべき振る舞いの要求であり、本書作成時にテスト実装・実行済みとは扱わない。

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC-01 | 未登録の有効な TKL 提案と根拠 fixture | 提案を受領する | 一つの Candidate と永続 receipt を作成し、キーから対応を照会できる |
| AC-02 | 既に受領済みの提案 | 同一内容を同じキーで再送する | 同じ receipt / Candidate に解決し、現在の編集・レビュー状態を変更しない |
| AC-03 | 同じキー・同じ内容の提案 | 同時に二つの受領処理が走る | Candidate は一つだけ成立し、両応答は同じ受領対応へ解決する |
| AC-04 | 既に受領済みのキー | 異なる内容を同じキーで送る | 内容競合を返し、元の提案と候補を変更しない |
| AC-05 | 不正 schema / 必須参照の欠落 | 提案を送る | 理由付きで拒否し、正常な Candidate 作成を報告しない |
| AC-06 | TKW は受領確定したが応答を送信側が得られない | 同じキーで照会・再送する | 既存 receipt / Candidate を返し、二重作成しない |
| AC-07 | 候補作成と受領記録の処理に失敗を注入する | 同じキーで再開する | 成功を早まって返さず、一貫した受領対応へ回復する |
| AC-08 | 版付き PreparedMaterial / Evidence と Run の参照 | Candidate の根拠を開き、TKL Context の現在集合も変更する | 元の提案・Run・入力版を辿れ、過去の来歴は変わらない |
| AC-09 | 根拠に内容未保持・取得不能・未確認情報がある | 候補を閲覧・レビューする | 限界を確認でき、根拠が確認済みであると偽って扱わない |
| AC-10 | 同じ候補版を二者が編集する | 両者が更新する | stale revision の更新が黙って他方を上書きしない |
| AC-11 | 人が特定の候補版を承認済み | 内容・判断根拠を変えた版を作る | 旧承認を新しい版へ暗黙に適用せず、承認対象を確認できる |

初期 fixture は TKL 提案、受領結果、版付き根拠と編集・レビュー判断を含める。実 Drive / Notebook の操作、Gmail / Slack、mobile capture、KnowledgeHub 実接続はこの連携 fixture の前提にしない。UI は既存 View Model / Display Model 方針を利用し、独自の Flutter/protocol 契約を要求しない。

後続受入には、実 Admission trace と feedback の再配信、CandidatePreparationRequest の成功・失敗・重複返却・stale Candidate への返却を加える。

## 並行開発の分担と契約確定事項

| 所有者 | 先に進められること |
| --- | --- |
| TKL | proposal fixture、版付き PreparedMaterial / Evidence / Run の参照、手動 Drive 準備と成果取り込み |
| TKW | fixture の受領、receipt / Candidate の永続化、重複防止、編集・レビューと根拠表示 |
| TKL + TKW | schema の必須性・互換性、同一内容の定義、冪等性キーの保持、結果照会・相関とエラー意味 |
| TKW + KnowledgeHub | Information Admission の対象版、再試行、成立確認と canonical Information trace |

transport / operation 名・endpoint、認証済み producer と producerRef の対応、永続化保証、詳細状態遷移、参照解決、retention、変更通知の保証は共同レビューで確定する。server/CNCF の既存契約を確認して利用し、存在しない本番 API を fixture の実績から推測しない。

この要求記録から他プロジェクトの実装・Phase 終了・本番 Admission を自動的に承認しない。まず初期契約と fixture を共有し、それぞれの owning repository で実装範囲を定める。

## 関連資料

- [TKW architecture](../../../notes/architecture.md)
- [TKW Phase 1](../../../phase/phase-1.md)
- [Workbench DB and raw-data index](../../2026-09-25-workbench-db-raw-data-index.md)
- [Knowledge application integration strategy](../../../strategy/knowledge-application-integration.md)
- [TKL Google Workspace backend/context note](../../../../../textus-knowledge-lake/docs/notes/google-workspace-backed-tkl.md)
- [TKL design review](../../../../../textus-knowledge-lake/docs/journal/2026-10-02-google-workspace-backed-tkl-review.md)
- [TKL review follow-up decisions](../../../../../textus-knowledge-lake/docs/journal/2026-10-02-google-workspace-backed-tkl-review-follow-up.md)
