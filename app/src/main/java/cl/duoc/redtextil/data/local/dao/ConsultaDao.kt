package cl.duoc.redtextil.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.redtextil.data.local.entity.ConsultaConNegocio
import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultaDao {

    @Insert
    suspend fun insertar(consulta: ConsultaEntity): Long

    @Insert
    suspend fun insertarLote(lista: List<ConsultaEntity>)

    @Update
    suspend fun actualizar(consulta: ConsultaEntity)

    @Query("SELECT * FROM consultas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ConsultaEntity?

    @Query("SELECT * FROM consultas WHERE emprendimientoId = :emprendimientoId ORDER BY fechaEnvio DESC")
    fun observarDeEmprendimiento(emprendimientoId: Long): Flow<List<ConsultaEntity>>

    @Query("SELECT COUNT(*) FROM consultas WHERE emprendimientoId = :emprendimientoId AND estado = 'PENDIENTE'")
    fun contarPendientesDe(emprendimientoId: Long): Flow<Int>

    @Query(
        """
        SELECT c.*, e.nombreComercial AS nombreComercial
        FROM consultas c INNER JOIN emprendimientos e ON e.id = c.emprendimientoId
        WHERE c.enviadaDesdeApp = 1
        ORDER BY c.fechaEnvio DESC
        """
    )
    fun observarEnviadasDesdeApp(): Flow<List<ConsultaConNegocio>>

    @Query("SELECT * FROM consultas WHERE estadoSync = 'PENDIENTE_SINCRONIZAR'")
    suspend fun pendientesDeSincronizar(): List<ConsultaEntity>

    @Query("SELECT COUNT(*) FROM consultas WHERE estadoSync = 'PENDIENTE_SINCRONIZAR'")
    fun contarPendientes(): Flow<Int>

    @Query(
        """
        UPDATE consultas SET estadoSync = 'SINCRONIZADO', remoteId = :remoteId
        WHERE id = :id AND fechaActualizacion = :version
        """
    )
    suspend fun marcarSincronizada(id: Long, remoteId: Long?, version: Long)
}
