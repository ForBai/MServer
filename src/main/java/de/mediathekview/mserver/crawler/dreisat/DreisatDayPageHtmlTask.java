package de.mediathekview.mserver.crawler.dreisat;

import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractDocumentTask;
import de.mediathekview.mserver.crawler.basic.AbstractRecursiveConverterTask;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import org.jsoup.nodes.Document;

import java.util.Queue;

public class DreisatDayPageHtmlTask extends AbstractDocumentTask<CrawlerUrlDTO, CrawlerUrlDTO> {

  private final transient DreisatDayPageHtmlDeserializer deserializer;
  private final String apiUrlBase;

  public DreisatDayPageHtmlTask(
      final String apiUrlBase,
      final AbstractCrawler crawler,
      final Queue<CrawlerUrlDTO> urlToCrawlDTOs,
      final JsoupConnection jsoupConnection) {
    super(crawler, urlToCrawlDTOs, jsoupConnection);
    this.apiUrlBase = apiUrlBase;
    deserializer = new DreisatDayPageHtmlDeserializer(apiUrlBase);
  }

  @Override
  protected void processDocument(final CrawlerUrlDTO aUrlDTO, final Document aDocument) {
    taskResults.addAll(deserializer.deserialize(aDocument));
  }

  @Override
  protected AbstractRecursiveConverterTask<CrawlerUrlDTO, CrawlerUrlDTO> createNewOwnInstance(
      final Queue<CrawlerUrlDTO> aElementsToProcess) {
    return new DreisatDayPageHtmlTask(
        apiUrlBase, crawler, aElementsToProcess, getJsoupConnection());
  }
}
