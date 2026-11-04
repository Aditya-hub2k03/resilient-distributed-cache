# Jira Automation & Phase Epic Linkage

### 1. Smart Commits
All pull requests and commits should reference Jira issue keys:
- Pattern: `DC-[0-9]+: <Commit description>`
- Transitions:
  - `DC-101 #in-progress` transitions task to In Progress.
  - `DC-101 #done` transitions task on merge.

### 2. Automated SLO Breach Alert Hook
Whenever `dc_data_loss_keys_total > 0` or Prometheus fires `ClusterPartitionCritical`, Prometheus Alertmanager issues a webhook to:
`POST /jira/rest/api/2/issue`
- Issue Type: Bug
- Priority: Highest
- Summary: `[SLO BREACH] Data loss detected in Self-Healing Cluster`
