package org.ungs.core.observability.output.impl;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.routing.api.AlgorithmType;

public record GifRouteOutputObserver(Path outDir, int delayMs) implements SimulationObserver {

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    AlgorithmType algo = ctx.getCurrentAlgorithm();

    Path framesDir = outDir.resolve(algo.name()).resolve("outputs").resolve("frames");
    Path gifPath = outDir.resolve(algo.name()).resolve("outputs").resolve("route.gif");

    new Thread(
            () -> {
              try {
                createGifFromPngFolder(framesDir.toString(), gifPath.toString(), delayMs);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            },
            "gif-writer-" + algo.name())
        .start();
  }

  public static void createGifFromPngFolder(String folderPath, String outputGifPath, int delayMs)
      throws IOException {
    File dir = new File(folderPath);
    File[] pngFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
    if (pngFiles == null || pngFiles.length == 0) {
      throw new IllegalArgumentException("No PNG files found in folder: " + folderPath);
    }
    Arrays.sort(pngFiles); // Sort by filename

    try (FileOutputStream output = new FileOutputStream(outputGifPath)) {
      AnimatedGifEncoder encoder = new AnimatedGifEncoder();
      encoder.start(output);
      encoder.setDelay(delayMs); // delay in ms
      encoder.setRepeat(0); // 0 = infinite loop

      for (File png : pngFiles) {
        BufferedImage img = ImageIO.read(png);
        encoder.addFrame(img);
      }
      encoder.finish();
    }
  }
}
