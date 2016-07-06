package mesosphere.marathon.state

import mesosphere.marathon.{ MarathonSpec, Protos }
import mesosphere.marathon.api.JsonTestHelper
import mesosphere.marathon.api.serialization.{ ContainerSerializer, MesosDockerSerializer, VolumeSerializer }
import org.apache.mesos.{ Protos => Mesos }
import org.scalatest.Matchers
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

class MesosDockerContainerTest extends MarathonSpec with Matchers {
  import mesosphere.marathon.api.v2.json.Formats._

  class Fixture {
    lazy val volumes = Seq(
      DockerVolume("/etc/a", "/var/data/a", Mesos.Volume.Mode.RO),
      DockerVolume("/etc/b", "/var/data/b", Mesos.Volume.Mode.RW)
    )

    lazy val container1 = Container.MesosDocker(
      volumes = volumes,
      image = "group/image"
    )

    lazy val container2 = Container.MesosDocker(
      volumes = Nil,
      image = "group/image",
      credential = Some(Container.Credential("aPrincipal")),
      forcePullImage = false
    )

    lazy val container3 = Container.MesosDocker(
      volumes = Nil,
      image = "group/image",
      credential = Some(Container.Credential("aPrincipal", Some("aSecret"))),
      forcePullImage = true
    )
  }

  def fixture(): Fixture = new Fixture

  test("ToProto") {
    val f = fixture()

    val proto1 = ContainerSerializer.toProto(f.container1)
    assert(Mesos.ContainerInfo.Type.MESOS == proto1.getType)
    assert(proto1.hasMesosDocker)
    assert("group/image" == proto1.getMesosDocker.getImage)
    assert(f.container1.volumes == proto1.getVolumesList.asScala.map(Volume(_)))
    assert(proto1.getMesosDocker.hasForcePullImage)
    assert(f.container1.forcePullImage == proto1.getMesosDocker.getForcePullImage)

    val proto2: mesosphere.marathon.Protos.ExtendedContainerInfo = ContainerSerializer.toProto(f.container2)
    assert(Mesos.ContainerInfo.Type.MESOS == proto2.getType)
    assert(proto2.hasMesosDocker)
    assert("group/image" == proto2.getMesosDocker.getImage)
    assert(proto2.getMesosDocker.hasCredential)
    assert(f.container2.credential.get.principal == proto2.getMesosDocker.getCredential.getPrincipal)
    assert(!proto2.getMesosDocker.getCredential.hasSecret)
    assert(proto2.getMesosDocker.hasForcePullImage)
    assert(f.container2.forcePullImage == proto2.getMesosDocker.getForcePullImage)

    val proto3 = ContainerSerializer.toProto(f.container3)
    assert(Mesos.ContainerInfo.Type.MESOS == proto3.getType)
    assert(proto3.hasMesosDocker)
    assert("group/image" == proto3.getMesosDocker.getImage)
    assert(proto3.getMesosDocker.hasCredential)
    assert(f.container3.credential.get.principal == proto3.getMesosDocker.getCredential.getPrincipal)
    assert(proto3.getMesosDocker.getCredential.hasSecret)
    assert(f.container3.credential.get.secret.get == proto3.getMesosDocker.getCredential.getSecret)
    assert(proto3.getMesosDocker.hasForcePullImage)
    assert(f.container3.forcePullImage == proto3.getMesosDocker.getForcePullImage)
  }

  test("ToMesos") {
    val f = fixture()

    val proto1 = ContainerSerializer.toMesos(f.container1)
    assert(Mesos.ContainerInfo.Type.MESOS == proto1.getType)
    assert(proto1.hasMesos)
    assert(proto1.getMesos.hasImage)
    assert(proto1.getMesos.getImage.hasDocker)
    assert(proto1.getMesos.getImage.getDocker.hasName)
    assert("group/image" == proto1.getMesos.getImage.getDocker.getName)
    assert(proto1.getMesos.getImage.hasCached)
    assert(f.container1.forcePullImage == !proto1.getMesos.getImage.getCached)

    val proto2 = ContainerSerializer.toMesos(f.container2)
    assert(Mesos.ContainerInfo.Type.MESOS == proto2.getType)
    assert(proto2.hasMesos)
    assert(proto2.getMesos.hasImage)
    assert(proto2.getMesos.getImage.hasDocker)
    assert(proto2.getMesos.getImage.getDocker.hasName)
    assert("group/image" == proto2.getMesos.getImage.getDocker.getName)
    assert(proto2.getMesos.getImage.getDocker.hasCredential)
    assert(f.container2.credential.get.principal == proto2.getMesos.getImage.getDocker.getCredential.getPrincipal)
    assert(!proto2.getMesos.getImage.getDocker.getCredential.hasSecret)
    assert(proto2.getMesos.getImage.hasCached)
    assert(f.container2.forcePullImage == !proto2.getMesos.getImage.getCached)

    val proto3 = ContainerSerializer.toMesos(f.container3)
    assert(Mesos.ContainerInfo.Type.MESOS == proto3.getType)
    assert(proto3.hasMesos)
    assert(proto3.getMesos.hasImage)
    assert(proto3.getMesos.getImage.hasDocker)
    assert(proto3.getMesos.getImage.getDocker.hasName)
    assert("group/image" == proto3.getMesos.getImage.getDocker.getName)
    assert(proto3.getMesos.getImage.getDocker.hasCredential)
    assert(f.container3.credential.get.principal == proto3.getMesos.getImage.getDocker.getCredential.getPrincipal)
    assert(proto3.getMesos.getImage.getDocker.getCredential.hasSecret)
    assert(f.container3.credential.get.secret.get == proto3.getMesos.getImage.getDocker.getCredential.getSecret)
    assert(proto3.getMesos.getImage.hasCached)
    assert(f.container3.forcePullImage == !proto3.getMesos.getImage.getCached)
  }

  test("ConstructFromProto") {
    val f = fixture()

    val containerInfo = Protos.ExtendedContainerInfo.newBuilder
      .setType(Mesos.ContainerInfo.Type.MESOS)
      .addAllVolumes(f.volumes.map(VolumeSerializer.toProto).asJava)
      .setMesosDocker(MesosDockerSerializer.toProto(f.container1))
      .build

    val container1 = ContainerSerializer.fromProto(containerInfo)
    assert(container1 == f.container1)

    val containerInfo2 = Protos.ExtendedContainerInfo.newBuilder
      .setType(Mesos.ContainerInfo.Type.MESOS)
      .setMesosDocker(MesosDockerSerializer.toProto(f.container2))
      .build

    val container2 = ContainerSerializer.fromProto(containerInfo2)
    assert(container2 == f.container2)

    val containerInfo3 = Protos.ExtendedContainerInfo.newBuilder
      .setType(Mesos.ContainerInfo.Type.MESOS)
      .setMesosDocker(MesosDockerSerializer.toProto(f.container3))
      .build

    val container3 = ContainerSerializer.fromProto(containerInfo3)
    assert(container3 == f.container3)
  }

  test("SerializationRoundtrip simple Mesos Docker") {
    val container: Container = Container.MesosDocker(image = "group/image")
    JsonTestHelper.assertSerializationRoundtripWorks(container)
  }

  test("SerializationRoundtrip with slightly more complex Mesos Docker data") {
    val container: Container = fixture().container1
    JsonTestHelper.assertSerializationRoundtripWorks(container)
  }

  test("SerializationRoundTrip with full credential") {
    val container: Container = fixture().container3
    JsonTestHelper.assertSerializationRoundtripWorks(container)
  }

  private[this] def fromJson(json: String): Container = {
    Json.fromJson[Container](Json.parse(json)).get
  }

  test("Reading JSON with volumes") {
    val json3 =
      """
      {
        "type": "MESOS",
        "docker": {
          "image": "group/image"
        },
        "volumes": [
          {
            "containerPath": "/etc/a",
            "hostPath": "/var/data/a",
            "mode": "RO"
          },
          {
            "containerPath": "/etc/b",
            "hostPath": "/var/data/b",
            "mode": "RW"
          }
        ]
      }
      """

    val readResult3 = fromJson(json3)
    val f = fixture()
    assert (readResult3 == f.container1)
  }

  test("Reading JSON with full credential") {
    val json6 =
      """
      {
        "type": "MESOS",
        "docker": {
          "image": "group/image",
          "credential": {
            "principal": "aPrincipal",
            "secret": "aSecret"
          },
          "forcePullImage": true
        }
      }
      """

    val readResult6 = fromJson(json6)
    assert(readResult6 == fixture().container3)
  }
}
