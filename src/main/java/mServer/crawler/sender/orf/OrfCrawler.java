package mServer.crawler.sender.orf;

import de.mediathekview.mlib.Config;
import de.mediathekview.mlib.Const;
import de.mediathekview.mlib.daten.DatenFilm;
import de.mediathekview.mlib.tool.Log;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import mServer.crawler.FilmeSuchen;
import mServer.crawler.sender.MediathekReader;
import mServer.crawler.sender.orf.tasks.OrfDayTask;
import mServer.crawler.sender.orf.tasks.OrfFilmDetailTask;
import mServer.crawler.sender.orf.tasks.OrfLetterPageTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OrfCrawler extends MediathekReader {

  public static final String SENDERNAME = Const.ORF;
  private static final Logger LOG = LogManager.getLogger(OrfCrawler.class);

  private final ForkJoinPool forkJoinPool;

  public OrfCrawler(FilmeSuchen ssearch, int startPrio) {
    super(ssearch, SENDERNAME, 0, 1, startPrio);

    forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 4);
  }

  @Override
  protected void addToList() {
    meldungStart();

    try {
      RecursiveTask<Set<DatenFilm>> filmTask = createCrawlerTask();
      Set<DatenFilm> films = forkJoinPool.invoke(filmTask);

      LOG.info("ORF Filme einsortieren...");

      films.forEach(film -> {
        if (!Config.getStop()) {
          addFilm(film);
        }
      });

      LOG.info("ORF Film einsortieren fertig");
    } finally {
      //explicitely shutdown the pool
      shutdownAndAwaitTermination(forkJoinPool, 60, TimeUnit.SECONDS);
    }

    LOG.info("ORF fertig");

    meldungThreadUndFertig();
  }

  void shutdownAndAwaitTermination(ExecutorService pool, long delay, TimeUnit delayUnit) {
    pool.shutdown();
    try {
      if (!pool.awaitTermination(delay, delayUnit)) {
        pool.shutdownNow();
        if (!pool.awaitTermination(delay, delayUnit)) {
          LOG.info("Pool nicht beendet");
        }
      }
    } catch (InterruptedException ie) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private Set<TopicUrlDTO> getDaysEntries() throws InterruptedException, ExecutionException {
    final OrfDayTask dayTask = new OrfDayTask(this, getDayUrls());
    final Set<TopicUrlDTO> shows = forkJoinPool.submit(dayTask).get();

    Log.sysLog("Anzahl Sendungen " + getSendername() + ": " + shows.size());

    return shows;
  }

  private ConcurrentLinkedQueue<CrawlerUrlDTO> getDayUrls() {
    final int maximumDaysForSendungVerpasstSection = 8;
    final int maximumDaysForSendungVerpasstSectionFuture = 0;

    final ConcurrentLinkedQueue<CrawlerUrlDTO> urls = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < maximumDaysForSendungVerpasstSection
            + maximumDaysForSendungVerpasstSectionFuture; i++) {
      urls.add(new CrawlerUrlDTO(OrfConstants.URL_DAY + LocalDateTime.now()
              .plus(maximumDaysForSendungVerpasstSectionFuture, ChronoUnit.DAYS)
              .minus(i, ChronoUnit.DAYS).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
    }

    return urls;
  }

  private ConcurrentLinkedQueue<TopicUrlDTO> getLetterEntries() throws InterruptedException, ExecutionException {
    final OrfLetterPageTask letterTask = new OrfLetterPageTask();
    final ConcurrentLinkedQueue<TopicUrlDTO> shows = forkJoinPool.submit(letterTask).get();

    Log.sysLog("Anzahl Sendungen " + getSendername() + ": " + shows.size());

    return shows;
  }

  protected RecursiveTask<Set<DatenFilm>> createCrawlerTask() {

    final ConcurrentLinkedQueue<TopicUrlDTO> shows = new ConcurrentLinkedQueue<>();
    try {

      //shows.addAll(getLetterEntries());
      getDaysEntries().forEach(show -> {
        if (!shows.contains(show)) {
          shows.add(show);
        }
      });

    } catch (InterruptedException | ExecutionException exception) {
      LOG.fatal("Something wen't terrible wrong on gathering the films", exception);
    }
    LOG.debug("ORF Anzahl: " + shows.size());

    meldungAddMax(shows.size());

    return new OrfFilmDetailTask(this, shows);
  }

}
