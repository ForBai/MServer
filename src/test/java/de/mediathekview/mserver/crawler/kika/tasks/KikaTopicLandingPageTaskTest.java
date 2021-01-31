package de.mediathekview.mserver.crawler.kika.tasks;

import de.mediathekview.mserver.base.webaccess.JsoupConnection;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import de.mediathekview.mserver.crawler.kika.KikaConstants;
import de.mediathekview.mserver.testhelper.JsoupMock;
import org.jsoup.Connection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class KikaTopicLandingPageTaskTest extends KikaTaskTestBase {

  public KikaTopicLandingPageTaskTest(
      final String aRequestUrl, final String aHtmlFile, final CrawlerUrlDTO[] aExpectedUrls) {
    requestUrl = aRequestUrl;
    htmlFile = aHtmlFile;
    expectedUrls = aExpectedUrls;
  }

  private final String requestUrl;
  private final String htmlFile;
  private final CrawlerUrlDTO[] expectedUrls;

  @Mock JsoupConnection jsoupConnection;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "https://www.kika.de/mama-fuchs-und-papa-dachs/sendereihe2694.html",
            "/kika/kika_topic1_landing_page.html",
            new CrawlerUrlDTO[] {
              new CrawlerUrlDTO(
                  "https://www.kika.de/mama-fuchs-und-papa-dachs/buendelgruppe2670.html")
            }
          },
          {
            "https://www.kika.de/singalarm/sendungen/sendung105928.html",
            "/kika/kika_topic2_landing_page.html",
            new CrawlerUrlDTO[] {
              new CrawlerUrlDTO(
                  "https://www.kika.de/singalarm/sendungen/buendelgruppe2234_page-2_zc-d5c4767c_zs-e540764b.html"),
                new CrawlerUrlDTO("https://www.kika.de/singalarm/sendungen/videos-singalarm-100.html")
            }
          },
          {
            "https://www.kika.de/alles-neu-fuer-lina/sendereihe2648.html",
            "/kika/kika_topic3_landing_page.html",
            new CrawlerUrlDTO[] {
              new CrawlerUrlDTO("https://www.kika.de/alles-neu-fuer-lina/buendelgruppe2624.html")
            }
          },
          {
            "https://www.kika.de/sendungen/special/s/schnitzeljagd/uebersicht-116.html",
            "/kika/kika_topic4_is_overview_page_without_all_button.html",
            new CrawlerUrlDTO[] {
              new CrawlerUrlDTO(
                  "https://www.kika.de/sendungen/special/s/schnitzeljagd/uebersicht-116.html")
            }
          },
          {
            "https://www.kika.de/tib-tumtum/tib-und-tumtum-180.html",
            "/kika/kika_topic5_only_new_videos_link.html",
            new CrawlerUrlDTO[] {
              new CrawlerUrlDTO("https://www.kika.de/tib-tumtum/buendelgruppe2730.html")
            }
          }
        });
  }

  @Test
  public void testLandingPageWithMoreButton() throws IOException {
    final Connection connection = JsoupMock.mock(requestUrl, htmlFile);
    when(jsoupConnection.getConnection(eq(requestUrl))).thenReturn(connection);

    final Queue<CrawlerUrlDTO> urls = new ConcurrentLinkedQueue<>();
    urls.add(new CrawlerUrlDTO(requestUrl));

    final KikaTopicLandingPageTask target =
        new KikaTopicLandingPageTask(
            createCrawler(), urls, KikaConstants.BASE_URL, jsoupConnection);
    final Set<CrawlerUrlDTO> actual = target.invoke();

    assertThat(actual.size(), equalTo(expectedUrls.length));
    assertThat(actual, containsInAnyOrder(expectedUrls));
  }
}
