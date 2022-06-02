package jp.co.soramitsu.feature_staking_impl.presentation.validators.details.model

import android.graphics.drawable.PictureDrawable

class ValidatorDetailsModel(
    val stake: ValidatorStakeModel,
    val address: String,
    val addressImage: PictureDrawable,
    val identity: IdentityModel?,
)

class CollatorDetailsModel(
    val stake: ValidatorStakeModel,
    val address: String,
    val addressImage: PictureDrawable,
    val identity: IdentityModel?,
    val statusText: String,
    val statusColor: Int,
)
