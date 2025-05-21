package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.util.mapSortToSortOrder
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.photos.MonitorMediaDiscoveryNodesUseCase
import javax.inject.Inject

class MediaDiscoveryImageNodeFetcher @Inject constructor(
    private val monitorMediaDiscoveryNodesUseCase: MonitorMediaDiscoveryNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        val sort = bundle.getString(SORT)?.let {
            try {
                Sort.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Sort.NEWEST  // default fallback
            }
        } ?: Sort.NEWEST
        val sortOrder = mapSortToSortOrder(sort)
        return monitorMediaDiscoveryNodesUseCase(
            parentId = NodeId(bundle.getLong(PARENT_ID)),
            recursive = bundle.getBoolean(IS_RECURSIVE),
            sortOrder = sortOrder
        ).mapLatest { imageNodes ->
            if (sortOrder == SortOrder.ORDER_NONE) {
                imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
            } else {
                imageNodes
            }
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val PARENT_ID = "parentId"

        const val IS_RECURSIVE = "recursive"
        const val SORT = "sort_order"
    }
}