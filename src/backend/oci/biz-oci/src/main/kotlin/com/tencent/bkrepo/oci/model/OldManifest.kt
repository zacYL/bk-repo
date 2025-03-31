package com.tencent.bkrepo.oci.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OldManifest(@JsonProperty("Config")val config: String, @JsonProperty("Layers")val layers: List<String>)
