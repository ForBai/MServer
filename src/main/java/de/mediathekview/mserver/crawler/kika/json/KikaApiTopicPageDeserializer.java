package de.mediathekview.mserver.crawler.kika.json;

import com.google.gson.*;

import de.mediathekview.mserver.base.utils.JsonUtils;
import de.mediathekview.mserver.base.utils.UrlUtils;
import de.mediathekview.mserver.crawler.basic.TopicUrlDTO;
import de.mediathekview.mserver.crawler.kika.KikaApiConstants;

import java.lang.reflect.Type;
import java.util.Optional;


public class KikaApiTopicPageDeserializer implements JsonDeserializer<KikaApiTopicDto> {
  private static final String[] TAG_ERROR_CODE = new String[] {"error", "code"};
  private static final String[] TAG_ERROR_MESSAGE = new String[] {"error", "message"};
  private static final String[] TAG_NEXT_PAGE = new String[] {"_links", "next", "href"};
  private static final String[] TAG_FILM_ARRAY = new String[] {"_embedded","items"};
  private static final String[] TAG_BRAND_WEBSITE = new String[] {"_embedded", "brand", "homepageTeaserLinkUrl"};
  private static final String TAG_FILM_NAME = "title";
  private static final String TAG_FILM_ID = "id";
  private static final String TAG_FILM_EXPIRATIONDATE = "expirationDate";
  private static final String TAG_FILM_APPEARDATE = "appearDate";
  private static final String TAG_FILM_DESCRIPTION = "description";
  private static final String TAG_FILM_DATE = "date";
  private static final String TAG_FILM_DURATION = "duration";
  private static final String TAG_FILM_GEO = "geoProtection";
  private static final String[] TAG_FILM_TOPIC = new String[] {"_embedded","brand","title"};
  private static final String TAG_FILM_SOPHORAID = "sophoraId";
  private static final String TAG_FILM_TEASERIMAGEURL = "teaserImageUrl";
  //
  
  @Override
  public KikaApiTopicDto deserialize(
      final JsonElement jsonElement, final Type typeOfT, final JsonDeserializationContext context)
      throws JsonParseException {
    //
    KikaApiTopicDto aKikaApiTopicDto = new KikaApiTopicDto();
    // catch error
    Optional<String> errorCode = JsonUtils.getElementValueAsString(jsonElement, TAG_ERROR_MESSAGE);
    Optional<String> errorMessage = JsonUtils.getElementValueAsString(jsonElement, TAG_ERROR_CODE);
    if (errorCode.isPresent()) {
      aKikaApiTopicDto.setError(errorCode, errorMessage);
      return aKikaApiTopicDto;
    }
    // next page
    Optional<String> nextPage = JsonUtils.getElementValueAsString(jsonElement, TAG_NEXT_PAGE);
    if (nextPage.isPresent()) {
      aKikaApiTopicDto.setNextPage(new TopicUrlDTO("next page", UrlUtils.addProtocolIfMissing(KikaApiConstants.HOST + nextPage.get(), UrlUtils.PROTOCOL_HTTPS)));
    }
    // film element
    final JsonObject searchElement = jsonElement.getAsJsonObject();
    if (searchElement.has(TAG_FILM_ARRAY[0])) {
      final JsonObject embeddedElement = searchElement.getAsJsonObject(TAG_FILM_ARRAY[0]);
      if (embeddedElement.has(TAG_FILM_ARRAY[1])) {
        final JsonArray itemArray = embeddedElement.getAsJsonArray(TAG_FILM_ARRAY[1]);
        for (JsonElement arrayElement : itemArray) {
          parseFilm(arrayElement).ifPresent(aKikaApiTopicDto::add);
        }
      }
    }
    return aKikaApiTopicDto;
  }
  
  protected Optional<KikaApiFilmDto> parseFilm(final JsonElement arrayElement) {
    Optional<KikaApiFilmDto> aFilm = Optional.empty();
    //
    Optional<String> oId = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_ID);
    Optional<String> oTitle = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_NAME);
    Optional<String> oDescription = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_DESCRIPTION);
    Optional<String> oDate = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_DATE);
    Optional<String> oDuration = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_DURATION);
    Optional<String> oGeo = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_GEO);
    Optional<String> oExpire = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_EXPIRATIONDATE);
    Optional<String> oAired = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_APPEARDATE);
    Optional<String> website = JsonUtils.getElementValueAsString(arrayElement, TAG_BRAND_WEBSITE);
    Optional<String> oTopic = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_TOPIC);
    Optional<String> oSophoraId = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_SOPHORAID);
    Optional<String> oTeaserImageUrl = JsonUtils.getElementValueAsString(arrayElement, TAG_FILM_TEASERIMAGEURL);
    if (oSophoraId.isPresent() && oTeaserImageUrl.isPresent()) {
      // try to reconstruct the website
      website = reconstructWebsite(oTeaserImageUrl,oSophoraId);
    }
    //
    if (oId.isPresent()) {
      aFilm = Optional.of(new KikaApiFilmDto(
          String.format(KikaApiConstants.FILM, oId.get()),
          oTopic,
          oTitle,
          oId,
          oDescription,
          oDate,
          oDuration,
          oGeo,
          oExpire,
          oAired,
          website
          ));
    }
    return aFilm;
  }
  
  protected Optional<String> reconstructWebsite(Optional<String> oTeaserImageUrl, Optional<String> oSophoraId) {
    String base = KikaApiConstants.WEBSITE;
    int start = oTeaserImageUrl.get().indexOf("/", "https://".length()+1);
    int stop = oTeaserImageUrl.get().indexOf("/", start+1);
    base += oTeaserImageUrl.get().substring(start, stop);
    base += "/videos/" + oSophoraId.get() + ".html";
    return Optional.of(base);
  }
}
