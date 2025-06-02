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

/**
 * Serializer/deserializer for [Any] class.
 *
 * Arrays are deserialized as [List].
 *
 * @param javaClassKey The name of the JSON object property that holds the name of the Java class that was used during
 * serialization, or must be used during deserialization.
 * @param isClassNameSerialized If `true` then [javaClassKey] property is encoded for object values during serialisation.
 */
class AnySerializer(
    val javaClassKey: String = "__javaClass",
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
        val serializer = getSerializer(encoder.serializersModule, value::class) ?: inferSerializer(value)

        if (serializer == null) {
            encoder.encodeNull()
            return
        }

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

        require(!element.containsKey(javaClassKey)) { "Class ${value::class.java.name} redefines internal property \"$javaClassKey\"" }

        val properties = element.toMutableMap()

        properties[javaClassKey] = JsonPrimitive(value::class.java.name)

        encoder.encodeJsonElement(JsonObject(properties))
    }

    @ExperimentalSerializationApi
    @InternalSerializationApi
    override fun deserialize(decoder: Decoder): Any {
        check(decoder is JsonDecoder) { "Unsupported decoder" }

        val element = decoder.decodeJsonElement()

        if (element is JsonPrimitive) {
            return element.booleanOrNull
                ?: element.doubleOrNull
                ?: element.contentOrNull
                ?: throw IllegalArgumentException("AnySerializer cannot deserialize null values")
        }

        if (element is JsonArray) {
            return decoder.json.decodeFromJsonElement(listSerializer, element)
        }

        val className = element.jsonObject[javaClassKey]?.jsonPrimitive?.contentOrNull
            ?: return decoder.json.decodeFromJsonElement(mapSerializer, element)

        val serializer =
            checkNotNull(
                getSerializer(
                    decoder.serializersModule,
                    loadClass(className)
                )
            ) { "Cannot deserialize $className" }

        val jsonObject = JsonObject(element.jsonObject - this.javaClassKey)

        return checkNotNull(decoder.json.decodeFromJsonElement(serializer, jsonObject)) { "Unexpected null" }
    }

    @ExperimentalSerializationApi
    @InternalSerializationApi
    private fun getSerializer(serializersModule: SerializersModule, kClass: KClass<*>): KSerializer<*>? =
        serializersModule.getContextual(kClass) ?: kClass.serializerOrNull()

    /**
     * Returns a serializer that best fits the class.
     */
    @ExperimentalSerializationApi
    private fun inferSerializer(value: Any): KSerializer<*>? = when (value) {
        is Pair<*, *> -> pairSerializer
        is Map.Entry<*, *> -> mapEntrySerializer
        is Triple<*, *, *> -> tripleSerializer
        is CharArray -> CharArraySerializer()
        is ByteArray -> ByteArraySerializer()
        is ShortArray -> ShortArraySerializer()
        is IntArray -> IntArraySerializer()
        is LongArray -> LongArraySerializer()
        is FloatArray -> FloatArraySerializer()
        is DoubleArray -> DoubleArraySerializer()
        is BooleanArray -> BooleanArraySerializer()
        is Array<*> -> arraySerializer
        is List<*> -> listSerializer
        is Set<*> -> setSerializer
        is Map<*, *> -> mapSerializer
        else -> null
    }
}

private object MapKeySerializer : KSerializer<Any> {
    override val descriptor =
        PrimitiveSerialDescriptor("org.racehorse.serializers.MapKeySerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Any) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Any = decoder.decodeString()
}
