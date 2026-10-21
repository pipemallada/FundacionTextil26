package cl.duoc.redtextil.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.redtextil.domain.model.EstadoConsulta
import cl.duoc.redtextil.domain.model.EstadoSync

@Entity(
    tableName = "consultas",
    foreignKeys = [
        ForeignKey(
            entity = EmprendimientoEntity::class,
            parentColumns = ["id"],
            childColumns = ["emprendimientoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["emprendimientoId"])]
)
data class ConsultaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emprendimientoId: Long,
    val nombreCliente: String,
    val correoCliente: String,
    val telefonoCliente: String,
    val servicio: String,
    val mensaje: String,
    val estado: EstadoConsulta = EstadoConsulta.PENDIENTE,
    val respuesta: String = "",
    val enviadaDesdeApp: Boolean = true,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
    val remoteId: Long? = null,
    val fechaEnvio: Long = System.currentTimeMillis(),
    val fechaRespuesta: Long? = null,
    val fechaActualizacion: Long = System.currentTimeMillis()
)

data class ConsultaConNegocio(
    @Embedded val consulta: ConsultaEntity,
    val nombreComercial: String
)
