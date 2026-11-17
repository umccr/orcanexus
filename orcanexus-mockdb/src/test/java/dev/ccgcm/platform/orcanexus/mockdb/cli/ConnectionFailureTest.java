package dev.ccgcm.platform.orcanexus.mockdb.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.mockdb.db.Connections;
import dev.ccgcm.platform.orcanexus.mockdb.testsupport.RecordingOutput;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Needs no database: a closed local port. Validates: Requirement 4.10. */
class ConnectionFailureTest {

  @Test
  void closedPortGivesExitOneNamingHostPortAndComposeHint() throws IOException {
    int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    var out = new RecordingOutput();

    int rc =
        new MockdbCli(new RealCommandActions())
            .run(
                new String[] {"schema"},
                Map.of("MOCKDB_HOST", "127.0.0.1", "MOCKDB_PORT", String.valueOf(port)),
                out,
                new Connections());

    assertThat(rc).isEqualTo(1);
    assertThat(out.allErr())
        .contains("127.0.0.1:" + port)
        .contains("Compose_Stack")
        .contains("nexus_docker_up");
  }
}
