{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "bkrepo.imagePullSecrets" -}}
{{- include "common.images.pullSecrets" (dict "images" (list
    .Values.gateway.image
    .Values.repository.image
    .Values.auth.image
    .Values.init.curl.image
    .Values.init.mongodb.image
    .Values.init.iam.image
    .Values.generic.image
    .Values.docker.image
    .Values.npm.image
    .Values.pypi.image
    .Values.helm.image
    .Values.job.image
    .Values.maven.image
    .Values.opdata.image
    .Values.preview.image
    .Values.replication.image
    .Values.s3.image
) "global" .Values.global) -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "bkrepo.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (printf "%s-foo" (include "common.names.fullname" .)) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
define init project id
*/}}
{{- define "bkrepo.init.project" -}}
{{- if .Values.gateway.enableMultiTenantMode -}}
{{- $tenantId := default "system" .Values.gateway.oPTenantId -}}
{{- printf "%s.blueking" $tenantId -}}
{{- else -}}
{{- printf "blueking" -}}
{{- end -}}
{{- end -}}



{{/*
Create a default fully qualified mongodb subchart.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "bkrepo.mongodb.fullname" -}}
{{- if .Values.mongodb.fullnameOverride -}}
{{- .Values.mongodb.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "mongodb" .Values.mongodb.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the mongodb connection uri
*/}}
{{- define "bkrepo.mongodbUri" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- printf "mongodb://%s:%s@%s:27017/%s" .Values.mongodb.auth.username (include "bkrepo.mongodb.password" . | urlquery) (include "bkrepo.mongodb.fullname" .) .Values.mongodb.auth.database -}}
{{- else -}}
{{- include "bkrepo.externalMongodb.uri" . -}}
{{- end -}}
{{- end -}}

{{/*
Return the label key of bk-repo scope
*/}}
{{- define "bkrepo.labelValues.scope" -}}
    {{- printf "bk.repo.scope" -}}
{{- end -}}

{{/*
Return the label value of bk-repo scope backend
*/}}
{{- define "bkrepo.labelValues.scope.backend" -}}
    {{- printf "backend" -}}
{{- end -}}

{{/*
Return the label value of bk-repo scope gateway
*/}}
{{- define "bkrepo.labelValues.scope.gateway" -}}
    {{- printf "gateway" -}}
{{- end -}}

{{/*
Return the proper image name
{{ include "bkrepo.images.image" ( dict "imageRoot" .Values.path.to.the.image "global" $) }}
*/}}
{{- define "bkrepo.images.image" -}}
{{- $registryName := .imageRoot.registry -}}
{{- $repositoryName := .imageRoot.repository -}}
{{- $tag := .imageRoot.tag | toString -}}
{{- if .global }}
    {{- if .global.imageRegistry }}
     {{- $registryName = .global.imageRegistry -}}
    {{- end -}}
{{- end -}}
{{- if .bkrepo.imageRegistry }}
    {{- $registryName = .bkrepo.imageRegistry -}}
{{- end -}}
{{- if .bkrepo.imageTag }}
    {{- $tag = .bkrepo.imageTag -}}
{{- end -}}
{{- if $registryName }}
{{- printf "%s/%s:%s" $registryName $repositoryName $tag -}}
{{- else -}}
{{- printf "%s:%s" $repositoryName $tag -}}
{{- end -}}
{{- end -}}

{{/*
Return the effective JVM option.
Service-level jvmOption takes precedence over common.jvmOption.
*/}}
{{- define "bkrepo.jvmOption" -}}
{{- default .common.jvmOption .service.jvmOption -}}
{{- end -}}

{{/*
Return the value of auth url
*/}}
{{- define "bkrepo.oci.authUrl" -}}
{{- if and .Values.gateway.service.nodeIP (and .Values.gateway.service.dockerNodePort (or (eq .Values.gateway.service.type "NodePort") (eq .Values.gateway.service.type "LoadBalancer"))) -}}
    {{- printf "%s:%s/v2/auth" .Values.gateway.service.nodeIP (.Values.gateway.service.dockerNodePort | toString) -}}
{{- else -}}
    {{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
    {{- printf "%s%s/docker/v2/auth" .Values.gateway.host .Values.bkWebSiteAccess.subPath -}}
    {{- else -}}
    {{- printf "%s/docker/v2/auth" .Values.gateway.host -}}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
True if value is a Platform auth header: "Platform " + base64(accessKey:secretKey).
*/}}
{{- define "bkrepo.platformAuth.valid" -}}
{{- $auth := . | toString | trim -}}
{{- if hasPrefix "Platform " $auth -}}
{{- $payload := trimPrefix "Platform " $auth | trim -}}
{{- if regexMatch "^[A-Za-z0-9+/]+=*$" $payload -}}
{{- $decoded := $payload | b64dec -}}
{{- $parts := splitn ":" 2 $decoded -}}
{{- if and $parts._0 $parts._1 -}}
true
{{- end -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return the value of authorization.

New installs must set gateway.accessKey and gateway.secretKey.
Upgrades may keep a valid gateway.authorization and leave accessKey/secretKey empty.
*/}}
{{- define "bkrepo.authorization" -}}
{{- $ak := .Values.gateway.accessKey | toString -}}
{{- $sk := .Values.gateway.secretKey | toString -}}
{{- if and $ak $sk -}}
{{- if or (eq $ak "18b61c9c-901b-4ea3-89c3-1f74be944b66") (eq $sk "Us8ZGDXPqk86cwMukYABQqCZLAkM3K") -}}
{{- fail "gateway.accessKey/secretKey must not use the publicly known default pair. Set unique values." -}}
{{- end -}}
{{- printf "Platform %s" (printf "%s:%s" $ak $sk | b64enc) -}}
{{- else if .Release.IsInstall -}}
{{- fail "New installs must set gateway.accessKey and gateway.secretKey (do not rely on gateway.authorization). Example: --set gateway.accessKey=<ak> --set gateway.secretKey=<sk>" -}}
{{- else -}}
{{- $auth := .Values.gateway.authorization | toString | trim -}}
{{- if ne (include "bkrepo.platformAuth.valid" $auth | trim) "true" -}}
{{- fail "Upgrade requires a valid gateway.authorization (Platform <base64(accessKey:secretKey)>) or set both gateway.accessKey and gateway.secretKey." -}}
{{- end -}}
{{- $auth -}}
{{- end -}}
{{- end -}}


{{- define "bkrepo.subPath" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ printf "%s(/?)(.*)" .Values.bkWebSiteAccess.subPath }}
{{- else -}}
{{ printf "/" }}
{{- end -}}
{{- end -}}

{{- define "bkrepo.webSubPath" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ printf "%s/" .Values.bkWebSiteAccess.subPath }}
{{- else -}}
{{ printf "/" }}
{{- end -}}
{{- end -}}

{{/*
Escape special characters for Kafka JAAS configuration
Escapes backslash and double quote characters
*/}}
{{- define "bkrepo.jaasEscape" -}}
{{- . | replace "\\" "\\\\" | replace "\"" "\\\"" -}}
{{- end -}}

{{/*
Generate Kafka SASL JAAS configuration string
*/}}
{{- define "bkrepo.kafka.jaasConfig" -}}
{{- $username := include "bkrepo.jaasEscape" .Values.kafka.username -}}
{{- $password := include "bkrepo.jaasEscape" .Values.kafka.password -}}
{{- printf "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";" $username $password -}}
{{- end -}}

{{/*
Return the initial admin password.
Must be explicitly set and must not be a publicly known default.
*/}}
{{- define "bkrepo.admin.password" -}}
{{- $password := .Values.common.password | toString -}}
{{- if or (eq $password "") (eq $password "password") (eq $password "blueking") -}}
{{- fail "common.password must be set to a strong value (do not use empty, 'password' or 'blueking'). Example: --set common.password=<your-password>" -}}
{{- else -}}
{{- $password -}}
{{- end -}}
{{- end -}}

{{/*
Return the in-cluster MongoDB password.
Must be explicitly set when mongodb.enabled=true and must not be the public default.
*/}}
{{- define "bkrepo.mongodb.password" -}}
{{- $password := .Values.mongodb.auth.password | toString -}}
{{- if or (eq $password "") (eq $password "bkrepo") -}}
{{- fail "mongodb.auth.password must be set to a strong value when mongodb.enabled=true (do not use empty or 'bkrepo'). Example: --set mongodb.auth.password=<your-password>" -}}
{{- else -}}
{{- $password -}}
{{- end -}}
{{- end -}}

{{/*
Return the external MongoDB URI.
Required when mongodb.enabled=false; reject the hardcoded default credentials.
*/}}
{{- define "bkrepo.externalMongodb.uri" -}}
{{- $uri := .Values.externalMongodb.uri | toString -}}
{{- if or (eq $uri "") (contains "bkrepo:bkrepo@" $uri) -}}
{{- fail "externalMongodb.uri must be set to a real MongoDB URI when mongodb.enabled=false (do not use default credentials bkrepo/bkrepo)." -}}
{{- else -}}
{{- $uri -}}
{{- end -}}
{{- end -}}

{{- define "bkrepo.fdtpSecretKey" -}}
{{- $msg := "replicationUDP.secretKey is required (>=64 bytes). Set values.replicationUDP.secretKey" -}}
{{- $key := required $msg .Values.replicationUDP.secretKey -}}
{{- if lt (len $key) 64 -}}
{{- fail "replicationUDP.secretKey must be at least 64 bytes (HS512)" -}}
{{- end -}}
{{- $key -}}
{{- end -}}
