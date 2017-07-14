package pl.touk.esp.engine.management

import java.io.File
import java.nio.file.Files

import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.typesafe.config.ConfigValueFactory.fromAnyRef
import com.typesafe.config.{Config, ConfigFactory}
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{ContainerLink, DockerContainer, DockerFactory, DockerReadyChecker}
import org.apache.commons.io.FileUtils
import org.scalatest.Suite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

trait DockerTest extends DockerTestKit with ScalaFutures {
  self: Suite =>

  private val client: DockerClient = DefaultDockerClient.fromEnv().build()

  override implicit val patienceConfig = PatienceConfig(
    timeout = Span(90, Seconds),
    interval = Span(1, Millis)
  )

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(client)

  //TODO: make pull request to flink out of it?
  private def prepareDockerImage() = {
    val dir = Files.createTempDirectory("forDockerfile")
    val dirFile = dir.toFile
    FileUtils.copyInputStreamToFile(getClass.getResourceAsStream("/docker/Dockerfile"), new File(dirFile, "Dockerfile"))
    FileUtils.copyInputStreamToFile(getClass.getResourceAsStream("/docker/entrypointWithIP.sh"), new File(dirFile, "/entrypointWithIP.sh"))
    client.build(dir, "flinkesp:1.3.1")
  }

  prepareDockerImage()

  val KafkaPort = 9092
  val ZookeeperDefaultPort = 2181
  val FlinkJobManagerPort = 6123

  lazy val zookeeperContainer =
    DockerContainer("wurstmeister/zookeeper:3.4.6", name = Some("zookeeper"))

  lazy val kafkaContainer = DockerContainer("wurstmeister/kafka:0.10.2.1", name = Some("kafka"))
    .withEnv(s"KAFKA_ADVERTISED_PORT=$KafkaPort",
              s"KAFKA_ZOOKEEPER_CONNECT=zookeeper:$ZookeeperDefaultPort",
              "KAFKA_BROKER_ID=0",
              "HOSTNAME_COMMAND=grep $HOSTNAME /etc/hosts | awk '{print $1}'")
    .withLinks(ContainerLink(zookeeperContainer, "zookeeper"))
    .withReadyChecker(DockerReadyChecker.LogLineContains("started (kafka.server.KafkaServer)"))

  def baseFlink(name: String) = DockerContainer("flinkesp:1.3.1", Some(name))

  lazy val jobManagerContainer = baseFlink("jobmanager")
    .withCommand("jobmanager")
    .withEnv("JOB_MANAGER_RPC_ADDRESS_COMMAND=grep $HOSTNAME /etc/hosts | awk '{print $1}'")
    .withReadyChecker(DockerReadyChecker.LogLineContains("New leader reachable"))


  lazy val taskManagerContainer = baseFlink("taskmanager")
    .withCommand("taskmanager")
    .withEnv("JOB_MANAGER_RPC_ADDRESS_COMMAND=ping -q -c 1 jobmanager | grep PING | sed -e \"s/).*//\" | sed -e \"s/.*(//\"")
    .withReadyChecker(DockerReadyChecker.LogLineContains("Starting TaskManager actor"))
    .withLinks(
      ContainerLink(kafkaContainer, "kafka"),
      ContainerLink(zookeeperContainer, "zookeeper"),
      ContainerLink(jobManagerContainer, "jobmanager"))

  def config : Config = ConfigFactory.load()
    .withValue("flinkConfig.jobmanager.rpc.address", fromAnyRef(ipOfContainer(jobManagerContainer)))
    .withValue("flinkConfig.jobmanager.rpc.port", fromAnyRef(FlinkJobManagerPort))
    .withValue("prod.kafka.zkAddress", fromAnyRef(s"${ipOfContainer(zookeeperContainer)}:$ZookeeperDefaultPort"))
    .withValue("prod.kafka.kafkaAddress", fromAnyRef(s"${ipOfContainer(kafkaContainer)}:$KafkaPort"))

  private def ipOfContainer(container: DockerContainer) = container.getIpAddresses().futureValue.head

  abstract override def dockerContainers: List[DockerContainer] =
    List(zookeeperContainer, kafkaContainer, jobManagerContainer, taskManagerContainer) ++ super.dockerContainers

}
