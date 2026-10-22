package cl.duoc.redtextil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.redtextil.domain.model.TipoInteraccion

@Entity(
    tableName = "interacciones",
    indices = [Index(value = ["tipo"]), Index(value = ["sincronizada"])]
)
data class InteraccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoInteraccion,
    val emprendimientoId: Long? = null,
    val nombreComercial: String = "",
    val region: String = "",
    val rubro: String = "",
    val detalle: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val sincronizada: Boolean = false
)
