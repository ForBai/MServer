package de.mediathekview.mserver.crawler;

import de.mediathekview.mlib.filmlisten.FilmlistFormats;
import de.mediathekview.mlib.messages.Message;
import de.mediathekview.mlib.messages.MessageTypes;
import de.mediathekview.mlib.messages.MessageUtil;
import de.mediathekview.mlib.messages.listener.MessageListener;
import de.mediathekview.mserver.testhelper.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

@RunWith(Parameterized.class)
public class CrawlerManagerTest implements MessageListener {

  private static final Logger LOG = LogManager.getLogger(CrawlerManagerTest.class);
  private static final String TEMP_FOLDER_NAME_PATTERN = "MSERVER_TEST_%d";
  private static final CrawlerManager CRAWLER_MANAGER = CrawlerManager.getInstance();
  private static Path testFileFolderPath;

  private final String filmlistPath;
  private final FilmlistFormats format;

  public CrawlerManagerTest(final String aFilmlistPath, final FilmlistFormats aFormat) {
    filmlistPath = aFilmlistPath;
    format = aFormat;
  }

  @Parameterized.Parameters(name = "Test {index} Filmlist for {0} with {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"filmlists/TestFilmlistNewJson.json", FilmlistFormats.JSON},
          {"filmlists/TestFilmlistNewJson.json.xz", FilmlistFormats.JSON_COMPRESSED_XZ},
          {"filmlists/TestFilmlistNewJson.json.bz", FilmlistFormats.JSON_COMPRESSED_BZIP},
          {"filmlists/TestFilmlistNewJson.json.gz", FilmlistFormats.JSON_COMPRESSED_GZIP},
          {"filmlists/TestFilmlist.json", FilmlistFormats.OLD_JSON},
          {"filmlists/TestFilmlist.json.xz", FilmlistFormats.OLD_JSON_COMPRESSED_XZ},
          {"filmlists/TestFilmlist.json.bz", FilmlistFormats.OLD_JSON_COMPRESSED_BZIP},
          {"filmlists/TestFilmlist.json.gz", FilmlistFormats.OLD_JSON_COMPRESSED_GZIP}
        });
  }

  @AfterClass
  public static void deleteTempFiles() throws IOException {
    Files.walk(testFileFolderPath)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @BeforeClass
  public static void initTestData() throws IOException {
    testFileFolderPath = Files.createTempDirectory(formatWithDate(TEMP_FOLDER_NAME_PATTERN));
    Files.createDirectory(testFileFolderPath.resolve("filmlists"));
  }

  private static String formatWithDate(final String aPattern) {
    return String.format(aPattern, new Date().getTime());
  }

  @Override
  public void consumeMessage(final Message aMessage, final Object... aParameters) {
    if (MessageTypes.FATAL_ERROR.equals(aMessage.getMessageType())) {
      Assert.fail(String.format(MessageUtil.getInstance().loadMessageText(aMessage), aParameters));
    } else {
      LOG.info(
          String.format(
              "%s: %s",
              aMessage.getMessageType().name(),
              String.format(MessageUtil.getInstance().loadMessageText(aMessage), aParameters)));
    }
  }

  @Test
  public void testSaveAndImport() {
    final Path filmListFilePath = FileReader.getPath(filmlistPath);
    synchronized (CRAWLER_MANAGER) {
      CRAWLER_MANAGER.addMessageListener(this);
      CRAWLER_MANAGER.importFilmlist(format, filmListFilePath.toAbsolutePath().toString());
      CRAWLER_MANAGER.saveFilmlist(testFileFolderPath.resolve(filmlistPath), format);
    }
  }
}
