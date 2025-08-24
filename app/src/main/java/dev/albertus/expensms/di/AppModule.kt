package dev.albertus.expensms.di

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.albertus.expensms.data.UserPreferences
import dev.albertus.expensms.data.api.ApiClient
import dev.albertus.expensms.data.api.ApiLogger
import dev.albertus.expensms.data.api.ApiService
import dev.albertus.expensms.data.local.ApiLogDao
import dev.albertus.expensms.data.local.AppDatabase
import dev.albertus.expensms.data.local.SyncMetadataDao
import dev.albertus.expensms.data.local.TransactionDao
import dev.albertus.expensms.data.repository.TransactionRepository
import dev.albertus.expensms.data.serializer.UserPreferencesSerializer
import dev.albertus.expensms.utils.SecureStorage
import dev.albertus.expensms.utils.SmsForwardingService
import dev.albertus.expensms.utils.SmsSync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
        userPreferencesSerializer: UserPreferencesSerializer,
    ): DataStore<UserPreferences> =
        DataStoreFactory.create(
            serializer = userPreferencesSerializer,
            scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        ) {
            context.dataStoreFile("user_preferences.pb")
        }

    @Provides
    @IODispatcher
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun providesCoroutineScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao,
        syncMetadataDao: SyncMetadataDao
    ): TransactionRepository {
        return TransactionRepository(transactionDao, syncMetadataDao)
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }

    @Provides
    @Singleton
    fun provideApiLogDao(database: AppDatabase): ApiLogDao {
        return database.apiLogDao()
    }

    @Provides
    @Singleton
    fun provideSmsSync(
        contentResolver: ContentResolver,
        transactionRepository: TransactionRepository
    ): SmsSync {
        return SmsSync(contentResolver, transactionRepository)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideApiLogger(
        apiLogDao: ApiLogDao,
        @ApplicationScope coroutineScope: CoroutineScope
    ): ApiLogger {
        return ApiLogger(apiLogDao, coroutineScope)
    }

    @Provides
    @Singleton
    fun provideApiClient(apiLogger: ApiLogger): ApiClient {
        return ApiClient(apiLogger)
    }

    @Provides
    @Singleton
    fun provideApiService(
        apiClient: ApiClient,
        secureStorage: SecureStorage,
        userPreferencesDataStore: DataStore<UserPreferences>,
        apiLogger: ApiLogger
    ): ApiService {
        return ApiService(apiClient, secureStorage, userPreferencesDataStore, apiLogger)
    }

    @Provides
    @Singleton
    fun provideSmsForwardingService(
        apiService: ApiService,
        @ApplicationScope coroutineScope: CoroutineScope
    ): SmsForwardingService {
        return SmsForwardingService(apiService, coroutineScope)
    }

    @Provides
    @Singleton
    fun provideWorkerFactory(
        transactionRepository: TransactionRepository,
        smsForwardingService: SmsForwardingService
    ): ExpenSMSWorkerFactory {
        return ExpenSMSWorkerFactory(transactionRepository, smsForwardingService)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IODispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope