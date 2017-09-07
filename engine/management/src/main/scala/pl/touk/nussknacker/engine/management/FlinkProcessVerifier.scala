package pl.touk.nussknacker.engine.management

import java.io.File
import java.net.URL

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import pl.touk.nussknacker.engine.util.ReflectUtils.StaticMethodRunner

import scala.concurrent.Future
import scala.util.control.NonFatal

object FlinkProcessVerifier {
  def apply(config: Config, jarFile: File) = {
    new FlinkProcessVerifier(config, List(jarFile.toURI.toURL))
  }
}

class FlinkProcessVerifier(config: Config, jars: List[URL]) extends StaticMethodRunner(jars,
  "pl.touk.nussknacker.engine.process.runner.FlinkVerificationMain", "run") with LazyLogging {

  def verify(processJson: String, savepointPath: String): Future[Unit] = {
    try {
      logger.info("Starting to verify")
      tryToInvoke(processJson, config, savepointPath, jars)
      logger.info("Verification successful")
      Future.successful(())
    } catch {
      case NonFatal(e) =>
        logger.info("Failed to verify", e)
        Future.failed(
          new IllegalArgumentException("State is incompatible, please stop process and start again with clean state", e))
    }
  }
}
