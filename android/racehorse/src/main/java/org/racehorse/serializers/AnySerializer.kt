package org.racehorse.serializers

import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.BooleanArraySerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.CharArraySerializer
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.ShortArraySerializer
import kotlinx.serialization.builtins.TripleSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializerOrNull
import org.racehorse.utils.loadClass
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Serializer/deserializer for [Any] class.
 *
 * Arrays are deserialized as [List].
 *
 * @param className The name of the JSON object property that holds the name of the Java class that was used during
 * serialization, or must be used during deserialization.
 * @param isClassNameSerialized If `true` then [className] property is encoded for object values during serialisation.
 */
class AnySerializer(
    val className: String = "__javaClass",
    val isClassNameSerialized: Boolean = false
) : KSerializer<Any> {

    @ExperimentalSerializationApi
    override val descriptor = ContextualSerializer(Any::class, null, emptyArray()).descriptor

    @ExperimentalSerializationApi
    private val arraySerializer = ArraySerializer(this)
    private val listSerializer = ListSerializer(this)
    private val setSerializer = SetSerializer(this)
    private val mapSerializer = MapSerializer(MapKeySerializer, this)
    private val pairSerializer = PairSerializer(this, this)
    private val mapEntrySerializer = MapEntrySerializer(this, this)
    private val tripleSerializer = TripleSerializer(this, this, this)

    @ExperimentalSerializationApi
    @InternalSerializationApi
    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Any) {
        val serializer = value::class.getSerializer(encoder.serializersModule) ?: value::class.guessSerializer()

        if (!isClassNameSerialized) {
            encoder.encodeSerializableValue(serializer as KSerializer<Any>, value)
            return
        }

        check(encoder is JsonEncoder) { "Unsupported encoder" }

        val element = encoder.json.encodeToJsonElement(serializer as KSerializer<Any>, value)

        if (element !is JsonObject) {
            encoder.encodeJsonElement(element)
            return
        }

        require(!element.containsKey(className)) { "Class ${value::class.java.name} redefines internal property \"$className\"" }

        val properties = element.toMutableMap()

        properties[className] = JsonPrimitive(value::class.java.name)

        encoder.encodeJsonElement(JsonObject(properties))
    }

    @ExperimentalSerializationApi
    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): Any {
        check(decoder is JsonDecoder) { "Unsupported decoder" }

        val element = decoder.decodeJsonElement()

        if (element is JsonPrimitive) {
            return element.booleanOrNull ?: element.doubleOrNull ?: element.content
        }

        if (element is JsonArray) {
            return decoder.json.decodeFromJsonElement(listSerializer, element)
        }

        val className = element.jsonObject[className]?.jsonPrimitive?.contentOrNull
            ?: return decoder.json.decodeFromJsonElement(mapSerializer, element)

        val serializer =
            checkNotNull(loadClass(className).getSerializer(decoder.serializersModule)) { "Cannot deserialize $className" }

        val jsonObject = JsonObject(element.jsonObject - this.className)

        return checkNotNull(decoder.json.decodeFromJsonElement(serializer, jsonObject)) { "Unexpected null" }
    }

    @ExperimentalSerializationApi
    @InternalSerializationApi
    private fun KClass<*>.getSerializer(serializersModule: SerializersModule): KSerializer<*>? =
        serializersModule.getContextual(this) ?: serializerOrNull()

    /**
     * Returns a serializer that best fits the class.
     */
    @ExperimentalSerializationApi
    private fun KClass<*>.guessSerializer(): KSerializer<*> = when {
        isSubclassOf(Pair::class) -> pairSerializer
        isSubclassOf(Map.Entry::class) -> mapEntrySerializer
        isSubclassOf(Triple::class) -> tripleSerializer
        isSubclassOf(CharArray::class) -> CharArraySerializer()
        isSubclassOf(ByteArray::class) -> ByteArraySerializer()
        isSubclassOf(ShortArray::class) -> ShortArraySerializer()
        isSubclassOf(IntArray::class) -> IntArraySerializer()
        isSubclassOf(LongArray::class) -> LongArraySerializer()
        isSubclassOf(FloatArray::class) -> FloatArraySerializer()
        isSubclassOf(DoubleArray::class) -> DoubleArraySerializer()
        isSubclassOf(BooleanArray::class) -> BooleanArraySerializer()
        isSubclassOf(Array::class) -> arraySerializer
        isSubclassOf(List::class) -> listSerializer
        isSubclassOf(Set::class) -> setSerializer
        isSubclassOf(Map::class) -> mapSerializer

        else -> error("Cannot serialize ${java.name}")
    }
}

private object MapKeySerializer : KSerializer<Any> {
    override val descriptor =
        PrimitiveSerialDescriptor("org.racehorse.serializers.MapKeySerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Any) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Any = decoder.decodeString()
}
