package com.example.sleeprism.dto.sound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SoundInfo {
  private String id;
  private String name;
  private String url;
  private double relativeVolume;

  public SoundInfo(String id, String name, String url, double relativeVolume) {
    this.id = id;
    this.name = name;
    this.url = url;
    this.relativeVolume = relativeVolume;
  }

  // Getters
  public String getId() { return id; }
  public String getName() { return name; }
  public String getUrl() { return url; }
  public double getRelativeVolume() { return relativeVolume; }

  // Setters (필요하다면)
  public void setId(String id) { this.id = id; }
  public void setName(String name) { this.name = name; }
  public void setUrl(String url) { this.url = url; }
  public void setRelativeVolume(double relativeVolume) { this.relativeVolume = relativeVolume; }
}
