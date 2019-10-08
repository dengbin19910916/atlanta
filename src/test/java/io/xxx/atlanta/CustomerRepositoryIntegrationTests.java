/*
 * Copyright 2018 the original author or authors.
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
package io.xxx.atlanta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Oliver Gierke
 */
@SpringBootTest(classes = InfrastructureConfiguration.class)
public class CustomerRepositoryIntegrationTests {

	@Autowired CustomerRepository customers;
	@Autowired DatabaseClient database;

	@BeforeEach
	public void setUp() {

		Hooks.onOperatorDebug();

		List<String> statements = Arrays.asList(//
				"DROP TABLE IF EXISTS customer;",
				"CREATE TABLE customer ( id SERIAL PRIMARY KEY, firstname VARCHAR(100) NOT NULL, lastname VARCHAR(100) NOT NULL);");

		statements.forEach(it -> database.execute(it) //
				.fetch() //
				.rowsUpdated() //
				.as(StepVerifier::create) //
//				.expectNextCount(1) //
				.verifyComplete());
	}

	@Test
	public void executesFindAll() throws IOException {

		Customer dave = new Customer(null, "Dave", "Matthews");
		Customer carter = new Customer(null, "Carter", "Beauford");

		insertCustomers(dave, carter);

		customers.findAll().subscribe(System.out::println);

		customers.findAll() //
				.as(StepVerifier::create) //
				.assertNext(dave::equals) //
				.assertNext(carter::equals) //
				.verifyComplete();
	}

	@Test
	public void executesAnnotatedQuery() throws IOException {

		Customer dave = new Customer(null, "Dave", "Matthews");
		Customer carter = new Customer(null, "Carter", "Beauford");

		insertCustomers(dave, carter);

		customers.findByLastname("Matthews")
				.zipWith(Flux.range(1, 3))
				.subscribe(tuple2 -> System.out.println(tuple2.getT1().firstname + " : " + tuple2.getT2()));

		customers.findByLastname("Matthews") //
				.as(StepVerifier::create) //
				.assertNext(dave::equals) //
				.verifyComplete();
	}

	private void insertCustomers(Customer... customers) {

		this.customers.saveAll(Arrays.asList(customers))//
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}
}