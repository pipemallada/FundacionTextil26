package cl.duoc.redtextil.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.redtextil.data.local.entity.PublicacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicacionDao {

    @Insert
    suspend fun insertar(publicacion: PublicacionEntity): Long

    @Insert
    suspend fun insertarLote(lista: List<PublicacionEntity>)

    @Update
    suspend fun actualizar(publicacion: PublicacionEntity)

    @Delete
    suspend fun eliminarDefinitivo(publicacion: PublicacionEntity)

    @Query("SELECT * FROM publicaciones WHERE id = :id")
    suspend fun obtenerPorId(id: Long): PublicacionEntity?

    @Query("SELECT * FROM publicaciones WHERE emprendimientoId = :emprendimientoId AND eliminada = 0 ORDER BY fechaCreacion DESC")
    fun observarDeEmprendimiento(emprendimientoId: Long): Flow<List<PublicacionEntity>>

    @Query("SELECT COUNT(*) FROM publicaciones WHERE emprendimientoId = :emprendimientoId AND eliminada = 0")
    fun contarDeEmprendimiento(emprendimientoId: Long): Flow<Int>

    @Query(
        """
        SELECT emprendimientoId, fotoRuta FROM publicaciones
        WHERE id IN (
            SELECT MAX(id) FROM publicaciones
            WHERE eliminada = 0 AND fotoRuta IS NOT NULL
            GROUP BY emprendimientoId
        )
        """
    )
    fun observarPortadas(): Flow<List<PortadaCatalogo>>

    @Query("SELECT * FROM publicaciones WHERE estadoSync = 'PENDIENTE_SINCRONIZAR'")
    suspend fun pendientesDeSincronizar(): List<PublicacionEntity>

    @Query("SELECT COUNT(*) FROM publicaciones WHERE estadoSync = 'PENDIENTE_SINCRONIZAR'")
    fun contarPendientes(): Flow<Int>

    @Query(
        """
        UPDATE publicaciones SET estadoSync = 'SINCRONIZADO', remoteId = :remoteId, fotoSincronizada = 1
        WHERE id = :id AND fechaActualizacion = :version
        """
    )
    suspend fun marcarSincronizada(id: Long, remoteId: Long?, version: Long)
}
