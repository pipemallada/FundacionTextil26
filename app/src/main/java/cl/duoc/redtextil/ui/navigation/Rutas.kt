package cl.duoc.redtextil.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector

object Rutas {
    const val PERFIL = "emp/perfil"
    const val REGISTRO = "emp/registro"
    const val FICHA = "emp/ficha"
    const val CATALOGO = "emp/catalogo"
    const val PUBLICACION = "emp/publicacion?id={id}"
    const val BANDEJA = "emp/consultas"

    const val DIRECTORIO = "dir/buscar"
    const val DETALLE = "dir/emprendimiento/{id}"
    const val CONSULTA = "dir/consulta/{id}"
    const val MIS_CONSULTAS = "dir/mis-consultas"

    const val ADMIN_FICHAS = "admin/fichas"
    const val ADMIN_METRICAS = "admin/metricas"
    const val ADMIN_CORREOS = "admin/correos"

    const val ARG_ID = "id"

    fun publicacion(id: Long?): String = "emp/publicacion?id=${id ?: -1L}"
    fun detalle(id: Long): String = "dir/emprendimiento/$id"
    fun consulta(id: Long): String = "dir/consulta/$id"

    fun modoDe(ruta: String?): ModoApp? = when {
        ruta == null -> null
        ruta.startsWith("emp/") -> ModoApp.EMPRENDEDORA
        ruta.startsWith("dir/") -> ModoApp.DIRECTORIO
        ruta.startsWith("admin/") -> ModoApp.ADMIN
        else -> null
    }
}

data class Pestana(val ruta: String, val titulo: String, val icono: ImageVector)

enum class ModoApp(
    val titulo: String,
    val corto: String,
    val descripcion: String,
    val rutaInicio: String
) {
    EMPRENDEDORA("Modo Emprendedora", "Emprendedora", "Mi ficha, catálogo y consultas", Rutas.PERFIL),
    DIRECTORIO("Modo Directorio Cliente", "Cliente", "Buscar y contactar emprendimientos", Rutas.DIRECTORIO),
    ADMIN("Modo Administradora", "Admin", "Fichas, métricas y correos automáticos", Rutas.ADMIN_FICHAS);

    val icono: ImageVector
        get() = when (this) {
            EMPRENDEDORA -> Icons.Filled.Storefront
            DIRECTORIO -> Icons.Filled.TravelExplore
            ADMIN -> Icons.Filled.AdminPanelSettings
        }

    val pestanas: List<Pestana>
        get() = when (this) {
            EMPRENDEDORA -> listOf(
                Pestana(Rutas.PERFIL, "Mi perfil", Icons.Filled.AccountCircle),
                Pestana(Rutas.FICHA, "Mi ficha", Icons.Filled.Badge),
                Pestana(Rutas.CATALOGO, "Catálogo", Icons.Filled.PhotoLibrary),
                Pestana(Rutas.BANDEJA, "Consultas", Icons.Filled.Forum)
            )
            DIRECTORIO -> listOf(
                Pestana(Rutas.DIRECTORIO, "Buscar", Icons.Filled.Search),
                Pestana(Rutas.MIS_CONSULTAS, "Mis consultas", Icons.Filled.QuestionAnswer)
            )
            ADMIN -> listOf(
                Pestana(Rutas.ADMIN_FICHAS, "Fichas", Icons.AutoMirrored.Filled.FactCheck),
                Pestana(Rutas.ADMIN_METRICAS, "Métricas", Icons.Filled.BarChart),
                Pestana(Rutas.ADMIN_CORREOS, "Correos", Icons.Filled.MarkEmailRead)
            )
        }

    val rutasPrincipales: Set<String>
        get() = pestanas.map { it.ruta }.toSet()
}
