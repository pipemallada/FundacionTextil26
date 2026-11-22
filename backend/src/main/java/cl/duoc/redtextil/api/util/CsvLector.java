package cl.duoc.redtextil.api.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Mismo lector que usa la app: respeta comillas porque las descripciones traen comas
public final class CsvLector {

    private CsvLector() {
    }

    public static List<Map<String, String>> leer(String texto) {
        List<List<String>> filas = separarFilas(texto.replace("﻿", ""));
        if (filas.isEmpty()) return List.of();
        List<String> encabezados = filas.get(0).stream().map(String::trim).toList();
        List<Map<String, String>> resultado = new ArrayList<>();
        for (List<String> fila : filas.subList(1, filas.size())) {
            if (fila.stream().allMatch(String::isBlank)) continue;
            Map<String, String> mapa = new LinkedHashMap<>();
            for (int i = 0; i < encabezados.size(); i++) {
                mapa.put(encabezados.get(i), i < fila.size() ? fila.get(i) : "");
            }
            resultado.add(mapa);
        }
        return resultado;
    }

    private static List<List<String>> separarFilas(String texto) {
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas && c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                campo.append('"');
                i++;
            } else if (c == '"') {
                entreComillas = !entreComillas;
            } else if (c == ',' && !entreComillas) {
                fila.add(campo.toString());
                campo.setLength(0);
            } else if ((c == '\n' || c == '\r') && !entreComillas) {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') i++;
                fila.add(campo.toString());
                campo.setLength(0);
                filas.add(fila);
                fila = new ArrayList<>();
            } else {
                campo.append(c);
            }
        }
        if (campo.length() > 0 || !fila.isEmpty()) {
            fila.add(campo.toString());
            filas.add(fila);
        }
        return filas;
    }
}
