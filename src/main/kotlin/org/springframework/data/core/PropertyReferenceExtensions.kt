/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress(
	"UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS",
	"NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST"
)

package org.springframework.data.core

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

/**
 * Extension function to compose a [TypedPropertyPath] with a [KProperty1].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> PropertyReference<T, P>.then(next: KProperty1<P, N?>): TypedPropertyPath<T, N> {
	val nextPath = KPropertyReference.of<P, N>(next)
	return TypedPropertyPaths.compose(this, nextPath as PropertyReference<P, N>)
}

/**
 * Extension function to compose a [TypedPropertyPath] with a [KProperty].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> PropertyReference<T, P>.then(next: KProperty<N?>): TypedPropertyPath<T, N> {
	val nextPath = KPropertyReference.of<P, N>(next)
	return TypedPropertyPaths.compose(this, nextPath as PropertyReference<P, N>)
}

/**
 * Extension function to compose a [TypedPropertyPath] with a [KProperty1].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> TypedPropertyPath<T, P>.then(next: KProperty1<P, N?>): TypedPropertyPath<T, N> {
	val nextPath = KPropertyReference.of<P, N>(next)
	return TypedPropertyPaths.compose(this, nextPath as PropertyReference<P, N>)
}

/**
 * Extension function to compose a [TypedPropertyPath] with a [KProperty].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> TypedPropertyPath<T, P>.then(next: KProperty<N?>): TypedPropertyPath<T, N> {
	val nextPath = KPropertyReference.of<P, N>(next)
	return TypedPropertyPaths.compose(this, nextPath as PropertyReference<P, N>)
}

/**
 * Extension function to compose a [TypedPropertyPath] with a collection [KProperty1].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> PropertyReference<T, P>.thenMany(next: KProperty1<P, Iterable<N?>?>): TypedPropertyPath<T, N> {
	val nextPath = KPropertyReference.ofMany<P, N>(next)
	return TypedPropertyPaths.compose(this, nextPath as PropertyReference<P, N>)
}

/**
 * Extension function to compose a [TypedPropertyPath] with a collection [KProperty1].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> TypedPropertyPath<T, P>.thenMany(next: KProperty1<P, Iterable<N?>?>): TypedPropertyPath<T, N> {
	val nextPath = KPropertyReference.ofMany<P, N>(next)
	return TypedPropertyPaths.compose(this, nextPath as PropertyReference<P, N>)
}

/**
 * Create a [PropertyReference] from a [KProperty1] reference.
 *
 * @since 4.1
 */
fun <T : Any, P : Any> PropertyReference.Companion.property(property: KProperty1<T, P?>): PropertyReference<T, P> {
	return KPropertyReference.of(property)
}

/**
 * Create a [PropertyReference] from a collection [KProperty1] reference.
 *
 * @since 4.1
 */
@JvmName("propertyMany")
fun <T : Any, P : Any> PropertyReference.Companion.property(property: KProperty1<T, Iterable<P?>?>): PropertyReference<T, P> {
	return KPropertyReference.ofMany(property)
}

/**
 * Create a [TypedPropertyPath] from a [KProperty1] reference.
 *
 * @since 4.1
 */
fun <T : Any, P : Any> TypedPropertyPath.Companion.path(property: KProperty1<T, P?>): TypedPropertyPath<T, P> {
	return TypedPropertyPaths.of(KPropertyReference.of(property))
}

/**
 * Create a [TypedPropertyPath] from a collection [KProperty1] reference.
 *
 * @since 4.1
 */
@JvmName("pathMany")
fun <T : Any, P : Any> TypedPropertyPath.Companion.path(property: KProperty1<T, Iterable<P?>?>): TypedPropertyPath<T, P> {
	return TypedPropertyPaths.of(KPropertyReference.ofMany(property))
}

/**
 * Helper to create [PropertyReference] from [KProperty].
 *
 * @since 4.1
 */
class KPropertyReference {

	/**
	 * Companion object for static factory methods.
	 */
	companion object {

		/**
		 * Create a [PropertyReference] from a [KProperty1] reference.
		 * @param property the property reference, must not be a property path.
		 */
		fun <T : Any, P : Any> of(property: KProperty1<T, P?>): PropertyReference<T, out P> {
			return of((property as KProperty<P?>))
		}

		/**
		 * Create a [PropertyReference] from a collection-like [KProperty1] reference.
		 * @param property the property reference, must not be a property path.
		 */
		@JvmName("ofMany")
		fun <T : Any, P : Any> of(property: KProperty1<T, Iterable<P?>?>): PropertyReference<T, out P> {
			return of((property as KProperty<P?>))
		}

		/**
		 * Create a [PropertyReference] from a [KProperty].
		 */
		fun <T : Any, P> of(property: KProperty<P?>): PropertyReference<T, out P> {

			if (property is KPropertyPath<*, *>) {
				throw PropertyResolutionException("Property reference '${property.toDotPath()}' must be a single property reference, not a property path")
			}

			if (property is KProperty1<*, *>) {

				val property1 = property as KProperty1<*, *>
				val owner = (property1.javaField?.declaringClass
					?: property1.javaGetter?.declaringClass) as Class<Any>
				val metadata = PropertyReferences.KPropertyMetadata.of(
					MemberDescriptor.KPropertyReferenceDescriptor.create(
						owner,
						property1
					)
				)
				return PropertyReferences.ResolvedKPropertyReference(metadata)
			}

			throw PropertyResolutionException("Property '${property.name}' is not a KProperty")
		}

	}

}
