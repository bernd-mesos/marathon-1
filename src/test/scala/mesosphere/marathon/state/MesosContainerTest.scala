package mesosphere.marathon.state

import mesosphere.marathon.MarathonSpec
import mesosphere.marathon.api.JsonTestHelper
import org.apache.mesos.{ Protos => Mesos }
import org.scalatest.Matchers
import play.api.libs.json.Json

import scala.collection.immutable.Seq

class MesosContainerTest extends MarathonSpec with Matchers {
  import mesosphere.marathon.api.v2.json.Formats._

  class Fixture {
    lazy val volumes = Seq(
      DockerVolume("/etc/a", "/var/data/a", Mesos.Volume.Mode.RO),
      DockerVolume("/etc/b", "/var/data/b", Mesos.Volume.Mode.RW)
    )

    lazy val mesosContainerWithPersistentVolume: Container = Container.Mesos(
      volumes = Seq[Volume](
        PersistentVolume(
          containerPath = "/local/container/",
          persistent = PersistentVolumeInfo(1024),
          mode = Mesos.Volume.Mode.RW
        )
      )
    )

    lazy val mesosContainerWithPersistentVolumeJsonStr =
      """
        |{
        |  "id": "test",
        |  "container": {
        |     "type": "MESOS",
        |     "volumes": [{
        |        "containerPath": "/local/container/",
        |        "mode": "RW",
        |        "persistent": {
        |           "size": 1024
        |        }
        |     }]
        |  }
        |}""".stripMargin

  }

  def fixture(): Fixture = new Fixture

  test("SerializationRoundtrip empty") {
    val container: Container = Container.Mesos()
    JsonTestHelper.assertSerializationRoundtripWorks(container)
  }

  test("""FromJSON with Mesos ContainerInfo should parse successfully""") {
    val f = new Fixture
    val appDef = Json.parse(f.mesosContainerWithPersistentVolumeJsonStr).as[AppDefinition]
    val expectedContainer = f.mesosContainerWithPersistentVolume

    appDef.container should equal(Some(expectedContainer))
  }

  test("""ToJson should correctly handle container type MESOS""") {
    val f = new Fixture
    val appDefinition = AppDefinition(id = PathId("test"), container = Some(f.mesosContainerWithPersistentVolume))
    val json = Json.toJson(appDefinition)
    (json \ "container" \ "type").asOpt[String] should be(Some("MESOS"))
  }

}
