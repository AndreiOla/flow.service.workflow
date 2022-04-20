package io.boomerang.service;

import java.time.Duration;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import io.boomerang.eventing.nats.ConnectionPrimer;
import io.boomerang.eventing.nats.jetstream.PubSubConfiguration;
import io.boomerang.eventing.nats.jetstream.PubSubTransceiver;
import io.boomerang.eventing.nats.jetstream.PubSubTunnel;
import io.boomerang.eventing.nats.jetstream.SubHandler;
import io.boomerang.eventing.nats.jetstream.SubOnlyTunnel;
import io.boomerang.mongo.entity.ActivityEntity;
import io.nats.client.Options;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

@Service
@ConditionalOnProperty(value = "eventing.enabled", havingValue = "true", matchIfMissing = false)
public class EventingServiceImpl implements EventingService, SubHandler {

  private static final Logger logger = LogManager.getLogger(EventingServiceImpl.class);

  @Value("#{'${eventing.nats.server.urls}'.split(',')}")
  private List<String> serverUrls;

  @Value("${eventing.nats.server.reconnect-wait-time:PT10S}")
  private Duration serverReconnectWaitTime;

  @Value("${eventing.nats.server.reconnect-max-attempts:-1}")
  private Integer serverReconnectMaxAttempts;

  @Value("${eventing.jetstream.stream.name}")
  private String jetstreamStreamName;

  @Value("${eventing.jetstream.stream.storage-type}")
  private StorageType jetstreamStreamStorageType;

  @Value("${eventing.jetstream.stream.subject}")
  private String jetstreamStreamSubject;

  @Value("${eventing.jetstream.consumer.name}")
  private String jetstreamConsumerDurableName;

  @Value("${eventing.jetstream.consumer.resub-wait-time:PT10S}")
  private Duration jetstreamConsumerResubscribeWaitTime;

  @Lazy
  @Autowired
  private EventProcessor eventProcessor;

  private ConnectionPrimer connectionPrimer;

  private PubSubTunnel pubSubTunnel;

  @EventListener(ApplicationReadyEvent.class)
  void onApplicationReadyEvent() throws InterruptedException {

    // @formatter:off
    Options.Builder optionsBuilder = new Options.Builder()
        .servers(serverUrls.toArray(new String[0]))
        .reconnectWait(serverReconnectWaitTime)
        .maxReconnects(serverReconnectMaxAttempts);
    StreamConfiguration streamConfiguration = new StreamConfiguration.Builder()
        .name(jetstreamStreamName)
        .storageType(jetstreamStreamStorageType)
        .subjects(jetstreamStreamSubject)
        .build();
    ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration.Builder()
        .durable(jetstreamConsumerDurableName)
        .build();
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration.Builder()
        .automaticallyCreateStream(true)
        .automaticallyCreateConsumer(true)
        .build();
    // @formatter:on

    connectionPrimer = new ConnectionPrimer(optionsBuilder);
    pubSubTunnel = new PubSubTransceiver(connectionPrimer, streamConfiguration,
        consumerConfiguration, pubSubConfiguration);

    // Start subscription
    pubSubTunnel.subscribe(this);
  }

  @Override
  public void subscriptionSucceeded(SubOnlyTunnel tunnel) {
    logger.info("Successfully subscribed to consume messages from NATS Jetstream.");
  }

  @Override
  public void subscriptionFailed(SubOnlyTunnel tunnel, Exception exception) {
    logger.debug("Failed to subscribe for consuming messages from NATS Jetstream. Resubscribing...",
        exception);
    try {
      Thread.sleep(jetstreamConsumerResubscribeWaitTime.toMillis());
    } catch (Exception e) {
      logger.warn("Sleep failed: resubscribing without a waiting time...", e);
    } finally {
      tunnel.subscribe(this);
    }
  }

  @Override
  public void newMessageReceived(SubOnlyTunnel tunnel, String subject, String message) {
    eventProcessor.processNATSMessage(message);
  }

  /**
   * This method will publish a Cloud Event encoded as a string to the NATS server. Please make sure
   * the status of the {@link ActivityEntity} is updated when invoking this method.
   * 
   * @param activityEntity Activity entity.
   * 
   * @note Do not invoke this method with if the status of the {@link ActivityEntity} has not been
   *       changed, as this would result in publishing a Cloud Event with the same status multiple
   *       times.
   */
  @Override
  public void publishWorkflowActivityStatusUpdateCE(ActivityEntity activityEntity) {

    logger.error("---- Workflow with ID " + activityEntity.getWorkflowId()
        + " has changed its status to " + activityEntity.getStatus() + " ----");
  }
}
