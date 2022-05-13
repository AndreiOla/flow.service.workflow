package io.boomerang.model.eventing;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;

public class EventTrigger extends Event {

  private String workflowId;

  private String topic;

  private Map<String, String> properties;

  public EventTrigger() {}

  public static EventTrigger fromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    // Identify the type of event (it must be of type "trigger")
    EventType eventType;

    try {
      String eventTypeString = cloudEvent.getType().replace(EVENT_TYPE_PREFIX, "").toUpperCase();
      eventType = EventType.valueOf(eventTypeString);
    } catch (Exception e) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Invalid cloud event type : \"{0}\"!", cloudEvent.getType()));
    }

    if (eventType != EventType.TRIGGER) {
      throw new InvalidPropertiesFormatException(MessageFormat.format(
          "Cloud event type must be \"{0}\" but is \"{0}\"!",
          EVENT_TYPE_PREFIX + EventType.TRIGGER.toString().toLowerCase(), cloudEvent.getType()));
    }

    // Create trigger event object and set base properties
    EventTrigger eventTrigger = new EventTrigger();
    eventTrigger.setId(cloudEvent.getId());
    eventTrigger.setSource(cloudEvent.getSource());
    eventTrigger.setSubject(cloudEvent.getSubject());
    eventTrigger.setToken(Optional.ofNullable(cloudEvent.getExtension(EXTENSION_ATTRIBUTE_TOKEN))
        .orElseGet(() -> "").toString());
    eventTrigger.setDate(new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    eventTrigger.setType(eventType);

    // Extract workflow ID and topic from the subject
    String[] subjectTokens = eventTrigger.getSubject().trim().replaceFirst("^/", "").split("/");

    if (subjectTokens.length != 2) {
      throw new InvalidPropertiesFormatException(
          "For trigger cloud event types, the subject must have the format: \"/<workflow_id>/<workflow_custom_topic>\"");
    }

    eventTrigger.workflowId = subjectTokens[0];
    eventTrigger.topic = subjectTokens[1];

    // Extract the properties
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> properties = new HashMap<>();
    PojoCloudEventData<JsonNode> cloudEventData = CloudEventUtils.mapData(cloudEvent,
        PojoCloudEventDataMapper.from(objectMapper, JsonNode.class));

    if (cloudEventData != null) {
      cloudEventData.getValue().fields()
          .forEachRemaining(entry -> properties.put(entry.getKey(), entry.getValue().toString()));
    }

    eventTrigger.properties = properties;

    return eventTrigger;
  }

  public String getWorkflowId() {
    return this.workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getTopic() {
    return this.topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Map<String, String> getProperties() {
    return this.properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " workflowId='" + getWorkflowId() + "'" +
      ", topic='" + getTopic() + "'" +
      ", properties='" + getProperties() + "'" +
      "}";
  }
  // @formatter:on
}
