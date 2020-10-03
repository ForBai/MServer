package de.mediathekview.mserver.crawler.hr.tasks;

import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;

import java.util.Queue;

public class HrSendungsfolgenVerpasstOverviewPageTask extends HrSendungsfolgenOverviewPageTask {

  private static final String SENDUNGSFOLGEN_URL_SELECTOR = "a.c-epgBroadcast__programLink";
  private static final long serialVersionUID = 550079618104128843L;

  public HrSendungsfolgenVerpasstOverviewPageTask(
      final AbstractCrawler aCrawler,
      final Queue<CrawlerUrlDTO> urlToCrawlDTOs,
      final JsoupConnection jsoupConnection) {
    super(aCrawler, urlToCrawlDTOs, jsoupConnection);
  }

  @Override
  protected String[] getSendungsfoleUrlSelector() {
    return new String[] {SENDUNGSFOLGEN_URL_SELECTOR};
  }

  @Override
  protected boolean isUrlRelevant(final String aUrl) {
    // filter urls containing overview pages because they are handled by HrSendungenOverviewPageTask
    return super.isUrlRelevant(aUrl)
        && !aUrl.contains("index.html")
        && !aUrl.contains("hs-kompakt-100.html");
  }
}
