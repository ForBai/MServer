package de.mediathekview.mserver.crawler.wdr.tasks;

import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractDocumentTask;
import de.mediathekview.mserver.crawler.basic.AbstractUrlTask;
import de.mediathekview.mserver.crawler.basic.TopicUrlDTO;
import de.mediathekview.mserver.crawler.wdr.WdrTopicUrlDTO;
import de.mediathekview.mserver.crawler.wdr.parser.WdrTopicOverviewDeserializer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

public class WdrTopicOverviewTask extends AbstractDocumentTask<TopicUrlDTO, WdrTopicUrlDTO> {

  private static final Logger LOG = LogManager.getLogger(WdrTopicOverviewTask.class);  
  
  private final int recursiveCount;
  
  public WdrTopicOverviewTask(AbstractCrawler aCrawler, 
    ConcurrentLinkedQueue<WdrTopicUrlDTO> aUrlToCrawlDTOs,
    int aRecursiveCount
  ) {
    super(aCrawler, aUrlToCrawlDTOs);
    
    recursiveCount = aRecursiveCount;
  }
  
  @Override
  protected void processDocument(WdrTopicUrlDTO aUrlDTO, Document aDocument) {
    if (aUrlDTO.isFileUrl()) {
      taskResults.add(new TopicUrlDTO(aUrlDTO.getTheme(), aUrlDTO.getUrl()));
      return;
    } 
    
    final ConcurrentLinkedQueue<WdrTopicUrlDTO> subpages = new ConcurrentLinkedQueue<>();
    
    WdrTopicOverviewDeserializer deserializer = new WdrTopicOverviewDeserializer();
    List<WdrTopicUrlDTO> dtos = deserializer.deserialize(aUrlDTO.getTheme(), aDocument);
    dtos.forEach(dto -> {
      if (dto.isFileUrl()) {
        taskResults.add(new TopicUrlDTO(dto.getTheme(), dto.getUrl()));
      } else {
        subpages.add(dto);
      }
    });
    
    if (crawler.getCrawlerConfig().getMaximumSubpages() > recursiveCount) {
      taskResults.addAll(createNewOwnInstance(subpages).invoke());
    }
  }

  @Override
  protected AbstractUrlTask<TopicUrlDTO, WdrTopicUrlDTO> createNewOwnInstance(ConcurrentLinkedQueue<WdrTopicUrlDTO> aURLsToCrawl) {
    return new WdrTopicOverviewTask(crawler, aURLsToCrawl, recursiveCount + 1);
  }
}
