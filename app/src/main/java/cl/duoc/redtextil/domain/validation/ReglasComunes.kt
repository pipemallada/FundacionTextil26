package cl.duoc.redtextil.domain.validation

import cl.duoc.redtextil.util.TextoUtil

object ReglasComunes {

    private val regexNombre = Regex("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' .-]+$")
    private val regexNombreComercial = Regex("^[\\p{L}0-9 &'.,!¡()-]+$")
    private val regexCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val regexTelefonoChile = Regex("^\\+569\\d{8}$")
    private val regexRutLimpio = Regex("^\\d{7,8}[0-9K]$")
    private val regexUsuarioInstagram = Regex("^[a-z0-9._]{1,30}$")
    private val regexUrlInstagram = Regex(
        "^(https?://)?(www\\.)?(instagram\\.com|instagr\\.am)/([^/?#\\s]+)/?([?#].*)?$",
        RegexOption.IGNORE_CASE
    )
    private val regexUrl = Regex(
        "^https?://[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}(:\\d{2,5})?(/\\S*)?$",
        RegexOption.IGNORE_CASE
    )
    private val rutasReservadasInstagram = setOf("p", "reel", "reels", "stories", "explore", "tv", "accounts")

    fun obligatorio(valor: String, mensaje: String): String? =
        if (valor.isBlank()) mensaje else null

    fun validarLargo(valor: String, minimo: Int, maximo: Int, nombreCampo: String): String? {
        val largo = valor.trim().length
        return when {
            largo == 0 -> "Escribe $nombreCampo"
            largo < minimo -> "Escribe al menos $minimo caracteres (llevas $largo)"
            largo > maximo -> "Máximo $maximo caracteres (llevas $largo)"
            else -> null
        }
    }

    fun validarEntero(valor: String, minimo: Int, maximo: Int, nombreCampo: String): String? {
        if (valor.isBlank()) return "Escribe $nombreCampo"
        val numero = valor.trim().toIntOrNull() ?: return "Usa solo números"
        return if (numero < minimo || numero > maximo) "Debe estar entre $minimo y $maximo" else null
    }

    fun validarOpcion(valor: String, opciones: List<String>, mensaje: String): String? =
        if (valor !in opciones) mensaje else null

    fun validarNombreCompleto(valor: String): String? {
        val limpio = TextoUtil.limpiarEspacios(valor)
        return when {
            limpio.isEmpty() -> "Escribe tu nombre y apellido"
            !regexNombre.matches(limpio) -> "Usa solo letras, sin números ni símbolos"
            limpio.length < 5 -> "El nombre es muy corto"
            limpio.length > 60 -> "Máximo 60 caracteres"
            limpio.split(" ").count { it.length >= 2 } < 2 -> "Escribe al menos un nombre y un apellido"
            else -> null
        }
    }

    fun validarNombrePersona(valor: String): String? {
        val limpio = TextoUtil.limpiarEspacios(valor)
        return when {
            limpio.isEmpty() -> "Escribe tu nombre"
            !regexNombre.matches(limpio) -> "Usa solo letras, sin números ni símbolos"
            limpio.length < 3 -> "El nombre es muy corto"
            limpio.length > 60 -> "Máximo 60 caracteres"
            else -> null
        }
    }

    fun validarNombreComercial(valor: String): String? {
        val limpio = TextoUtil.limpiarEspacios(valor)
        return when {
            limpio.isEmpty() -> "Escribe el nombre de tu emprendimiento"
            limpio.length < 3 -> "El nombre es muy corto"
            limpio.length > 50 -> "Máximo 50 caracteres"
            !regexNombreComercial.matches(limpio) -> "Evita símbolos especiales como @, # o emojis"
            else -> null
        }
    }

    fun validarCorreo(valor: String): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> "Escribe tu correo electrónico"
            limpio.contains(" ") -> "El correo no puede tener espacios"
            !regexCorreo.matches(limpio) -> "Correo no válido (ej: nombre@correo.cl)"
            else -> null
        }
    }

    // Aceptamos "9 1234 5678", "+56 9 1234 5678" o "56912345678" y lo dejamos siempre como +569XXXXXXXX
    fun normalizarTelefono(valor: String): String? {
        if (valor.any { it.isLetter() }) return null
        val digitos = valor.filter { it.isDigit() }
        val completo = when {
            digitos.length == 11 && digitos.startsWith("569") -> digitos
            digitos.length == 9 && digitos.startsWith("9") -> "56$digitos"
            else -> return null
        }
        val conMas = "+$completo"
        return if (regexTelefonoChile.matches(conMas)) conMas else null
    }

    fun validarTelefono(valor: String, nombreCampo: String = "tu teléfono"): String? {
        if (valor.isBlank()) return "Escribe $nombreCampo"
        return if (normalizarTelefono(valor) == null) "Debe ser un celular chileno: +569 y 8 números" else null
    }

    fun limpiarRut(valor: String): String =
        valor.uppercase().filter { it.isDigit() || it == 'K' }

    fun calcularDigitoVerificador(cuerpo: String): Char {
        var suma = 0
        var factor = 2
        for (c in cuerpo.reversed()) {
            suma += (c - '0') * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        return when (val resto = 11 - (suma % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + resto
        }
    }

    fun validarRut(valor: String, nombreCampo: String = "tu RUT"): String? {
        if (valor.isBlank()) return "Escribe $nombreCampo"
        val limpio = limpiarRut(valor)
        if (!regexRutLimpio.matches(limpio)) return "Formato no válido (ej: 12.345.678-5)"
        val cuerpo = limpio.dropLast(1)
        if (cuerpo.toInt() < 1_000_000) return "El RUT es muy corto"
        return if (calcularDigitoVerificador(cuerpo) != limpio.last()) {
            "El dígito verificador no corresponde"
        } else {
            null
        }
    }

    fun formatearRut(valor: String): String {
        val limpio = limpiarRut(valor)
        if (limpio.length < 2) return valor.trim()
        val cuerpo = limpio.dropLast(1)
        val conPuntos = cuerpo.reversed().chunked(3).joinToString(".").reversed()
        return "$conPuntos-${limpio.last()}"
    }

    fun normalizarInstagram(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty() || texto.contains(" ")) return null
        val desdeUrl = regexUrlInstagram.find(texto)?.groupValues?.get(4)
        if (desdeUrl == null && texto.contains("/")) return null
        val usuario = (desdeUrl ?: texto.removePrefix("@")).lowercase()
        if (usuario in rutasReservadasInstagram) return null
        if (!regexUsuarioInstagram.matches(usuario)) return null
        if (usuario.startsWith(".") || usuario.endsWith(".") || usuario.contains("..")) return null
        return "@$usuario"
    }

    fun validarInstagram(valor: String, obligatorio: Boolean = false): String? {
        if (valor.isBlank()) return if (obligatorio) "Escribe tu usuario de Instagram" else null
        return if (normalizarInstagram(valor) == null) {
            "Usuario no válido. Escribe @tuusuario o el enlace de tu perfil"
        } else {
            null
        }
    }

    fun normalizarUrl(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty() || texto.contains(" ")) return null
        val conProtocolo = if (texto.startsWith("http://", true) || texto.startsWith("https://", true)) {
            texto
        } else {
            "https://$texto"
        }
        return if (regexUrl.matches(conProtocolo)) conProtocolo else null
    }

    fun validarUrl(valor: String, obligatorio: Boolean = false): String? {
        if (valor.isBlank()) return if (obligatorio) "Escribe el enlace" else null
        return if (normalizarUrl(valor) == null) "Enlace no válido (ej: www.mitienda.cl)" else null
    }
}
