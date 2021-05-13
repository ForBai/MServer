package mServer.crawler.sender.phoenix;

import de.mediathekview.mlib.Const;
import de.mediathekview.mlib.daten.DatenFilm;
import de.mediathekview.mlib.tool.Log;
import mServer.crawler.FilmeSuchen;
import mServer.crawler.sender.MediathekCrawler;
import mServer.crawler.sender.base.CrawlerUrlDTO;
import mServer.crawler.sender.phoenix.tasks.PhoenixFilmDetailTask;
import mServer.crawler.sender.phoenix.tasks.PhoenixOverviewTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class PhoenixCrawler extends MediathekCrawler {

  private static final Logger LOG = LogManager.getLogger(PhoenixCrawler.class);

  public static final String SENDERNAME = Const.PHOENIX;

  public PhoenixCrawler(FilmeSuchen ssearch, int startPrio) {
    super(ssearch, SENDERNAME, 0, 1, startPrio);
  }

  @Override
  protected RecursiveTask<Set<DatenFilm>> createCrawlerTask() {
    final ConcurrentLinkedQueue<CrawlerUrlDTO> shows = new ConcurrentLinkedQueue<>();

    try {
      shows.addAll(getShows());

      Log.sysLog("PHÖNIX Anzahl: " + shows.size());

      meldungAddMax(shows.size());

      return new PhoenixFilmDetailTask(this, shows, Optional.empty(), PhoenixConstants.URL_BASE);
    } catch (ExecutionException | InterruptedException ex) {
      LOG.fatal("Exception in Phönix crawler.", ex);
    }

    return null;
  }

  private Collection<CrawlerUrlDTO> getShows() throws ExecutionException, InterruptedException {
    // load sendungen page
    CrawlerUrlDTO url = new CrawlerUrlDTO(PhoenixConstants.URL_BASE + PhoenixConstants.URL_OVERVIEW_JSON);

    final ConcurrentLinkedQueue<CrawlerUrlDTO> queue = new ConcurrentLinkedQueue<>();
    queue.add(url);

    final Set<CrawlerUrlDTO> overviewUrls = loadOverviewPages(queue);

    // load sendung overview pages
    final ConcurrentLinkedQueue<CrawlerUrlDTO> queue1 = new ConcurrentLinkedQueue<>();
    queue1.addAll(overviewUrls);
    final Set<CrawlerUrlDTO> filmUrls = loadOverviewPages(queue1);

    return filmUrls;
  }

  private Set<CrawlerUrlDTO> loadOverviewPages(final ConcurrentLinkedQueue<CrawlerUrlDTO> aQueue)
    throws ExecutionException, InterruptedException {
    PhoenixOverviewTask overviewTask = new PhoenixOverviewTask(this, aQueue, Optional.empty(), PhoenixConstants.URL_BASE);
    final Set<CrawlerUrlDTO> urls = forkJoinPool.submit(overviewTask).get();

    return urls;
  }
}
