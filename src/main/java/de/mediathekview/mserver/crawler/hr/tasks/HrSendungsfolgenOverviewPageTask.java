package de.mediathekview.mserver.crawler.hr.tasks;

import de.mediathekview.mserver.base.HtmlConsts;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractDocumentTask;
import de.mediathekview.mserver.crawler.basic.AbstractUrlTask;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import de.mediathekview.mserver.crawler.hr.HrConstants;
import org.apache.logging.log4j.Level;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Queue;

public class HrSendungsfolgenOverviewPageTask
    extends AbstractDocumentTask<CrawlerUrlDTO, CrawlerUrlDTO> {

  private static final String SENDUNGSFOLGEN_URL_SELECTOR =
      ".c-teaser__headlineLink.link.js-loadScript";
  private static final String SENDUNGSFOLGEN_URL_SELECTOR_HESSENSCHAU =
      ".c-clusterTeaser:lt(1) .c-clusterTeaser__link.link";
  private static final long serialVersionUID = -6727831751148817578L;

  public HrSendungsfolgenOverviewPageTask(
      final AbstractCrawler aCrawler,
      final Queue<CrawlerUrlDTO> urlToCrawlDTOs) {
    super(aCrawler, urlToCrawlDTOs);
    // Some HR entries for "Programm" don't have a "sendungen" sub page which will be tried to load
    // because this sub page usually contains the "Sendungsfolgen".
    setIncrementErrorCounterOnHttpErrors(false);
    setHttpErrorLogLevel(Level.DEBUG);
    setPrintErrorMessage(false);
  }

  @Override
  protected AbstractUrlTask<CrawlerUrlDTO, CrawlerUrlDTO> createNewOwnInstance(
      final Queue<CrawlerUrlDTO> aURLsToCrawl) {
    return new HrSendungsfolgenOverviewPageTask(crawler, aURLsToCrawl);
  }

  protected String[] getSendungsfoleUrlSelector() {
    return new String[] {SENDUNGSFOLGEN_URL_SELECTOR, SENDUNGSFOLGEN_URL_SELECTOR_HESSENSCHAU};
  }

  @Override
  protected void processDocument(final CrawlerUrlDTO aUrlDTO, final Document aDocument) {
    for (final String selector : getSendungsfoleUrlSelector()) {
      for (final Element filmUrlElement : aDocument.select(selector)) {
        if (filmUrlElement.hasAttr(HtmlConsts.ATTRIBUTE_HREF)) {
          crawler.incrementAndGetMaxCount();
          crawler.updateProgress();

          final String url = filmUrlElement.absUrl(HtmlConsts.ATTRIBUTE_HREF);
          if (isUrlRelevant(url)) {
            taskResults.add(new CrawlerUrlDTO(url));
          }
        }
      }
    }
  }

  /**
   * filters urls of other ARD stations.
   *
   * @param aUrl the url to check
   * @return true if the url is a HR url else false
   */
  protected boolean isUrlRelevant(final String aUrl) {
    return aUrl.contains(HrConstants.BASE_URL) || aUrl.contains(HrConstants.BASE_URL_HESSENSCHAU);
  }
}
