package org.ungs.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class FileUtils {

  private static volatile String RESULTS_FILE_NAME;

  /**
   * Returns the current results folder or generates the next one if not set.
   *
   * @return the path to the results folder as a String
   */
  public static String getOrGenerateNextResultsFolder() {
    if (RESULTS_FILE_NAME != null) {
      return RESULTS_FILE_NAME;
    }
    try {
      RESULTS_FILE_NAME = generateNextResultsFolder();
      return RESULTS_FILE_NAME;
    } catch (Exception ex) {
      log.error("Error generating results folder", ex);
      return System.getProperty("user.dir");
    }
  }

  private static String generateNextResultsFolder() throws IOException {
    Path resultsDir = Paths.get(System.getProperty("user.dir"), "results");
    Files.createDirectories(resultsDir);

    int maxNumber = 0;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(resultsDir)) {
      for (Path entry : stream) {
        String name = entry.getFileName().toString();
        String[] parts = name.split("_", 2);
        if (parts.length > 0) {
          try {
            int num = Integer.parseInt(parts[0]);
            if (num > maxNumber) maxNumber = num;
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }

    String folderName = String.valueOf(maxNumber + 1);
    Path newFolder = resultsDir.resolve(folderName);
    Files.createDirectories(newFolder);
    return newFolder.toString();
  }
}
