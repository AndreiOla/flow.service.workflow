package net.boomerangplatform.service.crud;

import static java.util.stream.Collectors.groupingBy;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import net.boomerangplatform.model.Execution;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.model.InsightsSummary;
import net.boomerangplatform.model.ListActivityResponse;
import net.boomerangplatform.model.Sort;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.service.FlowTeamService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.util.DateUtil;

@Service
public class FlowActivityServiceImpl implements FlowActivityService {

  @Autowired
  private FlowWorkflowActivityService flowActivityService;

  @Autowired
  private ActivityTaskService taskService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private RevisionService versionService;

  @Autowired
  private FlowWorkflowService workflowService;

  @Autowired
  private FlowTeamService flowTeamService;

  @Value("${controller.rest.url.base}")
  private String controllerBaseUrl;

  @Value("${controller.rest.url.streamlogs}")
  private String getStreamDownloadPath;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  private List<FlowActivity> convert(List<ActivityEntity> records) {

    final List<FlowActivity> flowActivities = new LinkedList<>();

    for (final ActivityEntity record : records) {
      final FlowActivity flow = new FlowActivity(record);
      final WorkflowEntity workflow = workflowService.getWorkflow(record.getWorkflowId());

      if (workflow != null) {
        flow.setWorkflowName(workflow.getName());
        flow.setDescription(workflow.getDescription());
        flow.setIcon(workflow.getIcon());
        flow.setShortDescription(workflow.getShortDescription());
      }

      flowActivities.add(flow);
    }
    return flowActivities;
  }

  @Override
  public ActivityEntity createFlowActivity(String workflowVersionId,
      Optional<FlowTriggerEnum> trigger, FlowExecutionRequest request) {
    /* Create new one based of work flow version id. */
    final RevisionEntity entity = versionService.getWorkflowlWithId(workflowVersionId);

    final ActivityEntity activity = new ActivityEntity();
    activity.setWorkflowRevisionid(workflowVersionId);
    activity.setWorkflowId(entity.getWorkFlowId());
    activity.setCreationDate(new Date());


    if (trigger.isPresent()) {
      activity.setTrigger(trigger.get());
    }

    if (!trigger.isPresent() || FlowTriggerEnum.manual == trigger.get()) {
      final FlowUserEntity userEntity = userIdentityService.getCurrentUser();
      activity.setInitiatedByUserId(userEntity.getId());
      activity.setInitiatedByUserName(userEntity.getName());
      activity.setTrigger(FlowTriggerEnum.manual);
    }

    if (request.getProperties() != null) {
      Map<String, String> properties = request.getProperties();
      List<CoreProperty> propertyList = new LinkedList<>();
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        String key = entry.getKey();
        String value = properties.get(key);
        CoreProperty prop = new CoreProperty();
        prop.setKey(key);
        prop.setValue(value);
        propertyList.add(prop);
      }
      activity.setProperties(propertyList);
    }
    return flowActivityService.saveWorkflowActivity(activity);
  }

  @Override
  public ActivityEntity findWorkflowActivity(String id) {
    return flowActivityService.findWorkflowActiivtyById(id);
  }

  @Override
  public ListActivityResponse getAllActivites(Optional<Date> from, Optional<Date> to, Pageable page,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<List<String>> statuses, Optional<List<String>> triggers, String property,
      Direction direction) {

    ListActivityResponse response = new ListActivityResponse();
    Page<ActivityEntity> records =
        flowActivityService.getAllActivites(from, to, page, workflowIds, statuses, triggers);

    final List<FlowActivity> activities = convert(records.getContent());
    List<FlowActivity> activitiesFiltered = new ArrayList<>();

    for (FlowActivity activity : activities) {
      String workFlowId = activity.getWorkflowId();
      addTeamInformation(teamIds, activitiesFiltered, activity, workFlowId);
    }

    net.boomerangplatform.model.Pageable pageable = createPageable(records, property, direction);
    response.setPageable(pageable);
    response.setRecords(activitiesFiltered);

    return response;
  }

  @Override
  public Map<String, Long> getActivitySummary(Pageable pageable, List<String> teamIds,
      List<String> triggers, Long fromDate, Long toDate) {
    Optional<Date> to =
        toDate == null ? Optional.empty() : Optional.of(DateUtil.asDate(getDateTime(toDate)));
    Optional<Date> from =
        fromDate == null ? Optional.empty() : Optional.of(DateUtil.asDate(getDateTime(fromDate)));

    List<String> workflowIds = new ArrayList<>();
    if (teamIds != null && !teamIds.isEmpty()) {
      workflowIds = workflowService.getWorkflowsForTeams(teamIds).stream()
          .map(WorkflowEntity::getId).collect(Collectors.toList());
    }

    List<ActivityEntity> flowWorkflowActivityEntities =
        flowActivityService.getAllActivites(from, to, pageable, getOptional(workflowIds),
            Optional.empty(), getOptional(triggers)).getContent();
    Map<String, Long> result = flowWorkflowActivityEntities.stream()
        .collect(groupingBy(v -> getStatusValue(v), Collectors.counting())); // NOSONAR
    result.put("all", Long.valueOf(flowWorkflowActivityEntities.size()));

    Arrays.stream(TaskStatus.values()).forEach(v -> initializeValue(v.getStatus(), result));
    return result;
  }

  private LocalDateTime getDateTime(Long toDate) {
    return Instant.ofEpochSecond(toDate.longValue()).atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }

  private <T> Optional<List<T>> getOptional(List<T> list) {
    if (list == null || list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list);
  }

  private String getStatusValue(ActivityEntity v) {
    return v.getStatus() == null ? "no_status" : v.getStatus().getStatus();
  }

  private void initializeValue(String key, Map<String, Long> result) {
    if (!result.containsKey(key)) {
      result.put(key, Long.valueOf(0));
    }
  }

  private void addTeamInformation(Optional<List<String>> teamIds,
      List<FlowActivity> activitiesFiltered, FlowActivity activity, String workFlowId) {
    String teamId;
    if ((workflowService.getWorkflow(workFlowId) != null)) {
      teamId = workflowService.getWorkflow(workFlowId).getFlowTeamId();
    } else {
      teamId = null;
    }

    if (teamId != null) {

      FlowTeamEntity team = flowTeamService.findById(teamId);
      String teamName = team.getName();

      activity.setTeamName(teamName);

      if (teamIds.isPresent()) {
        for (String teamID : teamIds.get()) {
          if (teamId.equals(teamID)) {
            activitiesFiltered.add(activity);
          }
        }
      } else {
        activitiesFiltered.add(activity);
      }
    }
  }

  @Override
  public ListActivityResponse getAllActivitesForUser(FlowUserEntity user, Optional<Date> from,
      Optional<Date> to, Pageable page, String property, Direction direction) {

    final Page<ActivityEntity> records =
        flowActivityService.findAllActivities(from, to, page);
    final ListActivityResponse response = new ListActivityResponse();

    final List<FlowActivity> activities = convert(records.getContent());
    net.boomerangplatform.model.Pageable pageable = createPageable(records, property, direction);
    response.setPageable(pageable);
    response.setRecords(activities);

    return response;
  }

  protected net.boomerangplatform.model.Pageable createPageable(
      final Page<ActivityEntity> records, String property, Direction direction) {
    net.boomerangplatform.model.Pageable pageable = new net.boomerangplatform.model.Pageable();
    pageable.setNumber(records.getNumber());
    pageable.setSize(records.getSize());
    pageable.setTotalElements(records.getTotalElements());
    pageable.setFirst(records.isFirst());
    pageable.setLast(records.isLast());
    pageable.setTotalPages(records.getTotalPages());
    pageable.setNumberOfElements(records.getNumberOfElements());

    List<Sort> listSort = new ArrayList<>();
    Sort sort = new Sort();
    sort.setDirection(direction);
    sort.setProperty(property);
    listSort.add(sort);
    pageable.setSort(listSort);

    return pageable;
  }

  @Override
  public List<TaskExecutionEntity> getTaskExecutions(String activityId) {
    return taskService.findTaskActiivtyForActivity(activityId);
  }

  @Override
  public TaskExecutionEntity saveTaskExecution(TaskExecutionEntity task) {
    return taskService.save(task);
  }

  @Override
  public InsightsSummary getInsightsSummary(Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<String> teamId) {

    final Page<ActivityEntity> records =
        flowActivityService.findAllActivities(from, to, pageable);
    final InsightsSummary response = new InsightsSummary();
    final List<FlowActivity> activities = convert(records.getContent());
    List<Execution> executions = new ArrayList<>();
    Long totalExecutionTime = 0L;
    Long executionTime;

    for (FlowActivity activity : activities) {

      executionTime = activity.getDuration();

      if (executionTime != null) {
        totalExecutionTime = totalExecutionTime + executionTime;
      }

      addActivityDetail(teamId, executions, activity);
    }
    response.setTotalActivitiesExecuted(executions.size());
    response.setExecutions(executions);

    if (response.getTotalActivitiesExecuted() != 0) {
      response.setMedianExecutionTime(totalExecutionTime / executions.size());

    } else {
      response.setMedianExecutionTime(0L);
    }
    return response;
  }

  private void addActivityDetail(Optional<String> teamId, List<Execution> executions,
      FlowActivity activity) {
    String teamName;
    String workflowName;
    String workflowId;
    String activityTeamId;
    if (workflowService.getWorkflow(activity.getWorkflowId()) != null) {
      activityTeamId = workflowService.getWorkflow(activity.getWorkflowId()).getFlowTeamId();
      FlowTeamEntity team = flowTeamService.findById(activityTeamId);

      if (team != null) {
        teamName = team.getName();

      } else {
        teamName = null;
      }

      workflowName = workflowService.getWorkflow(activity.getWorkflowId()).getName();
      workflowId = activity.getWorkflowId();
    } else {
      activityTeamId = null;
      teamName = null;
      workflowName = null;
      workflowId = null;
    }

    if (teamId.isPresent()) {
      String teamID = teamId.get();

      if (activityTeamId != null && teamID.equals(activityTeamId)) {
        Execution execution = createExecution(activity, teamName, workflowName, workflowId);
        executions.add(execution);
      }
    } else {
      Execution execution = createExecution(activity, teamName, workflowName, workflowId);
      executions.add(execution);
    }
  }

  private Execution createExecution(FlowActivity activity, String teamName, String workflowName,
      String workflowId) {
    Execution execution = new Execution();
    execution.setActivityId(activity.getId());
    execution.setStatus(activity.getStatus());
    execution.setDuration(activity.getDuration());
    execution.setCreationDate(activity.getCreationDate());
    execution.setTeamName(teamName);
    execution.setWorkflowName(workflowName);
    execution.setWorkflowId(workflowId);
    return execution;
  }

  @Override
  public StreamingResponseBody getTaskLog(String activityId, String taskId) {

    TaskExecutionEntity executionEntity =
        taskService.findByTaskIdAndActiityId(taskId, activityId);
    if (executionEntity == null) {
      return null;
    }

    return outputStream -> {
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("workflowId",
          flowActivityService.findWorkflowActiivtyById(activityId).getWorkflowId());
      requestParams.put("workflowActivityId", activityId);
      requestParams.put("taskActivityId", executionEntity.getId());
      requestParams.put("taskId", taskId);

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", controllerBaseUrl + getStreamDownloadPath + "?", ""));

      RequestCallback requestCallback = request -> request.getHeaders()
          .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

      ResponseExtractor<Void> responseExtractor = restTemplateResponse -> {
        InputStream is = restTemplateResponse.getBody();

        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
          outputStream.write(data, 0, nRead);
        }

        return null;
      };
      restTemplate.execute(encodedURL, HttpMethod.GET, requestCallback, responseExtractor);

      outputStream.close();

    };

  }
}
