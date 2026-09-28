# syntax=docker/dockerfile:1.7

# ============================================================
# Build do MinIO Server
# ============================================================

FROM --platform=$BUILDPLATFORM golang:1.24-alpine AS build

ARG TARGETOS=linux
ARG TARGETARCH
ARG MINIO_RELEASE=RELEASE.2025-09-07T16-13-09Z

RUN apk add --no-cache \
    git \
    ca-certificates

WORKDIR /src

# Clona exatamente a release definida no Compose.
RUN git clone \
    --depth 1 \
    --branch "${MINIO_RELEASE}" \
    https://github.com/minio/minio.git \
    .

# Usa o mesmo mecanismo de ldflags utilizado pelo próprio projeto MinIO.
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    LDFLAGS="$(go run buildscripts/gen-ldflags.go)" && \
    CGO_ENABLED=0 \
    GOOS="${TARGETOS}" \
    GOARCH="${TARGETARCH}" \
    go build \
      -tags kqueue \
      -trimpath \
      -ldflags "${LDFLAGS}" \
      -o /out/minio \
      .

# ============================================================
# Runtime
# ============================================================

FROM alpine:3.21

RUN apk add --no-cache \
    ca-certificates \
    curl

COPY --from=build /out/minio /usr/local/bin/minio

RUN chmod +x /usr/local/bin/minio

EXPOSE 9000 9001

VOLUME ["/data"]

ENTRYPOINT ["minio"]