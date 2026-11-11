package io.correcta.cohort.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/** Enables JPA auditing for `@CreatedDate` and `@LastModifiedDate` fields. */
@Configuration
@EnableJpaAuditing
class JpaConfig