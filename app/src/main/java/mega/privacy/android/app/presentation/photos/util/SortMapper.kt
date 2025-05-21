package mega.privacy.android.app.presentation.photos.util

import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.SortOrder

fun mapSortOrderToSort(sortOrder: SortOrder): Sort = when (sortOrder) {
    SortOrder.ORDER_MODIFICATION_DESC -> Sort.NEWEST
    SortOrder.ORDER_MODIFICATION_ASC -> Sort.OLDEST
    else -> Sort.NEWEST
}

fun mapSortToSortOrder(sort: Sort): SortOrder = when (sort) {
    Sort.NEWEST -> SortOrder.ORDER_MODIFICATION_DESC
    Sort.OLDEST -> SortOrder.ORDER_MODIFICATION_ASC
    else -> SortOrder.ORDER_MODIFICATION_DESC
}