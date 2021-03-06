package com.lakesidemutual.policyeventsconstraints

import java.time.Duration
import java.util.Properties

import com.github.annterina.stream_constraints.CStreamsBuilder
import com.github.annterina.stream_constraints.constraints.ConstraintBuilder
import com.github.annterina.stream_constraints.constraints.window.WindowConstraintBuilder
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Produced}
import org.apache.kafka.streams.state.QueryableStoreTypes.keyValueStore
import org.apache.kafka.streams.state.{KeyValueStore, QueryableStoreTypes, ReadOnlyKeyValueStore, ValueAndTimestamp}
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsConfig, Topology}

object PolicyEventsConstraints extends App {

  val kafkaStreamsConfig: Properties = {
    val properties = new Properties()
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "policy-events-constraints-application")
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    properties.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG")
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    properties
  }

  val policyEventSerde = Serdes.serdeFrom(PolicyEventSerde.serializer(), PolicyEventSerde.deserializer())

  val windowConstraint = new WindowConstraintBuilder[String, PolicyDomainEvent]
    .before(((_, e) => e.`type` == "DeletePolicyEvent", "policy-deleted"))
    .after(((_, e) => e.`type` == "UpdatePolicyEvent", "policy-updated"))
    .window(Duration.ofSeconds(1))
    .swap

  val constraint = new ConstraintBuilder[String, PolicyDomainEvent, String]
    .prerequisite(((_, e) => e.`type` == "UpdatePolicyEvent", "policy-updated"),
      ((_, e) => e.`type` == "DeletePolicyEvent", "policy-deleted"))
    .windowConstraint(windowConstraint)
    .terminal(((_, e) => e.`type` == "DeletePolicyEvent", "policy-deleted"))
    .redirect("policy-events-redirect")
    .link((_, e) => e.policyId)(Serdes.String)
    .build(Serdes.String, policyEventSerde)

  val builder = new CStreamsBuilder()

  builder
    .stream("policy-events")(Consumed.`with`(Serdes.String, policyEventSerde))
    .constrain(constraint)
    .to("policy-events-constrained")(Produced.`with`(Serdes.String, policyEventSerde))

  val topology: Topology = builder.build()

  val streams = new KafkaStreams(topology, kafkaStreamsConfig)

  streams.cleanUp()
  streams.start()

  sys.ShutdownHookThread {
    val keyValueStore = streams.store("Graph", QueryableStoreTypes.keyValueStore())
    val it = keyValueStore.all()
    var i = 0
    while (it.hasNext) {
      val next = it.next
      i = i + 1
    }
    print(s"TOTAL GRAPH KEYS: $i \n" )
    it.close()

    val deletedKeyValueStore = streams.store("policy-deleted", QueryableStoreTypes.keyValueStore())
    val itDeleted = deletedKeyValueStore.all()
    var j = 0
    while (itDeleted.hasNext) {
      val next = itDeleted.next
      j = j + 1
    }
    print(s"TOTAL DELETED KEYS: $j \n" )
    itDeleted.close()

    val deletedWindowStore = streams.store("policy-deleted-window", QueryableStoreTypes.windowStore())
    val itDeletedWindow = deletedWindowStore.all()
    var k = 0
    while (itDeletedWindow.hasNext) {
      val next = itDeletedWindow.next
      k = k + 1
    }
    print(s"TOTAL DELETED WINDOW KEYS: $k \n" )
    itDeletedWindow.close()

    streams.close(Duration.ofSeconds(5))
  }
}