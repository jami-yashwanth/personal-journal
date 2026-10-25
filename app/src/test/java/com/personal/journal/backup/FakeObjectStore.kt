package com.personal.journal.backup

/** In-memory ObjectStore. Set [failPut] to make specific keys fail once. */
class FakeObjectStore : ObjectStore {
    val objects = linkedMapOf<String, ByteArray>()
    val failPut = mutableSetOf<String>()
    val gets = mutableListOf<String>()

    override suspend fun put(key: String, bytes: ByteArray) {
        if (failPut.remove(key)) throw BackupException(BackupErrorKind.NETWORK)
        objects[key] = bytes
    }

    override suspend fun get(key: String): ByteArray? {
        gets += key
        return objects[key]
    }

    override suspend fun delete(key: String) {
        objects.remove(key)
    }

    override suspend fun list(prefix: String): List<String> = objects.keys.filter { it.startsWith(prefix) }
}
