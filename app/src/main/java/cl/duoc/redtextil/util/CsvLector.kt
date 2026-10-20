package cl.duoc.redtextil.util

import java.io.InputStream

object CsvLector {

    fun leer(entrada: InputStream): List<Map<String, String>> {
        val lineas = entrada.bufferedReader(Charsets.UTF_8).readText()
            .removePrefix("﻿")
        val filas = separarFilas(lineas)
        if (filas.isEmpty()) return emptyList()
        val encabezados = filas.first().map { it.trim() }
        return filas.drop(1)
            .filter { fila -> fila.any { it.isNotBlank() } }
            .map { fila -> encabezados.mapIndexed { i, columna -> columna to (fila.getOrNull(i) ?: "") }.toMap() }
    }

    // Parser simple que respeta comillas, porque las descripciones de la planilla traen comas y saltos de linea
    private fun separarFilas(texto: String): List<List<String>> {
        val filas = mutableListOf<List<String>>()
        var fila = mutableListOf<String>()
        val campo = StringBuilder()
        var entreComillas = false
        var i = 0
        while (i < texto.length) {
            val c = texto[i]
            when {
                entreComillas && c == '"' && i + 1 < texto.length && texto[i + 1] == '"' -> {
                    campo.append('"')
                    i++
                }
                c == '"' -> entreComillas = !entreComillas
                c == ',' && !entreComillas -> {
                    fila.add(campo.toString())
                    campo.clear()
                }
                (c == '\n' || c == '\r') && !entreComillas -> {
                    if (c == '\r' && i + 1 < texto.length && texto[i + 1] == '\n') i++
                    fila.add(campo.toString())
                    campo.clear()
                    filas.add(fila)
                    fila = mutableListOf()
                }
                else -> campo.append(c)
            }
            i++
        }
        if (campo.isNotEmpty() || fila.isNotEmpty()) {
            fila.add(campo.toString())
            filas.add(fila)
        }
        return filas
    }
}
