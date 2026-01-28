package org.ungs.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileUtils {

  public static final String RESULTS_FILE_NAME;

  static {
    try {
      RESULTS_FILE_NAME = FileUtils.getNextResultsFolder();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getNextResultsFolder() throws IOException {
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

    String folderName = String.valueOf((maxNumber + 1));
    Path newFolder = resultsDir.resolve(folderName);
    Files.createDirectories(newFolder);
    return newFolder.toString();
  }
}
