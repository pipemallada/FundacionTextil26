package cl.duoc.redtextil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.redtextil.domain.model.EstadoFicha
import cl.duoc.redtextil.domain.model.EstadoSync
import cl.duoc.redtextil.domain.model.FichaDatos
import cl.duoc.redtextil.domain.model.OrigenFicha
import cl.duoc.redtextil.domain.model.Rubro

@Entity(
    tableName = "emprendimientos",
    indices = [
        Index(value = ["idHistorico"], unique = true),
        Index(value = ["region", "comuna"]),
        Index(value = ["estado"]),
        Index(value = ["rut"])
    ]
)
data class EmprendimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreEmprendedora: String,
    val rut: String = "",
    val fechaNacimiento: String = "",
    val nacionalidad: String = "",
    val correo: String = "",
    val telefono: String = "",
    val jefaHogar: String = "",
    val nombreComercial: String,
    val descripcion: String = "",
    val anioInicio: Int? = null,
    val lugarTrabajo: String = "",
    val cantidadPersonas: Int? = null,
    val formalizado: Boolean = false,
    val rutEmpresa: String = "",
    val tipoFormalizacion: String = "",
    val region: String,
    val comuna: String,
    val atencionPresencial: Boolean = false,
    val direccionTaller: String = "",
    val realizaEnvios: Boolean = false,
    val rubroPrincipal: String = "",
    val rubroSecundario: String = "",
    val tecnicas: String = "",
    val recibeDonaciones: Boolean = false,
    val tiposDonacion: String = "",
    val cuposTaller: Int? = null,
    val modalidadTaller: String = "",
    val tallasArriendo: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val sitioWeb: String = "",
    val horarioAtencion: String = "",
    val aceptaPublicacion: Boolean = false,
    val aceptaPrivacidad: Boolean = false,
    val estado: EstadoFicha = EstadoFicha.PENDIENTE,
    val observacion: String = "",
    val origen: OrigenFicha = OrigenFicha.APP,
    val idHistorico: String? = null,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
    val remoteId: Long? = null,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
) {
    fun listaRubros(): List<Rubro> =
        listOfNotNull(Rubro.desdeCodigo(rubroPrincipal), Rubro.desdeCodigo(rubroSecundario))

    fun aFichaDatos(): FichaDatos = FichaDatos(
        nombreEmprendedora = nombreEmprendedora,
        rut = rut,
        fechaNacimiento = fechaNacimiento,
        nacionalidad = nacionalidad,
        correo = correo,
        telefono = telefono,
        jefaHogar = jefaHogar,
        nombreComercial = nombreComercial,
        descripcion = descripcion,
        anioInicio = anioInicio?.toString().orEmpty(),
        lugarTrabajo = lugarTrabajo,
        cantidadPersonas = cantidadPersonas?.toString().orEmpty(),
        formalizado = formalizado,
        rutEmpresa = rutEmpresa,
        tipoFormalizacion = tipoFormalizacion,
        region = region,
        comuna = comuna,
        atencionPresencial = atencionPresencial,
        direccionTaller = direccionTaller,
        realizaEnvios = realizaEnvios,
        rubros = listaRubros().toSet(),
        tecnicas = tecnicas,
        recibeDonaciones = recibeDonaciones,
        tiposDonacion = tiposDonacion,
        cuposTaller = cuposTaller?.toString().orEmpty(),
        modalidadTaller = modalidadTaller,
        tallasArriendo = tallasArriendo,
        whatsapp = whatsapp,
        instagram = instagram,
        sitioWeb = sitioWeb,
        horarioAtencion = horarioAtencion,
        aceptaPublicacion = aceptaPublicacion
    )

    fun conDatos(datos: FichaDatos): EmprendimientoEntity {
        val lista = datos.rubros.toList()
        return copy(
            nombreEmprendedora = datos.nombreEmprendedora.trim(),
            rut = datos.rut,
            fechaNacimiento = datos.fechaNacimiento.trim(),
            nacionalidad = datos.nacionalidad,
            correo = datos.correo.trim().lowercase(),
            telefono = datos.telefono,
            jefaHogar = datos.jefaHogar,
            nombreComercial = datos.nombreComercial.trim(),
            descripcion = datos.descripcion.trim(),
            anioInicio = datos.anioInicio.trim().toIntOrNull(),
            lugarTrabajo = datos.lugarTrabajo,
            cantidadPersonas = datos.cantidadPersonas.trim().toIntOrNull(),
            formalizado = datos.formalizado,
            rutEmpresa = datos.rutEmpresa,
            tipoFormalizacion = datos.tipoFormalizacion,
            region = datos.region,
            comuna = datos.comuna,
            atencionPresencial = datos.atencionPresencial,
            direccionTaller = datos.direccionTaller.trim(),
            realizaEnvios = datos.realizaEnvios,
            rubroPrincipal = lista.getOrNull(0)?.name.orEmpty(),
            rubroSecundario = lista.getOrNull(1)?.name.orEmpty(),
            tecnicas = datos.tecnicas.trim(),
            recibeDonaciones = datos.recibeDonaciones,
            tiposDonacion = datos.tiposDonacion.trim(),
            cuposTaller = datos.cuposTaller.trim().toIntOrNull(),
            modalidadTaller = datos.modalidadTaller,
            tallasArriendo = datos.tallasArriendo.trim(),
            whatsapp = datos.whatsapp,
            instagram = datos.instagram,
            sitioWeb = datos.sitioWeb,
            horarioAtencion = datos.horarioAtencion.trim(),
            aceptaPublicacion = datos.aceptaPublicacion
        )
    }
}
