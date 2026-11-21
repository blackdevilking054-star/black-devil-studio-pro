package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WorkspaceType(val label: String) {
    VIDEO("Video Editor"),
    PHOTO("Photo & Thumbnail"),
    AUDIO("Audio Studio")
}

@Entity(tableName = "studio_projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val workspaceType: String,
    val resolutionLabel: String,
    val summary: String,
    val updatedAt: Long,
    val stateJson: String
)
