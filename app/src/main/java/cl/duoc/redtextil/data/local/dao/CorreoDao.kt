package cl.duoc.redtextil.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CorreoDao {

    @Insert
    suspend fun insertar(correo: CorreoEntity): Long

    @Insert
    suspend fun insertarLote(lista: List<CorreoEntity>)

    @Update
    suspend fun actualizar(correo: CorreoEntity)

    @Query("SELECT * FROM correos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): CorreoEntity?

    @Query("SELECT * FROM correos WHERE (:estado = '' OR estado = :estado) ORDER BY fechaCreacion DESC")
    fun observar(estado: String): Flow<List<CorreoEntity>>

    @Query("SELECT * FROM correos WHERE estado != 'ENVIADO_OK' ORDER BY fechaCreacion ASC")
    suspend fun noEnviados(): List<CorreoEntity>

    @Query("SELECT estado AS clave, COUNT(*) AS total FROM correos GROUP BY estado")
    fun contarPorEstado(): Flow<List<ConteoPorClave>>

    @Query("SELECT COUNT(*) FROM correos WHERE estado = 'EN_COLA_OFFLINE'")
    fun contarEnCola(): Flow<Int>
}
