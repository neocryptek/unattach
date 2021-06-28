package app.unattach.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class EmailTest {
  private Email getEmail(String from) {
    return new Email("id3", "uid42", null, from, "to@example.com", "subject",
        1501545600000L, 32141, Collections.singletonList("data.zip"));
  }

  @Test
  void getFromEmail() {
    assertEquals("", getEmail(null).getFromEmail());
    assertEquals("rok.strnisa@gmail.com", getEmail("rok.strnisa@gmail.com").getFromEmail());
    assertEquals("rok.strnisa@gmail.com", getEmail("Rok Strniša <rok.strnisa@gmail.com>").getFromEmail());
    assertEquals("rok.strnisa@gmail.com", getEmail("\"Rok Strniša\" <rok.strnisa@gmail.com>").getFromEmail());
  }

  @Test
  void getFromName() {
    assertEquals("", getEmail("rok.strnisa@gmail.com").getFromName());
    assertEquals("Rok Strniša", getEmail("Rok Strniša <rok.strnisa@gmail.com>").getFromName());
    assertEquals("Rok Strniša", getEmail("\"Rok Strniša\" <rok.strnisa@gmail.com>").getFromName());
  }
}