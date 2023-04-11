static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

		@Bean
		StandardJackson2ObjectMapperBuilderCustomizer standardJacksonObjectMapperBuilderCustomizer(
				JacksonProperties jacksonProperties, ObjectProvider<Module> modules) {
			return new StandardJackson2ObjectMapperBuilderCustomizer(jacksonProperties, modules.stream().toList());
		}

		static final class StandardJackson2ObjectMapperBuilderCustomizer
				implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

			private final JacksonProperties jacksonProperties;

			private final Collection<Module> modules;

			StandardJackson2ObjectMapperBuilderCustomizer(JacksonProperties jacksonProperties,
					Collection<Module> modules) {
				this.jacksonProperties = jacksonProperties;
				this.modules = modules;
			}

			@Override
			public int getOrder() {
				return 0;
			}

			@Override
			public void customize(Jackson2ObjectMapperBuilder builder) {
				if (this.jacksonProperties.getDefaultPropertyInclusion() != null) {
					builder.serializationInclusion(this.jacksonProperties.getDefaultPropertyInclusion());
				}
				if (this.jacksonProperties.getTimeZone() != null) {
					builder.timeZone(this.jacksonProperties.getTimeZone());
				}
				configureFeatures(builder, FEATURE_DEFAULTS);
				configureVisibility(builder, this.jacksonProperties.getVisibility());
				configureFeatures(builder, this.jacksonProperties.getDeserialization());
				configureFeatures(builder, this.jacksonProperties.getSerialization());
				configureFeatures(builder, this.jacksonProperties.getMapper());
				configureFeatures(builder, this.jacksonProperties.getParser());
				configureFeatures(builder, this.jacksonProperties.getGenerator());
				configureDateFormat(builder);
				configurePropertyNamingStrategy(builder);
				configureModules(builder);
				configureLocale(builder);
				configureDefaultLeniency(builder);
				configureConstructorDetector(builder);
			}

			private void configureFeatures(Jackson2ObjectMapperBuilder builder, Map<?, Boolean> features) {
				features.forEach((feature, value) -> {
					if (value != null) {
						if (value) {
							builder.featuresToEnable(feature);
						}
						else {
							builder.featuresToDisable(feature);
						}
					}
				});
			}

			private void configureVisibility(Jackson2ObjectMapperBuilder builder,
					Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
				visibilities.forEach(builder::visibility);
			}

			private void configureDateFormat(Jackson2ObjectMapperBuilder builder) {
				// We support a fully qualified class name extending DateFormat or a date
				// pattern string value
				String dateFormat = this.jacksonProperties.getDateFormat();
				if (dateFormat != null) {
					try {
						Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
						builder.dateFormat((DateFormat) BeanUtils.instantiateClass(dateFormatClass));
					}
					catch (ClassNotFoundException ex) {
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
						// Since Jackson 2.6.3 we always need to set a TimeZone (see
						// gh-4170). If none in our properties fallback to the Jackson's
						// default
						TimeZone timeZone = this.jacksonProperties.getTimeZone();
						if (timeZone == null) {
							timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
						}
						simpleDateFormat.setTimeZone(timeZone);
						builder.dateFormat(simpleDateFormat);
					}
				}
			}

			private void configurePropertyNamingStrategy(Jackson2ObjectMapperBuilder builder) {
				// We support a fully qualified class name extending Jackson's
				// PropertyNamingStrategy or a string value corresponding to the constant
				// names in PropertyNamingStrategy which hold default provided
				// implementations
				String strategy = this.jacksonProperties.getPropertyNamingStrategy();
				if (strategy != null) {
					try {
						configurePropertyNamingStrategyClass(builder, ClassUtils.forName(strategy, null));
					}
					catch (ClassNotFoundException ex) {
						configurePropertyNamingStrategyField(builder, strategy);
					}
				}
			}

			private void configurePropertyNamingStrategyClass(Jackson2ObjectMapperBuilder builder,
					Class<?> propertyNamingStrategyClass) {
				builder.propertyNamingStrategy(
						(PropertyNamingStrategy) BeanUtils.instantiateClass(propertyNamingStrategyClass));
			}

			private void configurePropertyNamingStrategyField(Jackson2ObjectMapperBuilder builder, String fieldName) {
				// Find the field (this way we automatically support new constants
				// that may be added by Jackson in the future)
				Field field = findPropertyNamingStrategyField(fieldName);
				Assert.notNull(field, () -> "Constant named '" + fieldName + "' not found");
				try {
					builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}

			private Field findPropertyNamingStrategyField(String fieldName) {
				return ReflectionUtils.findField(com.fasterxml.jackson.databind.PropertyNamingStrategies.class,
						fieldName, PropertyNamingStrategy.class);
			}

			private void configureModules(Jackson2ObjectMapperBuilder builder) {
				builder.modulesToInstall(this.modules.toArray(new Module[0]));
			}

			private void configureLocale(Jackson2ObjectMapperBuilder builder) {
				Locale locale = this.jacksonProperties.getLocale();
				if (locale != null) {
					builder.locale(locale);
				}
			}

			private void configureDefaultLeniency(Jackson2ObjectMapperBuilder builder) {
				Boolean defaultLeniency = this.jacksonProperties.getDefaultLeniency();
				if (defaultLeniency != null) {
					builder.postConfigurer((objectMapper) -> objectMapper.setDefaultLeniency(defaultLeniency));
				}
			}

			private void configureConstructorDetector(Jackson2ObjectMapperBuilder builder) {
				ConstructorDetectorStrategy strategy = this.jacksonProperties.getConstructorDetector();
				if (strategy != null) {
					builder.postConfigurer((objectMapper) -> {
						switch (strategy) {
							case USE_PROPERTIES_BASED ->
								objectMapper.setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
							case USE_DELEGATING ->
								objectMapper.setConstructorDetector(ConstructorDetector.USE_DELEGATING);
							case EXPLICIT_ONLY ->
								objectMapper.setConstructorDetector(ConstructorDetector.EXPLICIT_ONLY);
							default -> objectMapper.setConstructorDetector(ConstructorDetector.DEFAULT);
						}
					});
				}
			}

		}

	}
