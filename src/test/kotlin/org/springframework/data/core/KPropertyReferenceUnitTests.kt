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
package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KPropertyReference] and related functionality.
 *
 * @author Mark Paluch
 */
class KPropertyReferenceUnitTests {

	@Test // GH-3400
	fun shouldCreatePropertyReference() {

		val path = KPropertyReference.of(Person::name)

		assertThat(path.name).isEqualTo("name")
	}

	@Test // GH-3400
	fun shouldComposePropertyPath() {

		val path = KPropertyReference.of(Person::address).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("address.city")
	}

	@Test // GH-3400
	fun shouldComposeManyPropertyPath() {

		val path = KPropertyReference.of(Person::addresses).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("addresses.city")
	}

	@Test // GH-3400
	fun composedReferenceCreationShouldFail() {
		assertThatExceptionOfType(PropertyResolutionException::class.java).isThrownBy {
			PropertyReference.property(
				Person::address / Address::city
			)
		}
		assertThatExceptionOfType(PropertyResolutionException::class.java).isThrownBy {
			KPropertyReference.of(
				Person::address / Address::city
			)
		}
	}

	@Test
	fun shouldComposePropertyPathViaTypedPropertyPathThen() {
		val path = TypedPropertyPath.path(Person::address).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("address.city")
		assertThat(path.owningType.type).isEqualTo(Person::class.java)
	}

	@Test
	fun shouldComposeMultiLevelPropertyPathViaTypedPropertyPathThen() {
		val path = TypedPropertyPath.path(Person::address)
			.then(Address::country)
			.then(Country::name)

		assertThat(path.toDotPath()).isEqualTo("address.country.name")
		assertThat(path.owningType.type).isEqualTo(Person::class.java)
	}

	@Test
	fun shouldComposePropertyPathViaKPropertyReferenceThen() {
		val addressRef = KPropertyReference.of(Person::address)
		val path = addressRef.then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("address.city")
		assertThat(path.owningType.type).isEqualTo(Person::class.java)
	}

	@Test
	fun shouldComposeMultiLevelPropertyPathViaKPropertyReferenceThen() {
		val path = KPropertyReference.of(Person::address)
			.then(Address::country)
			.then(Country::name)

		assertThat(path.toDotPath()).isEqualTo("address.country.name")
		assertThat(path.owningType.type).isEqualTo(Person::class.java)
	}

	@Test
	fun shouldComposeCollectionPropertyPathViaKPropertyReferenceThen() {
		val path = KPropertyReference.of(Person::addresses)
			.then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("addresses.city")
		assertThat(path.owningType.type).isEqualTo(Person::class.java)
	}

	@Test
	fun shouldComposeMultiLevelCollectionPropertyPathViaKPropertyReferenceThen() {
		val path = KPropertyReference.of(Person::addresses)
			.then(Address::country)
			.then(Country::name)

		assertThat(path.toDotPath()).isEqualTo("addresses.country.name")
		assertThat(path.owningType.type).isEqualTo(Person::class.java)
	}

	@Test
	fun shouldComposeSelfReferencingPropertyPath() {
		val path = KPropertyReference.of(Person::emergencyContact)
			.then(Person::address)
			.then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("emergencyContact.address.city")
		assertThat(path.owningType.type).isEqualTo(Person::class.java)
	}

	class Person {
		var name: String? = null
		var age: Int = 0
		var address: Address? = null
		var addresses: List<Address> = emptyList()
		var emergencyContact: Person? = null
	}

	class Address {
		var city: String? = null
		var street: String? = null
		var country: Country? = null
	}

	data class Country(val name: String)
}
