package de.mediathekview.mserver.crawler.ard;

import de.mediathekview.mserver.crawler.basic.FilmUrlInfoDto;

import java.util.Objects;

public class ArdFilmUrlInfoDto extends FilmUrlInfoDto {

  private final String quality;

  public ArdFilmUrlInfoDto(final String aUrl, final String aQuality) {
    super(aUrl);
    quality = aQuality;
  }

  public String getQuality() {
    return quality;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final ArdFilmUrlInfoDto that)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    return Objects.equals(quality, that.quality);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), quality);
  }
}
