package cl.duoc.redtextil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.redtextil.domain.model.EstadoCorreo
import cl.duoc.redtextil.domain.model.TipoCorreo

@Entity(
    tableName = "correos",
    indices = [Index(value = ["estado"])]
)
data class CorreoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emprendimientoId: Long? = null,
    val destinatario: String,
    val nombreDestinatario: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: TipoCorreo,
    val estado: EstadoCorreo = EstadoCorreo.EN_COLA_OFFLINE,
    val intentos: Int = 0,
    val detalleError: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaEnvio: Long? = null
)
