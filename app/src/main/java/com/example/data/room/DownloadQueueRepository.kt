package com.example.data.room

import kotlinx.coroutines.flow.Flow

class DownloadQueueRepository(private val dao: DownloadQueueDao) {
    val allItems: Flow<List<DownloadQueueEntity>> = dao.getAllItems()

    suspend fun insert(item: DownloadQueueEntity) = dao.insertItem(item)
    
    suspend fun insertAll(items: List<DownloadQueueEntity>) = dao.insertItems(items)

    suspend fun deleteById(id: String) = dao.deleteItemById(id)
    
    suspend fun clearAll() = dao.clearAll()
}
