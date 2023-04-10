/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * General cache condition used with all cache configuration classes.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class CacheCondition extends SpringBootCondition {
	
	private static final String CACHE_TYPE_PROPERTY = "spring.cache.type";
    private static final String CACHE_CONDITION = "Cache";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String sourceClass = "";
		if (metadata instanceof ClassMetadata classMetadata) {
			sourceClass = classMetadata.getClassName();
		}
		ConditionMessage.Builder message = ConditionMessage.forCondition(CACHE_CONDITION, sourceClass);
		Environment environment = context.getEnvironment();
		try {
			BindResult<CacheType> cacheTypeResult = Binder.get(environment).bind(CACHE_TYPE_PROPERTY, CacheType.class);
			if (!cacheTyperesult.isBound()) {
				return ConditionOutcome.match(message.because("automatic cache type"));
			}
			CacheType required = CacheConfigurations.getType(((AnnotationMetadata) metadata).getClassName());
			if (cacheTyperesult.get() == required) {
				return ConditionOutcome.match(message.because(cacheTypeResult.get() + " cache type"));
			}
		}
		catch (BindException ex) {
		}
		return ConditionOutcome.noMatch(message.because("unknown cache type"));
	}

}
