package com.thefatrat.eddiejunior.util;

import java.util.Map;

public interface MetadataHolder {

    String getMetadataId();

    void setMetadata(Map<String, Object> metadata);

    Map<String, Object> getMetadata();

}
