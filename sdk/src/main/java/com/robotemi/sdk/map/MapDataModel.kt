package com.robotemi.sdk.map

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntRange
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlin.math.round

data class MapDataModel(
    @SerializedName("Map_Image")
    var mapImage: MapImage,
    var mapId: String = "",
    var mapInfo: MapInfo = MapInfo(),
    var virtualWalls: MutableList<Layer> = mutableListOf(),
    var greenPaths: MutableList<Layer> = mutableListOf(),
    var locations: MutableList<Layer> = mutableListOf(),
    var mapName: String = "",
    var mapEraser: MutableList<Layer> = mutableListOf(), // This is added in 133 version.
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(MapImage::class.java.classLoader)!!,
        parcel.readString() ?: "",
        parcel.readParcelable(MapInfo::class.java.classLoader)!!,
        parcel.createTypedArrayList(Layer) ?: mutableListOf(),
        parcel.createTypedArrayList(Layer) ?: mutableListOf(),
        parcel.createTypedArrayList(Layer) ?: mutableListOf(),
        parcel.readString() ?: "",
        parcel.createTypedArrayList(Layer) ?: mutableListOf(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(mapImage, flags)
        parcel.writeString(mapId)
        parcel.writeParcelable(mapInfo, flags)
        parcel.writeTypedList(virtualWalls)
        parcel.writeTypedList(greenPaths)
        parcel.writeTypedList(locations)
        parcel.writeString(mapName)
        parcel.writeTypedList(mapEraser)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<MapDataModel> {
        override fun createFromParcel(parcel: Parcel): MapDataModel {
            return MapDataModel(parcel)
        }

        override fun newArray(size: Int): Array<MapDataModel?> {
            return arrayOfNulls(size)
        }
    }
}

data class MapImage(
    @SerializedName("type_id")
    val typeId: String,
    val rows: Int,
    val cols: Int,
    val dt: String,
    val data: List<Int>
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readString()!!,
        source.readInt(),
        source.readInt(),
        source.readString()!!,
        ArrayList<Int>().apply { source.readList(this, Int::class.java.classLoader) }
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(typeId)
        writeInt(rows)
        writeInt(cols)
        writeString(dt)
        writeList(data)
    }

    companion object CREATOR : Parcelable.Creator<MapImage> {
        override fun createFromParcel(parcel: Parcel): MapImage {
            return MapImage(parcel)
        }

        override fun newArray(size: Int): Array<MapImage?> {
            return arrayOfNulls(size)
        }
    }
}

data class MapInfo(
    @SerializedName("height") val height: Int = 0,
    @SerializedName("width") val width: Int = 0,
    @SerializedName("origin_x") val originX: Float = 0f,
    @SerializedName("origin_y") val originY: Float = 0f,
    @SerializedName("resolution") val resolution: Float = 0f
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(height)
        parcel.writeInt(width)
        parcel.writeFloat(originX)
        parcel.writeFloat(originY)
        parcel.writeFloat(resolution)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MapInfo> {
        override fun createFromParcel(parcel: Parcel): MapInfo {
            return MapInfo(parcel)
        }

        override fun newArray(size: Int): Array<MapInfo?> {
            return arrayOfNulls(size)
        }
    }
}

data class Layer internal constructor(
    @SerializedName("layer_creation_universal_time") val layerCreationUTC: Int,
    @SerializedName("layer_category") val layerCategory: Int,
    @SerializedName("layer_id") val layerId: String = "",
    @SerializedName("layer_thickness") val layerThickness: Float,
    @SerializedName("layer_status") val layerStatus: Int,
    @SerializedName("layer_poses") val layerPoses: List<LayerPose>?,
    @SerializedName("layer_direction") val layerDirection: Int = 0, // added in version 132 for one-way virtual wall, value can be -1, 0, 1.
    @SerializedName("layer_data") val layerData: String, // added in version 133 for map eraser layer
) : Parcelable {

    /**
     * FIXME: Using Parcelable to pass layerPoses is not working as expected, the data received is different from data sent.
     */
    internal constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readInt(),
        parcel.createTypedArrayList(LayerPose),
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun toString(): String {
        val category = when (layerCategory) {
            GREEN_PATH -> "greenPath"
            VIRTUAL_WALL -> "virtualWall"
            LOCATION -> "location"
            MAP_ERASER -> "mapEraser"
            else -> "unknown"
        }

        val status = when (layerStatus) {
            STATUS_UPDATE -> "UPDATE"
            STATUS_ADD_POSE -> "ADD_POSE"
            STATUS_DELETE -> "DELETE"
            STATUS_CURRENT -> "CURRENT"
            else -> "unknown"
        }
        return if (layerCategory == MAP_ERASER) {
            """
                
                { "layerCategory": $category, "layerId": $layerId, "layerCreationUTC": $layerCreationUTC, "layerStatus": $status, data-length: ${layerData?.length}" }
            """.trimIndent()
        } else {
            """
                
                { "layerCategory": $category, "layerId": $layerId, "layerCreationUTC": $layerCreationUTC, "layerStatus": $status, "layerThickness": $layerThickness, "layerPoses": "Size : ${layerPoses?.size ?: 0}" }
            """.trimIndent()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(layerCreationUTC)
        parcel.writeInt(layerCategory)
        parcel.writeString(layerId)
        parcel.writeFloat(layerThickness)
        parcel.writeInt(layerStatus)
        parcel.writeList(layerPoses)
        parcel.writeInt(layerDirection)
        parcel.writeString(layerData)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Layer> {
        override fun createFromParcel(parcel: Parcel): Layer {
            return Layer(parcel)
        }

        override fun newArray(size: Int): Array<Layer?> {
            return arrayOfNulls(size)
        }

        /**
         * @param layerId, Use existing layerId to update target layer.
         * Use null to create a new layer.
         * For location operation, there must be a valid layerId, which will be taken as location name
         *
         *
         * @param layerCategory, layer category, [GREEN_PATH], [VIRTUAL_WALL], [LOCATION]
         * @param tiltAngle, only used when saving location.
         *
         * @param layerPoses, the x, y in the pose will be rounded to 2 digits after decimal.
         *                  theta will be converted to 0 for [GREEN_PATH] and [VIRTUAL_WALL],
         *                  in [LOCATION] theta will be rounded to 4 digits after decimal.
         */
        fun upsertLayer(layerId: String?,
                        layerCategory: Int,
                        layerPoses: List<LayerPose>,
                        @IntRange(from = -25L, to = 55L) tiltAngle: Int? = null
        ): Layer? {
            val sessionId = (1000..9999).random().toString()
            var layerThickness = 1f
            val finalLayerId = when (layerCategory) {
                GREEN_PATH -> {
                    if (layerPoses.size <= 1) {
                        // path should have more than 1 pose
                        return null
                    }
                    layerId ?: "path_${System.currentTimeMillis()}_$sessionId"
                }
                VIRTUAL_WALL -> {
                    if (layerPoses.size <= 1) {
                        // virtual wall should have more than 1 pose
                        return null
                    }
                    layerId ?: "wall_${System.currentTimeMillis()}_$sessionId"
                }
                LOCATION -> {
                    layerId ?: return null
                    if (layerPoses.size != 1) {
                        // location should have only 1 pose
                        return null
                    }
                    layerThickness = tiltAngle?.toFloat() ?: 0f
                    // Location shall be lower-cased
                    layerId.lowercase()
                }
                else -> return null
            }
            return Layer(
                layerCreationUTC = (System.currentTimeMillis() / 1000).toInt(),
                layerCategory = layerCategory,
                layerId = finalLayerId,
                layerStatus = STATUS_UPDATE,
                layerThickness = layerThickness,
                layerPoses = layerPoses,
                layerData = ""
            )
        }

        internal fun Layer.roundByCategory(): Layer {
            val layerPosesRounded = when (layerCategory) {
                GREEN_PATH, VIRTUAL_WALL -> {
                    layerPoses?.map {
                        LayerPose(
                            it.x.keep2digits(),
                            it.y.keep2digits(),
                            0f
                        )
                    }
                }
                LOCATION -> {
                    layerPoses?.map {
                        LayerPose(
                            it.x.keep2digits(),
                            it.y.keep2digits(),
                            it.theta.keep4digits()
                        )
                    }
                }
                else -> layerPoses
            }
            return this.copy(layerPoses = layerPosesRounded)
        }

        private fun Float.keep2digits(): Float {
            return round(this * 100) / 100
        }

        private fun Float.keep4digits(): Float {
            return round(this * 10000) / 10000
        }
    }
}

const val GREEN_PATH = 0
const val VIRTUAL_WALL = 3
const val LOCATION = 4
const val MAP_ERASER = 6

const val STATUS_CURRENT = 0
const val STATUS_UPDATE = 1
const val STATUS_ADD_POSE = 2
const val STATUS_DELETE = 3

const val MAP_ID = "map_id"
const val MAP_INFO = "map_info"
const val MAP_ELEMENTS = "map_elements"
const val MAP_NAME = "map_name"
const val MAP_IMAGE = "map_image"
const val MAP_BASE64 = "map_base64"


@Keep
data class LayerPose(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("theta") val theta: Float
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(x)
        parcel.writeFloat(y)
        parcel.writeFloat(theta)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<LayerPose> {
        override fun createFromParcel(parcel: Parcel): LayerPose {
            return LayerPose(parcel)
        }

        override fun newArray(size: Int): Array<LayerPose?> {
            return arrayOfNulls(size)
        }
    }
}