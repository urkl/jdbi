/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.vavr;

import java.util.List;

import io.vavr.Lazy;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestVavrValueArgumentFactoryWithDB {

    private static final String SELECT_BY_NAME = "select * from something "
            + "where :name is null or name = :name "
            + "order by id";

    private static final Something ERICSOMETHING = new Something(1, "eric");
    private static final Something BRIANSOMETHING = new Something(2, "brian");

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).installPlugins();

    @BeforeEach
    public void createTestData() {
        Handle handle = h2Extension.openHandle();
        handle.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        handle.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
    }

    @Test
    public void testGetOptionShouldReturnCorrectRow() {
        Something result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Option.of("eric"))
            .mapToBean(Something.class)
            .one();

        assertThat(result).isEqualTo(ERICSOMETHING);
    }

    @Test
    public void testGetOptionEmptyShouldReturnAllRows() {
        List<Something> result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Option.none())
            .mapToBean(Something.class)
            .list();

        assertThat(result).hasSize(2);
    }

    @Test
    public void testGetLazyShouldReturnCorrectRow() {
        Something result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Lazy.of(() -> "brian"))
            .mapToBean(Something.class)
            .one();

        assertThat(result).isEqualTo(BRIANSOMETHING);
    }

    @Test
    public void testGetTrySuccessShouldReturnCorrectRow() {
        Something result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Try.success("brian"))
            .mapToBean(Something.class)
            .one();

        assertThat(result).isEqualTo(BRIANSOMETHING);
    }

    @Test
    public void testGetTryFailureShouldReturnAllRows() {
        List<Something> result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Try.failure(new Throwable()))
            .mapToBean(Something.class)
            .list();

        assertThat(result).hasSize(2);
    }

    @Test
    public void testGetEitherRightShouldReturnCorrectRow() {
        Something result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Either.right("brian"))
            .mapToBean(Something.class)
            .one();

        assertThat(result).isEqualTo(BRIANSOMETHING);
    }

    @Test
    public void testGetEitherLeftShouldReturnAllRows() {
        List<Something> result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Either.left("eric"))
            .mapToBean(Something.class)
            .list();

        assertThat(result).hasSize(2);
    }

    @Test
    public void testGetValidationValidShouldReturnCorrectRow() {
        Something result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Validation.valid("brian"))
            .mapToBean(Something.class)
            .one();

        assertThat(result).isEqualTo(BRIANSOMETHING);
    }

    @Test
    public void testGetValidationInvalidShouldReturnAllRows() {
        List<Something> result = h2Extension.getSharedHandle().createQuery(SELECT_BY_NAME)
            .bind("name", Validation.invalid("eric"))
            .mapToBean(Something.class)
            .list();

        assertThat(result).hasSize(2);
    }

}
