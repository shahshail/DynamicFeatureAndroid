package com.garmin.apps.dynamicdeliveryfeature

import android.os.Bundle
import com.garmin.apps.dynamicdeliveryfeature.base.BaseSplitActivity
import com.garmin.apps.dynamicdeliveryfeature.image_feature.R

class ImageActivity : BaseSplitActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
    }
}
