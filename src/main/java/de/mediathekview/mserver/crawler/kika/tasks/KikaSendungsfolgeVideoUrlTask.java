package de.mediathekview.mserver.crawler.kika.tasks;

import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractDocumentTask;
import de.mediathekview.mserver.crawler.basic.AbstractUrlTask;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KikaSendungsfolgeVideoUrlTask
    extends AbstractDocumentTask<CrawlerUrlDTO, CrawlerUrlDTO> {
  private static final String URL_TEMPLATE = "https://www.kika.de%s";
  private static final String HTTP = "http";
  private static final String ATTRIBUTE_ONCLICK = "onclick";
  private static final long serialVersionUID = -2633978090540666539L;
  private static final String VIDEO_DATA_ELEMENT_SELECTOR =
      ".sectionArticle .av-playerContainer a[onclick]";
  private static final String VIDEO_URL_REGEX_PATTERN = "(?<=dataURL:')[^']*";

  public KikaSendungsfolgeVideoUrlTask(
      final AbstractCrawler aCrawler,
      final Queue<CrawlerUrlDTO> aUrlToCrawlDTOs,
      final JsoupConnection jsoupConnection) {
    super(aCrawler, aUrlToCrawlDTOs, jsoupConnection);
  }

  private String toKikaUrl(final String aUrl) {
    final String kikaUrl;
    if (aUrl.contains(HTTP)) {
      kikaUrl = aUrl;
    } else {
      kikaUrl = String.format(URL_TEMPLATE, aUrl);
    }
    return kikaUrl;
  }

  @Override
  protected AbstractUrlTask<CrawlerUrlDTO, CrawlerUrlDTO> createNewOwnInstance(
      final Queue<CrawlerUrlDTO> aURLsToCrawl) {
    return new KikaSendungsfolgeVideoUrlTask(crawler, aURLsToCrawl, getJsoupConnection());
  }

  @Override
  protected void processDocument(final CrawlerUrlDTO aUrlDTO, final Document aDocument) {
    final Elements videoElements = aDocument.select(VIDEO_DATA_ELEMENT_SELECTOR);
    for (final Element videoDataElement : videoElements) {
      if (videoDataElement.hasAttr(ATTRIBUTE_ONCLICK)) {
        final String rawVideoData = videoDataElement.attr(ATTRIBUTE_ONCLICK);
        final Matcher videoUrlMatcher =
            Pattern.compile(VIDEO_URL_REGEX_PATTERN).matcher(rawVideoData);
        if (videoUrlMatcher.find()) {
          taskResults.add(new CrawlerUrlDTO(toKikaUrl(videoUrlMatcher.group())));

        } else {
          crawler.printMissingElementErrorMessage("data url");
        }
      } else {
        crawler.printMissingElementErrorMessage(ATTRIBUTE_ONCLICK);
      }
    }
  }
}
