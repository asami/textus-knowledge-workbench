package org.goldenport.cncf.http

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.goldenport.cncf.subsystem.Subsystem
import org.http4s.HttpApp
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.syntax.all.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Test-scope access to the existing native HTTP router; no product API added. */
object WorkbenchHttpFixture {
  def app(subsystem: Subsystem): HttpApp[IO] = {
    val server = new Http4sHttpServer(new HttpExecutionEngine(subsystem))
    WebSocketBuilder2[IO].map(builder => server.routes(builder).orNotFound).unsafeRunSync()
  }
}
