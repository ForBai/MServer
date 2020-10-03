package de.mediathekview.mserver.crawler.basic;

import de.mediathekview.mserver.base.messages.ServerMessages;
import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * This is a abstract task based on {@link AbstractUrlTask} which takes a {@link Queue} of {@link D}
 * and loads the URL with JSOUP as {@link Document}.
 *
 * @author Nicklas Wiegandt (Nicklas2751)<br>
 *     <b>Mail:</b> nicklas@wiegandt.eu<br>
 * @param <T> The type of objects which will be created from this task.
 * @param <D> A sub type of {@link CrawlerUrlDTO} which this task will use to create the result
 *     objects.
 */
public abstract class AbstractDocumentTask<T, D extends CrawlerUrlDTO>
    extends AbstractUrlTask<T, D> {
  private static final long serialVersionUID = -4124779055395250981L;
  private static final Logger LOG = LogManager.getLogger(AbstractDocumentTask.class);
  private static final String LOAD_DOCUMENT_ERRORTEXTPATTERN =
      "Something terrible happened while crawl the %s page \"%s\".";
  private static final String LOAD_DOCUMENT_HTTPERROR =
      "Some HTTP error happened while crawl the %s page \"%s\": %d.";
  private boolean incrementErrorCounterOnHttpErrors;
  private boolean printErrorMessage;
  private Level httpErrorLogLevel;

  JsoupConnection jsoupConnection;

  public AbstractDocumentTask(
      final AbstractCrawler aCrawler,
      final Queue<D> urlToCrawlDTOs,
      final JsoupConnection jsoupConnection) {
    super(aCrawler, urlToCrawlDTOs);
    incrementErrorCounterOnHttpErrors = true;
    printErrorMessage = true;
    httpErrorLogLevel = Level.ERROR;
    this.jsoupConnection = jsoupConnection;
  }

  /**
   * In this method you have to use the JSOUP {@link Document} to create a object of the return type
   * {@link T}. Add the results to {@link AbstractUrlTask#taskResults}.
   *
   * @param aUrlDTO A DTO containing at least the URL of the given document.
   * @param aDocument The JSOUP {@link Document}.
   */
  protected abstract void processDocument(final D aUrlDTO, final Document aDocument);

  @Override
  protected void processElement(final D urlDTO) {
    try {
      // maxBodySize(0)=unlimited
      // necessary for ORF documents which are larger than the default size
      final Document document =
          jsoupConnection
              .getConnection(urlDTO.getUrl())
              .timeout((int) TimeUnit.SECONDS.toMillis(config.getSocketTimeoutInSeconds()))
              .maxBodySize(0)
              .get();
      processDocument(urlDTO, document);
    } catch (final HttpStatusException httpStatusError) {
      LOG.log(
          httpErrorLogLevel,
          String.format(
              LOAD_DOCUMENT_HTTPERROR,
              crawler.getSender().getName(),
              urlDTO.getUrl(),
              httpStatusError.getStatusCode()));
      if (printErrorMessage) {
        crawler.printMessage(
            ServerMessages.CRAWLER_DOCUMENT_LOAD_ERROR,
            crawler.getSender().getName(),
            urlDTO.getUrl(),
            httpStatusError.getStatusCode());
      }
      if (incrementErrorCounterOnHttpErrors) {
        crawler.incrementAndGetErrorCount();
      }
    } catch (final IOException ioException) {
      LOG.fatal(
          String.format(
              LOAD_DOCUMENT_ERRORTEXTPATTERN, crawler.getSender().getName(), urlDTO.getUrl()),
          ioException);
      if (incrementErrorCounterOnHttpErrors) {
        crawler.incrementAndGetErrorCount();
      }
      if (printErrorMessage) {
        crawler.printErrorMessage();
      }
    }
  }

  protected void setHttpErrorLogLevel(final Level aHttpErrorLogLevel) {
    httpErrorLogLevel = aHttpErrorLogLevel;
  }

  protected void setIncrementErrorCounterOnHttpErrors(
      final boolean aIncrementErrorCounterOnHttpErrors) {
    incrementErrorCounterOnHttpErrors = aIncrementErrorCounterOnHttpErrors;
  }

  protected void setPrintErrorMessage(final boolean aPrintErrorMessage) {
    printErrorMessage = aPrintErrorMessage;
  }

  public JsoupConnection getJsoupConnection() {
    return jsoupConnection;
  }
}
