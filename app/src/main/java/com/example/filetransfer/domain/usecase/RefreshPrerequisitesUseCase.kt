package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.repository.P2pRepository
import javax.inject.Inject

/** FT-06: baca ulang izin + lokasi dan emit ulang state prasyarat. */
class RefreshPrerequisitesUseCase @Inject constructor(
    private val repository: P2pRepository
) {
    operator fun invoke() = repository.refreshPrerequisites()
}
