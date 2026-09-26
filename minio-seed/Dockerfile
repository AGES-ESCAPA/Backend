# syntax=docker/dockerfile:1.7

# ============================================================
# Build do MinIO Client (mc)
# ============================================================

FROM --platform=$BUILDPLATFORM golang:1.24-alpine AS build

ARG TARGETOS=linux
ARG TARGETARCH
ARG MC_RELEASE=RELEASE.2025-08-13T08-35-41Z

RUN apk add --no-cache \
    git \
    ca-certificates

WORKDIR /src

RUN git clone \
    --depth 1 \
    --branch "${MC_RELEASE}" \
    https://github.com/minio/mc.git \
    .

RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    LDFLAGS="$(go run buildscripts/gen-ldflags.go)" && \
    CGO_ENABLED=0 \
    GOOS="${TARGETOS}" \
    GOARCH="${TARGETARCH}" \
    go build \
      -trimpath \
      -tags kqueue \
      -ldflags "${LDFLAGS}" \
      -o /out/mc \
      .

# ============================================================
# Runtime
# ============================================================

FROM alpine:3.21

RUN apk add --no-cache ca-certificates

COPY --from=build /out/mc /usr/local/bin/mc

RUN chmod +x /usr/local/bin/mc