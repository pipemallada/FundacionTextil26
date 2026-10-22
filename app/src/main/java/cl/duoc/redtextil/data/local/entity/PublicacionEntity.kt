package cl.duoc.redtextil.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.redtextil.domain.model.CategoriaTrabajo
import cl.duoc.redtextil.domain.model.EstadoSync

@Entity(
    tableName = "publicaciones",
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
data class PublicacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emprendimientoId: Long,
    val titulo: String,
    val categoria: CategoriaTrabajo,
    val tecnica: String,
    val precioReferencial: Int,
    val descripcion: String,
    val fotoRuta: String?,
    val kbOriginal: Long = 0,
    val kbOptimizado: Long = 0,
    val fotoSincronizada: Boolean = false,
    val eliminada: Boolean = false,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE_SINCRONIZAR,
    val remoteId: Long? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)
