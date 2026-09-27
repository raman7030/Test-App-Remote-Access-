package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.AdminRole
import com.example.model.AuditSeverity
import com.example.model.CommandStatus
import com.example.model.CommandType
import com.example.model.ComplianceState
import com.example.model.DeviceOwnerStatus

class Converters {
    @TypeConverter
    fun fromDeviceOwnerStatus(value: DeviceOwnerStatus): String = value.name

    @TypeConverter
    fun toDeviceOwnerStatus(value: String): DeviceOwnerStatus = runCatching { DeviceOwnerStatus.valueOf(value) }.getOrDefault(DeviceOwnerStatus.NOT_ADMIN)

    @TypeConverter
    fun fromComplianceState(value: ComplianceState): String = value.name

    @TypeConverter
    fun toComplianceState(value: String): ComplianceState = runCatching { ComplianceState.valueOf(value) }.getOrDefault(ComplianceState.COMPLIANT)

    @TypeConverter
    fun fromAdminRole(value: AdminRole): String = value.name

    @TypeConverter
    fun toAdminRole(value: String): AdminRole = runCatching { AdminRole.valueOf(value) }.getOrDefault(AdminRole.DEVICE_ADMIN)

    @TypeConverter
    fun fromCommandStatus(value: CommandStatus): String = value.name

    @TypeConverter
    fun toCommandStatus(value: String): CommandStatus = runCatching { CommandStatus.valueOf(value) }.getOrDefault(CommandStatus.PENDING)

    @TypeConverter
    fun fromCommandType(value: CommandType): String = value.name

    @TypeConverter
    fun toCommandType(value: String): CommandType = runCatching { CommandType.valueOf(value) }.getOrDefault(CommandType.APPLY_POLICY)

    @TypeConverter
    fun fromAuditSeverity(value: AuditSeverity): String = value.name

    @TypeConverter
    fun toAuditSeverity(value: String): AuditSeverity = runCatching { AuditSeverity.valueOf(value) }.getOrDefault(AuditSeverity.INFO)
}

@Database(
    entities = [
        DeviceEntity::class,
        PolicyEntity::class,
        CommandEntity::class,
        AuditLogEntity::class,
        AdminUserEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun policyDao(): PolicyDao
    abstract fun commandDao(): CommandDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun adminUserDao(): AdminUserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "droidcommand_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
