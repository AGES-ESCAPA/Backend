package com.escapa.backend.infrastructure.storage;

import io.minio.MinioClient;

public final class MinioClientFactory {

    private MinioClientFactory() {
    }

    /**
     * @param region regiao usada na assinatura das requisicoes. Em branco, o client descobre a regiao
     *               do bucket (comportamento do MinIO). O Garage exige a regiao configurada em
     *               {@code s3_region} (aqui, {@code garage}); sem ela o client assina com
     *               {@code us-east-1} e o Garage recusa a requisicao.
     */
    public static MinioClient create(String endpoint, String accessKey, String secretKey, String region) {
        final MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey);
        if (region != null && !region.isBlank()) {
            builder.region(region);
        }
        return builder.build();
    }
}
