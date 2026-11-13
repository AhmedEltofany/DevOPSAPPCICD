package com.example.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class BuildInfoTest {

  @Test
  void mavenFilteringFillsInTheRealVersion() {
    // Reads the filtered file from target/classes - proves resource filtering works.
    BuildInfo info = BuildInfo.load();

    assertEquals("devopsappcicd", info.appName());
    assertNotEquals(BuildInfo.UNKNOWN, info.version());
    assertFalse(info.version().contains("${"));
    assertNotEquals(BuildInfo.UNKNOWN, info.buildTime());
  }

  @Test
  void missingOrUnfilteredValuesBecomeUnknown() {
    Properties properties = new Properties();
    properties.setProperty("app.version", "${project.version}");
    properties.setProperty("build.commit", "   ");
    BuildInfo info = new BuildInfo(properties);

    assertEquals(BuildInfo.UNKNOWN, info.version());
    assertEquals(BuildInfo.UNKNOWN, info.commit());
    assertEquals(BuildInfo.UNKNOWN, info.appName());
  }
}
