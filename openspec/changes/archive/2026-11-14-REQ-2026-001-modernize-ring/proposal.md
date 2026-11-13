<!-- docs-architect-meta {"schema_version":1,"id":"REQ-2026-001","type":"requirement","title":"Modernize Bezier loading component","status":"done","owners":[],"origin":{"kind":"request","ref":"openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/request.md"},"approval":{"kind":"request-record","ref":"openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/request.md"},"sources":[],"update_when":["Scope, acceptance, risk, implementation, evidence, or documentation disposition changes"],"relations":[],"dependencies":[],"acceptance":[{"id":"AC-1","text":"A loading-only Kotlin View retains the original four-cubic ring, supports validated style parameters, idempotent start/stop, lifecycle and reduced motion.","status":"passed","evidence":[{"kind":"command","ref":"openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/evidence/checks.txt","description":"Observed aggregate build, lint, publication and device test results"},{"kind":"log","ref":"openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/evidence/review.txt","description":"Integrated geometry, performance, lifecycle and accessibility review disposition"}]},{"id":"AC-2","text":"Stable tooling, focused sample, current-only README, AAR publication and AVD verification are complete.","status":"passed","evidence":[{"kind":"command","ref":"openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/evidence/checks.txt","description":"Observed aggregate build, lint, publication and device test results"},{"kind":"log","ref":"openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/evidence/review.txt","description":"Integrated geometry, performance, lifecycle and accessibility review disposition"}]}],"evidence":[{"kind":"command","ref":"openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/evidence/checks.txt","description":"Observed aggregate build, lint, publication and device test results"}],"affected_code":["app/**","besselloadingview/**","build.gradle.kts","settings.gradle.kts","gradle/**","gradle.properties","gradlew","gradlew.bat",".github/**"],"affected_docs":["README.md","docs/verification.md"],"open_questions":[],"documentation_disposition":"updated","no_doc_change_scope":[],"no_doc_change_reason":null,"slug":"modernize-ring","complexity":"small","risk":{"level":"medium","drivers":["performance"]},"retention":"summary","domains":["ring"],"spec_baselines":{"ring":null},"ephemeral_artifacts":[],"created_at":"2026-11-14T01:56:53+08:00","updated_at":"2026-11-14T02:17:33+08:00","status_changed_at":"2026-11-14T02:17:33+08:00","completed_at":"2026-11-14T02:17:33+08:00","archived_at":"2026-11-14T02:17:33+08:00","status_history":[{"status":"accepted","at":"2026-11-14T01:56:53+08:00","reason":"Change created"},{"status":"verifying","at":"2026-11-13T18:16:52.989010+00:00","reason":"Loading-only implementation and validation completed; preparing baseline"},{"status":"done","at":"2026-11-14T02:17:33+08:00","reason":"Close completed"}],"verified_at":"2026-11-13T18:16:52.989010+00:00","verified_against":"sha256:68f05d44964d967c301adb4e9b6c84b5c9dff4ffcbe47e49340df0a58e2ca843@openspec/changes/archive/2026-11-14-REQ-2026-001-modernize-ring/evidence/revision.json"} -->

# REQ-2026-001: Modernize Bezier loading component

Replace redundant renderers with one small Kotlin loading component. Keep the original curve and rotating gradient; related extensions are styling, lifecycle handling, state restoration and accessibility.

- [x] `AC-1` - loading-only API and original geometry, validated parameters, visibility/motion-aware animation.
- [x] `AC-2` - stable build, runnable sample, current documentation and device evidence.

Documentation disposition: updated — README.md and docs/verification.md describe current code. No migration guide. Git branch rename, commit and push are authorized separately by the captured request.

## Documentation Disposition

- Result: updated
- Affected docs: `README.md`, `docs/verification.md`

## History

- User clarified loading-only scope; implementation and documentation updated accordingly.

- 2026-11-14T02:17:33+08:00 - Close completed; requirement archived.
