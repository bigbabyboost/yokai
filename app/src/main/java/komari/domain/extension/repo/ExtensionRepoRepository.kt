package komari.domain.extension.repo

import kotlinx.coroutines.flow.Flow
import komari.domain.extension.repo.model.ExtensionRepo

interface ExtensionRepoRepository {
    fun subscribeAll(): Flow<List<ExtensionRepo>>
    suspend fun getAll(): List<ExtensionRepo>
    suspend fun getRepository(baseUrl: String): ExtensionRepo?
    suspend fun getRepositoryBySigningKeyFingerprint(fingerprint: String): ExtensionRepo?
    fun getCount(): Flow<Int>
    suspend fun insertRepository(
        baseUrl: String,
        name: String,
        shortName: String?,
        website: String,
        signingKeyFingerprint: String,
    )
    suspend fun upsertRepository(
        baseUrl: String,
        name: String,
        shortName: String?,
        website: String,
        signingKeyFingerprint: String,
    )
    suspend fun upsertRepository(repo: ExtensionRepo) {
        upsertRepository(
            baseUrl = repo.baseUrl,
            name = repo.name,
            shortName = repo.shortName,
            website = repo.website,
            signingKeyFingerprint = repo.signingKeyFingerprint,
        )
    }
    suspend fun replaceRepository(newRepo: ExtensionRepo)
    suspend fun deleteRepository(baseUrl: String)
}
