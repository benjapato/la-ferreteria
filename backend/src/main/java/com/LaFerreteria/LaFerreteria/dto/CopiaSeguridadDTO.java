package com.LaFerreteria.LaFerreteria.dto;

import java.time.LocalDateTime;
import java.util.Locale;

public record CopiaSeguridadDTO(
        String nombre,
        long tamanioBytes,
        LocalDateTime fechaModificacion
) {

    public String tamanioLegible() {

        if (tamanioBytes < 1024) {
            return tamanioBytes + " B";
        }

        double kilobytes = tamanioBytes / 1024.0;

        if (kilobytes < 1024) {
            return String.format(
                    Locale.ROOT,
                    "%.2f KB",
                    kilobytes
            );
        }

        double megabytes = kilobytes / 1024.0;

        if (megabytes < 1024) {
            return String.format(
                    Locale.ROOT,
                    "%.2f MB",
                    megabytes
            );
        }

        double gigabytes = megabytes / 1024.0;

        return String.format(
                Locale.ROOT,
                "%.2f GB",
                gigabytes
        );
    }
}
