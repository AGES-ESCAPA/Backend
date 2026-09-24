FROM quay.io/minio/mc:RELEASE.2025-08-13T08-35-41Z AS mc
FROM alpine:3.21
COPY --from=mc /usr/bin/mc /usr/local/bin/mc
