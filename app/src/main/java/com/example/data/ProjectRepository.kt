package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val dao: ProjectDao) {
    val projects: Flow<List<ProjectEntity>> = dao.observeAllProjects()

    suspend fun saveProject(project: ProjectEntity) {
        dao.upsertProject(project)
    }

    suspend fun loadProject(id: String): ProjectEntity? = dao.getProjectById(id)

    suspend fun deleteProject(id: String) {
        dao.deleteProjectById(id)
    }

    suspend fun clearVault() {
        dao.clearAll()
    }
}
