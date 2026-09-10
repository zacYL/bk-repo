package com.tencent.bkrepo.oci.model

class ManifestList(
    override var schemaVersion: Int,
    var mediaType: String,
    var manifests: List<ManifestDescriptor>,
    var subject: Descriptor? = null,
    var artifactType: String? = null
) : SchemaVersion(schemaVersion)
