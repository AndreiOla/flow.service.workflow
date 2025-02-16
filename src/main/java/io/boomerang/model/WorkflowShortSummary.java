package io.boomerang.model;

import io.boomerang.mongo.model.WorkflowScope;

public class WorkflowShortSummary {
  
  public String getWorkflowId() {
    return workflowId;
  }
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
  public String getWorkflowName() {
    return workflowName;
  }
  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
  public String getTeamId() {
    return teamId;
  }
  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }
  public String getTeamName() {
    return teamName;
  }
  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }
  public boolean isWebhookEnabled() {
    return webhookEnabled;
  }
  public void setWebhookEnabled(boolean webHookEnabled) {
    this.webhookEnabled = webHookEnabled;
  }
  private String workflowId;
  private String workflowName;
  private String teamId;
  private String teamName;
  private boolean webhookEnabled;
  private WorkflowScope scope;
  public WorkflowScope getScope() {
    return scope;
  }
  public void setScope(WorkflowScope scope) {
    this.scope = scope;
  }
  
}
