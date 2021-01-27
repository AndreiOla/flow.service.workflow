package net.boomerangplatform.mongo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.WorkflowScope;
import net.boomerangplatform.mongo.repository.FlowWorkflowRepository;

@Service
public class FlowWorkflowServiceImpl implements FlowWorkflowService {

  @Autowired
  private FlowWorkflowRepository workFlowRepository;

  @Override
  public void deleteWorkflow(String id) {
    workFlowRepository.deleteById(id);
  }

  @Override
  public WorkflowEntity getWorkflow(String id) {
    return workFlowRepository.findById(id).orElse(null);
  }

  @Override
  public List<WorkflowEntity> getWorkflowsForTeams(String flowId) {
    return workFlowRepository.findByFlowTeamId(flowId);
  }

  @Override
  public List<WorkflowEntity> getWorkflowsForTeams(List<String> flowTeamIds) {
    return workFlowRepository.findByFlowTeamIdIn(flowTeamIds);
  }

  @Override
  public WorkflowEntity saveWorkflow(WorkflowEntity entity) {
    return workFlowRepository.save(entity);
  }

  @Override
  public WorkflowEntity findByTokenString(String tokenString) {
    return workFlowRepository.findByToken(tokenString);
  }

  @Override
  public List<WorkflowEntity> getScheduledWorkflows() {
    return workFlowRepository.findAllScheduledWorkflows();
  }

  @Override
  public List<WorkflowEntity> getEventWorkflows() {
    return workFlowRepository.findAllEventWorkflows();
  }

  @Override
  public List<WorkflowEntity> getEventWorkflowsForTopic(String topic) {
    return workFlowRepository.findAllEventWorkflowsForTopic(topic);
  }

  @Override
  public List<WorkflowEntity> getAllWorkflows() {
    return workFlowRepository.findAll();
  }

  @Override
  public List<WorkflowEntity> getSystemWorkflows(Optional<List<String>> statuses,
      Optional<List<String>> triggers) {
    if (statuses.isPresent() && triggers.isEmpty()) {

      return workFlowRepository.findByScopeAndStatusIn(WorkflowScope.system, statuses.get());
    } else if (triggers.isPresent() && statuses.isEmpty()) {

      return workFlowRepository.findByScopeAndTriggersIn(WorkflowScope.system, triggers.get());
    } else if (triggers.isPresent() && statuses.isPresent()) {

      return workFlowRepository.findByScopeAndStatusInAndTriggersIn(WorkflowScope.system,
          statuses.get(), triggers.get());
    } else {
      return workFlowRepository.findByScope(WorkflowScope.system);
    }

  }


  @Override
  public List<WorkflowEntity> getSystemWorkflows() {
    return workFlowRepository.findByScope(WorkflowScope.system);
  }
}
