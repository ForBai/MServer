package de.mediathekview.mserver.crawler.dw.tasks;

import de.mediathekview.mserver.base.HtmlConsts;
import de.mediathekview.mserver.base.utils.UrlUtils;
import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractDocumentTask;
import de.mediathekview.mserver.crawler.basic.AbstractUrlTask;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import de.mediathekview.mserver.crawler.dw.DwCrawler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DWUebersichtTask extends AbstractDocumentTask<URL, CrawlerUrlDTO> {
  private static final String PARAMETER_RESULTS = "results";
  private static final String PARAMETER_VALUE_SORT = "date";
  private static final String PARAMETER_VALUE_TYPE = "18";
  private static final String PARAMETER_VALUE_FILTER = "";
  private static final String PARAMETER_SORT = "sort";
  private static final String PARAMETER_TYPE = "type";
  private static final String PARAMETER_FILTER = "filter";
  private static final String ADD_PAGE_NUMBER = ".addPage .number";
  private static final long serialVersionUID = 2080583393530906001L;
  private static final Logger LOG = LogManager.getLogger(DWUebersichtTask.class);
  private static final String SENDUNG_LINK_SELEKTOR = ".mcProgramsTeaser .smallList li:eq(1) a";
  private static final String RESULTS_COUNT_REGEX_PATTERN = "(?<=results=)\\d+";

  public DWUebersichtTask(
      final AbstractCrawler aCrawler,
      final Queue<CrawlerUrlDTO> urlToCrawlDTOs,
      final JsoupConnection jsoupConnection) {
    super(aCrawler, urlToCrawlDTOs, jsoupConnection);
  }

  private String addBaseParameters(final String aUrl) {
    String newUrl;
    newUrl = UrlUtils.changeOrAddParameter(aUrl, PARAMETER_FILTER, PARAMETER_VALUE_FILTER);
    newUrl = UrlUtils.changeOrAddParameter(newUrl, PARAMETER_TYPE, PARAMETER_VALUE_TYPE);
    newUrl = UrlUtils.changeOrAddParameter(newUrl, PARAMETER_SORT, PARAMETER_VALUE_SORT);
    return newUrl;
  }

  private Optional<AbstractUrlTask<URL, CrawlerUrlDTO>> createNextPageCrawler(
      final CrawlerUrlDTO aUrlDTO, final Elements aFoundNextSiteLink) {
    final Optional<Integer> resultsCount = gatherResultsCount(aUrlDTO);
    final Optional<Integer> nextPageCount = getNextSiteCount(aFoundNextSiteLink);

    final Queue<CrawlerUrlDTO> nextPageUrls = new ConcurrentLinkedQueue<>();
    if (resultsCount.isPresent() && nextPageCount.isPresent()) {
      nextPageUrls.offer(
          new CrawlerUrlDTO(
              UrlUtils.changeOrAddParameter(
                  addBaseParameters(aUrlDTO.getUrl()),
                  PARAMETER_RESULTS,
                  Integer.toString(resultsCount.get() + nextPageCount.get()))));
      return Optional.of(createNewOwnInstance(nextPageUrls));
    }
    return Optional.empty();
  }

  private Optional<Integer> gatherResultsCount(final CrawlerUrlDTO aUrlDTO) {
    final Matcher resultsRegexMatcher =
        Pattern.compile(RESULTS_COUNT_REGEX_PATTERN).matcher(aUrlDTO.getUrl());
    if (resultsRegexMatcher.find()) {
      try {
        return Optional.of(Integer.parseInt(resultsRegexMatcher.group()));
      } catch (final NumberFormatException numberFormatException) {
        LOG.error(
            "Something went wrong on gathering the results count for DW.", numberFormatException);
      }
    }
    return Optional.empty();
  }

  private Optional<Integer> getNextSiteCount(final Elements aFoundNextSiteLink) {
    try {
      return Optional.of(Integer.parseInt(aFoundNextSiteLink.text()));
    } catch (final NumberFormatException numberFormatException) {
      LOG.error(
          "Something went wrong on gathering the next page results count for DW.",
          numberFormatException);
    }
    return Optional.empty();
  }

  @Override
  protected AbstractUrlTask<URL, CrawlerUrlDTO> createNewOwnInstance(
      final Queue<CrawlerUrlDTO> aURLsToCrawl) {
    return new DWUebersichtTask(crawler, aURLsToCrawl, getJsoupConnection());
  }

  @Override
  protected void processDocument(final CrawlerUrlDTO aUrlDTO, final Document aDocument) {
    try {
      final Elements foundLinks = aDocument.select(SENDUNG_LINK_SELEKTOR);
      if (foundLinks.isEmpty()) {
        foundLinks.addAll(aDocument.select(".searchres a"));
      }
      final Elements foundNextSiteLink = aDocument.select(ADD_PAGE_NUMBER);

      final Optional<AbstractUrlTask<URL, CrawlerUrlDTO>> nextPageTask;
      if (foundNextSiteLink.isEmpty()) {
        nextPageTask = Optional.empty();
      } else {
        nextPageTask = createNextPageCrawler(aUrlDTO, foundNextSiteLink);
        nextPageTask.ifPresent(ForkJoinTask::fork);
      }

      for (final Element link : foundLinks) {
        if (link.hasAttr(HtmlConsts.ATTRIBUTE_HREF)) {
          taskResults.add(new URL(DwCrawler.BASE_URL + link.attr(HtmlConsts.ATTRIBUTE_HREF)));
          crawler.incrementAndGetMaxCount();
          crawler.updateProgress();
        }
      }

      nextPageTask.ifPresent(
          urlCrawlerUrlDTOAbstractUrlTask ->
              taskResults.addAll(urlCrawlerUrlDTOAbstractUrlTask.join()));
    } catch (final IOException ioException) {
      LOG.fatal(
          "Something went terrible wrong on getting the Sendung Verpasst for DW.", ioException);
    }
  }
}
