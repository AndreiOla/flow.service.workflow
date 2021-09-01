package io.boomerang.mongo.entity;

import java.util.Date;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.mongo.model.Audit;
import io.boomerang.mongo.model.ManualType;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows_activity_approval')}")
public class ApprovalEntity {
  
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getActivityId() {
    return activityId;
  }
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }
  public String getTaskActivityId() {
    return taskActivityId;
  }
  public void setTaskActivityId(String taskActivityId) {
    this.taskActivityId = taskActivityId;
  }
  public Audit getAudit() {
    return audit;
  }
  public void setAudit(Audit audit) {
    this.audit = audit;
  }
  @Id
  private String id;
  
  public String getWorkflowId() {
    return workflowId;
  }
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
  public String getTeamId() {
    return teamId;
  }
  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }
  public ApprovalStatus getStatus() {
    return status;
  }
  public void setStatus(ApprovalStatus status) {
    this.status = status;
  }
 
  public ManualType getType() {
    return type;
  }
  public void setType(ManualType type) {
    this.type = type;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
  private String activityId;
  
  private String taskActivityId;
  private String workflowId;
  private String teamId;
  private Audit audit;
  private ApprovalStatus status;
  private ManualType type;
  private Date creationDate;

}
